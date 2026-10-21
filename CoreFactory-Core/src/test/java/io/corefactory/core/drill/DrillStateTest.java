package io.corefactory.core.drill;

import io.corefactory.core.process.Machine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DrillStateTest {

    private static DrillState state(DrillKind kind, int level) {
        DrillSpec spec = DrillTable.level(level);
        return new DrillState(new DrillDefinition("drill", "Drill", "BARREL", kind, spec));
    }

    @Test
    void aCycleAdvancesByTheEfficiencyEachTick() {
        DrillState drill = state(DrillKind.PETROLEUM, 1);
        for (int tick = 0; tick < 10; tick++) {
            drill.advance(1.0);
        }
        assertThat(drill.progress()).isEqualTo(10.0);
        assertThat(drill.ready()).isFalse();
    }

    @Test
    void aPartEfficiencyTakesProportionallyLonger() {
        DrillState drill = state(DrillKind.PETROLEUM, 1);
        drill.advance(0.5);
        drill.advance(0.5);
        assertThat(drill.progress()).isEqualTo(1.0);
    }

    @Test
    void aZeroEfficiencyDoesNotAdvanceTheCycle() {
        DrillState drill = state(DrillKind.PETROLEUM, 1);
        drill.advance(0.0);
        assertThat(drill.progress()).isZero();
    }

    @Test
    void aFullCycleIsReadyAndDoesNotRunPast() {
        DrillState drill = state(DrillKind.ORE, 5);
        for (int tick = 0; tick < 500; tick++) {
            drill.advance(1.0);
        }
        assertThat(drill.ready()).isTrue();
        assertThat(drill.progress()).isEqualTo(120.0);
    }

    @Test
    void finishingACycleRestartsItAndCountsFuelOnlyWhenItBurned() {
        DrillState drill = state(DrillKind.PETROLEUM, 1);
        drill.advance(600);
        drill.finishCycle(false);
        assertThat(drill.progress()).isZero();
        assertThat(drill.fuelCycles()).isZero();
        drill.advance(600);
        drill.finishCycle(true);
        assertThat(drill.fuelCycles()).isEqualTo(1);
    }

    @Test
    void aPetroleumDrillBurnsCharcoalEveryEighthCycle() {
        DrillState drill = state(DrillKind.PETROLEUM, 1);
        for (int cycle = 1; cycle <= 7; cycle++) {
            finishBurning(drill);
            assertThat(drill.fuelDue()).as("after cycle " + cycle).isFalse();
        }
        finishBurning(drill);
        assertThat(drill.fuelDue()).isTrue();
        drill.fuelPaid();
        assertThat(drill.fuelCycles()).isZero();
    }

    @Test
    void anOreDrillBurnsPetroleumEveryOtherCycle() {
        DrillState drill = state(DrillKind.ORE, 3);
        finishBurning(drill);
        assertThat(drill.fuelDue()).isFalse();
        finishBurning(drill);
        assertThat(drill.fuelDue()).isTrue();
    }

    @Test
    void aCycleThatBurnsNoFuelDoesNotCountTowardsTheNextItem() {
        DrillState drill = state(DrillKind.PETROLEUM, 1);
        for (int cycle = 0; cycle < 20; cycle++) {
            drill.advance(600);
            drill.finishCycle(false);
        }
        assertThat(drill.fuelDue()).isFalse();
    }

    @Test
    void aSavedCycleIsRestoredAndClamped() {
        DrillState drill = state(DrillKind.PETROLEUM, 1);
        drill.restore(250.5, 3);
        assertThat(drill.progress()).isEqualTo(250.5);
        assertThat(drill.fuelCycles()).isEqualTo(3);
        drill.restore(9999, -4);
        assertThat(drill.progress()).isEqualTo(600.0);
        assertThat(drill.fuelCycles()).isZero();
    }

    @Test
    void theDefinitionOfADrillIsItsMachineWithOneFuelSlotAndNineOutputs() {
        Machine machine = new DrillDefinition("ore_drill_1", "Ore Drill", "BARREL", DrillKind.ORE,
                DrillTable.level(1)).machine();
        assertThat(machine.host()).isEqualTo("BARREL");
        assertThat(machine.inputSlots()).isZero();
        assertThat(machine.fuelSlots()).isEqualTo(1);
        assertThat(machine.outputSlots()).isEqualTo(DrillTable.OUTPUT_SLOTS);
    }

    private static void finishBurning(DrillState drill) {
        drill.advance(drill.definition().spec().cycleTicks());
        drill.finishCycle(true);
    }
}
