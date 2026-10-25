package io.corefactory.paper.chunks;

import java.util.Objects;
import java.util.UUID;

/**
 * The identity of one chunk: the id of its world and its chunk coordinates. It holds no server types, so the planner can be tested
 * without a server.
 *
 * @param world the id of the chunk's world
 * @param x     the chunk's x, which is the block x shifted right by 4
 * @param z     the chunk's z, which is the block z shifted right by 4
 */
public record ChunkKey(UUID world, int x, int z) {

    public ChunkKey {
        Objects.requireNonNull(world, "world");
    }

    /**
     * The chunk that holds a block. A negative block coordinate rounds down, as the server does, so the block at -1 is in chunk -1.
     *
     * @param world  the id of the block's world
     * @param blockX the block's x
     * @param blockZ the block's z
     */
    public static ChunkKey of(UUID world, int blockX, int blockZ) {
        return new ChunkKey(world, blockX >> 4, blockZ >> 4);
    }
}
