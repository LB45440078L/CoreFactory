package io.corefactory.core.drill;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DrillStorageTest {

    private static final ItemRef PETROLEUM = ItemRef.item("petroleum");
    private static final ItemRef RAW = ItemRef.item("raw_galena");

    /** A slot held in memory: the same contract as a barrel slot. */
    private static final class Slot implements Stack {

        private ItemRef ref;
        private int count;

        @Override
        public ItemRef ref() {
            return count > 0 ? ref : null;
        }

        @Override
        public int count() {
            return count;
        }

        @Override
        public void set(ItemRef newRef, int newCount) {
            if (newRef == null || newCount <= 0) {
                ref = null;
                count = 0;
            } else {
                ref = newRef;
                count = newCount;
            }
        }
    }

    private static List<Stack> slots(int size) {
        List<Stack> slots = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            slots.add(new Slot());
        }
        return slots;
    }

    @Test
    void anEmptyOutputGridHasRoomForEveryItemInEverySlot() {
        assertThat(DrillStorage.room(slots(9), PETROLEUM, 64)).isEqualTo(9 * 64);
        assertThat(DrillStorage.room(slots(9), null, 64)).isEqualTo(9 * 64);
    }

    @Test
    void aPartStackOfTheSameItemHasOnlyItsRemainingRoom() {
        List<Stack> slots = slots(2);
        slots.get(0).set(PETROLEUM, 60);
        assertThat(DrillStorage.room(slots, PETROLEUM, 64)).isEqualTo(4 + 64);
    }

    @Test
    void anotherItemTakesNoRoomInAStackOfADifferentItem() {
        List<Stack> slots = slots(1);
        slots.get(0).set(RAW, 10);
        assertThat(DrillStorage.room(slots, PETROLEUM, 64)).isZero();
        assertThat(DrillStorage.room(slots, null, 64)).isZero();
    }

    @Test
    void aNullReferenceCountsOnlyEmptySlots() {
        List<Stack> slots = slots(3);
        slots.get(0).set(PETROLEUM, 5);
        assertThat(DrillStorage.room(slots, null, 64)).isEqualTo(2 * 64);
    }

    @Test
    void addingFillsTheSlotThatHoldsTheItemFirstThenEmptySlots() {
        List<Stack> slots = slots(3);
        slots.get(1).set(PETROLEUM, 63);
        assertThat(DrillStorage.add(slots, PETROLEUM, 3, 64)).isTrue();
        assertThat(slots.get(1).count()).isEqualTo(64);
        assertThat(slots.get(0).count()).isEqualTo(2);
        assertThat(slots.get(2).count()).isZero();
    }

    @Test
    void addingIsAllOrNothing() {
        List<Stack> slots = slots(1);
        slots.get(0).set(PETROLEUM, 62);
        assertThat(DrillStorage.add(slots, PETROLEUM, 3, 64)).isFalse();
        assertThat(slots.get(0).count()).isEqualTo(62);
    }

    @Test
    void addingNothingIsRefused() {
        assertThat(DrillStorage.add(slots(1), PETROLEUM, 0, 64)).isFalse();
    }

    @Test
    void theStoredTotalIsTheSumOfTheSlots() {
        List<Stack> slots = slots(3);
        slots.get(0).set(PETROLEUM, 4);
        slots.get(2).set(RAW, 7);
        assertThat(DrillStorage.stored(slots)).isEqualTo(11);
    }
}
