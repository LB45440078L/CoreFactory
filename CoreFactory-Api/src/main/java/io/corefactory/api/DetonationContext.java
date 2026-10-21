package io.corefactory.api;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

/**
 * Everything a {@link Mechanic} needs to run one detonation.
 *
 * @param tnt       the TNT type being detonated
 * @param origin    the blast centre, at the middle of the block (x.5, y.5, z.5)
 * @param source    the entity that ignited it, or {@code null} for environmental causes
 * @param direction the placement direction when the block was placed with one, or {@code null}
 * @param placed    the placed block this detonation came from (a snapshot taken at ignition), or {@code null}
 * @param depth     how many chain links led here; mechanics use it to stop runaway chains
 * @param api       the service facade
 */
public record DetonationContext(
        TntDefinition tnt,
        Location origin,
        Entity source,
        Vector direction,
        PlacedTnt placed,
        int depth,
        CoreFactoryApi api
) {

    /** Shortcut for {@code tnt().settings()}. */
    public MechanicSettings settings() {
        return tnt.settings();
    }

    /** The world the blast happens in. */
    public World world() {
        return origin.getWorld();
    }
}
