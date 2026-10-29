package io.corefactory.core.drill;

import io.corefactory.core.deposit.Deposit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DrillTextTest {

    private static final Pattern COLOUR_CODE = Pattern.compile("&[0-9a-fk-or]");

    private static String visible(String line) {
        return COLOUR_CODE.matcher(line).replaceAll("");
    }

    private static Deposit deposit(long remaining, long initial) {
        return new Deposit(4, "world", 10, -3, 30, "galena_ore", initial, remaining);
    }

    @Test
    void theRateReadsPerSecondAndPerMinute() {
        assertThat(DrillText.rate(1.5)).isEqualTo("1.50/s  (90.0/min)");
        assertThat(DrillText.rate(0.05)).isEqualTo("0.05/s  (3.0/min)");
        assertThat(DrillText.rate(0.0)).isEqualTo("0.00/s  (0.0/min)");
        assertThat(DrillText.rate(-1.0)).isEqualTo("0.00/s  (0.0/min)");
    }

    @Test
    void thePowerReadsTheGridAndItsPercentage() {
        assertThat(DrillText.power(1.0)).isEqualTo("grid 100%");
        assertThat(DrillText.power(0.6)).isEqualTo("grid 60%");
        assertThat(DrillText.power(0.0)).isEqualTo("grid 0%");
    }

    @Test
    void amountsReadWithAUnitAndTruncate() {
        assertThat(DrillText.amount(0)).isEqualTo("0");
        assertThat(DrillText.amount(999)).isEqualTo("999");
        assertThat(DrillText.amount(1_000)).isEqualTo("1.0k");
        assertThat(DrillText.amount(4_250)).isEqualTo("4.2k");
        assertThat(DrillText.amount(9_999)).isEqualTo("9.9k");
        assertThat(DrillText.amount(10_000)).isEqualTo("10k");
        assertThat(DrillText.amount(850_000)).isEqualTo("850k");
        assertThat(DrillText.amount(999_999)).isEqualTo("999k");
        assertThat(DrillText.amount(1_000_000)).isEqualTo("1.0M");
        assertThat(DrillText.amount(1_249_999)).isEqualTo("1.2M");
        assertThat(DrillText.amount(9_999_999)).isEqualTo("9.9M");
        assertThat(DrillText.amount(12_000_000)).isEqualTo("12M");
        assertThat(DrillText.amount(25_000_000)).isEqualTo("25M");
        assertThat(DrillText.amount(-5)).isEqualTo("0");
    }

    @Test
    void aDistanceReadsInMetresThenKilometres() {
        assertThat(DrillText.distance(0)).isEqualTo("0 m");
        assertThat(DrillText.distance(999)).isEqualTo("999 m");
        assertThat(DrillText.distance(1_000)).isEqualTo("1.0 km");
        assertThat(DrillText.distance(12_500)).isEqualTo("12.5 km");
        assertThat(DrillText.distance(12_999)).isEqualTo("12.9 km");
        assertThat(DrillText.distance(-4)).isEqualTo("0 m");
    }

    @Test
    void aDepositReadsWhatIsLeftOfWhatItStartedWithAndTheShareLeft() {
        assertThat(DrillText.remaining(deposit(1_200_000, 2_500_000))).isEqualTo("1.2M / 2.5M (48%)");
        assertThat(DrillText.remaining(deposit(0, 500_000))).isEqualTo("0 / 500k (0%)");
        assertThat(DrillText.remaining(deposit(500_000, 500_000))).isEqualTo("500k / 500k (100%)");
        assertThat(DrillText.share(deposit(0, 0))).isEqualTo("0%");
    }

    @Test
    void theDepositLineNamesTheOreAndItsAmounts() {
        assertThat(DrillText.deposit("&7Galena Ore", deposit(1_200_000, 2_500_000)))
                .isEqualTo("&7Deposit &f&7Galena Ore &71.2M / 2.5M (48%)");
        assertThat(DrillText.deposit("&7Galena Ore", null)).isEqualTo("&7Deposit &cnone in range");
    }

    @Test
    void aRunningPetroleumDrillInfoShowsItsRateStatusAndItsGrid() {
        List<String> lines = DrillText.infoLines(DrillKind.PETROLEUM, DrillTable.level(1), 1.0, DrillStatus.RUNNING, 0.05,
                "&7Deposit &fnone");

        assertThat(lines).contains("&7Status &aRunning", "&7Rate &f0.05/s  (3.0/min)", "&7Power: &fgrid 100%");
        assertThat(lines).contains("&7Deposit &fnone", "&7Powered by: &fthe energy network");
        assertThat(lines).noneMatch(line -> line.contains("charcoal") || line.contains("solar") || line.contains("redstone"));
    }

    @Test
    void aStoppedDrillInfoReadsStoppedAndItsStatus() {
        List<String> lines = DrillText.infoLines(DrillKind.PETROLEUM, DrillTable.level(1), 0.0,
                DrillStatus.NO_POWER, 0.0, "&7Deposit &cnone in range");

        assertThat(lines).contains("&7Rate &cstopped", "&7Status &cNo power", "&7Power: &fgrid 0%");
        assertThat(lines).noneMatch(line -> line.contains("/min"));
    }

    @Test
    void anOreDrillWithNoPowerReadsNoPowerNotNoPetroleum() {
        List<String> lines = DrillText.infoLines(DrillKind.ORE, DrillTable.level(3), 0.0,
                DrillStatus.NO_POWER, 0.0, "&7Deposit &cnone in range");

        assertThat(lines).contains("&7Status &cNo power", "&7Powered by: &fthe energy network");
        assertThat(lines).noneMatch(line -> line.contains("petroleum"));
    }

    @Test
    void anOreDrillInfoNamesNoFluidAsItsSource() {
        List<String> lines = DrillText.infoLines(DrillKind.ORE, DrillTable.level(3), 1.0,
                DrillStatus.NO_TARGET, 0.0, "&7Deposit &cnone in range");

        assertThat(lines).contains("&7Status &cNo deposit in range");
        assertThat(lines).noneMatch(line -> line.contains("petroleum") || line.contains("charcoal"));
    }

    @Test
    void noDepositInRangeReadsTheSameForEveryKind() {
        assertThat(DrillStatus.NO_TARGET.text(DrillKind.PETROLEUM)).isEqualTo("No deposit in range");
        assertThat(DrillStatus.NO_TARGET.text(DrillKind.ORE)).isEqualTo("No deposit in range");
    }

    @Test
    void noInfoLineLeavesAStrayAmpersandAfterItsColourCodes() {
        for (DrillKind kind : DrillKind.values()) {
            for (DrillStatus status : DrillStatus.values()) {
                for (String line : DrillText.infoLines(kind, DrillTable.level(2), 1.0, status,
                        0.2, DrillText.deposit("&7Galena Ore", deposit(10, 20)))) {
                    assertThat(visible(line)).as(line).doesNotContain("&");
                }
            }
        }
    }
}
