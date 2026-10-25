package io.corefactory.paper.machine;

import io.corefactory.core.process.Machine;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.paper.ShippedContent;
import io.corefactory.paper.machine.MachineSlots.Role;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The menus of the eight process machines: each one's size and its input and output slots, read from its layout and from
 * processes.yml. Nothing here starts a server.
 */
class ProcessMachineLayoutsTest {

    /** The menu a process machine is given: its size, and its input and output slot counts. */
    private record Expected(String id, int size, int inputs, int outputs) {
    }

    private static final List<Expected> MENUS = List.of(
            new Expected("induction_furnace", 27, 2, 1),
            new Expected("powder_mill", 27, 3, 1),
            new Expected("cartridge_press", 45, 2, 1),
            new Expected("bullet_swaging_machine", 27, 2, 1),
            new Expected("ammunition_assembly", 27, 3, 1),
            new Expected("metal_lathe", 27, 2, 1),
            new Expected("component_assembly", 27, 4, 1),
            new Expected("heavy_weapons_assembly", 54, 6, 1));

    private static ProcessCatalog processes;

    @BeforeAll
    static void load() {
        processes = ShippedContent.load().processes();
    }

    @Test
    void eachNewMenuHasItsSizeAndItsInputAndOutputSlots() {
        for (Expected expected : MENUS) {
            Machine machine = processes.machine(expected.id()).orElseThrow();
            MachineLayout layout = MachineLayouts.of(machine);
            assertThat(layout.size()).as(expected.id()).isEqualTo(expected.size());
            assertThat(layout.inputSlots()).as(expected.id()).hasSize(expected.inputs());
            assertThat(layout.outputSlots()).as(expected.id()).hasSize(expected.outputs());
        }
    }

    @Test
    void theYamlGivesEachNewMachineTheSameSlotCountsAsItsMenu() {
        for (Expected expected : MENUS) {
            Machine machine = processes.machine(expected.id()).orElseThrow();
            assertThat(machine.inputSlots()).as(expected.id()).isEqualTo(expected.inputs());
            assertThat(machine.outputSlots()).as(expected.id()).isEqualTo(expected.outputs());
            assertThat(machine.fuelSlots()).as(expected.id()).isZero();
        }
    }

    @Test
    void noNewMachineHasAFuelSlotAndEveryMenuSlotMapsOntoItsBarrel() {
        for (Expected expected : MENUS) {
            Machine machine = processes.machine(expected.id()).orElseThrow();
            MachineLayout layout = MachineLayouts.of(machine);
            MachineSlots slots = MachineSlots.of(machine);
            assertThat(layout.fuelSlots()).as(expected.id()).isEmpty();
            assertThat(slots.used()).as(expected.id()).isLessThanOrEqualTo(MachineSlots.BARREL_SIZE);
            for (int i = 0; i < layout.inputSlots().size(); i++) {
                assertThat(slots.indexAt(slots.barrelIndex(Role.INPUT, i))).as(expected.id()).isEqualTo(i);
            }
            for (int i = 0; i < layout.outputSlots().size(); i++) {
                assertThat(slots.indexAt(slots.barrelIndex(Role.OUTPUT, i))).as(expected.id()).isEqualTo(i);
            }
        }
    }

    @Test
    void thePressesAndTheHeavyAssemblyHaveTheLargestMenus() {
        int small = 27;
        assertThat(MachineLayouts.of(processes.machine("cartridge_press").orElseThrow()).size()).isGreaterThan(small);
        assertThat(MachineLayouts.of(processes.machine("heavy_weapons_assembly").orElseThrow()).size())
                .isGreaterThan(MachineLayouts.of(processes.machine("cartridge_press").orElseThrow()).size());
    }
}
