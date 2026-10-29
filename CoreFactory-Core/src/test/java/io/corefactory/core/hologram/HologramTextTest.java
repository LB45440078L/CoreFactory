package io.corefactory.core.hologram;

import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.drill.DrillKind;
import io.corefactory.core.drill.DrillRules;
import io.corefactory.core.drill.DrillStatus;
import io.corefactory.core.drill.DrillText;
import io.corefactory.core.drill.DrillTable;
import io.corefactory.core.process.MachineState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class HologramTextTest {

    private static final String GREEN = "&a";
    private static final String DARK_GRAY = "&8";
    private static final String SEGMENT = "■";

    private static String bar(int filled, int empty) {
        return GREEN + SEGMENT.repeat(filled) + DARK_GRAY + SEGMENT.repeat(empty);
    }

    @Test
    void emptyBarHasNoFilledSegments() {
        assertThat(HologramText.progressBar(0.0, 20)).isEqualTo(bar(0, 20));
    }

    @Test
    void halfBarHasHalfFilled() {
        assertThat(HologramText.progressBar(0.5, 20)).isEqualTo(bar(10, 10));
    }

    @Test
    void fullBarHasNoEmptySegments() {
        assertThat(HologramText.progressBar(1.0, 20)).isEqualTo(bar(20, 0));
    }

    @Test
    void barRoundsToTheNearestSegment() {
        assertThat(HologramText.progressBar(0.33, 20)).isEqualTo(bar(7, 13));
        assertThat(HologramText.progressBar(0.32, 20)).isEqualTo(bar(6, 14));
        assertThat(HologramText.progressBar(0.025, 20)).isEqualTo(bar(1, 19));
        assertThat(HologramText.progressBar(0.024, 20)).isEqualTo(bar(0, 20));
    }

    @Test
    void barClampsFractionsOutsideZeroToOne() {
        assertThat(HologramText.progressBar(-0.5, 20)).isEqualTo(bar(0, 20));
        assertThat(HologramText.progressBar(1.7, 20)).isEqualTo(bar(20, 0));
        assertThat(HologramText.progressBar(Double.NaN, 20)).isEqualTo(bar(0, 20));
    }

    @Test
    void barHasTheWidthAsked() {
        assertThat(HologramText.progressBar(0.5, 4)).isEqualTo(bar(2, 2));
        assertThat(HologramText.progressBar(0.5, 0)).isEqualTo(bar(1, 0));
    }

    @Test
    void percentIsTruncatedAndClamped() {
        assertThat(HologramText.percent(0.0)).isEqualTo("0%");
        assertThat(HologramText.percent(0.5)).isEqualTo("50%");
        assertThat(HologramText.percent(0.29)).isEqualTo("29%");
        assertThat(HologramText.percent(0.999)).isEqualTo("99%");
        assertThat(HologramText.percent(1.0)).isEqualTo("100%");
        assertThat(HologramText.percent(2.0)).isEqualTo("100%");
        assertThat(HologramText.percent(-1.0)).isEqualTo("0%");
    }

    @Test
    void fractionIsProgressOverTotalAndClamped() {
        assertThat(HologramText.fraction(5, 10)).isEqualTo(0.5);
        assertThat(HologramText.fraction(11, 10)).isEqualTo(1.0);
        assertThat(HologramText.fraction(-3, 10)).isEqualTo(0.0);
        assertThat(HologramText.fraction(4, 0)).isEqualTo(0.0);
    }

    private static final Pattern COLOUR_CODE = Pattern.compile("&[0-9a-fk-or]");

    /** The text a player reads: the line with its colour codes taken out. */
    private static String visible(String line) {
        return COLOUR_CODE.matcher(line).replaceAll("");
    }

    private static MachineState crushing(int progress) {
        MachineState state = new MachineState("crusher", List.of(), List.of(), List.of());
        state.start("crush_uraninite");
        state.restore("crush_uraninite", progress);
        return state;
    }

    @Test
    void aRunningLabToolHasThreeLinesProcessBarAndPercentWithTheSecondsLeft() {
        List<String> lines = HologramText.forMachine("&7Crusher", crushing(620), 1000);

        assertThat(lines).hasSize(HologramText.MACHINE_LINES);
        assertThat(lines.get(0)).isEqualTo("&fCrush uraninite");
        assertThat(lines.get(1)).isEqualTo(HologramText.progressBar(0.62, HologramText.BAR_WIDTH));
        assertThat(lines.get(2)).isEqualTo("&f62%&7  ·  about 19 s");
    }

    @Test
    void theSecondsLeftRoundUpAndAFinishedPassSaysSo() {
        assertThat(HologramText.forMachine("&7Crusher", crushing(0), 1200).get(2)).isEqualTo("&f0%&7  ·  about 60 s");
        assertThat(HologramText.forMachine("&7Crusher", crushing(3), 4).get(2)).isEqualTo("&f75%&7  ·  about 1 s");
        assertThat(HologramText.forMachine("&7Crusher", crushing(4), 4).get(2)).isEqualTo("&f100%&7  ·  finishing");
    }

    @Test
    void anIdleLabToolHasThreeLinesWithAnEmptyBarAndIdle() {
        MachineState state = new MachineState("crusher", List.of(), List.of(), List.of());

        assertThat(HologramText.forMachine("&7Crusher", state, 4))
                .containsExactly("&7Crusher", HologramText.progressBar(0.0, HologramText.BAR_WIDTH), "&7idle");
    }

    @Test
    void aProcessIdBecomesAReadableName() {
        assertThat(HologramText.processName("crush_uraninite")).isEqualTo("Crush uraninite");
        assertThat(HologramText.processName("crush")).isEqualTo("Crush");
        assertThat(HologramText.processName("")).isEmpty();
    }

    private static Deposit galena(long remaining) {
        return new Deposit(9, "world", 0, 0, 20, "galena_ore", 2_500_000, remaining);
    }

    @Test
    void aPetroleumDrillOnAPartFedGridShowsItsFourLines() {
        List<String> lines = HologramText.drillLines(DrillKind.PETROLEUM, DrillTable.level(1), 0.35, DrillStatus.RUNNING,
                0.35, DrillText.deposit("&7Crude Petroleum Deposit", null), 12, 576);

        assertThat(lines).containsExactly(
                "&bL1 Petroleum drill",
                "&7Power: &egrid 35%",
                "&7Extracting &f0.35/s  (21.0/min)",
                "&7Deposit &cnone in range",
                "&7Stored &f12&7/576");
    }

    @Test
    void theDepositLineShowsTheOreAndWhatIsLeft() {
        List<String> lines = HologramText.drillLines(DrillKind.ORE, DrillTable.level(5), 1.0, DrillStatus.RUNNING,
                6.0, DrillText.deposit("&7Galena Ore", galena(1_200_000)), 40, 576);

        assertThat(lines.get(3)).isEqualTo("&7Deposit &f&7Galena Ore &71.2M / 2.5M (48%)");
        assertThat(lines.get(2)).isEqualTo("&7Extracting &f6.00/s  (360.0/min)");
    }

    @Test
    void aFullyFedDrillShowsItsGridAsOneHundredPercent() {
        List<String> lines = HologramText.drillLines(DrillKind.PETROLEUM, DrillTable.level(3), 1.0,
                DrillStatus.RUNNING, 2.5, "", 0, 576);

        assertThat(lines.get(1)).isEqualTo("&7Power: &egrid 100%");
        assertThat(lines.get(2)).isEqualTo("&7Extracting &f2.50/s  (150.0/min)");
    }

    @Test
    void anOreDrillShowsItsRateAndItsGridPower() {
        List<String> lines = HologramText.drillLines(DrillKind.ORE, DrillTable.level(5), 1.0, DrillStatus.RUNNING,
                6.0, "", 40, 576);

        assertThat(lines.get(0)).isEqualTo("&bL5 Ore drill");
        assertThat(lines.get(1)).isEqualTo("&7Power: &egrid 100%");
        assertThat(lines.get(2)).isEqualTo("&7Extracting &f6.00/s  (360.0/min)");
        assertThat(lines.get(4)).isEqualTo("&7Stored &f40&7/576");
    }

    @Test
    void aDrillWithNoGridPowerShowsNoPowerInPlaceOfTheRate() {
        assertThat(HologramText.drillLines(DrillKind.PETROLEUM, DrillTable.level(1), 0.0,
                DrillStatus.NO_POWER, 0, "", 0, 576).get(2)).isEqualTo("&7No power");
        assertThat(HologramText.drillLines(DrillKind.ORE, DrillTable.level(1), 0.0,
                DrillStatus.NO_POWER, 0, "", 0, 576).get(2)).isEqualTo("&7No power");
        assertThat(HologramText.drillLines(DrillKind.PETROLEUM, DrillTable.level(1), 1.0,
                DrillStatus.NO_TARGET, 0, "", 0, 576).get(2)).isEqualTo("&7No deposit in range");
        assertThat(HologramText.drillLines(DrillKind.ORE, DrillTable.level(1), 1.0,
                DrillStatus.FULL, 0, "", 576, 576).get(2)).isEqualTo("&7Output full");
    }

    @Test
    void anExhaustedDepositSaysSo() {
        assertThat(visible(HologramText.drillLines(DrillKind.ORE, DrillTable.level(2), 1.0,
                DrillStatus.EXHAUSTED, 0, "", 0, 576).get(2)))
                .isEqualTo("Deposit exhausted");
    }

    @Test
    void everyDrillLineStaysWithinAHologramWidth() {
        for (DrillStatus status : DrillStatus.values()) {
            for (DrillKind kind : DrillKind.values()) {
                List<String> lines = HologramText.drillLines(kind, DrillTable.level(5), 1.0, status, 999, DrillText.deposit(
                        "&8Crude Petroleum Deposit", galena(2_000_000)), 999, 576);
                assertThat(lines).hasSize(HologramText.DRILL_LINES);
                for (String line : lines) {
                    assertThat(visible(line).length()).as(line).isLessThanOrEqualTo(64);
                }
            }
        }
    }
}
