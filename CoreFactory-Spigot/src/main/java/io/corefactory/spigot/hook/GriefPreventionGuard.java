package io.corefactory.spigot.hook;

import io.corefactory.core.engine.ClaimGuard;
import io.corefactory.core.engine.ContainerAccess;
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
 * <p>GriefPrevention is reached by reflection, so CoreFactory builds without it and simply skips this guard when it is
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

    /**
     * True when the player is trusted in the claim at the location: the access or manager list of that claim. Fails closed: when the
     * claim API cannot answer, the player counts as trusted, so a turret does not fire at it. An unclaimed location trusts nobody.
     *
     * @param location the location whose claim is asked
     * @param player   the player's id
     * @param name     the player's name, for a GriefPrevention version that trusts by name
     */
    public boolean trusts(Location location, UUID player, String name) {
        try {
            Object claim = getClaimAt.invoke(dataStore, location, true, null);
            if (claim == null) {
                return false;
            }
            Object permission = permissionOf(claim, player, name);
            if (!(permission instanceof Enum<?> level)) {
                return false;
            }
            return "Access".equalsIgnoreCase(level.name()) || "Manage".equalsIgnoreCase(level.name());
        } catch (ReflectiveOperationException | RuntimeException e) {
            return true;
        }
    }

    /**
     * True when a container at the location may take items for the owner of a logistics device. An unclaimed container always does. In a
     * claim, the owner must own the claim or hold a container trust level there; a device with no owner (null) is refused. Fails closed:
     * when the claim API cannot answer, the container is refused.
     *
     * @param location the container's location
     * @param owner    the device's owner, or null when it stores none
     * @param name     the owner's name, for a GriefPrevention version that trusts by name; may be null
     */
    public boolean permitsContainer(Location location, UUID owner, String name) {
        try {
            Object claim = getClaimAt.invoke(dataStore, location, true, null);
            if (claim == null) {
                return true;
            }
            boolean trusted = owner != null && (trustedWithContainers(claim, owner, name) || ownsClaim(claim, owner));
            return ContainerAccess.permits(true, owner != null, trusted);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** True when the claim gives the player a trust level that lets a container take items. */
    private static boolean trustedWithContainers(Object claim, UUID player, String name) throws ReflectiveOperationException {
        Object permission = permissionOf(claim, player, name);
        return permission instanceof Enum<?> level && ContainerAccess.grantsContainer(level.name());
    }

    /** True when the player is the owner of the claim. */
    private static boolean ownsClaim(Object claim, UUID player) throws ReflectiveOperationException {
        return player.equals(claim.getClass().getMethod("getOwnerID").invoke(claim));
    }

    /** The trust level the claim gives the player, read through the claim's own permission lookup, by id or by name. */
    private static Object permissionOf(Object claim, UUID player, String name) throws ReflectiveOperationException {
        for (Method method : claim.getClass().getMethods()) {
            if (!method.getName().equals("getPermission") || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> type = method.getParameterTypes()[0];
            if (type == UUID.class) {
                return method.invoke(claim, player);
            }
            if (type == String.class) {
                return method.invoke(claim, name);
            }
        }
        throw new NoSuchMethodException("Claim.getPermission");
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
