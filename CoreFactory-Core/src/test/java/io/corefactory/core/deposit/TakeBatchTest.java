package io.corefactory.core.deposit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TakeBatchTest {

    @Test
    void aTakeIsDueOnceTheBatchSizeIsPending() {
        TakeBatch batch = new TakeBatch(10, 20);
        batch.produce(9);
        assertThat(batch.due()).isFalse();
        batch.produce(1);
        assertThat(batch.due()).isTrue();
    }

    @Test
    void aSmallBatchIsDueOnceItsOldestItemHasWaitedTheLimit() {
        TakeBatch batch = new TakeBatch(10, 20);
        batch.produce(3);
        for (int tick = 0; tick < 19; tick++) {
            batch.tick();
        }
        assertThat(batch.due()).isFalse();
        batch.tick();
        assertThat(batch.due()).isTrue();
    }

    @Test
    void theWaitStartsAtTheFirstItemNotAtTheLatest() {
        TakeBatch batch = new TakeBatch(10, 20);
        batch.produce(3);
        for (int tick = 0; tick < 10; tick++) {
            batch.tick();
        }
        batch.produce(2);
        for (int tick = 0; tick < 9; tick++) {
            batch.tick();
        }
        assertThat(batch.due()).isFalse();
        batch.tick();
        assertThat(batch.due()).isTrue();
    }

    @Test
    void nothingPendingIsNeverDue() {
        TakeBatch batch = new TakeBatch(10, 20);
        for (int tick = 0; tick < 100; tick++) {
            batch.tick();
        }
        assertThat(batch.due()).isFalse();
        batch.produce(0);
        assertThat(batch.due()).isFalse();
    }

    @Test
    void aFullTakeEmptiesTheBatchAndRestartsTheWait() {
        TakeBatch batch = new TakeBatch(10, 20);
        batch.produce(10);
        batch.settle(10, 10);
        assertThat(batch.pending()).isZero();
        assertThat(batch.due()).isFalse();
        batch.produce(2);
        for (int tick = 0; tick < 19; tick++) {
            batch.tick();
        }
        assertThat(batch.due()).isFalse();
    }

    @Test
    void aPartialTakeKeepsTheRestAndTheClockRunning() {
        TakeBatch batch = new TakeBatch(10, 20);
        batch.produce(12);
        batch.settle(4, 4);
        assertThat(batch.pending()).isEqualTo(8);
        assertThat(batch.due()).isFalse();
        for (int tick = 0; tick < 20; tick++) {
            batch.tick();
        }
        assertThat(batch.due()).isTrue();
        TakeBatch slow = new TakeBatch(10, 20);
        slow.produce(5);
        for (int tick = 0; tick < 20; tick++) {
            slow.tick();
        }
        slow.settle(2, 2);
        assertThat(slow.pending()).isEqualTo(3);
        assertThat(slow.due()).isTrue();
    }

    @Test
    void aShortTakeMeansTheDepositRanOutAndTheRestIsGone() {
        TakeBatch batch = new TakeBatch(10, 20);
        batch.produce(10);
        batch.settle(10, 4);
        assertThat(batch.pending()).isZero();
        assertThat(batch.due()).isFalse();
    }

    @Test
    void discardDropsThePendingItems() {
        TakeBatch batch = new TakeBatch(10, 20);
        batch.produce(6);
        batch.discard();
        assertThat(batch.pending()).isZero();
        assertThat(batch.due()).isFalse();
    }

    @Test
    void aTakeMustFitThePendingItems() {
        TakeBatch batch = new TakeBatch(10, 20);
        batch.produce(4);
        assertThatThrownBy(() -> batch.settle(5, 5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> batch.settle(2, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> batch.produce(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TakeBatch(0, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TakeBatch(10, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theDefaultsAreTenItemsOrOneSecond() {
        assertThat(TakeBatch.DEFAULT_SIZE).isEqualTo(10);
        assertThat(TakeBatch.DEFAULT_TICKS).isEqualTo(20);
    }
}
