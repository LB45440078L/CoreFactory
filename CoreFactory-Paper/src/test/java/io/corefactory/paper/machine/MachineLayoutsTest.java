package io.corefactory.paper.machine;

import io.corefactory.core.process.Machine;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.paper.ShippedContent;
import io.corefactory.paper.machine.MachineSlots.Role;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every lab tool's menu has the slots processes.yml gives it, every menu slot maps onto its barrel in order, and every lab tool
 * stands on a barrel in both the processes and the materials file. Read from the shipped files, with no server.
 */
class MachineLayoutsTest {

    private static ProcessCatalog processes;

    @BeforeAll
    static void load() {
        processes = ShippedContent.load().processes();
    }

    @Test
    void everyShippedMenuHasTheSlotCountsProcessesYmlGivesItsMachine() {
        for (Machine machine : processes.machines()) {
            MachineLayout layout = MachineLayouts.of(machine);
            assertThat(layout.inputSlots()).as(machine.id()).hasSize(machine.inputSlots());
            assertThat(layout.outputSlots()).as(machine.id()).hasSize(machine.outputSlots());
            assertThat(layout.fuelSlots()).as(machine.id()).hasSize(machine.fuelSlots());
        }
    }

    @Test
    void everyMenuSlotIsInsideTheMenuAndHasOneRole() {
        for (Machine machine : processes.machines()) {
            MachineLayout layout = MachineLayouts.of(machine);
            List<Integer> all = new ArrayList<>();
            all.addAll(layout.inputSlots());
            all.addAll(layout.outputSlots());
            all.addAll(layout.fuelSlots());
            assertThat(new HashSet<>(all)).as(machine.id()).hasSize(all.size());
            assertThat(all).as(machine.id()).allMatch(slot -> slot >= 0 && slot < layout.size());
        }
    }

    @Test
    void everyMachineFitsItsBarrelAndItsMenuSlotsMapOntoItInOrder() {
        for (Machine machine : processes.machines()) {
            MachineLayout layout = MachineLayouts.of(machine);
            MachineSlots slots = MachineSlots.of(machine);
            assertThat(slots.used()).as(machine.id()).isLessThanOrEqualTo(MachineSlots.BARREL_SIZE);
            assertMapping(layout, slots, Role.INPUT, layout.inputSlots());
            assertMapping(layout, slots, Role.OUTPUT, layout.outputSlots());
            assertMapping(layout, slots, Role.FUEL, layout.fuelSlots());
        }
    }

    @Test
    void theResearchConsolesSixPackSlotsAreBarrelSlotsZeroToFive() {
        Machine console = processes.machine("research_console").orElseThrow();
        MachineLayout layout = MachineLayouts.of(console);
        assertThat(layout.inputSlots()).hasSize(6);
        assertThat(MachineSlots.of(console).indexes(Role.INPUT)).containsExactly(0, 1, 2, 3, 4, 5);
    }

    @Test
    void everyShippedHostIsABarrel() {
        for (Machine machine : processes.machines()) {
            assertThat(machine.host()).as(machine.id()).isEqualTo("BARREL");
        }
    }

    @Test
    void everyShippedMachineComponentStandsOnABarrel() throws Exception {
        YamlConfiguration materials;
        try (Reader reader = new InputStreamReader(
                MachineLayoutsTest.class.getClassLoader().getResourceAsStream("materials.yml"), StandardCharsets.UTF_8)) {
            materials = YamlConfiguration.loadConfiguration(reader);
        }
        ConfigurationSection components = materials.getConfigurationSection("components");
        for (Machine machine : processes.machines()) {
            ConfigurationSection component = components.getConfigurationSection(machine.id());
            assertThat(component).as(machine.id()).isNotNull();
            assertThat(component.getString("material")).as(machine.id()).isEqualTo("BARREL");
        }
    }

    /** The menu's slots of one role, in their numbered order, sit on the barrel slots that the role maps to. */
    private static void assertMapping(MachineLayout layout, MachineSlots slots, Role role, List<Integer> menuSlots) {
        assertThat(slots.count(role)).isEqualTo(menuSlots.size());
        for (int i = 0; i < menuSlots.size(); i++) {
            int menuSlot = menuSlots.get(i);
            assertThat(layout.kindAt(menuSlot).name()).isEqualTo(role.name());
            assertThat(layout.indexAt(menuSlot)).isEqualTo(i);
            int barrel = slots.barrelIndex(role, i);
            assertThat(slots.roleAt(barrel)).contains(role);
            assertThat(slots.indexAt(barrel)).isEqualTo(i);
        }
    }
}
