package io.corefactory.spigot.geology;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PatchExclusionsTest {

    @Test
    void theEntriesOfOneChunkAreDistinct() {
        Set<Integer> seen = new HashSet<>();
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = -64; y <= 96; y++) {
                    seen.add(PatchExclusions.pack(x, y, z));
                    count++;
                }
            }
        }

        assertThat(seen).hasSize(count);
    }

    @Test
    void aBlockInAnotherChunkWithTheSameOffsetHasTheSameEntry() {
        assertThat(PatchExclusions.pack(16 + 3, 20, -16 + 5)).isEqualTo(PatchExclusions.pack(3, 20, 5));
    }

    @Test
    void aYOutsideTheStorableRangeHasNoEntry() {
        assertThat(PatchExclusions.pack(0, -65, 0)).isEqualTo(-1);
        assertThat(PatchExclusions.pack(0, 960, 0)).isEqualTo(-1);
        assertThat(PatchExclusions.pack(0, -64, 0)).isNotNegative();
        assertThat(PatchExclusions.pack(0, 959, 0)).isNotNegative();
    }

    @Test
    void aPositionIsFoundOnlyAfterItIsRecorded() {
        int key = PatchExclusions.pack(4, 10, 9);

        assertThat(PatchExclusions.has(null, key)).isFalse();
        assertThat(PatchExclusions.has(new int[0], key)).isFalse();

        int[] record = PatchExclusions.union(null, new int[]{key});

        assertThat(PatchExclusions.has(record, key)).isTrue();
        assertThat(PatchExclusions.has(record, PatchExclusions.pack(4, 11, 9))).isFalse();
        assertThat(PatchExclusions.has(record, PatchExclusions.pack(5, 10, 9))).isFalse();
    }

    @Test
    void theRecordIsSortedAndKeepsEachEntryOnce() {
        int[] record = PatchExclusions.union(new int[]{7, 30}, new int[]{30, 2, 7, 19, 2});

        assertThat(record).containsExactly(2, 7, 19, 30);
    }

    @Test
    void aLargeRecordHoldsEveryEntryPlacedInIt() {
        Random random = new Random(0x5EED);
        Set<Integer> placed = new HashSet<>();
        int[] record = null;
        for (int round = 0; round < 40; round++) {
            int[] batch = new int[25];
            for (int i = 0; i < batch.length; i++) {
                batch[i] = random.nextInt(1 << 18);
                placed.add(batch[i]);
            }
            record = PatchExclusions.union(record, batch);
        }

        assertThat(record).hasSize(placed.size());
        for (int key : placed) {
            assertThat(PatchExclusions.has(record, key)).isTrue();
        }
        for (int i = 1; i < record.length; i++) {
            assertThat(record[i]).isGreaterThan(record[i - 1]);
        }
    }
}
