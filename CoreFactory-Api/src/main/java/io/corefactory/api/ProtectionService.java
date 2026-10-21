package io.corefactory.api;

import org.bukkit.Location;
import org.bukkit.block.Block;

/**
 * Decides which blocks an explosion may change. Consulted by every mechanic before it queues a write.
 */
public interface ProtectionService {

    /** True when the block must not be destroyed or replaced by any blast. */
    boolean isProtected(Block block);

    /** True when the location lies inside an active anti-TNT field (anchor TNT). */
    boolean inAnchorField(Location location);
}
