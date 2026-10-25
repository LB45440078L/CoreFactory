package io.corefactory.paper.chunks;

import org.bukkit.Location;

import java.util.Optional;

/** The chunk keys of the locations the stores keep. */
public final class ChunkKeys {

    private ChunkKeys() {
    }

    /**
     * The chunk a location is in.
     *
     * @return the chunk, or empty when the location is null or has no world
     */
    public static Optional<ChunkKey> of(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.of(ChunkKey.of(location.getWorld().getUID(), location.getBlockX(), location.getBlockZ()));
    }
}
