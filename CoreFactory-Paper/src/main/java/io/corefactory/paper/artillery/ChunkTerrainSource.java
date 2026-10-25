package io.corefactory.paper.artillery;

import io.corefactory.paper.artillery.TerrainCache.ChunkId;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.function.Consumer;

/**
 * The Paper side of the terrain loader: a chunk is read with {@link World#getChunkAtAsync(int, int, boolean, Consumer)}, and its terrain is
 * sampled in the callback, on the main thread. The server loads the chunk for the callback and lets it unload after, as it does any chunk
 * that nothing else keeps loaded. A chunk that is read without generation and is not generated gives null, and so does a chunk that is not
 * generated after a generation.
 */
final class ChunkTerrainSource implements TerrainLoader.ChunkSource {

    @Override
    public void fetch(ChunkId id, boolean generate, Consumer<byte[]> done) {
        World world = Bukkit.getWorld(id.world());
        if (world == null) {
            done.accept(null);
            return;
        }
        world.getChunkAtAsync(id.x(), id.z(), generate, chunk -> {
            if (chunk == null || !chunk.isGenerated()) {
                done.accept(null);
                return;
            }
            done.accept(TargetSurface.sample(world, chunk));
        });
    }
}
