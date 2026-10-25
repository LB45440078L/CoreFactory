package io.corefactory.paper.artillery;

import io.corefactory.paper.artillery.TerrainCache.ChunkId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the cache of sampled terrain: the column layout, the least recently used eviction at the cap, and the drop of a world. */
class TerrainCacheTest {

    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final byte[] TERRAIN = new byte[TerrainCache.COLUMNS];

    @Test
    void theColumnsAreRowByRowFromTheLowestCorner() {
        assertThat(TerrainCache.index(0, 0)).isZero();
        assertThat(TerrainCache.index(15, 0)).isEqualTo(15);
        assertThat(TerrainCache.index(0, 1)).isEqualTo(16);
        assertThat(TerrainCache.index(15, 15)).isEqualTo(255);
    }

    @Test
    void theColumnIndexIsTheSameInEveryChunk() {
        assertThat(TerrainCache.index(16 + 3, -16 + 5)).isEqualTo(TerrainCache.index(3, 5));
        assertThat(TerrainCache.index(-1, -1)).isEqualTo(255);
    }

    @Test
    void theLeastRecentlyUsedChunkIsDroppedAtTheCap() {
        TerrainCache cache = new TerrainCache(2);
        ChunkId a = new ChunkId(WORLD, 0, 0);
        ChunkId b = new ChunkId(WORLD, 1, 0);
        ChunkId c = new ChunkId(WORLD, 2, 0);
        cache.put(a, TERRAIN);
        cache.put(b, TERRAIN);
        assertThat(cache.get(a)).isNotNull();

        cache.put(c, TERRAIN);

        assertThat(cache.size()).isEqualTo(2);
        assertThat(cache.contains(a)).isTrue();
        assertThat(cache.contains(b)).isFalse();
        assertThat(cache.contains(c)).isTrue();
    }

    @Test
    void checkingAChunkDoesNotMakeItRecent() {
        TerrainCache cache = new TerrainCache(2);
        ChunkId a = new ChunkId(WORLD, 0, 0);
        ChunkId b = new ChunkId(WORLD, 1, 0);
        cache.put(a, TERRAIN);
        cache.put(b, TERRAIN);

        assertThat(cache.contains(a)).isTrue();
        cache.put(new ChunkId(WORLD, 2, 0), TERRAIN);

        assertThat(cache.contains(a)).isFalse();
        assertThat(cache.contains(b)).isTrue();
    }

    @Test
    void aWorldThatUnloadsTakesOnlyItsChunks() {
        TerrainCache cache = new TerrainCache(8);
        cache.put(new ChunkId(WORLD, 0, 0), TERRAIN);
        cache.put(new ChunkId(OTHER, 0, 0), TERRAIN);

        cache.forget(WORLD);

        assertThat(cache.contains(new ChunkId(WORLD, 0, 0))).isFalse();
        assertThat(cache.contains(new ChunkId(OTHER, 0, 0))).isTrue();
    }

    @Test
    void anUnavailableChunkIsKeptAsEmpty() {
        TerrainCache cache = new TerrainCache(8);
        ChunkId id = new ChunkId(WORLD, 4, 4);
        cache.put(id, TerrainCache.UNAVAILABLE);

        assertThat(cache.get(id)).isSameAs(TerrainCache.UNAVAILABLE);
    }
}
