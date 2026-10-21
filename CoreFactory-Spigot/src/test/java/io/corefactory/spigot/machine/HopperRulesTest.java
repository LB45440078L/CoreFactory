package io.corefactory.spigot.machine;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessDefinition;
import io.corefactory.spigot.ShippedContent;
import io.corefactory.spigot.machine.HopperRules.Action;
import io.corefactory.spigot.machine.MachineSlots.Role;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
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
