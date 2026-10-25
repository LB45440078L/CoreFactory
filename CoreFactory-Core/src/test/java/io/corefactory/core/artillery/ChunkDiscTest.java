package io.corefactory.core.artillery;

import io.corefactory.core.artillery.ChunkDisc.Chunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunkDiscTest {

    @Test
    void aRadiusOfZeroCoversTheOneChunkThatHoldsThePoint() {
        assertThat(ChunkDisc.covering(8.0, 8.0, 0.0)).containsExactly(new Chunk(0, 0));
        assertThat(ChunkDisc.covering(16.0, 16.0, 0.0)).containsExactly(new Chunk(1, 1));
        assertThat(ChunkDisc.covering(15.99, 0.0, 0.0)).containsExactly(new Chunk(0, 0));
    }

    @Test
    void aRadiusOfSixteenAtTheCentreOfAChunkCoversThreeByThree() {
        List<Chunk> chunks = ChunkDisc.covering(8.0, 8.0, 16.0);

        assertThat(chunks).hasSize(9);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                assertThat(chunks).contains(new Chunk(x, z));
            }
        }
    }

    @Test
    void theCoverIsTheSameShiftedWhenTheCentreMovesByWholeChunks() {
        List<Chunk> here = ChunkDisc.covering(40.0, -23.0, 100.0);
        List<Chunk> moved = ChunkDisc.covering(40.0 + 16 * 5, -23.0 - 16 * 3, 100.0);

        assertThat(moved).hasSameSizeAs(here);
        for (int i = 0; i < here.size(); i++) {
            assertThat(moved.get(i)).isEqualTo(new Chunk(here.get(i).x() + 5, here.get(i).z() - 3));
        }
    }

    @Test
    void theCoverIsSymmetricAcrossTheTwoAxes() {
        List<Chunk> xz = ChunkDisc.covering(37.0, -91.0, 60.0);
        List<Chunk> zx = ChunkDisc.covering(-91.0, 37.0, 60.0);

        assertThat(zx).hasSameSizeAs(xz);
        for (Chunk chunk : xz) {
            assertThat(zx).contains(new Chunk(chunk.z(), chunk.x()));
        }
    }

    @Test
    void theCoverOfADiscAroundTheOriginIsCenteredOnTheOrigin() {
        // A radius of 16 reaches the chunks on both sides of the origin, the chunk 0 and the chunk -1 that holds the block at -1.
        assertThat(ChunkDisc.covering(0.0, 0.0, 16.0)).hasSize(9).contains(new Chunk(-1, -1), new Chunk(1, 1));
    }

    @Test
    void negativeCoordinatesRoundDownIntoTheChunkThatHoldsThem() {
        assertThat(ChunkDisc.covering(-1.0, -1.0, 0.0)).containsExactly(new Chunk(-1, -1));
        assertThat(ChunkDisc.covering(-16.0, 0.0, 0.0)).containsExactly(new Chunk(-1, 0));
        assertThat(ChunkDisc.covering(-17.0, 0.0, 0.0)).containsExactly(new Chunk(-2, 0));
    }

    @Test
    void theCoverReachesEveryChunkThatTheDiscTouches() {
        double cx = 3.5;
        double cz = -70.25;
        double radius = 45.0;
        List<Chunk> chunks = ChunkDisc.covering(cx, cz, radius);

        // Every block of the disc's bounding square lies in a covered chunk, so no block of the disc is outside the cover.
        for (double x = cx - radius; x <= cx + radius; x += 1.0) {
            for (double z = cz - radius; z <= cz + radius; z += 1.0) {
                if (Math.hypot(x - cx, z - cz) <= radius) {
                    assertThat(chunks).contains(new Chunk((int) Math.floor(x / 16), (int) Math.floor(z / 16)));
                }
            }
        }
    }

    @Test
    void nearestFirstPutsTheChunkOfTheCentreFirstAndSortsByDistance() {
        List<Chunk> chunks = ChunkDisc.nearestFirst(8.0, 8.0, 200.0);

        assertThat(chunks.get(0)).isEqualTo(new Chunk(0, 0));
        double previous = -1.0;
        for (Chunk chunk : chunks) {
            double distance = Math.hypot(chunk.x() * 16 + 8.0 - 8.0, chunk.z() * 16 + 8.0 - 8.0);
            assertThat(distance).isGreaterThanOrEqualTo(previous);
            previous = distance;
        }
    }

    @Test
    void aNegativeRadiusIsRefused() {
        assertThatThrownBy(() -> ChunkDisc.covering(0.0, 0.0, -1.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChunkDisc.covering(0.0, 0.0, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    }
}
