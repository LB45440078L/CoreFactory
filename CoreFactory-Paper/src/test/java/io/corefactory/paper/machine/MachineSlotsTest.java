package io.corefactory.paper.machine;

import io.corefactory.core.drill.DrillTable;
import io.corefactory.core.process.Machine;
import io.corefactory.paper.ShippedContent;
import io.corefactory.paper.machine.MachineSlots.Role;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The order in which a lab tool's layout slots map onto its barrel. Pure, so no server is needed. */
class MachineSlotsTest {

    @Test
    void inputsComeFirstThenOutputsThenFuel() {
        MachineSlots slots = new MachineSlots(2, 3, 1);
        assertThat(slots.indexes(Role.INPUT)).containsExactly(0, 1);
        assertThat(slots.indexes(Role.OUTPUT)).containsExactly(2, 3, 4);
        assertThat(slots.indexes(Role.FUEL)).containsExactly(5);
        assertThat(slots.used()).isEqualTo(6);
    }

    @Test
    void aMachineWithoutFuelOrOutputsStillMapsInOrder() {
        MachineSlots slots = new MachineSlots(6, 0, 0);
        assertThat(slots.indexes(Role.INPUT)).containsExactly(0, 1, 2, 3, 4, 5);
        assertThat(slots.indexes(Role.OUTPUT)).isEmpty();
        assertThat(slots.indexes(Role.FUEL)).isEmpty();
    }

    @Test
    void everyBarrelSlotGivesBackItsRoleAndNumber() {
        MachineSlots slots = new MachineSlots(8, 4, 1);
        for (Role role : Role.values()) {
            for (int index = 0; index < slots.count(role); index++) {
                int barrel = slots.barrelIndex(role, index);
                assertThat(slots.roleAt(barrel)).contains(role);
                assertThat(slots.indexAt(barrel)).isEqualTo(index);
            }
        }
    }

    @Test
    void unusedBarrelSlotsHaveNoRole() {
        MachineSlots slots = new MachineSlots(2, 3, 1);
        assertThat(slots.roleAt(6)).isEmpty();
        assertThat(slots.roleAt(MachineSlots.BARREL_SIZE - 1)).isEmpty();
        assertThat(slots.roleAt(-1)).isEmpty();
        assertThat(slots.indexAt(6)).isEqualTo(-1);
    }

    @Test
    void aNumberOutsideItsRoleIsRefused() {
        MachineSlots slots = new MachineSlots(2, 3, 1);
        assertThatThrownBy(() -> slots.barrelIndex(Role.OUTPUT, 3)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> slots.barrelIndex(Role.INPUT, -1)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> slots.barrelIndex(Role.FUEL, 1)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void aMachineLargerThanABarrelIsRefused() {
        assertThat(new MachineSlots(27, 0, 0).used()).isEqualTo(MachineSlots.BARREL_SIZE);
        assertThatThrownBy(() -> new MachineSlots(14, 14, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MachineSlots(-1, 0, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void everyShippedMachineFitsABarrelAndUsesEachSlotOnce() {
        ShippedContent.Loaded content = ShippedContent.load();
        assertThat(content.processes().machines()).isNotEmpty();
        for (Machine machine : content.processes().machines()) {
            MachineSlots slots = MachineSlots.of(machine);
            Set<Integer> used = new HashSet<>();
            for (Role role : Role.values()) {
                used.addAll(slots.indexes(role));
            }
            assertThat(used).as(machine.id()).hasSize(slots.used());
            assertThat(used).as(machine.id()).allMatch(index -> index >= 0 && index < MachineSlots.BARREL_SIZE);
        }
    }

    @Test
    void aMachineFitsOnlyAContainerWithAsManySlotsAsItUses() {
        MachineSlots slots = new MachineSlots(2, 3, 1);
        assertThat(slots.fits(6)).isTrue();
        assertThat(slots.fits(MachineSlots.BARREL_SIZE)).isTrue();
        assertThat(slots.fits(5)).isFalse();
    }

    @Test
    void theDrillBarrelFitsTheBarrelButNotTheNineSlotDropper() {
        MachineSlots drill = MachineSlots.drill(DrillTable.OUTPUT_SLOTS);
        assertThat(drill.used()).isEqualTo(DrillTable.OUTPUT_SLOTS);
        assertThat(drill.fits(MachineSlots.BARREL_SIZE)).isTrue();
        assertThat(drill.fits(8)).isFalse();
    }

    @Test
    void everyDrillAndShippedMachineFitsTheBarrel() {
        assertThat(MachineSlots.BARREL_SIZE).isEqualTo(27);
        assertThat(MachineSlots.drill(DrillTable.OUTPUT_SLOTS).fits(MachineSlots.BARREL_SIZE)).isTrue();
        for (Machine machine : ShippedContent.load().processes().machines()) {
            assertThat(MachineSlots.of(machine).fits(MachineSlots.BARREL_SIZE)).as(machine.id()).isTrue();
        }
    }
}
