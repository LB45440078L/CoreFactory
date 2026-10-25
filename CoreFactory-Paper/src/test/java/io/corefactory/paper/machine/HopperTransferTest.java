package io.corefactory.paper.machine;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The one-item hopper transfer over slots, and the conservation of items across repeated moves. The slots are in-memory fakes,
 * so nothing here starts a server.
 */
class HopperTransferTest {

    private static final ItemRef GALENA = ItemRef.item("crushed_galena");
    private static final ItemRef COAL = ItemRef.material("COAL");
    private static final int LIMIT = 64;

    /** A slot held in memory. A slot that drops writes, or caps its count, behaves like a container that refuses them. */
    private static final class FakeSlot implements Stack {

        private ItemRef ref;
        private int count;
        private final boolean dropsWrites;
        private final int capacity;

        FakeSlot(ItemRef ref, int count) {
            this(ref, count, false, LIMIT);
        }

        FakeSlot(ItemRef ref, int count, boolean dropsWrites, int capacity) {
            this.ref = count > 0 ? ref : null;
            this.count = count;
            this.dropsWrites = dropsWrites;
            this.capacity = capacity;
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
        public void set(ItemRef ref, int count) {
            if (dropsWrites) {
                return;
            }
            if (ref == null || count <= 0) {
                this.ref = null;
                this.count = 0;
                return;
            }
            this.ref = ref;
            this.count = Math.min(count, capacity);
        }
    }

    @Test
    void exactlyOneItemMovesIntoAnEmptySlot() {
        FakeSlot slot = new FakeSlot(null, 0);

        assertThat(HopperTransfer.placeOne(1, slot, GALENA, LIMIT)).isTrue();

        assertThat(slot.count()).isEqualTo(1);
        assertThat(slot.ref()).isEqualTo(GALENA);
    }

    @Test
    void exactlyOneItemMovesOnTopOfTheSameItem() {
        FakeSlot slot = new FakeSlot(GALENA, 3);

        assertThat(HopperTransfer.placeOne(1, slot, GALENA, LIMIT)).isTrue();

        assertThat(slot.count()).isEqualTo(4);
    }

    @Test
    void insertionIntoAFullSlotIsRefusedAndNothingChanges() {
        FakeSlot slot = new FakeSlot(GALENA, LIMIT);

        assertThat(HopperTransfer.placeOne(1, slot, GALENA, LIMIT)).isFalse();

        assertThat(slot.count()).isEqualTo(LIMIT);
    }

    @Test
    void insertionIntoASlotOfAnotherItemIsRefused() {
        FakeSlot slot = new FakeSlot(COAL, 2);

        assertThat(HopperTransfer.placeOne(1, slot, GALENA, LIMIT)).isFalse();

        assertThat(slot.ref()).isEqualTo(COAL);
        assertThat(slot.count()).isEqualTo(2);
    }

    @Test
    void aSlotThatDoesNotTakeTheItemIsRestoredAndTheInsertionRefused() {
        FakeSlot slot = new FakeSlot(GALENA, 1, false, 1);

        assertThat(HopperTransfer.placeOne(1, slot, GALENA, LIMIT)).isFalse();

        assertThat(slot.ref()).isEqualTo(GALENA);
        assertThat(slot.count()).isEqualTo(1);
    }

    @Test
    void aSlotThatDropsWritesIsLeftAsItWasAndTheInsertionRefused() {
        FakeSlot slot = new FakeSlot(null, 0, true, LIMIT);

        assertThat(HopperTransfer.placeOne(1, slot, GALENA, LIMIT)).isFalse();

        assertThat(slot.count()).isZero();
    }

    @Test
    void aMoveOfMoreThanOneItemIsRefusedAndNothingIsInserted() {
        FakeSlot slot = new FakeSlot(null, 0);

        assertThat(HopperTransfer.placeOne(2, slot, GALENA, LIMIT)).isFalse();
        assertThat(HopperTransfer.placeOne(0, slot, GALENA, LIMIT)).isFalse();

        assertThat(slot.count()).isZero();
    }

