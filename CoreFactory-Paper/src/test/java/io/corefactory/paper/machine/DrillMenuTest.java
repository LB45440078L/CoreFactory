package io.corefactory.paper.machine;

import io.corefactory.core.drill.DrillKind;
import io.corefactory.core.drill.DrillTable;
import io.corefactory.core.process.ItemRef;
import io.corefactory.paper.machine.HopperRules.Action;
import io.corefactory.paper.machine.MachineLayout.Kind;
import io.corefactory.paper.machine.MachineSlots.Role;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The drill menu and barrel order, and the hopper decisions at a drill: the fuel slot first, the output grid after it, and
 * nothing accepted but the drill's own fuel. Nothing here starts a server.
 */
class DrillMenuTest {

    private static final ItemRef CHARCOAL = ItemRef.material("CHARCOAL");
    private static final ItemRef COAL = ItemRef.material("COAL");
    private static final ItemRef PETROLEUM = ItemRef.item("petroleum");
    private static final ItemRef PURIFIED_IRON = ItemRef.item("purified_iron");
    private static final ItemRef RAW_GALENA = ItemRef.item("raw_galena");
    private static final Predicate<ItemRef> NO_INPUTS = ref -> false;

    @Test
    void aDrillBarrelHasTheFuelSlotFirstAndTheOutputsAfterIt() {
        MachineSlots slots = MachineSlots.drill(DrillTable.OUTPUT_SLOTS);
        assertThat(slots.count(Role.INPUT)).isZero();
        assertThat(slots.indexes(Role.FUEL)).containsExactly(0);
        assertThat(slots.indexes(Role.OUTPUT)).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertThat(slots.used()).isEqualTo(10);
        assertThat(slots.roleAt(0)).contains(Role.FUEL);
        assertThat(slots.roleAt(1)).contains(Role.OUTPUT);
        assertThat(slots.roleAt(9)).contains(Role.OUTPUT);
        assertThat(slots.roleAt(10)).isEmpty();
        assertThat(slots.indexAt(1)).isZero();
    }

    @Test
    void aLabToolKeepsItsOwnOrderOfInputsOutputsAndFuel() {
        MachineSlots slots = new MachineSlots(2, 3, 1);
        assertThat(slots.indexes(Role.INPUT)).containsExactly(0, 1);
        assertThat(slots.indexes(Role.OUTPUT)).containsExactly(2, 3, 4);
        assertThat(slots.indexes(Role.FUEL)).containsExactly(5);
    }

    @Test
    void aPetroleumDrillMenuHasOneCharcoalSlotNoOutputsAndAnInfoItemAndAnOreDrillTheReverse() {
        for (DrillKind kind : DrillKind.values()) {
            MachineLayout layout = MachineLayouts.drill("&8Test", kind);
            assertThat(layout.inputSlots()).isEmpty();
            assertThat(layout.fuelSlots()).hasSize(kind == DrillKind.PETROLEUM ? 1 : 0);
            assertThat(layout.outputSlots()).hasSize(kind == DrillKind.ORE ? DrillTable.OUTPUT_SLOTS : 0);
            assertThat(layout.infoSlot()).isEqualTo(4);
            assertThat(layout.kindAt(4)).isEqualTo(Kind.INFO);
            assertThat(layout.statusSlot()).isEqualTo(-1);
        }
    }

    @Test
    void everyDrillMenuSlotIsInsideTheMenuAndHasOneRole() {
        for (DrillKind kind : DrillKind.values()) {
            MachineLayout layout = MachineLayouts.drill("&8Test", kind);
            List<Integer> all = new ArrayList<>();
            all.addAll(layout.fuelSlots());
            all.addAll(layout.outputSlots());
            all.add(layout.infoSlot());
            all.addAll(layout.progressSlots());
            assertThat(new HashSet<>(all)).as(kind.name()).hasSize(all.size());
            assertThat(all).as(kind.name()).allMatch(slot -> slot >= 0 && slot < layout.size());
        }
    }

    @Test
    void theDrillMenuSlotsMapOntoTheDrillBarrelInOrder() {
        for (DrillKind kind : DrillKind.values()) {
            MachineLayout layout = MachineLayouts.drill("&8Test", kind);
            MachineSlots slots = new MachineSlots(0, layout.outputSlots().size(), layout.fuelSlots().size(), true);
            assertMapping(layout, slots, Role.FUEL, layout.fuelSlots());
            assertMapping(layout, slots, Role.OUTPUT, layout.outputSlots());
        }
    }

    @Test
    void aPetroleumDrillTakesCharcoalIntoItsFuelSlotAndNothingElse() {
        Predicate<ItemRef> fuel = DrillKind.PETROLEUM.fuel().orElseThrow()::equals;
        assertThat(HopperRules.roleOf(NO_INPUTS, fuel, CHARCOAL)).contains(Role.FUEL);
        assertThat(HopperRules.roleOf(NO_INPUTS, fuel, COAL)).isEmpty();
        assertThat(HopperRules.roleOf(NO_INPUTS, fuel, PETROLEUM)).isEmpty();
        assertThat(HopperRules.roleOf(NO_INPUTS, fuel, RAW_GALENA)).isEmpty();
        assertThat(HopperRules.roleOf(NO_INPUTS, fuel, PURIFIED_IRON)).isEmpty();
    }

    @Test
    void anOreDrillTakesNoItemIntoAFuelSlotBecauseItsPetroleumComesFromTheNetwork() {
        assertThat(DrillKind.ORE.fuel()).isEmpty();
        Predicate<ItemRef> noFuel = ref -> false;
        assertThat(HopperRules.roleOf(NO_INPUTS, noFuel, PETROLEUM)).isEmpty();
        assertThat(HopperRules.roleOf(NO_INPUTS, noFuel, CHARCOAL)).isEmpty();
        assertThat(HopperRules.roleOf(NO_INPUTS, noFuel, COAL)).isEmpty();
    }

    @Test
    void aFuelItemGoesIntoTheFuelSlotAndAnOutputIsNeverPutIn() {
        Predicate<ItemRef> fuel = DrillKind.PETROLEUM.fuel().orElseThrow()::equals;
        Optional<Role> role = HopperRules.roleOf(NO_INPUTS, fuel, CHARCOAL);
        OptionalInt slot = HopperRules.insertionSlot(List.of(), List.of(), CHARCOAL, 64);
        assertThat(HopperRules.intake(role, slot.isPresent())).isEqualTo(Action.REFUSE);
        assertThat(HopperRules.intake(role, true)).isEqualTo(Action.PUT_FUEL);
    }

    @Test
    void aFullFuelSlotRefusesFuelAndAnOutputGivesItemsUpOnlyWhenTheDestinationTakesThem() {
        Predicate<ItemRef> fuel = DrillKind.PETROLEUM.fuel().orElseThrow()::equals;
        assertThat(HopperRules.intake(HopperRules.roleOf(NO_INPUTS, fuel, CHARCOAL), false)).isEqualTo(Action.REFUSE);
        assertThat(HopperRules.outflow(true, true)).isEqualTo(Action.TAKE_OUTPUT);
        assertThat(HopperRules.outflow(true, false)).isEqualTo(Action.REFUSE);
        assertThat(HopperRules.outflow(false, true)).isEqualTo(Action.REFUSE);
    }

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
