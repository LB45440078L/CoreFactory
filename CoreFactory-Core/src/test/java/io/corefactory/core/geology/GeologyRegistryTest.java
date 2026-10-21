package io.corefactory.core.geology;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeologyRegistryTest {

    @Test
    void packsIntoTheDocumentedBitLayout() {
        // x 5, z 9, y -64 (offset 0), ore 3.
        assertThat(GeologyRegistry.pack(5, -64, 9, 3)).isEqualTo(5 | (9 << 4) | (3 << 18));
        // y 959 is the top of the 10-bit offset, ore 255 the top of the 8-bit index.
        assertThat(GeologyRegistry.pack(15, 959, 15, 255)).isEqualTo(15 | (15 << 4) | (1023 << 8) | (255 << 18));
    }

    @Test
    void encodeAndDecodeRoundTripInsideAChunk() {
        int chunkX = 3;
        int chunkZ = -2;
        List<Placement> placed = List.of(
                new Placement(48, -48, -32, 0),
                new Placement(63, 0, -17, 6),
                new Placement(51, 895, -20, 255),
                new Placement(55, -64, -31, 1));

        int[] packed = GeologyRegistry.encode(placed);

        assertThat(packed).hasSize(4);
        assertThat(GeologyRegistry.decode(packed, chunkX, chunkZ)).containsExactlyElementsOf(placed);
    }

    @Test
    void entriesAreFourBytesEach() {
        int[] packed = GeologyRegistry.encode(List.of(new Placement(1, 2, 3, 4), new Placement(5, 6, 7, 8)));

        assertThat(packed).hasSize(2);
    }

    @Test
    void containsAndOreIndexFindTheBlock() {
        int[] packed = GeologyRegistry.encode(List.of(new Placement(-17, 10, 40, 7)));

        assertThat(GeologyRegistry.contains(packed, -17, 10, 40)).isTrue();
        assertThat(GeologyRegistry.oreIndex(packed, -17, 10, 40)).isEqualTo(7);
        assertThat(GeologyRegistry.contains(packed, -17, 11, 40)).isFalse();
        assertThat(GeologyRegistry.oreIndex(packed, -17, 11, 40)).isEqualTo(-1);
        assertThat(GeologyRegistry.contains(packed, -17, 10, 41)).isFalse();
        assertThat(GeologyRegistry.contains(null, 0, 0, 0)).isFalse();
    }

    @Test
    void positionsOutsideTheLayoutAreNeverFound() {
        int[] packed = GeologyRegistry.encode(List.of(new Placement(0, -64, 0, 1)));

        assertThat(GeologyRegistry.contains(packed, 0, -65, 0)).isFalse();
        assertThat(GeologyRegistry.contains(packed, 0, 960, 0)).isFalse();
    }

    @Test
    void removeDropsOnlyTheGivenBlock() {
        int[] packed = GeologyRegistry.encode(List.of(
                new Placement(1, 5, 1, 0),
                new Placement(2, 5, 1, 1),
                new Placement(3, 5, 1, 2)));

        int[] after = GeologyRegistry.remove(packed, 2, 5, 1);

        assertThat(after).hasSize(2);
        assertThat(GeologyRegistry.contains(after, 2, 5, 1)).isFalse();
        assertThat(GeologyRegistry.oreIndex(after, 1, 5, 1)).isZero();
        assertThat(GeologyRegistry.oreIndex(after, 3, 5, 1)).isEqualTo(2);
    }

    @Test
    void removeOfAnUnknownBlockReturnsTheSameArray() {
        int[] packed = GeologyRegistry.encode(List.of(new Placement(1, 5, 1, 0)));

        assertThat(GeologyRegistry.remove(packed, 9, 9, 9)).isSameAs(packed);
    }

    @Test
    void removingEverythingLeavesAnEmptyArray() {
        int[] packed = GeologyRegistry.encode(List.of(new Placement(1, 5, 1, 0)));

        assertThat(GeologyRegistry.remove(packed, 1, 5, 1)).isEmpty();
    }

    @Test
    void mergeAddsNewPositionsAndKeepsExistingOnes() {
        int[] packed = GeologyRegistry.encode(List.of(new Placement(1, 5, 1, 0)));

        int[] merged = GeologyRegistry.merge(packed, List.of(
                new Placement(1, 5, 1, 4),
                new Placement(2, 5, 1, 1),
                new Placement(2, 5, 1, 1)));

        assertThat(merged).hasSize(2);
        assertThat(GeologyRegistry.oreIndex(merged, 1, 5, 1)).isZero();
        assertThat(GeologyRegistry.oreIndex(merged, 2, 5, 1)).isEqualTo(1);
    }

    @Test
    void mergeOfNothingReturnsTheSameArray() {
        int[] packed = GeologyRegistry.encode(List.of(new Placement(1, 5, 1, 0)));

        assertThat(GeologyRegistry.merge(packed, List.of(new Placement(1, 5, 1, 0)))).isSameAs(packed);
    }

    @Test
    void packRejectsValuesOutsideTheLayout() {
        assertThatThrownBy(() -> GeologyRegistry.pack(0, -65, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GeologyRegistry.pack(0, 960, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GeologyRegistry.pack(0, 0, 0, 256)).isInstanceOf(IllegalArgumentException.class);
    }
}
