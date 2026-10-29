package io.corefactory.core.vertical;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerticalCarryCellTest {

    private static final int CAPACITY_LEVEL_1 = 4;

    @Test
    void aBlockWithNoItemsHasNoIndexAndCountsZero() {
        VerticalCarry carry = new VerticalCarry(3);
        carry.offer(0, CAPACITY_LEVEL_1);

        assertThat(carry.indexIn(1)).isEqualTo(-1);
        assertThat(carry.countIn(1)).isZero();
        assertThat(carry.countIn(0)).isEqualTo(1);
    }

    @Test
    void theItemNearestTheOutputEndIsTakenFirstWithinABlock() {
        VerticalCarry carry = new VerticalCarry(2);
        carry.place(0.0, CAPACITY_LEVEL_1);
        carry.place(0.5, CAPACITY_LEVEL_1);

        assertThat(carry.countIn(0)).isEqualTo(2);
        assertThat(carry.indexIn(0)).isZero();
        assertThat(carry.positions().get(0)).isEqualTo(0.5);
    }

    @Test
    void anItemWaitingAtTheOutputEndIsInNoBlock() {
        VerticalCarry carry = new VerticalCarry(2);
        carry.place(2.0, CAPACITY_LEVEL_1);

        assertThat(carry.waiting()).isEqualTo(1);
        assertThat(carry.indexIn(1)).isEqualTo(-1);
        assertThat(carry.countIn(1)).isZero();
    }

    @Test
    void removingTheIndexedItemLeavesTheOthersInOrder() {
        VerticalCarry carry = new VerticalCarry(3);
        carry.place(1.2, CAPACITY_LEVEL_1);
        carry.place(0.3, CAPACITY_LEVEL_1);

        carry.remove(carry.indexIn(1));

        assertThat(carry.size()).isEqualTo(1);
        assertThat(carry.positions()).containsExactly(0.3);
    }

    @Test
    void aPutAtAFullEntryIsRefused() {
        VerticalCarry carry = new VerticalCarry(2);
        carry.place(0.0, CAPACITY_LEVEL_1);

        assertThat(carry.slotFor(0, CAPACITY_LEVEL_1)).isEqualTo(-1);
        assertThat(carry.place(0, CAPACITY_LEVEL_1)).isEqualTo(-1);
        assertThat(carry.size()).isEqualTo(1);
    }

    @Test
    void aBlockOutOfRangeIsRejected() {
        VerticalCarry carry = new VerticalCarry(2);

        assertThatThrownBy(() -> carry.indexIn(2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> carry.countIn(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aRemovalOutOfRangeIsRejected() {
        VerticalCarry carry = new VerticalCarry(2);

        assertThatThrownBy(() -> carry.remove(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
