package io.corefactory.core.drill;

import io.corefactory.core.process.ItemRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DrillRulesTest {

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
}
