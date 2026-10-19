package io.moretntplus.spigot;

import io.moretntplus.api.MoreTntPlusProvider;
import io.moretntplus.core.MoreTntPlusCore;
import io.moretntplus.spigot.command.MoreTntCommand;
import io.moretntplus.spigot.gui.CatalogListener;
import io.moretntplus.spigot.hook.GriefPreventionGuard;
import io.moretntplus.spigot.hook.PacketClientEffects;
import io.moretntplus.spigot.hook.WorldEditBlockEditor;
import io.moretntplus.spigot.hook.WorldGuardGuard;
import io.moretntplus.spigot.listener.InteractionListener;
import io.moretntplus.spigot.listener.IgnitionListener;
import io.moretntplus.spigot.listener.PlacementListener;
import io.moretntplus.spigot.listener.ProtectionListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;

/**
 * Entry point. Checks the hard dependencies, builds the core with its WorldEdit and PacketEvents bridges,
 * and wires listeners, the command and the per-tick work.
 */
public final class MoreTntPlusPlugin extends JavaPlugin {

    private MoreTntPlusCore core;
    private BukkitTask ticker;

    @Override
    public void onEnable() {
        PluginManager manager = getServer().getPluginManager();
        if (manager.getPlugin("packetevents") == null) {
            getLogger().severe("PacketEvents (plugin name 'packetevents') is required and was not found. Disabling MoreTNT+.");
            manager.disablePlugin(this);
            return;
        }
        if (manager.getPlugin("WorldEdit") == null && manager.getPlugin("FastAsyncWorldEdit") == null) {
            getLogger().severe("WorldEdit or FastAsyncWorldEdit is required and neither was found. Disabling MoreTNT+.");
            manager.disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        saveResourceIfMissing("tnts.yml");
        saveResourceIfMissing("items.yml");

        WorldEditBlockEditor blockEditor = new WorldEditBlockEditor(getLogger());
        if (!getServer().getWorlds().isEmpty()) {
            blockEditor.verify(getServer().getWorlds().get(0));
        }
        core = new MoreTntPlusCore(this, blockEditor, new PacketClientEffects());
        reloadConfiguration();
        core.loadPlaced();
        registerIntegrations(manager);
        MoreTntPlusProvider.set(core);

        manager.registerEvents(new PlacementListener(core), this);
        manager.registerEvents(new IgnitionListener(core), this);
        manager.registerEvents(new ProtectionListener(core), this);
        manager.registerEvents(new InteractionListener(core), this);
        manager.registerEvents(new CatalogListener(core), this);

        MoreTntCommand command = new MoreTntCommand(this, core);
        if (getCommand("mtnt") != null) {
            getCommand("mtnt").setExecutor(command);
            getCommand("mtnt").setTabCompleter(command);
        }

        ticker = Bukkit.getScheduler().runTaskTimer(this, core::tick, 1L, 1L);
        getLogger().info("MoreTNT+ enabled: " + core.definitions().size() + " TNT types, "
                + core.placedTnts().size() + " placed.");
    }

    @Override
    public void onDisable() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        if (core != null) {
            core.savePlaced();
            core.shutdown();
        }
        MoreTntPlusProvider.set(null);
    }

    /**
     * Reloads config.yml, tnts.yml and items.yml, and rebuilds the crafting recipes.
     *
     * @return the number of TNT types and components loaded
     */
    public int reloadConfiguration() {
        reloadConfig();
        File tntFile = new File(getDataFolder(), "tnts.yml");
        File itemFile = new File(getDataFolder(), "items.yml");
        YamlConfiguration tnts = YamlConfiguration.loadConfiguration(tntFile);
        YamlConfiguration items = YamlConfiguration.loadConfiguration(itemFile);
        return core.reload(getConfig(), tnts, items, getDataFolder());
    }

    public MoreTntPlusCore core() {
        return core;
    }

    /**
     * Connects blasts to the claim systems that are installed, so explosions respect them. Each integration can be
     * switched off in config.yml. An integration that fails to attach is logged and skipped; the plugin still runs.
     */
    private void registerIntegrations(PluginManager manager) {
        if (getConfig().getBoolean("integrations.worldguard", true) && manager.getPlugin("WorldGuard") != null) {
            try {
                core.addClaimGuard(new WorldGuardGuard());
                getLogger().info("WorldGuard found: blasts respect region TNT flags.");
            } catch (LinkageError | RuntimeException e) {
                getLogger().warning("WorldGuard found but could not be linked; blasts will ignore regions. " + e);
            }
        }
        if (getConfig().getBoolean("integrations.griefprevention", true)) {
            Plugin griefPrevention = manager.getPlugin("GriefPrevention");
            if (griefPrevention != null) {
                GriefPreventionGuard.create(griefPrevention, getLogger()).ifPresent(core::addClaimGuard);
                getLogger().info("GriefPrevention found: blasts stay out of claims.");
            }
        }
    }

    private void saveResourceIfMissing(String name) {
        if (!new File(getDataFolder(), name).exists()) {
            saveResource(name, false);
        }
    }
}
