package io.corefactory.core.drill;

import io.corefactory.core.geology.Placement;
import io.corefactory.core.process.ItemRef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.IntPredicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DrillRulesTest {

    private static final int DEPOSIT = 7;

    @Test
    void theRatePerMinuteIsOneItemPerCycleScaledByEfficiency() {
        assertThat(DrillRules.itemsPerMinute(1.0, 600)).isEqualTo(2.0);
        assertThat(DrillRules.itemsPerMinute(1.0, 480)).isCloseTo(2.5, within(1e-9));
        assertThat(DrillRules.itemsPerMinute(1.0, 120)).isEqualTo(10.0);
        assertThat(DrillRules.itemsPerMinute(0.35, 600)).isCloseTo(0.7, within(1e-9));
        assertThat(DrillRules.itemsPerMinute(0.5, 240)).isEqualTo(2.5);
    }

    @Test
    void theRatePerSecondIsEfficiencyTimesTwentyOverTheCycle() {
        assertThat(DrillRules.itemsPerSecond(1.0, 120)).isCloseTo(1.0 / 6.0, within(1e-9));
        assertThat(DrillRules.itemsPerSecond(1.0, 600)).isCloseTo(1.0 / 30.0, within(1e-9));
        assertThat(DrillRules.itemsPerSecond(0.35, 600)).isCloseTo(0.35 / 30.0, within(1e-9));
    }

    @Test
    void thePerMinuteRateIsSixtyTimesThePerSecondRate() {
        for (double efficiency : new double[] {0.35, 0.5, 1.0}) {
            for (int cycle : new int[] {120, 240, 480, 600}) {
                assertThat(DrillRules.itemsPerMinute(efficiency, cycle))
                        .isCloseTo(60 * DrillRules.itemsPerSecond(efficiency, cycle), within(1e-9));
            }
        }
    }

    @Test
    void aStoppedDrillOrAnInvalidCycleMakesNoItems() {
        assertThat(DrillRules.itemsPerMinute(0.0, 600)).isZero();
        assertThat(DrillRules.itemsPerMinute(-1.0, 600)).isZero();
        assertThat(DrillRules.itemsPerMinute(1.0, 0)).isZero();
        assertThat(DrillRules.itemsPerSecond(0.0, 600)).isZero();
        assertThat(DrillRules.itemsPerSecond(1.0, 0)).isZero();
    }

    @Test
    void aSignalIsItsPowerOverFifteenAndClamped() {
        assertThat(DrillRules.signalEfficiency(0)).isZero();
        assertThat(DrillRules.signalEfficiency(15)).isEqualTo(1.0);
        assertThat(DrillRules.signalEfficiency(5)).isCloseTo(1.0 / 3.0, within(1e-9));
        assertThat(DrillRules.signalEfficiency(-3)).isZero();
        assertThat(DrillRules.signalEfficiency(20)).isEqualTo(1.0);
    }

    @Test
    void aRedstoneBlockOnlyRunsThePetroleumDrillAtFullPower() {
        DrillPower power = DrillRules.petroleumPower(15, List.of(), false);
        assertThat(power.source()).isEqualTo(PowerSource.REDSTONE);
        assertThat(power.efficiency()).isEqualTo(1.0);
        assertThat(power.powered()).isTrue();
    }

    @Test
    void aPartRedstoneSignalGivesItsOwnEfficiency() {
        DrillPower power = DrillRules.petroleumPower(6, List.of(), false);
        assertThat(power.source()).isEqualTo(PowerSource.REDSTONE);
        assertThat(power.efficiency()).isCloseTo(0.4, within(1e-9));
    }

    @Test
    void aSolarPanelOnlyRunsTheDrillAtItsOwnPower() {
        DrillPower power = DrillRules.petroleumPower(0, List.of(9), false);
        assertThat(power.source()).isEqualTo(PowerSource.SOLAR);
        assertThat(power.efficiency()).isCloseTo(0.6, within(1e-9));
    }

    @Test
    void charcoalOnlyGivesThirtyFivePercent() {
        DrillPower power = DrillRules.petroleumPower(0, List.of(), true);
        assertThat(power.source()).isEqualTo(PowerSource.CHARCOAL);
        assertThat(power.efficiency()).isEqualTo(DrillRules.CHARCOAL_EFFICIENCY).isEqualTo(0.35);
    }

    @Test
    void theBestSourceWins() {
        assertThat(DrillRules.petroleumPower(5, List.of(9), true).source()).isEqualTo(PowerSource.SOLAR);
        assertThat(DrillRules.petroleumPower(5, List.of(9), true).efficiency()).isCloseTo(0.6, within(1e-9));
        assertThat(DrillRules.petroleumPower(5, List.of(), true).source()).isEqualTo(PowerSource.CHARCOAL);
        assertThat(DrillRules.petroleumPower(12, List.of(9), true).source()).isEqualTo(PowerSource.REDSTONE);
        assertThat(DrillRules.petroleumPower(12, List.of(9), true).efficiency()).isCloseTo(0.8, within(1e-9));
        assertThat(DrillRules.petroleumPower(0, List.of(3), true).source()).isEqualTo(PowerSource.CHARCOAL);
    }

    @Test
    void theStrongestOfSeveralPanelsIsTheSolarSource() {
        DrillPower power = DrillRules.petroleumPower(0, List.of(3, 11, 0), false);
        assertThat(power.source()).isEqualTo(PowerSource.SOLAR);
        assertThat(power.efficiency()).isCloseTo(11.0 / 15.0, within(1e-9));
    }

    @Test
    void allSourcesAtZeroMeanNoPower() {
        DrillPower power = DrillRules.petroleumPower(0, List.of(0, 0), false);
        assertThat(power).isEqualTo(DrillPower.NONE);
        assertThat(power.source()).isEqualTo(PowerSource.NONE);
        assertThat(power.efficiency()).isZero();
        assertThat(power.powered()).isFalse();
        assertThat(DrillRules.petroleumPower(0, List.of(), false)).isEqualTo(DrillPower.NONE);
    }

    @Test
    void aTieIsNamedBySolarBecauseAPanelAlsoShowsInTheRedstoneReading() {
        assertThat(DrillRules.petroleumPower(15, List.of(15), true).source()).isEqualTo(PowerSource.SOLAR);
        assertThat(DrillRules.petroleumPower(9, List.of(9), false).source()).isEqualTo(PowerSource.SOLAR);
        assertThat(DrillRules.petroleumPower(0, List.of(15), true).source()).isEqualTo(PowerSource.SOLAR);
    }

    @Test
    void aRedstoneSourceStrongerThanThePanelIsNamedRedstone() {
        assertThat(DrillRules.petroleumPower(15, List.of(9), false).source()).isEqualTo(PowerSource.REDSTONE);
        assertThat(DrillRules.petroleumPower(15, List.of(), false).source()).isEqualTo(PowerSource.REDSTONE);
    }

    @Test
    void aRedstoneReadingOutsideTheScaleIsClamped() {
        assertThat(DrillRules.petroleumPower(40, List.of(), false).efficiency()).isEqualTo(1.0);
        assertThat(DrillRules.petroleumPower(-4, List.of(), false)).isEqualTo(DrillPower.NONE);
    }

    @Test
    void charcoalIsBurnedOnlyWhenCharcoalIsTheSource() {
        assertThat(DrillRules.burnsCharcoal(DrillRules.petroleumPower(0, List.of(), true))).isTrue();
        assertThat(DrillRules.burnsCharcoal(DrillRules.petroleumPower(3, List.of(), true))).isTrue();
        assertThat(DrillRules.burnsCharcoal(DrillRules.petroleumPower(15, List.of(), true))).isFalse();
        assertThat(DrillRules.burnsCharcoal(DrillRules.petroleumPower(0, List.of(9), true))).isFalse();
        assertThat(DrillRules.burnsCharcoal(DrillPower.NONE)).isFalse();
    }

    @Test
    void theGridRaisesADrillOnlyAboveItsLocalPower() {
        DrillPower redstone = DrillRules.petroleumPower(7, List.of(), false);
        assertThat(DrillRules.withGrid(redstone, 0.0)).isEqualTo(redstone);
        assertThat(DrillRules.withGrid(redstone, 0.2)).isEqualTo(redstone);
        assertThat(DrillRules.withGrid(redstone, 1.0)).isEqualTo(new DrillPower(PowerSource.GRID, 1.0));
        assertThat(DrillRules.withGrid(DrillPower.NONE, 0.4)).isEqualTo(new DrillPower(PowerSource.GRID, 0.4));
        assertThat(DrillRules.withGrid(DrillPower.NONE, 0.0)).isEqualTo(DrillPower.NONE);
    }

    @Test
    void anOreDrillRunsAtFullPowerOnlyWithPetroleum() {
        assertThat(DrillRules.orePower(true)).isEqualTo(new DrillPower(PowerSource.PETROLEUM, 1.0));
        assertThat(DrillRules.orePower(false)).isEqualTo(DrillPower.NONE);
    }

    @Test
    void aRunningDrillNeedsPowerRoomAndATarget() {
        assertThat(DrillRules.status(true, true, true)).isEqualTo(DrillStatus.RUNNING);
        assertThat(DrillRules.status(true, true, false)).isEqualTo(DrillStatus.NO_TARGET);
        assertThat(DrillRules.status(true, false, true)).isEqualTo(DrillStatus.FULL);
        assertThat(DrillRules.status(false, true, true)).isEqualTo(DrillStatus.NO_POWER);
    }

    @Test
    void noPowerIsReportedBeforeNoTargetOrFullOutputs() {
        assertThat(DrillRules.status(false, true, false)).isEqualTo(DrillStatus.NO_POWER);
        assertThat(DrillRules.status(false, false, false)).isEqualTo(DrillStatus.NO_POWER);
        assertThat(DrillRules.status(true, false, false)).isEqualTo(DrillStatus.FULL);
    }

    @Test
    void aPetroleumDrillTakesOnlyDepositsAndAnOreDrillTakesEveryOtherOre() {
        assertThat(DrillRules.targets(DrillKind.PETROLEUM, DEPOSIT).test(DEPOSIT)).isTrue();
        assertThat(DrillRules.targets(DrillKind.PETROLEUM, DEPOSIT).test(2)).isFalse();
        assertThat(DrillRules.targets(DrillKind.ORE, DEPOSIT).test(DEPOSIT)).isFalse();
        assertThat(DrillRules.targets(DrillKind.ORE, DEPOSIT).test(2)).isTrue();
        assertThat(DrillRules.targets(DrillKind.ORE, -1).test(-1)).isFalse();
        assertThat(DrillRules.targets(DrillKind.PETROLEUM, -1).test(-1)).isFalse();
    }

    @Test
    void onlyTheOreDrillTakesItsTargetOutOfTheWorld() {
        assertThat(DrillKind.PETROLEUM.removesTarget()).isFalse();
        assertThat(DrillKind.ORE.removesTarget()).isTrue();
    }

    @Test
    void theNearestQualifyingBlockIsChosenInsideTheRegion() {
        DrillBox box = DrillBox.around(0, 60, 0, DrillTable.level(1));
        Placement far = new Placement(4, 46, 4, 1);
        Placement near = new Placement(1, 58, 0, 1);
        Placement middle = new Placement(-2, 50, -2, 1);
        assertThat(DrillRules.nearest(List.of(far, near, middle), box, 0, 60, 0, index -> true))
                .contains(near);
    }

    @Test
    void aBlockOutsideTheSquareOrBelowTheDepthIsNeverChosen() {
        DrillBox box = DrillBox.around(0, 60, 0, DrillTable.level(1));
        Placement outsideSquare = new Placement(5, 60, 0, 1);
        Placement belowDepth = new Placement(0, 12, 0, 1);
        assertThat(DrillRules.nearest(List.of(outsideSquare, belowDepth), box, 0, 60, 0, index -> true)).isEmpty();
    }

    @Test
    void theOreDrillNeverChoosesADeposit() {
        DrillBox box = DrillBox.around(0, 60, 0, DrillTable.level(1));
        Placement deposit = new Placement(0, 60, 0, DEPOSIT);
        Placement ore = new Placement(3, 59, 2, 1);
        IntPredicate ores = DrillRules.targets(DrillKind.ORE, DEPOSIT);
        assertThat(DrillRules.nearest(List.of(deposit, ore), box, 0, 60, 0, ores)).contains(ore);
        assertThat(DrillRules.nearest(List.of(deposit), box, 0, 60, 0, ores)).isEmpty();
    }

    @Test
    void thePetroleumDrillChoosesOnlyDeposits() {
        DrillBox box = DrillBox.around(0, 60, 0, DrillTable.level(1));
        Placement deposit = new Placement(2, 55, 2, DEPOSIT);
        Placement ore = new Placement(1, 58, 0, 1);
        IntPredicate deposits = DrillRules.targets(DrillKind.PETROLEUM, DEPOSIT);
        assertThat(DrillRules.nearest(List.of(ore, deposit), box, 0, 60, 0, deposits)).contains(deposit);
    }

    @Test
    void aTieGoesToTheLowerYThenXThenZ() {
        DrillBox box = DrillBox.around(0, 60, 0, DrillTable.level(1));
        Placement xHigh = new Placement(1, 59, 0, 1);
        Placement xLow = new Placement(0, 59, 1, 1);
        assertThat(DrillRules.nearest(List.of(xHigh, xLow), box, 0, 60, 0, index -> true)).contains(xLow);
        Placement yHigh = new Placement(1, 60, 0, 1);
        Placement yLow = new Placement(0, 59, 0, 1);
        assertThat(DrillRules.nearest(List.of(yHigh, yLow), box, 0, 60, 0, index -> true)).contains(yLow);
        Placement zHigh = new Placement(0, 59, 1, 1);
        Placement zLow = new Placement(0, 59, -1, 1);
        assertThat(DrillRules.nearest(List.of(zHigh, zLow), box, 0, 60, 0, index -> true)).contains(zLow);
    }

    @Test
    void onlyHardBaseStoneCanCarryADrill() {
        for (String stone : List.of("STONE", "DEEPSLATE", "GRANITE", "DIORITE", "ANDESITE", "TUFF")) {
            assertThat(DrillRules.hardBase(stone)).as(stone).isTrue();
        }
        for (String soft : List.of("DIRT", "GRASS_BLOCK", "SAND", "GRAVEL", "AIR", "COBBLESTONE", "BARREL", "")) {
            assertThat(DrillRules.hardBase(soft)).as(soft).isFalse();
        }
        assertThat(DrillRules.hardBase(null)).isFalse();
    }

    @Test
    void aMinedCoalOreYieldsVanillaCharcoalAndOtherOresTheirDropComponent() {
        assertThat(DrillRules.mined(DrillRules.COAL, "charcoal")).isEqualTo(DrillRules.CHARCOAL);
        assertThat(DrillRules.CHARCOAL.kind()).isEqualTo(ItemRef.Kind.MATERIAL);
        assertThat(DrillRules.CHARCOAL.name()).isEqualTo("CHARCOAL");
        assertThat(DrillRules.mined("raw_galena_ore", "raw_galena")).isEqualTo(ItemRef.item("raw_galena"));
    }

    @Test
    void coalIsAnOreTargetOfAnOreDrillAndNeverOfAPetroleumDrill() {
        int coal = 3;
        assertThat(DrillRules.targets(DrillKind.ORE, DEPOSIT).test(coal)).isTrue();
        assertThat(DrillRules.targets(DrillKind.PETROLEUM, DEPOSIT).test(coal)).isFalse();
    }
}
