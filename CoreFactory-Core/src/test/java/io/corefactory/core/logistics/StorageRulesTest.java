package io.corefactory.core.logistics;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageRulesTest {

    private static final int LIMIT = 64;
    private static final ItemRef COBBLE = ItemRef.material("COBBLESTONE");
    private static final ItemRef DIRT = ItemRef.material("DIRT");
    private static final ItemRef GEAR = ItemRef.item("gear");

    /** A slot held in memory: a count of zero or less empties it. */
    private static final class MemoryStack implements Stack {

        private ItemRef ref;
        private int count;

        MemoryStack(ItemRef ref, int count) {
            this.ref = ref;
            this.count = count;
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
            this.ref = count <= 0 ? null : ref;
            this.count = Math.max(0, count);
        }
    }

    private static MemoryStack empty() {
        return new MemoryStack(null, 0);
    }

    private static MemoryStack of(ItemRef ref, int count) {
        return new MemoryStack(ref, count);
    }

    private static List<Stack> chest(Stack... slots) {
        return new ArrayList<>(List.of(slots));
    }

    @Test
    void anEmptyChestAccepts() {
        List<Stack> stacks = chest(empty(), empty(), empty());

        assertThat(StorageRules.accepts(LIMIT, stacks, COBBLE)).isTrue();
        assertThat(StorageRules.room(LIMIT, stacks, COBBLE)).isEqualTo(3 * LIMIT);
        assertThat(StorageRules.slotFor(LIMIT, stacks, COBBLE)).isEqualTo(OptionalInt.of(0));
    }

    @Test
    void aFullChestRefuses() {
        List<Stack> stacks = chest(of(COBBLE, LIMIT), of(COBBLE, LIMIT), of(DIRT, LIMIT));

        assertThat(StorageRules.accepts(LIMIT, stacks, COBBLE)).isFalse();
        assertThat(StorageRules.room(LIMIT, stacks, COBBLE)).isZero();
        assertThat(StorageRules.slotFor(LIMIT, stacks, COBBLE)).isEqualTo(OptionalInt.empty());
    }

    @Test
    void aPartlyFilledChestAcceptsTheSameTypeUpToTheStackLimit() {
        List<Stack> stacks = chest(of(COBBLE, 10));

        assertThat(StorageRules.accepts(LIMIT, stacks, COBBLE)).isTrue();
        assertThat(StorageRules.room(LIMIT, stacks, COBBLE)).isEqualTo(LIMIT - 10);
        assertThat(StorageRules.slotFor(LIMIT, stacks, COBBLE)).isEqualTo(OptionalInt.of(0));
    }

    @Test
    void aDifferentItemIntoAFullSlotRefuses() {
        List<Stack> stacks = chest(of(DIRT, LIMIT));

        assertThat(StorageRules.accepts(LIMIT, stacks, COBBLE)).isFalse();
        assertThat(StorageRules.room(LIMIT, stacks, COBBLE)).isZero();
    }

    @Test
    void aDifferentItemInAPartSlotDoesNotTakeTheSlotButAnEmptyOneDoes() {
        List<Stack> stacks = chest(of(DIRT, 5), empty());

        assertThat(StorageRules.slotFor(LIMIT, stacks, COBBLE)).isEqualTo(OptionalInt.of(1));
        assertThat(StorageRules.room(LIMIT, stacks, COBBLE)).isEqualTo(LIMIT);
    }

    @Test
    void aComponentItemIsNotTheSameAsAMaterialOfTheSameName() {
        List<Stack> stacks = chest(of(ItemRef.material("gear"), 3));

        assertThat(StorageRules.accepts(LIMIT, stacks, GEAR)).isFalse();
    }

    @Test
    void amountsOverTheStackLimitSpillIntoTheNextSlot() {
        List<Stack> stacks = chest(of(COBBLE, LIMIT), of(COBBLE, 60), empty());

        assertThat(StorageRules.slotFor(LIMIT, stacks, COBBLE)).isEqualTo(OptionalInt.of(1));
        assertThat(StorageRules.room(LIMIT, stacks, COBBLE)).isEqualTo(4 + LIMIT);
    }

    @Test
    void aSameTypeSlotWithRoomIsFilledBeforeAnEmptySlot() {
        List<Stack> stacks = chest(empty(), of(COBBLE, 3));

        assertThat(StorageRules.slotFor(LIMIT, stacks, COBBLE)).isEqualTo(OptionalInt.of(1));
    }

    @Test
    void aSlotOverItsLimitTakesNothingOfThatItem() {
        List<Stack> stacks = chest(of(COBBLE, LIMIT + 2));

        assertThat(StorageRules.room(LIMIT, stacks, COBBLE)).isZero();
        assertThat(StorageRules.accepts(LIMIT, stacks, COBBLE)).isFalse();
    }

    @Test
    void aSmallerStackLimitCountsTheRoomOfThatLimit() {
        List<Stack> stacks = chest(of(GEAR, 15));

        assertThat(StorageRules.room(16, stacks, GEAR)).isOne();
        assertThat(StorageRules.accepts(16, stacks, GEAR)).isTrue();
        assertThat(StorageRules.accepts(15, stacks, GEAR)).isFalse();
    }

    @Test
    void aStackLimitBelowOneIsRefused() {
        assertThatThrownBy(() -> StorageRules.accepts(0, chest(empty()), COBBLE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
