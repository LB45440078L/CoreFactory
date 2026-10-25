package io.corefactory.paper.artillery;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The terrain the Set Target map has sampled from the chunks of the worlds, kept so the map draws a chunk without reading it again. The
 * terrain of a chunk is {@link #COLUMNS} map colour bytes, one for each column: row by row from the lowest z, each row 16 columns from the
 * lowest x. A chunk is keyed by its world and its chunk coordinates. When the cache holds more chunks than its capacity, the least recently
 * used chunk is dropped. A chunk that could not be generated is kept as {@link #UNAVAILABLE}, so it is not asked for again. Pure, so the
 * eviction is tested without a server.
 */
final class TerrainCache {

    /** The columns of a chunk's terrain: 16 by 16. */
    static final int COLUMNS = 256;

    /** The terrain of a chunk that could not be read or generated: an empty array, drawn as not loaded. */
    static final byte[] UNAVAILABLE = new byte[0];

    /**
     * A chunk, by its world and its chunk coordinates.
     *
     * @param world the world's id
     * @param x     the chunk's x
     * @param z     the chunk's z
     */
    record ChunkId(UUID world, int x, int z) {
    }

    private final LinkedHashMap<ChunkId, byte[]> chunks;

    /**
     * @param capacity the chunks kept, at least 1
     */
    TerrainCache(int capacity) {
        this.chunks = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<ChunkId, byte[]> eldest) {
                return size() > capacity;
            }
        };
    }

    /**
     * The index of a column in a chunk's terrain.
     *
     * @param x the block x of the column, in any chunk
     * @param z the block z of the column
     * @return the index, from 0 to {@link #COLUMNS} - 1
     */
    static int index(int x, int z) {
        return ((z & 15) << 4) | (x & 15);
    }

    /** The terrain of a chunk, or null when it is not kept. A read makes the chunk the most recently used. */
    byte[] get(ChunkId id) {
        return chunks.get(id);
    }

    /** True when the chunk's terrain is kept. A check does not make the chunk the most recently used. */
    boolean contains(ChunkId id) {
        return chunks.containsKey(id);
    }

    /**
     * Keeps a chunk's terrain, replacing any kept before.
     *
     * @param id      the chunk
     * @param colours the terrain, {@link #COLUMNS} bytes, or {@link #UNAVAILABLE}
     */
    void put(ChunkId id, byte[] colours) {
        chunks.put(id, colours);
    }

    /** Drops every chunk of a world, when the world unloads. */
    void forget(UUID world) {
        chunks.keySet().removeIf(id -> id.world().equals(world));
    }

    /** The number of chunks kept. */
    int size() {
        return chunks.size();
    }
}
