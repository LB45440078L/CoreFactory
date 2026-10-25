package io.corefactory.core.logistics;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.corefactory.core.logistics.Fixtures.A;
import static io.corefactory.core.logistics.Fixtures.B;
import static io.corefactory.core.logistics.Fixtures.C;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CrafterGridTest {

    private static final int LIMIT = 64;
    private static final ItemRef ITEM_A = ItemRef.material("A");
    private static final ItemRef ITEM_B = ItemRef.material("B");
    private static final ItemRef ITEM_C = ItemRef.material("C");

    /** A grid of nine empty slots with none disabled. */
    private static List<Stack> emptySlots() {
        List<Stack> slots = new ArrayList<>();
        for (int i = 0; i < CrafterGrid.SLOTS; i++) {
            slots.add(MemorySlot.empty());
        }
        return slots;
    }

    private static CrafterGrid grid(List<Stack> slots, boolean... disabled) {
        boolean[] flags = new boolean[CrafterGrid.SLOTS];
        for (int i = 0; i < disabled.length; i++) {
            flags[i] = disabled[i];
        }
        return new CrafterGrid(slots, flags, ref -> LIMIT);
    }

    @Test
    void aPickerOffersTheHeldItemsInSlotOrder() {
        List<Stack> slots = emptySlots();
        slots.set(2, MemorySlot.of(ITEM_B, 1));
        slots.set(0, MemorySlot.of(ITEM_A, 2));
        CrafterGrid grid = grid(slots);

        assertThat(grid.peekOutput()).isEqualTo(A);
        assertThat(grid.takeOutput()).isEqualTo(A);
        assertThat(grid.takeOutput()).isEqualTo(A);
        assertThat(grid.takeOutput()).isEqualTo(B);
        assertThat(grid.takeOutput()).isNull();
        assertThat(grid.storedCount()).isZero();
    }

    @Test
    void aDisabledSlotIsNotOfferedAndItsItemsStayPut() {
        List<Stack> slots = emptySlots();
        slots.set(0, MemorySlot.of(ITEM_C, 3));
        slots.set(1, MemorySlot.of(ITEM_A, 1));
        CrafterGrid grid = grid(slots, true, false);

        assertThat(grid.peekOutput()).isEqualTo(A);
        assertThat(grid.takeOutput()).isEqualTo(A);
        assertThat(grid.takeOutput()).isNull();
        assertThat(grid.storedCount()).isEqualTo(3);
        assertThat(slots.get(0).count()).isEqualTo(3);
    }

    @Test
    void aSlotWithAnItemIsOfferedOnlyWhileItIsEnabled() {
        List<Stack> slots = emptySlots();
        slots.set(4, MemorySlot.of(ITEM_B, 1));

        assertThat(grid(slots).peekOutput()).isEqualTo(B);
        assertThat(grid(slots, false, false, false, false, true).peekOutput()).isNull();
    }

    @Test
    void aNamedStackIsNotTakenByAPicker() {
        List<Stack> slots = emptySlots();
        slots.set(0, new NamedSlot(1));
        slots.set(1, MemorySlot.of(ITEM_B, 1));
        CrafterGrid grid = grid(slots);

        assertThat(grid.peekOutput()).isEqualTo(B);
    }

    @Test
    void anItemGoesIntoTheFirstEnabledSlotThatHoldsItWithRoom() {
        List<Stack> slots = emptySlots();
        slots.set(0, MemorySlot.of(ITEM_A, LIMIT));
        slots.set(1, MemorySlot.of(ITEM_A, 5));
        CrafterGrid grid = grid(slots);

        grid.accept(A);

        assertThat(slots.get(0).count()).isEqualTo(LIMIT);
        assertThat(slots.get(1).count()).isEqualTo(6);
    }

    @Test
    void anItemGoesIntoTheFirstEnabledEmptySlotWhenNoSlotHoldsIt() {
        List<Stack> slots = emptySlots();
        slots.set(0, MemorySlot.of(ITEM_C, 1));
        CrafterGrid grid = grid(slots, true, false);

        grid.accept(B);

        assertThat(slots.get(0).count()).isEqualTo(1);
        assertThat(slots.get(1).ref()).isEqualTo(ITEM_B);
        assertThat(slots.get(1).count()).isEqualTo(1);
    }

    @Test
    void anItemNeverGoesIntoADisabledSlotEvenWhenItIsTheFirstEmptyOne() {
        List<Stack> slots = emptySlots();
        CrafterGrid grid = grid(slots, true, true, false);

        grid.accept(A);

        assertThat(slots.get(0).count()).isZero();
        assertThat(slots.get(1).count()).isZero();
        assertThat(slots.get(2).ref()).isEqualTo(ITEM_A);
    }

    @Test
    void aFullCrafterAcceptsNothing() {
        List<Stack> slots = new ArrayList<>();
        for (int i = 0; i < CrafterGrid.SLOTS; i++) {
            slots.add(MemorySlot.of(ITEM_C, LIMIT));
        }
        CrafterGrid grid = grid(slots);

        assertThat(grid.accepts(A)).isFalse();
        assertThat(grid.accepts(C)).isFalse();
        assertThatThrownBy(() -> grid.accept(A)).isInstanceOf(IllegalStateException.class);
        assertThat(grid.storedCount()).isEqualTo(CrafterGrid.SLOTS * LIMIT);
    }

    @Test
    void aGridWhoseOnlyRoomIsInDisabledSlotsAcceptsNothing() {
        List<Stack> slots = emptySlots();
        CrafterGrid grid = grid(slots, true, true, true, true, true, true, true, true, true);

        assertThat(grid.accepts(A)).isFalse();
        assertThat(grid.peekOutput()).isNull();
    }

    @Test
    void anItemOnlyGoesIntoAnEmptySlotWhenNoSlotHoldsAnotherItem() {
        List<Stack> slots = new ArrayList<>();
        for (int i = 0; i < CrafterGrid.SLOTS; i++) {
            slots.add(MemorySlot.of(ITEM_C, 1));
        }
        slots.set(4, MemorySlot.empty());
        CrafterGrid grid = grid(slots);

        grid.accept(B);

        assertThat(slots.get(4).ref()).isEqualTo(ITEM_B);
        assertThat(grid.accepts(A)).isFalse();
    }

    @Test
    void aStackLimitOfOneSpreadsItemsOverSlots() {
        List<Stack> slots = emptySlots();
        CrafterGrid grid = new CrafterGrid(slots, new boolean[CrafterGrid.SLOTS], ref -> 1);

        grid.accept(A);
        grid.accept(A);

        assertThat(slots.get(0).count()).isEqualTo(1);
        assertThat(slots.get(1).count()).isEqualTo(1);
    }

    @Test
    void theGridNeedsExactlyNineSlotsAndFlags() {
        List<Stack> eight = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            eight.add(MemorySlot.empty());
        }
        assertThatThrownBy(() -> new CrafterGrid(eight, new boolean[CrafterGrid.SLOTS], ref -> LIMIT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CrafterGrid(emptySlots(), new boolean[8], ref -> LIMIT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void componentsAreStoredAsComponentReferences() {
        List<Stack> slots = emptySlots();
        CrafterGrid grid = grid(slots);

        grid.accept(Item.component("gear"));

        assertThat(slots.get(0).ref()).isEqualTo(ItemRef.item("gear"));
        assertThat(grid.peekOutput()).isEqualTo(Item.component("gear"));
    }

    /** A slot holding a named stack: it has items, but its ref reads as null, as a changed stack does on the server. */
    private static final class NamedSlot implements Stack {

        private int count;

        NamedSlot(int count) {
            this.count = count;
        }

        @Override
        public ItemRef ref() {
            return null;
        }

        @Override
        public int count() {
            return count;
        }

        @Override
        public void set(ItemRef ref, int count) {
            this.count = Math.max(0, count);
        }
    }
}
