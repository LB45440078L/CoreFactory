package io.corefactory.spigot.turret;

import io.corefactory.core.engine.ClaimGuard;
import io.corefactory.spigot.hook.GriefPreventionGuard;
import io.corefactory.spigot.hook.WorldGuardGuard;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The claim systems a turret respects, built from the same hooks the blasts use: WorldGuard regions and GriefPrevention claims, each only
 * when its plugin is installed and its integration is on in config.yml. A mob standing where a guard protects is never a target and never
 * takes a round. The guards are asked once per tick through {@link #beginTick}, as the blasts ask them.
 *
 * <p>The same systems also name the friends of a turret's claim: a player who is a member or owner of the WorldGuard region, or trusted in
 * the GriefPrevention claim, where the turret stands counts as a friend. Each such check fails closed, so a failing hook keeps its player
 * from being a target.
 */
public final class TurretProtection {

    /** Whether a player is trusted at a location by one claim system. */
    @FunctionalInterface
    interface Trust {
        boolean trusts(Location location, UUID player, String name);
    }

    private final List<ClaimGuard> guards;
    private final List<Trust> trusts;

    private TurretProtection(List<ClaimGuard> guards, List<Trust> trusts) {
        this.guards = List.copyOf(guards);
        this.trusts = List.copyOf(trusts);
    }

    /**
     * Builds the guards of the installed claim systems.
     *
     * @param plugin the plugin, whose server finds the claim plugins
     * @param config the plugin's configuration, for the integration switches
     * @param log    receives the failures to attach a guard
     */
    public static TurretProtection of(Plugin plugin, FileConfiguration config, Logger log) {
        PluginManager manager = plugin.getServer().getPluginManager();
        List<ClaimGuard> guards = new ArrayList<>();
        List<Trust> trusts = new ArrayList<>();
        if (config.getBoolean("integrations.worldguard", true) && manager.getPlugin("WorldGuard") != null) {
            try {
                WorldGuardGuard worldGuard = new WorldGuardGuard();
                guards.add(worldGuard);
                trusts.add((location, player, name) -> worldGuard.isMember(location, player));
            } catch (LinkageError | RuntimeException e) {
                log.log(Level.WARNING, "WorldGuard found but could not be linked; turrets will ignore regions.", e);
            }
        }
        if (config.getBoolean("integrations.griefprevention", true)) {
            Plugin griefPrevention = manager.getPlugin("GriefPrevention");
            if (griefPrevention != null) {
                GriefPreventionGuard.create(griefPrevention, log).ifPresent(claims -> {
                    guards.add(claims);
                    trusts.add(claims::trusts);
                });
            }
        }
        return new TurretProtection(guards, trusts);
    }

    /** Passes the new tick on to every guard so it can drop its per-tick answers. */
    public void beginTick() {
        for (ClaimGuard guard : guards) {
            guard.beginTick();
        }
    }

    /** True when a claim system protects the block at this location. */
    public boolean protects(Location location) {
        if (guards.isEmpty() || location.getWorld() == null) {
            return false;
        }
        Block block = location.getBlock();
        for (ClaimGuard guard : guards) {
            if (guard.protects(block)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when a claim system trusts the player at this location: a WorldGuard member or owner of a region there, or a GriefPrevention
     * player trusted in the claim there. Used for a turret's friends, with the turret's own block as the location.
     *
     * @param location the location whose regions and claims are asked
     * @param player   the player's id
     * @param name     the player's name, for a claim system that trusts by name
     */
    public boolean trusts(Location location, UUID player, String name) {
        if (trusts.isEmpty() || location.getWorld() == null) {
            return false;
        }
        for (Trust trust : trusts) {
            if (trust.trusts(location, player, name)) {
                return true;
            }
        }
        return false;
    }

    /** True when no claim system is installed, so nothing is protected by one. */
    public boolean isEmpty() {
        return guards.isEmpty();
    }
}
