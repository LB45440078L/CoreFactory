package io.moretntplus.api;

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
}
