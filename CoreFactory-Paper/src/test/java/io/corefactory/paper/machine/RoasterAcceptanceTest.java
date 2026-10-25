package io.corefactory.paper.machine;

import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Machine;
import io.corefactory.core.process.Portion;
import io.corefactory.core.process.Product;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessDefinition;
import io.corefactory.core.process.ProcessEngine;
import io.corefactory.paper.ShippedContent;
import io.corefactory.paper.machine.MachineSlots.Role;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The roasting kiln, read from the shipped processes.yml and the shipped layout, with no server. Every input of its processes goes
 * into an input slot, both forms of charcoal and coal go into the fuel slot, its products and unrelated items are refused, and the slot
 * map the layout builds leaves every input, fuel and output slot open, with glass only on the rest.
 */
class RoasterAcceptanceTest {

    private static final String KILN = "roaster";
    private static final int LIMIT = 64;
    private static final ItemRef COAL = ItemRef.material("COAL");
    private static final ItemRef VANILLA_CHARCOAL = ItemRef.material("CHARCOAL");
    private static final ItemRef CHARCOAL_COMPONENT = ItemRef.item("charcoal");

    private static ProcessCatalog catalog;
    private static Map<String, ComponentDefinition> components;
    private static Machine machine;
    private static MachineLayout kiln;

    @BeforeAll
    static void load() {
        ShippedContent.Loaded content = ShippedContent.load();
        catalog = content.processes();
        components = content.definitions().components();
        machine = catalog.machine(KILN).orElseThrow();
        kiln = MachineLayouts.of(machine);
    }

    @Test
    void everyInputOfEveryKilnProcessIsAcceptedAndHasAnInputSlot() {
        assertThat(catalog.processesFor(KILN)).isNotEmpty();
        for (ProcessDefinition process : catalog.processesFor(KILN)) {
            for (Portion input : process.inputs()) {
                String where = process.id() + " " + input.ref();
                assertThat(catalog.acceptsInput(KILN, input.ref())).as("accepted: " + where).isTrue();
                assertThat(HopperRules.roleOf(takesInput(), takesFuel(), input.ref())).as("role of " + where).contains(Role.INPUT);
                assertThat(emptyInputSlotFor(input.ref())).as("an input slot takes " + where).isTrue();
            }
        }
    }

    @Test
    void everyCrushedOreTheKilnRoastsIsAnInputAndNotFuel() {
        List<ItemRef> crushed = kilnInputs().stream().filter(ref -> ref.name().startsWith("crushed_")).toList();
        assertThat(crushed).as("crushed inputs of the kiln").isNotEmpty();
        for (ItemRef ore : crushed) {
            assertThat(catalog.acceptsInput(KILN, ore)).as(ore.toString()).isTrue();
            assertThat(ProcessEngine.FUEL.contains(ore)).as(ore.toString()).isFalse();
            assertThat(components.containsKey(ore.name())).as("a component " + ore).isTrue();
        }
    }

    @Test
    void theFuelSlotTakesCoalAndBothFormsOfCharcoal() {
        assertThat(kiln.fuelSlots()).hasSize(1);
        for (ItemRef fuel : List.of(COAL, VANILLA_CHARCOAL, CHARCOAL_COMPONENT)) {
            assertThat(HopperRules.roleOf(takesInput(), takesFuel(), fuel)).as(fuel.toString()).contains(Role.FUEL);
        }
        assertThat(components.containsKey("charcoal")).as("the charcoal component is defined").isTrue();
    }

    @Test
    void theKilnRefusesItsOwnProductsAndUnrelatedItems() {
        for (ProcessDefinition process : catalog.processesFor(KILN)) {
            for (Product product : process.outputs()) {
                assertThat(HopperRules.roleOf(takesInput(), takesFuel(), product.ref()))
                        .as("product " + product.ref()).isEmpty();
            }
        }
        for (ItemRef other : List.of(ItemRef.item("raw_uraninite"), ItemRef.material("STONE"),
                ItemRef.material("IRON_INGOT"), ItemRef.material("DIAMOND"))) {
            assertThat(HopperRules.roleOf(takesInput(), takesFuel(), other)).as(other.toString()).isEmpty();
        }
    }

    @Test
    void theLayoutHasTheSlotsProcessesYmlGivesIt() {
        assertThat(kiln.size()).isEqualTo(27);
        assertThat(kiln.inputSlots()).hasSize(machine.inputSlots());
        assertThat(kiln.outputSlots()).hasSize(machine.outputSlots());
        assertThat(kiln.fuelSlots()).hasSize(machine.fuelSlots());
        MachineSlots slots = MachineSlots.of(machine);
        assertThat(slots.count(Role.INPUT)).isEqualTo(kiln.inputSlots().size());
        assertThat(slots.count(Role.FUEL)).isEqualTo(kiln.fuelSlots().size());
    }

    @Test
    void noInputFuelOrOutputSlotIsCoveredByGlassOrDecorInTheBuiltSlotMap() {
        List<Integer> open = new ArrayList<>();
        open.addAll(kiln.inputSlots());
        open.addAll(kiln.fuelSlots());
        open.addAll(kiln.outputSlots());
        assertThat(open).doesNotHaveDuplicates();
        for (int slot : kiln.inputSlots()) {
            assertThat(kiln.kindAt(slot)).as("slot " + slot).isEqualTo(MachineLayout.Kind.INPUT);
        }
        for (int slot : kiln.fuelSlots()) {
            assertThat(kiln.kindAt(slot)).as("slot " + slot).isEqualTo(MachineLayout.Kind.FUEL);
        }
        for (int slot : open) {
            assertThat(kiln.decorAt(slot)).as("decor on slot " + slot).isNull();
        }
    }

    @Test
    void everyOtherSlotOfTheKilnIsFrameGlassSoNoSlotIsBare() {
        for (int slot = 0; slot < kiln.size(); slot++) {
            if (kiln.kindAt(slot) == MachineLayout.Kind.DECOR) {
                assertThat(kiln.decorAt(slot)).as("frame on slot " + slot).isEqualTo(Material.BROWN_STAINED_GLASS_PANE);
            }
        }
        assertThat(kiln.decorAt(14)).isEqualTo(Material.BROWN_STAINED_GLASS_PANE);
    }

    /** The distinct inputs of every kiln process, in the order the processes give them. */
    private static List<ItemRef> kilnInputs() {
        List<ItemRef> inputs = new ArrayList<>();
        for (ProcessDefinition process : catalog.processesFor(KILN)) {
            for (Portion input : process.inputs()) {
                if (!inputs.contains(input.ref())) {
                    inputs.add(input.ref());
                }
            }
        }
        return inputs;
    }

    /** True when an empty input slot of the layout takes the item, by the hopper's slot rule. */
    private static boolean emptyInputSlotFor(ItemRef ref) {
        List<ItemRef> held = Collections.nCopies(kiln.inputSlots().size(), (ItemRef) null);
        List<Integer> counts = Collections.nCopies(kiln.inputSlots().size(), 0);
        return HopperRules.insertionSlot(held, counts, ref, LIMIT).isPresent();
    }

    private static Predicate<ItemRef> takesInput() {
        return ref -> catalog.acceptsInput(KILN, ref);
    }

    private static Predicate<ItemRef> takesFuel() {
        return ProcessEngine.FUEL::contains;
    }
}
