package io.corefactory.core.engine;

import org.bukkit.block.Block;

/**
 * A claim or region system that can forbid explosions in part of the world (WorldGuard regions, GriefPrevention
 * claims). Guards are optional: with none installed, protection costs nothing extra.
 *
 * <p>Implementations must answer quickly. They cache per tick, so a blast of many thousands of blocks asks the
 * claim system once per chunk or column, not once per block.
 */
public interface ClaimGuard {

    /** Called once per server tick. Drop cached answers here: claims can change between ticks. */
    void beginTick();

    /** True when an explosion must not change this block. */
    boolean protects(Block block);
}
