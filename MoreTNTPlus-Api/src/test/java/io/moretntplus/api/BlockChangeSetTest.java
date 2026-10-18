package io.moretntplus.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlockChangeSetTest {

    @Test
    void keepsWritesInOrderAndGrowsPastCapacity() {
        BlockChangeSet set = new BlockChangeSet(16);
        for (int i = 0; i < 100; i++) {
            set.set(i, i + 1, i + 2, null);
        }

        assertThat(set.size()).isEqualTo(100);
        assertThat(set.x(0)).isZero();
        assertThat(set.y(99)).isEqualTo(100);
        assertThat(set.z(50)).isEqualTo(52);
    }

    @Test
    void clearKeepsCapacityAndResetsSize() {
        BlockChangeSet set = new BlockChangeSet();
        set.set(1, 2, 3, null);
        set.clear();

        assertThat(set.isEmpty()).isTrue();
        set.set(4, 5, 6, null);
        assertThat(set.size()).isEqualTo(1);
        assertThat(set.x(0)).isEqualTo(4);
    }
}
