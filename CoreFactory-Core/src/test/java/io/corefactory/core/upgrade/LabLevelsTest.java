package io.corefactory.core.upgrade;

import io.corefactory.core.upgrade.LabLevels.Upgrade;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LabLevelsTest {

    @Test
    void aKitTakesAMachineUpOneLevelOnly() {
        assertThat(LabLevels.canUpgrade(1, 2)).isEqualTo(Upgrade.OK);
        assertThat(LabLevels.canUpgrade(2, 3)).isEqualTo(Upgrade.OK);
        assertThat(LabLevels.canUpgrade(3, 4)).isEqualTo(Upgrade.OK);
        assertThat(LabLevels.canUpgrade(4, 5)).isEqualTo(Upgrade.OK);
    }

    @Test
    void aKitForAnotherLevelIsRefused() {
        assertThat(LabLevels.canUpgrade(1, 1)).isEqualTo(Upgrade.WRONG_LEVEL);
        assertThat(LabLevels.canUpgrade(1, 3)).isEqualTo(Upgrade.WRONG_LEVEL);
        assertThat(LabLevels.canUpgrade(2, 1)).isEqualTo(Upgrade.WRONG_LEVEL);
        assertThat(LabLevels.canUpgrade(2, 2)).isEqualTo(Upgrade.WRONG_LEVEL);
        assertThat(LabLevels.canUpgrade(3, 5)).isEqualTo(Upgrade.WRONG_LEVEL);
        assertThat(LabLevels.canUpgrade(4, 3)).isEqualTo(Upgrade.WRONG_LEVEL);
    }

    @Test
    void aMachineAtItsTopLevelRefusesEveryKit() {
        assertThat(LabLevels.canUpgrade(5, 1)).isEqualTo(Upgrade.MAX_LEVEL);
        assertThat(LabLevels.canUpgrade(5, 4)).isEqualTo(Upgrade.MAX_LEVEL);
        assertThat(LabLevels.canUpgrade(5, 5)).isEqualTo(Upgrade.MAX_LEVEL);
    }

    @Test
    void aLevelOutsideTheRangeIsRefused() {
        assertThatThrownBy(() -> LabLevels.canUpgrade(0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LabLevels.canUpgrade(6, 5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theLevelRangeIsOneToFive() {
        assertThat(LabLevels.MIN_LEVEL).isEqualTo(1);
        assertThat(LabLevels.MAX_LEVEL).isEqualTo(5);
    }

    @Test
    void theDefaultSpeedsAreOneOneAndAHalfTwoAndTwoAndAThreeQuartersAndThreeAndAHalf() {
        LabLevels levels = LabLevels.defaults();

        assertThat(levels.speed(1)).isEqualTo(1.0);
        assertThat(levels.speed(2)).isEqualTo(1.5);
        assertThat(levels.speed(3)).isEqualTo(2.0);
        assertThat(levels.speed(4)).isEqualTo(2.75);
        assertThat(levels.speed(5)).isEqualTo(3.5);
    }

    @Test
    void aSpeedIsClampedToTheLevelsThatExist() {
        LabLevels levels = LabLevels.defaults();

        assertThat(levels.speed(0)).isEqualTo(1.0);
        assertThat(levels.speed(6)).isEqualTo(3.5);
    }

    @Test
    void aSpeedListThatIsNotThreeFactorsIsRefused() {
        assertThatThrownBy(() -> new LabLevels(1.0, 1.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LabLevels(1.0, 1.5, 2.0, 2.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LabLevels(1.0, 1.5, 2.0, 2.5, 3.0, 3.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LabLevels((double[]) null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aSpeedThatIsNotPositiveOrFiniteIsRefused() {
        assertThatThrownBy(() -> new LabLevels(0.0, 1.5, 2.0, 2.5, 3.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LabLevels(1.0, -1.5, 2.0, 2.5, 3.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LabLevels(1.0, 1.5, 2.0, 2.5, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LabLevels(1.0, 1.5, 2.0, 2.5, Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aHigherLevelIsNeverSlowerThanTheLevelBelow() {
        assertThatThrownBy(() -> new LabLevels(1.0, 2.0, 1.5, 2.0, 2.0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new LabLevels(1.0, 1.0, 1.0, 1.0, 1.0).speed(5)).isEqualTo(1.0);
    }

    @Test
    void theKitLevelIsReadFromItsComponentId() {
        assertThat(LabLevels.kitLevel("machine_upgrade_2").getAsInt()).isEqualTo(2);
        assertThat(LabLevels.kitLevel("machine_upgrade_3").getAsInt()).isEqualTo(3);
        assertThat(LabLevels.kitLevel("machine_upgrade_4").getAsInt()).isEqualTo(4);
        assertThat(LabLevels.kitLevel("machine_upgrade_5").getAsInt()).isEqualTo(5);
    }

    @Test
    void anotherIdIsNotAKit() {
        assertThat(LabLevels.kitLevel("machine_upgrade_1").isPresent()).isFalse();
        assertThat(LabLevels.kitLevel("machine_upgrade_6").isPresent()).isFalse();
        assertThat(LabLevels.kitLevel("machine_upgrade_").isPresent()).isFalse();
        assertThat(LabLevels.kitLevel("machine_upgrade_2x").isPresent()).isFalse();
        assertThat(LabLevels.kitLevel("water_pump_upgrade_2").isPresent()).isFalse();
        assertThat(LabLevels.kitLevel("assembler").isPresent()).isFalse();
        assertThat(LabLevels.kitLevel(null).isPresent()).isFalse();
    }

    @Test
    void theModelIdIsTheMachineIdAtLevelOneAndSuffixedAbove() {
        assertThat(LabLevels.modelId("assembler", 1)).isEqualTo("assembler");
        assertThat(LabLevels.modelId("assembler", 2)).isEqualTo("assembler_2");
        assertThat(LabLevels.modelId("assembler", 3)).isEqualTo("assembler_3");
        assertThat(LabLevels.modelId("assembler", 4)).isEqualTo("assembler_4");
        assertThat(LabLevels.modelId("assembler", 5)).isEqualTo("assembler_5");
    }

    @Test
    void theNumeralsAreIThroughV() {
        assertThat(LabLevels.numeral(1)).isEqualTo("I");
        assertThat(LabLevels.numeral(2)).isEqualTo("II");
        assertThat(LabLevels.numeral(3)).isEqualTo("III");
        assertThat(LabLevels.numeral(4)).isEqualTo("IV");
        assertThat(LabLevels.numeral(5)).isEqualTo("V");
    }

    @Test
    void aFullNetworkRunsOneProcessTickAtSpeedOne() {
        LabLevels.Pace pace = LabLevels.pace(0.0, 1.0, 1.0);

        assertThat(pace.runs()).isEqualTo(1);
        assertThat(pace.credit()).isEqualTo(0.0);
    }

    @Test
    void aFullNetworkRunsTwoProcessTicksAtSpeedTwo() {
        LabLevels.Pace pace = LabLevels.pace(0.0, 1.0, 2.0);

        assertThat(pace.runs()).isEqualTo(2);
        assertThat(pace.credit()).isEqualTo(0.0);
    }

    @Test
    void aSpeedOfOneAndAHalfAveragesOneAndAHalfProcessTicks() {
        int runs = 0;
        double credit = 0.0;
        for (int tick = 0; tick < 100; tick++) {
            LabLevels.Pace pace = LabLevels.pace(credit, 1.0, 1.5);
            credit = pace.credit();
            runs += pace.runs();
        }

        assertThat(runs).isEqualTo(150);
    }

    @Test
    void aPartialNetworkAveragesItsSatisfactionTimesTheSpeed() {
        int runs = 0;
        double credit = 0.0;
        for (int tick = 0; tick < 100; tick++) {
            LabLevels.Pace pace = LabLevels.pace(credit, 0.5, 2.0);
            credit = pace.credit();
            runs += pace.runs();
        }

        assertThat(runs).isEqualTo(100);
    }

    @Test
    void noNetworkOrANanSatisfactionRunsNothing() {
        assertThat(LabLevels.pace(0.0, 0.0, 2.0).runs()).isZero();
        assertThat(LabLevels.pace(0.0, -1.0, 2.0).runs()).isZero();
        assertThat(LabLevels.pace(0.0, Double.NaN, 2.0).runs()).isZero();
    }

    @Test
    void aSatisfactionAboveOneCountsAsOne() {
        assertThat(LabLevels.pace(0.0, 3.0, 1.0).runs()).isEqualTo(1);
    }

    @Test
    void theCreditStaysBelowOne() {
        LabLevels.Pace pace = LabLevels.pace(0.9, 0.5, 1.0);

        assertThat(pace.runs()).isEqualTo(1);
        assertThat(pace.credit()).isBetween(0.0, 1.0);
    }
}
