package io.corefactory.core.drill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DrillTextTest {

    private static final Pattern COLOUR_CODE = Pattern.compile("&[0-9a-fk-or]");

    private static String visible(String line) {
        return COLOUR_CODE.matcher(line).replaceAll("");
    }

    @Test
    void theRateReadsPerSecondAndPerMinute() {
        assertThat(DrillText.rate(1.0, 600)).isEqualTo("0.03/s  (2.0/min)");
        assertThat(DrillText.rate(1.0, 120)).isEqualTo("0.17/s  (10.0/min)");
        assertThat(DrillText.rate(0.0, 600)).isEqualTo("0.00/s  (0.0/min)");
    }

    @Test
    void thePowerReadsItsSourceAndItsPercentage() {
        assertThat(DrillText.power(DrillRules.petroleumPower(15, List.of(), false))).isEqualTo("redstone 100%");
        assertThat(DrillText.power(DrillRules.petroleumPower(0, List.of(9), false))).isEqualTo("solar 60%");
        assertThat(DrillText.power(DrillRules.petroleumPower(0, List.of(), true))).isEqualTo("charcoal 35%");
        assertThat(DrillText.power(DrillPower.NONE)).isEqualTo("none 0%");
    }

    @Test
    void aRunningPetroleumDrillInfoShowsItsRateStatusPowerAndWhatPowersIt() {
        List<String> lines = DrillText.infoLines(DrillKind.PETROLEUM, DrillTable.level(1),
                DrillRules.petroleumPower(15, List.of(), false), DrillStatus.RUNNING);

        assertThat(lines).contains("&7Status &aRunning", "&7Rate &f0.03/s  (2.0/min)", "&7Power: &fredstone 100%");
        assertThat(lines).contains("&8- a redstone block or wire next to it", "&8- a solar panel next to it",
                "&8- charcoal in the fuel slot");
        assertThat(lines).doesNotContain("&8- petroleum in the fuel slot");
    }

    @Test
    void aStoppedDrillInfoReadsStoppedAndItsStatus() {
        List<String> lines = DrillText.infoLines(DrillKind.PETROLEUM, DrillTable.level(1), DrillPower.NONE,
                DrillStatus.NO_POWER);

        assertThat(lines).contains("&7Rate &cstopped", "&7Status &cNo power", "&7Power: &fnone 0%");
        assertThat(lines).noneMatch(line -> line.contains("/min"));
    }

    @Test
    void anOreDrillInfoNamesPetroleumAsItsOnlySource() {
        List<String> lines = DrillText.infoLines(DrillKind.ORE, DrillTable.level(3), DrillRules.orePower(true),
                DrillStatus.NO_TARGET);

        assertThat(lines).contains("&8- petroleum in the fuel slot", "&7Status &cNo special ore in range");
        assertThat(lines).noneMatch(line -> line.contains("charcoal") || line.contains("solar"));
    }

    @Test
    void noTargetInRangeIsTheStatusOfAPetroleumDrillAndDescribesItsDepth() {
        assertThat(DrillStatus.NO_TARGET.text(DrillKind.PETROLEUM, DrillTable.level(1)))
                .isEqualTo("No deposit in range (depth 48)");
        assertThat(DrillStatus.NO_TARGET.text(DrillKind.PETROLEUM, DrillTable.level(5)))
                .isEqualTo("No deposit in range (depth 280)");
    }

    @Test
    void noInfoLineLeavesAStrayAmpersandAfterItsColourCodes() {
        for (DrillKind kind : DrillKind.values()) {
            for (DrillStatus status : DrillStatus.values()) {
                for (String line : DrillText.infoLines(kind, DrillTable.level(2), DrillRules.orePower(true), status)) {
                    assertThat(visible(line)).as(line).doesNotContain("&");
                }
            }
        }
    }
}
