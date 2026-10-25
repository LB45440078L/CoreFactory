package io.corefactory.paper.machine;

import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Machine;
import io.corefactory.core.process.Portion;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessDefinition;
import io.corefactory.paper.ShippedContent;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every process input of every machine in processes.yml is taken by the machine's input slots: the menu's accept rule takes
 * it, the item a player holds names the same reference, and the input slots hold a whole run of it. Read from the shipped
 * files with no server.
 */
class MachineAcceptanceTest {

    private static ProcessCatalog catalog;
    private static Map<String, ComponentDefinition> components;

    @BeforeAll
    static void load() {
        ShippedContent.Loaded content = ShippedContent.load();
        catalog = content.processes();
        components = content.definitions().components();
    }

    @Test
    void everyProcessInputOfEveryMachineIsAcceptedByItsInputSlots() {
        for (Machine machine : catalog.machines()) {
            MachineLayout layout = MachineLayouts.of(machine);
            for (ProcessDefinition process : catalog.processesFor(machine.id())) {
                for (Portion input : process.inputs()) {
                    String where = machine.id() + " " + process.id() + " " + input.ref();
                    assertThat(catalog.acceptsInput(machine.id(), input.ref())).as("accepted: " + where).isTrue();
                    assertThat(layout.inputSlots()).as("input slots of " + where).isNotEmpty();
                    assertThat(heldBy(input.ref())).as("a player can hold " + where).isTrue();
                }
            }
        }
    }

    @Test
    void everyProcessRunHasAnInputSlotForEachItemItTakes() {
        for (Machine machine : catalog.machines()) {
            MachineLayout layout = MachineLayouts.of(machine);
            for (ProcessDefinition process : catalog.processesFor(machine.id())) {
                Set<ItemRef> distinct = new HashSet<>();
                for (Portion input : process.inputs()) {
                    distinct.add(input.ref());
                }
                assertThat(distinct.size()).as(machine.id() + " " + process.id() + " input slots")
                        .isLessThanOrEqualTo(layout.inputSlots().size());
            }
        }
    }

    @Test
    void theKilnTakesEveryCrushedOreItsRoastsName() {
        Machine kiln = catalog.machine("roaster").orElseThrow();
        for (String ore : new String[] {"crushed_uraninite", "crushed_pitchblende", "crushed_galena", "crushed_cinnabar"}) {
            assertThat(catalog.acceptsInput(kiln.id(), ItemRef.item(ore))).as(ore).isTrue();
        }
        assertThat(catalog.acceptsInput(kiln.id(), ItemRef.item("raw_uraninite"))).isFalse();
    }

    /** A process input is held by a player as a component of the same id, or as the vanilla material it names. */
    private static boolean heldBy(ItemRef ref) {
        if (ref.kind() == ItemRef.Kind.ITEM) {
            return components.containsKey(ref.name());
        }
        return Material.matchMaterial(ref.name()) != null;
    }
}
