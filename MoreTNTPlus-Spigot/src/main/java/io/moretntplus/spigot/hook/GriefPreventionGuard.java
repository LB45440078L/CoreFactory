package io.moretntplus.spigot.hook;

import io.moretntplus.core.engine.ClaimGuard;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Keeps blasts out of GriefPrevention claims.
 *
 * <p>GriefPrevention is reached by reflection, so MoreTNT+ builds without it and simply skips this guard when it is
 * absent or a different version changes the method. Claims are columns, so the answer is cached per block column and
 * tick: a blast asks GriefPrevention once per column, however many blocks that column holds.
 */
public final class GriefPreventionGuard implements ClaimGuard {

    private record Column(UUID world, int x, int z) {
    }

    private final Object dataStore;
    private final Method getClaimAt;
    private final Map<Column, Boolean> columns = new HashMap<>();
    private boolean reportedFailure;
    private final Logger log;

    private GriefPreventionGuard(Object dataStore, Method getClaimAt, Logger log) {
        this.dataStore = dataStore;
        this.getClaimAt = getClaimAt;
        this.log = log;
    }

    /** Builds the guard, or empty when GriefPrevention's API cannot be found. */
    public static Optional<GriefPreventionGuard> create(Plugin griefPrevention, Logger log) {
        try {
            Field store = griefPrevention.getClass().getField("dataStore");
            Object dataStore = store.get(griefPrevention);
            Class<?> claim = Class.forName("me.ryanhamshire.GriefPrevention.Claim", false,
                    griefPrevention.getClass().getClassLoader());
            Method getClaimAt = dataStore.getClass().getMethod("getClaimAt", Location.class, boolean.class, claim);
            return Optional.of(new GriefPreventionGuard(dataStore, getClaimAt, log));
        } catch (ReflectiveOperationException | RuntimeException e) {
            log.warning("GriefPrevention is installed but its claim API was not recognised; "
                    + "blasts will not check claims. (" + e.getClass().getSimpleName() + ")");
            return Optional.empty();
        }
    }

    @Override
    public void beginTick() {
        columns.clear();
    }

    @Override
    public boolean protects(Block block) {
        World world = block.getWorld();
        Column key = new Column(world.getUID(), block.getX(), block.getZ());
        Boolean claimed = columns.get(key);
        if (claimed == null) {
            claimed = isClaimed(block.getLocation());
            columns.put(key, claimed);
        }
        return claimed;
    }

    private boolean isClaimed(Location location) {
        try {
            return getClaimAt.invoke(dataStore, location, true, null) != null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            if (!reportedFailure) {
                reportedFailure = true;
                log.warning("GriefPrevention claim lookup failed; claims are not checked for blasts until restart.");
            }
            return false;
        }
    }
}