    /**
     * One pass of the server's hopper move: it takes one item out of the source, the plugin places it or refuses, and a refused
     * move puts the item back into the source, as the cancelled event does.
     */
    private static void hopperMove(FakeSlot source, FakeSlot destination, ItemRef item) {
        ItemRef ref = source.ref();
        int before = source.count();
        source.set(ref, before - 1);
        if (!HopperTransfer.placeOne(1, destination, item, LIMIT)) {
            source.set(ref, before);
        }
    }

    @Test
    void aRepeatedTransferLoopNeverCreatesItems() {
        FakeSlot source = new FakeSlot(GALENA, 10);
        FakeSlot destination = new FakeSlot(null, 0);

        for (int pass = 0; pass < 40; pass++) {
            if (source.count() > 0) {
                hopperMove(source, destination, GALENA);
            }
            assertThat(source.count() + destination.count()).isEqualTo(10);
        }

        assertThat(source.count()).isZero();
        assertThat(destination.count()).isEqualTo(10);
    }

    @Test
    void aRepeatedTransferIntoAFullSlotLeavesTheSourceWhole() {
        FakeSlot source = new FakeSlot(GALENA, 5);
        FakeSlot destination = new FakeSlot(GALENA, LIMIT);

        for (int pass = 0; pass < 10; pass++) {
            hopperMove(source, destination, GALENA);
        }

        assertThat(source.count()).isEqualTo(5);
        assertThat(destination.count()).isEqualTo(LIMIT);
    }

    @Test
    void exactlyOneItemLeavesASlotThatHoldsSeveral() {
        FakeSlot slot = new FakeSlot(GALENA, 3);

        assertThat(HopperTransfer.takeOne(slot, GALENA)).isTrue();

        assertThat(slot.count()).isEqualTo(2);
        assertThat(slot.ref()).isEqualTo(GALENA);
    }

    @Test
    void theLastItemLeavesAndEmptiesTheSlot() {
        FakeSlot slot = new FakeSlot(GALENA, 1);

        assertThat(HopperTransfer.takeOne(slot, GALENA)).isTrue();

        assertThat(slot.count()).isZero();
        assertThat(slot.ref()).isNull();
    }

    @Test
    void aRemovalFromAnEmptySlotOrOfAnotherItemIsRefused() {
        FakeSlot empty = new FakeSlot(null, 0);
        FakeSlot coal = new FakeSlot(COAL, 2);

        assertThat(HopperTransfer.takeOne(empty, GALENA)).isFalse();
        assertThat(HopperTransfer.takeOne(coal, GALENA)).isFalse();

        assertThat(coal.ref()).isEqualTo(COAL);
        assertThat(coal.count()).isEqualTo(2);
    }

    @Test
    void aSlotThatDropsWritesIsLeftAsItWasAndTheRemovalRefused() {
        FakeSlot slot = new FakeSlot(GALENA, 4, true, LIMIT);

        assertThat(HopperTransfer.takeOne(slot, GALENA)).isFalse();

        assertThat(slot.count()).isEqualTo(4);
        assertThat(slot.ref()).isEqualTo(GALENA);
    }

    @Test
    void aRepeatedRemovalNeverCreatesOrLosesItems() {
        FakeSlot source = new FakeSlot(GALENA, 10);
        FakeSlot destination = new FakeSlot(null, 0);

        for (int pass = 0; pass < 15; pass++) {
            if (HopperTransfer.takeOne(source, GALENA)) {
                assertThat(HopperTransfer.placeOne(1, destination, GALENA, LIMIT)).isTrue();
            }
            assertThat(source.count() + destination.count()).isEqualTo(10);
        }

        assertThat(source.count()).isZero();
        assertThat(destination.count()).isEqualTo(10);
    }
}
