package io.corefactory.paper.machine;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Machine;
import io.corefactory.core.process.Portion;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessDefinition;
import io.corefactory.paper.ShippedContent;
import io.corefactory.paper.machine.HopperRules.Action;
import io.corefactory.paper.machine.MachineSlots.Role;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/** The hopper decisions, against the shipped processes.yml. Nothing here starts a server. */
class HopperRulesTest {

    private static final ItemRef RAW_GALENA = ItemRef.item("raw_galena");
    private static final ItemRef CRUSHED_GALENA = ItemRef.item("crushed_galena");
    private static final ItemRef COAL = ItemRef.material("COAL");
    private static final ItemRef CHARCOAL = ItemRef.material("CHARCOAL");
    private static final ItemRef WATER_BUCKET = ItemRef.material("WATER_BUCKET");
    private static final ItemRef CRUSHED_URANINITE = ItemRef.item("crushed_uraninite");
    private static final ItemRef COBBLESTONE = ItemRef.material("COBBLESTONE");

    private static ProcessCatalog catalog;

    @BeforeAll
    static void load() {
        catalog = ShippedContent.load().processes();
    }

    private static Predicate<ItemRef> takesInputsOf(String machine) {
        return ref -> catalog.acceptsInput(machine, ref);
    }

    @Test
    void aProcessInputGoesIntoItsMachine() {
        assertThat(HopperRules.roleOf(takesInputsOf("crusher"), false, RAW_GALENA)).contains(Role.INPUT);
        assertThat(HopperRules.roleOf(takesInputsOf("purifier"), false, WATER_BUCKET)).contains(Role.INPUT);
        assertThat(HopperRules.roleOf(takesInputsOf("roaster"), true, CRUSHED_URANINITE)).contains(Role.INPUT);
    }

    @Test
    void anItemNoProcessTakesGoesNowhere() {
        assertThat(HopperRules.roleOf(takesInputsOf("crusher"), false, CRUSHED_GALENA)).isEmpty();
        assertThat(HopperRules.roleOf(takesInputsOf("crusher"), false, COBBLESTONE)).isEmpty();
        assertThat(HopperRules.roleOf(takesInputsOf("roaster"), true, RAW_GALENA)).isEmpty();
    }

    @Test
    void fuelGoesIntoAFuelBurningMachineOnly() {
        assertThat(HopperRules.roleOf(takesInputsOf("roaster"), true, COAL)).contains(Role.FUEL);
        assertThat(HopperRules.roleOf(takesInputsOf("roaster"), true, CHARCOAL)).contains(Role.FUEL);
        assertThat(HopperRules.roleOf(takesInputsOf("crusher"), false, COAL)).isEmpty();
    }

    @Test
    void anInputWinsOverFuelWhenAnItemIsBoth() {
        assertThat(HopperRules.roleOf(ref -> true, true, COAL)).contains(Role.INPUT);
    }

    @Test
    void anItemIntoAMachineWithNoRoomOrNoSlotIsRefused() {
        assertThat(HopperRules.intake(Optional.empty(), true)).isEqualTo(Action.REFUSE);
        assertThat(HopperRules.intake(Optional.of(Role.INPUT), false)).isEqualTo(Action.REFUSE);
        assertThat(HopperRules.intake(Optional.of(Role.FUEL), false)).isEqualTo(Action.REFUSE);
    }

    @Test
    void anItemWithARoleAndASlotGoesInThatRole() {
        assertThat(HopperRules.intake(Optional.of(Role.INPUT), true)).isEqualTo(Action.PUT_INPUT);
        assertThat(HopperRules.intake(Optional.of(Role.FUEL), true)).isEqualTo(Action.PUT_FUEL);
    }

    @Test
    void anOutputLeavesOnlyWhenTheDestinationTakesIt() {
        assertThat(HopperRules.outflow(true, true)).isEqualTo(Action.TAKE_OUTPUT);
        assertThat(HopperRules.outflow(true, false)).isEqualTo(Action.REFUSE);
        assertThat(HopperRules.outflow(false, true)).isEqualTo(Action.REFUSE);
    }

    @Test
    void onlyAProductOfTheLabLeavesByAHopper() {
        List<ProcessDefinition> crusher = catalog.processesFor("crusher");
        assertThat(HopperRules.isProduct(CRUSHED_GALENA, crusher)).isTrue();
        assertThat(HopperRules.isProduct(RAW_GALENA, crusher)).isFalse();
        assertThat(HopperRules.isProduct(COBBLESTONE, crusher)).isFalse();
    }

    @Test
    void theInsertionSlotIsAPartialStackOfTheSameItemFirst() {
        List<ItemRef> held = Arrays.asList(null, CRUSHED_GALENA, CRUSHED_GALENA);
        List<Integer> counts = List.of(0, 3, 64);
        assertThat(HopperRules.insertionSlot(held, counts, CRUSHED_GALENA, 64)).isEqualTo(OptionalInt.of(1));
    }

    @Test
    void theInsertionSlotFallsBackToTheFirstEmptySlot() {
        List<ItemRef> held = Arrays.asList(COBBLESTONE, null, CRUSHED_GALENA);
        List<Integer> counts = List.of(1, 0, 64);
        assertThat(HopperRules.insertionSlot(held, counts, CRUSHED_GALENA, 64)).isEqualTo(OptionalInt.of(1));
    }

    @Test
    void aFullSlotOfTheSameItemIsSkipped() {
        List<ItemRef> held = Arrays.asList(CRUSHED_GALENA, null);
        List<Integer> counts = List.of(64, 0);
        assertThat(HopperRules.insertionSlot(held, counts, CRUSHED_GALENA, 64)).isEqualTo(OptionalInt.of(1));
    }

