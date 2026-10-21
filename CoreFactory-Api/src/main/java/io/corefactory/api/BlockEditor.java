package io.corefactory.api;

import org.bukkit.World;

/**
 * Applies block writes in bulk. The Spigot module implements it on top of WorldEdit or FastAsyncWorldEdit,
 * which is what keeps large blasts fast.
 */
public interface BlockEditor {

    /**
     * Writes {@code changes[from, to)} to the world as one batch, without physics updates.
     *
     * @return the number of blocks written
     */
    int apply(World world, BlockChangeSet changes, int from, int to);

    /**
     * The most writes one call should carry in a tick. Editors that have fallen back to slower writes lower it, and the
     * core spreads the work over more ticks.
     */
    default int preferredBatch() {
        return Integer.MAX_VALUE;
    }
}
