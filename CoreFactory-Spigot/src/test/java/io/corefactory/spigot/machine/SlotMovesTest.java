package io.corefactory.spigot.machine;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The hotbar swap rule and the shift-click insertion, over plain slot views. Nothing here starts a server. */
class SlotMovesTest {

    private static final ItemRef ORE = ItemRef.item("crushed_galena");
    private static final ItemRef OTHER = ItemRef.item("crushed_cinnabar");

    /** A slot view. An absent slot, like one in a barrel whose chunk is not loaded, reads as empty and takes nothing. */
    private static final class Slot implements Stack {

        private final boolean present;
        private ItemRef ref;
        private int count;

        Slot(boolean present, ItemRef ref, int count) {
            this.present = present;
            this.ref = count > 0 ? ref : null;
            this.count = count;
        }

        Slot(ItemRef ref, int count) {
            this(true, ref, count);
        }

        @Override
        public ItemRef ref() {
            return ref;
        }

        @Override
        public int count() {
            return count;
        }

        @Override
        public void set(ItemRef newRef, int newCount) {
            if (!present) {
                return;
            }
            ref = newRef == null || newCount <= 0 ? null : newRef;
            count = ref == null ? 0 : newCount;
        }
    }

    private static int total(List<Slot> slots) {
        int total = 0;
        for (Slot slot : slots) {
            total += slot.count();
        }
        return total;
    }

    @Test
    void aHotbarSwapIsAllowedOverAnInputOrFuelSlotForAnAcceptedItemOnAHotbarKey() {
        for (int key = 0; key < 9; key++) {
            assertThat(SlotMoves.hotbarSwapAllowed(MachineLayout.Kind.INPUT, key, true)).isTrue();
            assertThat(SlotMoves.hotbarSwapAllowed(MachineLayout.Kind.FUEL, key, true)).isTrue();
        }
    }

    @Test
    void aHotbarSwapIsRefusedOverAnOutputSlot() {
        for (int key = 0; key < 9; key++) {
            assertThat(SlotMoves.hotbarSwapAllowed(MachineLayout.Kind.OUTPUT, key, true)).isFalse();
        }
    }

    @Test
    void aHotbarSwapIsRefusedForAnItemTheSlotDoesNotTake() {
        assertThat(SlotMoves.hotbarSwapAllowed(MachineLayout.Kind.INPUT, 0, false)).isFalse();
        assertThat(SlotMoves.hotbarSwapAllowed(MachineLayout.Kind.FUEL, 8, false)).isFalse();
    }

    @Test
    void aHotbarSwapIsRefusedForAKeyOutsideTheHotbarOrANonSlotKind() {
        assertThat(SlotMoves.hotbarSwapAllowed(MachineLayout.Kind.INPUT, 9, true)).isFalse();
        assertThat(SlotMoves.hotbarSwapAllowed(MachineLayout.Kind.INPUT, -1, true)).isFalse();
        assertThat(SlotMoves.hotbarSwapAllowed(MachineLayout.Kind.PROGRESS, 0, true)).isFalse();
        assertThat(SlotMoves.isHotbarKey(40)).isFalse();
    }

    @Test
    void aShiftClickFillsTheStackThatHoldsTheItemBeforeAnEmptyOne() {
        List<Slot> slots = List.of(new Slot(ORE, 3), new Slot(null, 0));

        int added = SlotMoves.insert(new ArrayList<>(slots), ORE, 20, 16);

        assertThat(added).isEqualTo(20);
        assertThat(slots.get(0).count()).isEqualTo(16);
        assertThat(slots.get(1).count()).isEqualTo(7);
    }

    @Test
    void aForeignItemIsNotMixedIntoAStack() {
        List<Slot> slots = List.of(new Slot(OTHER, 4), new Slot(null, 0));

        int added = SlotMoves.insert(new ArrayList<>(slots), ORE, 5, 16);

        assertThat(added).isEqualTo(5);
        assertThat(slots.get(0).ref()).isEqualTo(OTHER);
        assertThat(slots.get(0).count()).isEqualTo(4);
        assertThat(slots.get(1).count()).isEqualTo(5);
    }

    @Test
    void noSlotHoldsMoreThanItsLimitAndTheRestIsLeftForThePlayer() {
        List<Slot> slots = List.of(new Slot(null, 0), new Slot(null, 0));

        int added = SlotMoves.insert(new ArrayList<>(slots), ORE, 40, 16);

        assertThat(added).isEqualTo(32);
        assertThat(slots.get(0).count()).isEqualTo(16);
        assertThat(slots.get(1).count()).isEqualTo(16);
        assertThat(40 - added).isEqualTo(8);
    }

    @Test
    void theAmountTakenFromThePlayerEqualsTheAmountAddedToTheMachine() {
        List<Slot> slots = List.of(new Slot(ORE, 15), new Slot(null, 0), new Slot(OTHER, 2), new Slot(null, 0));
        int before = total(slots);

        int amount = 23;
        int added = SlotMoves.insert(new ArrayList<>(slots), ORE, amount, 16);
        int left = amount - added;

        assertThat(total(slots) - before).isEqualTo(added);
        assertThat(left + added).isEqualTo(amount);
        assertThat(left).isEqualTo(0);
    }

    @Test
    void aSlotThatRefusesTheWriteTakesNothingAndNothingIsLost() {
        List<Slot> slots = List.of(new Slot(false, null, 0), new Slot(null, 0));

        int added = SlotMoves.insert(new ArrayList<>(slots), ORE, 5, 16);

        assertThat(added).isEqualTo(5);
        assertThat(slots.get(0).count()).isEqualTo(0);
        assertThat(slots.get(1).count()).isEqualTo(5);
    }

    @Test
    void aFullMachineTakesNothing() {
        List<Slot> slots = List.of(new Slot(ORE, 16), new Slot(OTHER, 1));

        int added = SlotMoves.insert(new ArrayList<>(slots), ORE, 7, 16);

        assertThat(added).isEqualTo(0);
        assertThat(slots.get(0).count()).isEqualTo(16);
        assertThat(slots.get(1).count()).isEqualTo(1);
    }
}