    @Test
    void noRoomMeansNoInsertionSlot() {
        List<ItemRef> held = Arrays.asList(COBBLESTONE, CRUSHED_GALENA);
        List<Integer> counts = List.of(1, 64);
        assertThat(HopperRules.insertionSlot(held, counts, CRUSHED_GALENA, 64)).isEmpty();
    }

    private static final ItemRef YELLOWCAKE = ItemRef.item("yellowcake");
    private static final ItemRef CRUSHED_FLUORITE = ItemRef.item("crushed_fluorite");

    @Test
    void aDepletedInputSlotIsNotFilledByTheOtherIngredient() {
        List<ProcessDefinition> converter = catalog.processesFor("converter");
        List<ItemRef> held = Arrays.asList(null, CRUSHED_FLUORITE);
        List<Integer> counts = List.of(0, 64);
        assertThat(HopperRules.insertionSlot(held, counts, CRUSHED_FLUORITE, 64, converter)).isEmpty();
        assertThat(HopperRules.insertionSlot(held, counts, YELLOWCAKE, 64, converter)).isEqualTo(OptionalInt.of(0));
    }

    @Test
    void aFirstIngredientGoesToItsOwnSlotAndNotTheOthers() {
        List<ProcessDefinition> converter = catalog.processesFor("converter");
        List<ItemRef> empty = Arrays.asList(null, null);
        List<Integer> none = List.of(0, 0);
        assertThat(HopperRules.insertionSlot(empty, none, CRUSHED_FLUORITE, 64, converter)).isEqualTo(OptionalInt.of(1));
        assertThat(HopperRules.insertionSlot(empty, none, YELLOWCAKE, 64, converter)).isEqualTo(OptionalInt.of(0));
    }

    @Test
    void aThirdCopyGoesToItsOwnSlotNeverToTheOther() {
        List<ProcessDefinition> converter = catalog.processesFor("converter");
        List<ItemRef> partial = Arrays.asList(YELLOWCAKE, CRUSHED_FLUORITE);
        assertThat(HopperRules.insertionSlot(partial, List.of(5, 64), YELLOWCAKE, 64, converter)).isEqualTo(OptionalInt.of(0));
        List<ItemRef> full = Arrays.asList(YELLOWCAKE, CRUSHED_FLUORITE);
        assertThat(HopperRules.insertionSlot(full, List.of(64, 64), YELLOWCAKE, 64, converter)).isEmpty();
        assertThat(HopperRules.insertionSlot(full, List.of(64, 64), CRUSHED_FLUORITE, 64, converter)).isEmpty();
    }

    /** For every machine process with two or more distinct inputs: each input gets in from empty, and the others keep a slot after it. */
    @Test
    void everyMultiInputRecipeKeepsASlotForEachOfItsIngredients() {
        int checked = 0;
        for (Machine machine : catalog.machines()) {
            List<ProcessDefinition> processes = catalog.processesFor(machine.id());
            for (ProcessDefinition process : processes) {
                List<ItemRef> ingredients = process.inputs().stream().map(Portion::ref).distinct().toList();
                if (ingredients.size() < 2) {
                    continue;
                }
                checked++;
                int slots = machine.inputSlots();
                for (ItemRef first : ingredients) {
                    List<ItemRef> held = new ArrayList<>(Collections.nCopies(slots, (ItemRef) null));
                    List<Integer> counts = new ArrayList<>(Collections.nCopies(slots, 0));
                    OptionalInt placed = HopperRules.insertionSlot(held, counts, first, 64, processes);
                    assertThat(placed).as(machine.id() + " " + process.id() + " takes " + first).isPresent();
                    held.set(placed.getAsInt(), first);
                    counts.set(placed.getAsInt(), 64);
                    for (ItemRef other : ingredients) {
                        if (!other.equals(first)) {
                            assertThat(HopperRules.insertionSlot(held, counts, other, 64, processes))
                                    .as(machine.id() + " " + process.id() + ": " + other + " keeps a slot after " + first)
                                    .isPresent();
                        }
                    }
                    OptionalInt again = HopperRules.insertionSlot(held, counts, first, 64, processes);
                    if (again.isPresent() && machineIngredients(processes) <= slots) {
                        int slot = again.getAsInt();
                        assertThat(held.get(slot)).as(machine.id() + " overflow of " + first + " lands on an empty slot")
                                .isNull();
                        held.set(slot, first);
                        counts.set(slot, 1);
                        for (ItemRef other : ingredients) {
                            if (!other.equals(first)) {
                                assertThat(HopperRules.insertionSlot(held, counts, other, 64, processes))
                                        .as(machine.id() + " " + process.id() + ": " + other + " keeps a slot after an overflow")
                                        .isPresent();
                            }
                        }
                    }
                }
            }
        }
        assertThat(checked).isPositive();
    }

    private static int machineIngredients(List<ProcessDefinition> processes) {
        return processes.stream().flatMap(process -> process.inputs().stream()).map(Portion::ref).distinct().toList().size();
    }

    @Test
    void theShippedMachinesTakeTheInputsOfTheirOwnProcesses() {
        assertThat(HopperRules.roleOf(takesInputsOf("spectrometer"), false, CRUSHED_GALENA)).contains(Role.INPUT);
        assertThat(HopperRules.roleOf(takesInputsOf("spectrometer"), false, COAL)).isEmpty();
        assertThat(HopperRules.roleOf(takesInputsOf("transmuter"), false, ItemRef.item("neutron_flux_cell")))
                .contains(Role.INPUT);
        assertThat(HopperRules.roleOf(takesInputsOf("transmuter"), false, RAW_GALENA)).isEmpty();
        assertThat(HopperRules.roleOf(takesInputsOf("enrichment_cascade"), false, ItemRef.item("uf6_cylinder")))
                .contains(Role.INPUT);
    }
}
