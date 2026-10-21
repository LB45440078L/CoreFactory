package io.corefactory.core.geology;

import io.corefactory.core.geology.RetrofitQueue.ChunkRef;
import io.corefactory.core.geology.RetrofitQueue.Entry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RetrofitQueueTest {

    @Test
    void takesAtMostTheBudgetPerCall() {
        RetrofitQueue queue = new RetrofitQueue(2);
        for (int x = 0; x < 5; x++) {
            queue.offer("world", x, 0, false);
        }

        assertThat(queue.take()).hasSize(2);
        assertThat(queue.take()).hasSize(2);
        assertThat(queue.take()).hasSize(1);
        assertThat(queue.take()).isEmpty();
        assertThat(queue.isEmpty()).isTrue();
    }

    @Test
    void servesFirstInFirstOut() {
        RetrofitQueue queue = new RetrofitQueue(2);
        queue.offer("world", 1, 1, false);
        queue.offer("world", 2, 2, false);
        queue.offer("nether", 3, 3, false);

        List<Entry> first = queue.take();
        assertThat(first).containsExactly(
                new Entry(new ChunkRef("world", 1, 1), false),
                new Entry(new ChunkRef("world", 2, 2), false));
        assertThat(queue.take()).containsExactly(new Entry(new ChunkRef("nether", 3, 3), false));
    }

    @Test
    void aChunkIsQueuedOnce() {
        RetrofitQueue queue = new RetrofitQueue(4);

        assertThat(queue.offer("world", 0, 0, false)).isTrue();
        assertThat(queue.offer("world", 0, 0, false)).isFalse();
        assertThat(queue.size()).isEqualTo(1);
        assertThat(queue.offer("other", 0, 0, false)).isTrue();
        assertThat(queue.size()).isEqualTo(2);
    }

    @Test
    void aForcedRequestUpgradesAQueuedChunkWithoutMovingIt() {
        RetrofitQueue queue = new RetrofitQueue(4);
        queue.offer("world", 0, 0, false);
        queue.offer("world", 1, 0, false);

        assertThat(queue.offer("world", 0, 0, true)).isFalse();

        assertThat(queue.take()).containsExactly(
                new Entry(new ChunkRef("world", 0, 0), true),
                new Entry(new ChunkRef("world", 1, 0), false));
    }

    @Test
    void budgetIsAtLeastOne() {
        RetrofitQueue queue = new RetrofitQueue(0);
        queue.offer("world", 0, 0, false);
        queue.offer("world", 1, 0, false);

        assertThat(queue.perTick()).isEqualTo(1);
        assertThat(queue.take()).hasSize(1);
    }

    @Test
    void clearEmptiesTheQueue() {
        RetrofitQueue queue = new RetrofitQueue(2);
        queue.offer("world", 0, 0, false);
        queue.clear();

        assertThat(queue.size()).isZero();
        assertThat(queue.take()).isEmpty();
    }
}
