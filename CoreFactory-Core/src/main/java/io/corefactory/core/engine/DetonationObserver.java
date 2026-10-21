package io.corefactory.core.engine;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

/**
 * Told once for every detonation of a custom TNT, after its blast is scheduled, on the main thread. Platform code uses it
 * to act on the blocks and entities the blast reaches that the blast pipeline does not touch, such as machines and
 * energy devices. The observer is not told about detonations the core refused (no mechanic, an anchor field, the
 * chain or rate limits, or a second detonation of the same block on the same tick).
 */
@FunctionalInterface
public interface DetonationObserver {

    /**
     * @param world  the world of the blast
     * @param origin the blast centre, at the middle of the TNT's block
     * @param radius the TNT's blast radius, the same radius the blast uses for its entity damage
     * @param source the entity that ignited the TNT, or {@code null} for environmental causes
     */
    void onDetonation(World world, Location origin, double radius, Entity source);
}
