package io.corefactory.paper.hook;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Decides whether a logistics device may put items into a vanilla container that a claim system protects: a chest, a barrel, a
 * shulker box or a copper chest. It uses the same claim systems as the blasts and the turrets, each only when its plugin is installed and
 * its integration is on in config.yml.
 *
 * <p>The owner is the player who placed the device, when a device stores one. Belts, pickers and splitters store their placer
 * (logistics.yml, owner); a device placed before that is stored with no owner, and a protected container refuses it. A device with an owner must be trusted in every system that protects
 * the container. With no claim plugin installed, every container is open. A claim plugin that is installed but whose hook did not attach
 * closes every container, so a failed hook never opens a claim.
 */
public final class ContainerProtection {

    private final GriefPreventionGuard claims;
    private final WorldGuardGuard regions;
    private final boolean unavailable;

    private ContainerProtection(GriefPreventionGuard claims, WorldGuardGuard regions, boolean unavailable) {
        this.claims = claims;
        this.regions = regions;
        this.unavailable = unavailable;
    }

    /**
     * Builds the hooks of the installed claim systems.
     *
     * @param plugin the plugin, whose server finds the claim plugins
     * @param config the plugin's configuration, for the integration switches
     * @param log    receives the failures to attach a hook
     */
    public static ContainerProtection of(Plugin plugin, FileConfiguration config, Logger log) {
        PluginManager manager = plugin.getServer().getPluginManager();
        boolean unavailable = false;
        WorldGuardGuard regions = null;
        if (config.getBoolean("integrations.worldguard", true) && manager.getPlugin("WorldGuard") != null) {
            try {
                regions = new WorldGuardGuard();
            } catch (LinkageError | RuntimeException e) {
                unavailable = true;
                log.log(Level.WARNING, "WorldGuard found but could not be linked; containers in its regions are closed to devices.", e);
            }
        }
        GriefPreventionGuard claims = null;
        if (config.getBoolean("integrations.griefprevention", true)) {
            Plugin griefPrevention = manager.getPlugin("GriefPrevention");
            if (griefPrevention != null) {
                claims = GriefPreventionGuard.create(griefPrevention, log).orElse(null);
                unavailable |= claims == null;
            }
        }
        return new ContainerProtection(claims, regions, unavailable);
    }

    /** True when no claim system is installed, so no container is protected by one. */
    public boolean isEmpty() {
        return !unavailable && claims == null && regions == null;
    }

    /**
     * True when a logistics device with this owner may put items into the block. Asked again for each insert, since claims can change.
     *
     * @param block the container's block
     * @param owner the device's owner, or null when it stores none
     */
    public boolean permits(Block block, UUID owner) {
        return permits(block.getLocation(), owner);
    }

    /**
     * True when a logistics device with this owner may put items into the container at the location. A location that is not known, such
     * as an entity's inventory, is refused whenever a claim system is installed.
     *
     * @param location the container's location, or null when it has none
     * @param owner    the device's owner, or null when it stores none
     */
    public boolean permits(Location location, UUID owner) {
        if (unavailable) {
            return false;
        }
        if (isEmpty()) {
            return true;
        }
        if (location == null || location.getWorld() == null) {
            return false;
        }
        String name = owner == null ? null : nameOf(owner);
        try {
            if (claims != null && !claims.permitsContainer(location, owner, name)) {
                return false;
            }
            return regions == null || regions.permitsContainer(location, owner);
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** The name of a player by id, for a claim system that trusts by name; null when none is known. */
    private static String nameOf(UUID player) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(player);
        return offline.getName();
    }
}
