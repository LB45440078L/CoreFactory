package io.corefactory.core.drill;

import io.corefactory.core.deposit.DrillYield;
import io.corefactory.core.process.ItemRef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DrillRulesTest {

    @Test
    void theEfficiencyIsTheGridsSatisfactionClampedToZeroToOne() {
        assertThat(DrillRules.efficiency(1.0)).isEqualTo(1.0);
        assertThat(DrillRules.efficiency(0.4)).isEqualTo(0.4);
        assertThat(DrillRules.efficiency(0.0)).isZero();
        assertThat(DrillRules.efficiency(-0.5)).isZero();
        assertThat(DrillRules.efficiency(1.5)).isEqualTo(1.0);
        assertThat(DrillRules.efficiency(Double.NaN)).isZero();
    }

    @Test
    void aDrillWithNoGridPowerHasNoEfficiency() {
        assertThat(DrillRules.efficiency(0.0)).isZero();
        assertThat(DrillRules.status(DrillRules.efficiency(0.0) > 0, true, true, true)).isEqualTo(DrillStatus.NO_POWER);
    }

    @Test
    void noPowerIsCheckedFirst() {
        assertThat(DrillRules.status(false, false, false, false)).isEqualTo(DrillStatus.NO_POWER);
    }

    @Test
    void aFullFedDrillWithRoomAndAWorkingDepositRuns() {
        assertThat(DrillRules.status(true, true, true, true)).isEqualTo(DrillStatus.RUNNING);
    }

    @Test
    void noRoomIsFullBeforeNoTargetOrExhaustion() {
        assertThat(DrillRules.status(true, false, true, true)).isEqualTo(DrillStatus.FULL);
        assertThat(DrillRules.status(true, false, false, false)).isEqualTo(DrillStatus.FULL);
    }

    @Test
    void noDepositInSquareIsNoTarget() {
        assertThat(DrillRules.status(true, true, false, false)).isEqualTo(DrillStatus.NO_TARGET);
    }

    @Test
    void anEmptyDepositInSquareIsExhausted() {
        assertThat(DrillRules.status(true, true, true, false)).isEqualTo(DrillStatus.EXHAUSTED);
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
    void theCommonMetalsYieldTheirRawItemAndADiamondOreYieldsVanillaDiamond() {
        assertThat(DrillRules.mined("iron_ore", "raw_iron")).isEqualTo(ItemRef.item("raw_iron"));
        assertThat(DrillRules.mined("copper_ore", "raw_copper")).isEqualTo(ItemRef.item("raw_copper"));
        assertThat(DrillRules.mined("gold_ore", "raw_gold")).isEqualTo(ItemRef.item("raw_gold"));
        assertThat(DrillRules.mined(DrillRules.DIAMOND, "diamond")).isEqualTo(ItemRef.material("DIAMOND"));
        assertThat(DrillRules.mined(DrillRules.DIAMOND, "diamond").kind()).isEqualTo(ItemRef.Kind.MATERIAL);
    }

    @Test
    void aFullyFedDrillYieldsItsLevelRateAndAPartFedOneItsShareOfIt() {
        DrillYield yield = new DrillYield(new double[] {1.0, 1.5, 2.5, 4.0, 6.0});

        assertThat(yield.itemsPerSecond(3) * DrillRules.efficiency(1.0)).isEqualTo(2.5);
        assertThat(yield.itemsPerSecond(3) * DrillRules.efficiency(0.4)).isCloseTo(1.0, within(1e-9));
        assertThat(yield.itemsPerSecond(3) * DrillRules.efficiency(0.0)).isZero();
    }

    @Test
    void aPartFedDrillFinishesItsCycleInProportionallyMoreTicks() {
        DrillState drill = new DrillState(new DrillDefinition("ore_drill_1", "Ore Drill", "BARREL", DrillKind.ORE,
                DrillTable.level(1)));
        double efficiency = DrillRules.efficiency(0.5);
        int ticks = 0;
        while (!drill.ready()) {
            drill.advance(efficiency);
            ticks++;
        }
        assertThat(ticks).isEqualTo(2 * DrillTable.level(1).cycleTicks());
    }
}
