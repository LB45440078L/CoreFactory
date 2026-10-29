package io.corefactory.core.drill;

import io.corefactory.core.oil.Fluid;
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
    void finishingACycleRestartsIt() {
        DrillState drill = state(DrillKind.PETROLEUM, 1);
        drill.advance(600);
        assertThat(drill.ready()).isTrue();
        drill.finishCycle();
        assertThat(drill.progress()).isZero();
        assertThat(drill.ready()).isFalse();
    }

    @Test
    void aSavedCycleIsRestoredAndClamped() {
        DrillState drill = state(DrillKind.PETROLEUM, 1);
        drill.restore(250.5);
        assertThat(drill.progress()).isEqualTo(250.5);
        drill.restore(9999);
        assertThat(drill.progress()).isEqualTo(600.0);
        drill.restore(-4);
        assertThat(drill.progress()).isZero();
    }

    @Test
    void theDefinitionOfAnOreDrillIsItsMachineWithNoFuelSlotAndNineOutputs() {
        Machine machine = new DrillDefinition("ore_drill_1", "Ore Drill", "BARREL", DrillKind.ORE,
                DrillTable.level(1)).machine();
        assertThat(machine.host()).isEqualTo("BARREL");
        assertThat(machine.inputSlots()).isZero();
        assertThat(machine.fuelSlots()).isZero();
        assertThat(machine.outputSlots()).isEqualTo(DrillTable.OUTPUT_SLOTS);
    }

    @Test
    void theDefinitionOfAPetroleumDrillIsItsMachineWithNoSlotsAtAll() {
        Machine machine = new DrillDefinition("petroleum_drill_1", "Petroleum Drill", "BARREL", DrillKind.PETROLEUM,
                DrillTable.level(1)).machine();
        assertThat(machine.inputSlots()).isZero();
        assertThat(machine.fuelSlots()).isZero();
        assertThat(machine.outputSlots()).isZero();
    }

    @Test
    void onlyAPetroleumDrillPumpsIntoTheOilNetwork() {
        assertThat(DrillKind.PETROLEUM.pumps()).contains(Fluid.PETROLEUM);
        assertThat(DrillKind.ORE.pumps()).isEmpty();
    }
}
