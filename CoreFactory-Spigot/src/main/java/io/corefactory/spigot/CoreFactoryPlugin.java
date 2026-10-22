package io.corefactory.spigot;

import io.corefactory.api.CoreFactoryProvider;
import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.deposit.DepositSettings;
import io.corefactory.core.deposit.ScannerSettings;
import io.corefactory.core.deposit.DepositStore;
import io.corefactory.core.deposit.DrillYield;
import io.corefactory.core.drill.DrillCatalog;
import io.corefactory.core.geology.GeologyService;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessEngine;
import io.corefactory.spigot.machine.BarrelConcealment;
import io.corefactory.spigot.machine.DrillTicker;
import io.corefactory.spigot.machine.HopperListener;
import io.corefactory.spigot.machine.ItemRefs;
import io.corefactory.spigot.machine.MachineInteractions;
import io.corefactory.spigot.machine.SolarPanelListener;
import io.corefactory.spigot.machine.MachineListener;
import io.corefactory.spigot.machine.MachineMenu;
import io.corefactory.spigot.machine.MachineStore;
import io.corefactory.spigot.machine.MachineTicker;
import io.corefactory.spigot.machine.DrillRates;
import io.corefactory.spigot.machine.DrillResources;
import io.corefactory.spigot.ledger.DepositLedger;
import io.corefactory.spigot.research.ResearchCommand;
import io.corefactory.spigot.research.ResearchConsoleUi;
import io.corefactory.spigot.command.CoreFactoryCommand;
import io.corefactory.spigot.achievement.AchievementCatalog;
import io.corefactory.spigot.achievement.AchievementDatapack;
import io.corefactory.spigot.achievement.AchievementStore;
import io.corefactory.spigot.achievement.AchievementTracker;
import io.corefactory.spigot.geology.DepositChunkListener;
import io.corefactory.spigot.geology.DepositGenerator;
import io.corefactory.spigot.geology.GeologyCommand;
import io.corefactory.spigot.effects.SpriteEffects;
import io.corefactory.spigot.geology.GeologyListener;
import io.corefactory.spigot.geology.GeologyWorker;
import io.corefactory.spigot.guide.GuideBook;
import io.corefactory.spigot.friends.FriendsCommand;
import io.corefactory.spigot.gui.GuiListener;
import io.corefactory.spigot.hook.ContainerProtection;
import io.corefactory.spigot.hook.GriefPreventionGuard;
import io.corefactory.spigot.hook.PacketClientEffects;
import io.corefactory.spigot.hook.WorldEditBlockEditor;
import io.corefactory.spigot.hologram.HologramService;
import io.corefactory.spigot.hook.WorldGuardGuard;
import io.corefactory.spigot.listener.InteractionListener;
import io.corefactory.spigot.listener.LockListener;
import io.corefactory.spigot.lock.TntLocks;
import io.corefactory.spigot.listener.DeviceBlast;
import io.corefactory.spigot.listener.IgnitionListener;
import io.corefactory.spigot.listener.PlaceableItemGuard;
import io.corefactory.spigot.listener.PlacementListener;
import io.corefactory.spigot.listener.ProtectionListener;
import io.corefactory.core.research.ResearchTree;
import io.corefactory.spigot.config.DefaultsMerger;
import io.corefactory.spigot.energy.ChartCommand;
import io.corefactory.spigot.energy.ChartService;
import io.corefactory.spigot.energy.DeviceListener;
import io.corefactory.spigot.energy.DeviceRegistry;
import io.corefactory.spigot.energy.EnergyEffects;
import io.corefactory.spigot.energy.EnergyPlacementListener;
import io.corefactory.spigot.energy.EnergyService;
import io.corefactory.spigot.energy.EnergyStore;
import io.corefactory.spigot.logistics.LogisticsListener;
import io.corefactory.spigot.logistics.LogisticsPlacement;
import io.corefactory.spigot.logistics.LogisticsService;
import io.corefactory.spigot.pack.BundledPack;
import io.corefactory.spigot.radiation.GeigerDisplay;
import io.corefactory.spigot.research.ResearchGateListener;
import io.corefactory.spigot.research.ResearchRuntime;
import io.corefactory.spigot.scanner.DepositScanner;
import io.corefactory.spigot.turret.TurretCommand;
import io.corefactory.spigot.turret.TurretIds;
import io.corefactory.spigot.turret.TurretRuntime;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

/**
 * Entry point. Checks the hard dependencies, builds the core with its WorldEdit and PacketEvents bridges,
 * and wires listeners, the command and the per-tick work.
 */
public final class CoreFactoryPlugin extends JavaPlugin {

    private CoreFactoryCore core;
    private ResearchTree researchTree;
    private ProcessCatalog processCatalog;
    private List<OreDefinition> oreDefinitions;
    private AchievementDatapack.DataFormat dataFormat;
    private AchievementStore achievementStore;
    private AchievementTracker achievements;
    private BukkitTask ticker;
    private TurretRuntime turrets;
    private BukkitTask turretTicker;
    private BukkitTask recipeWatch;
    private BukkitTask geigerWatch;
    private BukkitTask saver;
    private ResearchRuntime research;
    private MachineStore machines;
    private BukkitTask machineTicker;
    private BukkitTask resourcesWatch;
    private SpriteEffects sprites;
    private HologramService holograms;
    private BarrelConcealment concealment;
    private EnergyService energy;
    private ChartService charts;
    private LogisticsService logistics;
    private ContainerProtection containers;
    private EnergyEffects energyEffects;
    private BukkitTask energyTicker;

    @Override
    public void onLoad() {
        // Before any plugin is enabled, so the pack is in place when the Resource Pack Manager reads its folder.
        BundledPack.install(this);
        // Written before the worlds load, so the server reads a valid pack.mcmeta at this start.
        File level = levelFolder();
        dataFormat = AchievementDatapack.dataFormat(level, getClass().getClassLoader()).orElse(null);
        try {
            if (dataFormat == null) {
                // Without a known format the server would reject the pack, so the old one is removed.
                AchievementDatapack.remove(level);
                getLogger().warning("Could not read this server's data pack format yet, so the achievements datapack "
                        + "was removed for this start. It is written once the world has loaded.");
            } else {
                AchievementDatapack.writePackMeta(level, dataFormat);
            }
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Could not update the achievements datapack.", e);
        }
    }

    @Override
    public void onEnable() {
        PluginManager manager = getServer().getPluginManager();
        if (manager.getPlugin("packetevents") == null) {
            getLogger().severe("PacketEvents (plugin name 'packetevents') is required and was not found. Disabling CoreFactory.");
            manager.disablePlugin(this);
            return;
        }
        if (manager.getPlugin("WorldEdit") == null && manager.getPlugin("FastAsyncWorldEdit") == null) {
            getLogger().severe("WorldEdit or FastAsyncWorldEdit is required and neither was found. Disabling CoreFactory.");
            manager.disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        saveResourceIfMissing("tnts.yml");
        saveResourceIfMissing("items.yml");
        saveResourceIfMissing("materials.yml");
        saveResourceIfMissing("processes.yml");
        saveResourceIfMissing("ores.yml");
        saveResourceIfMissing("research.yml");
        DefaultsMerger.merge(this);
        researchTree = ResearchTree.parse(
                YamlConfiguration.loadConfiguration(new File(getDataFolder(), "research.yml")), getLogger());
        YamlConfiguration processesFile = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "processes.yml"));
        processCatalog = ProcessCatalog.parse(processesFile, getLogger());
        oreDefinitions = OreDefinition.parse(YamlConfiguration.loadConfiguration(new File(getDataFolder(), "ores.yml"))
                .getConfigurationSection("ores"), getLogger());

        WorldEditBlockEditor blockEditor = new WorldEditBlockEditor(getLogger());
        if (!getServer().getWorlds().isEmpty()) {
            blockEditor.verify(getServer().getWorlds().get(0));
        }
        core = new CoreFactoryCore(this, blockEditor, new PacketClientEffects());
        sprites = new SpriteEffects(this, getConfig().getBoolean("effects.enabled", true),
                getConfig().getInt("effects.max-sprites", 600));
        core.setVisuals(sprites);
        core.setOres(oreDefinitions);
        reloadConfiguration();
        core.loadPlaced();
        registerIntegrations(manager);
        CoreFactoryProvider.set(core);

        research = ResearchRuntime.open(this, getConfig(), researchTree);
        core.setResearch(research.service());
        core.setProcesses(processCatalog);
        manager.registerEvents(new ResearchGateListener(core, research.service()), this);
        for (Player online : Bukkit.getOnlinePlayers()) {
            research.service().load(online.getUniqueId());
        }

        manager.registerEvents(new PlacementListener(core), this);
        manager.registerEvents(new IgnitionListener(core), this);
        manager.registerEvents(new LockListener(core, new TntLocks(core)), this);
        manager.registerEvents(new ProtectionListener(core), this);
        achievementStore = new AchievementStore(new File(getDataFolder(), "achievements.yml"), getLogger());
        achievementStore.load();
        achievements = new AchievementTracker(this, core, achievementStore,
                AchievementCatalog.build(core, researchTree, processCatalog, oreDefinitions));
        research.service().onFinished(achievements::researched);
        writeAdvancements();
        manager.registerEvents(achievements, this);
        manager.registerEvents(new InteractionListener(core, achievements), this);
        manager.registerEvents(new GuiListener(), this);
        DepositStore deposits = DepositLedger.open(research.database(), getLogger());
        DepositSettings depositSettings = DepositSettings.from(getConfig(), getLogger());
        DrillYield drillYield = DrillRates.read(getConfig().getDoubleList("drill-rates"), getLogger());
        Map<String, OreDefinition> oreById = new LinkedHashMap<>();
        for (OreDefinition ore : oreDefinitions) {
            oreById.putIfAbsent(ore.id(), ore);
        }
        manager.registerEvents(new DepositChunkListener(new DepositGenerator(deposits, depositSettings, oreDefinitions)), this);
        GeologyWorker geology = new GeologyWorker(core, new GeologyService(oreDefinitions), getLogger());
        manager.registerEvents(new GeologyListener(core, geology, deposits, depositSettings, achievements, sprites), this);

        ProcessCatalog processes = processCatalog;
        ItemRefs itemRefs = new ItemRefs(core);
        ProcessEngine engine = new ProcessEngine(processes, ProcessEngine.FUEL, itemRefs::limit);
        holograms = new HologramService(this, getConfig().getBoolean("holograms.enabled", true),
                getConfig().getDouble("holograms.view-distance", 32.0));
        manager.registerEvents(holograms, this);
        DrillCatalog drills = DrillCatalog.parse(processesFile, core.componentDefinitions(), getLogger());
        DrillResources resources = new DrillResources(oreById, () -> {
            if (machines != null) {
                machines.save();
            }
        });
        machines = new MachineStore(core, processes, drills, (machine, layout, site, slots, fuels) -> {
                if (machine.id().equals("research_console")) {
                    return new ResearchConsoleUi(core, processes, machine, layout, site, research.service(), sprites);
                }
                MachineMenu menu = new MachineMenu(core, processes, machine, layout, site, slots, fuels);
                menu.onButton(player -> machines.at(site).ifPresent(placed -> resources.open(placed, player)));
                return menu;
            },
                new File(getDataFolder(), "machines.yml"), getLogger(), holograms);
        machines.load();
        DeviceRegistry devices = new DeviceRegistry(core, holograms, getLogger());
        energyEffects = new EnergyEffects(this);
        energy = new EnergyService(machines, devices, new EnergyStore(new File(getDataFolder(), "energy.yml"), getLogger()),
                energyEffects, getConfig().getInt("energy.save-interval-ticks", 300),
                getConfig().getInt("energy.consumers.lab-demand", 4000), getConfig().getInt("energy.consumers.drill-demand", 15000),
                getLogger());
        energy.load();
        manager.registerEvents(new DeviceListener(core, devices, energy), this);
        manager.registerEvents(new EnergyPlacementListener(core, devices, energy, machines, research.service()), this);
        turrets = TurretRuntime.open(this, core, energy, devices, machines, research.service(), holograms,
                new File(getDataFolder(), "turrets.yml"), getLogger());
        manager.registerEvents(turrets.listener(), this);
        turretTicker = turrets.start(this);
        containers = ContainerProtection.of(this, getConfig(), getLogger());
        logistics = new LogisticsService(core, machines, devices, energy, holograms, turrets.registry(), itemRefs, containers,
                new File(getDataFolder(), "logistics.yml"), getConfig().getInt("logistics.max-item-displays", 2000),
                getConfig().getInt("logistics.save-interval-ticks", 300), getLogger());
        logistics.load();
        energy.addConsumerSource(logistics);
        charts = new ChartService(energy, holograms, this, getConfig().getInt("charts.charts-per-grid", 1),
                getConfig().getInt("charts.charts-total", 5), getConfig().getInt("charts.refresh-ticks", 10), getLogger());
        manager.registerEvents(charts, this);
        manager.registerEvents(new LogisticsListener(core, logistics, machines, devices,
                new LogisticsPlacement(core, logistics, research.service()), itemRefs), this);
        DeviceBlast deviceBlast = new DeviceBlast(core, machines, devices, energy, logistics);
        manager.registerEvents(deviceBlast, this);
        core.addDetonationObserver(deviceBlast);
        MachineListener machineListener = new MachineListener(core, processes, drills, machines, devices, achievements);
        manager.registerEvents(machineListener, this);
        concealment = new BarrelConcealment(this, core.clientEffects(), machines,
                getConfig().getBoolean("concealment.enabled", true), getConfig().getDouble("concealment.range", 48.0));
        manager.registerEvents(concealment, this);
        manager.registerEvents(new MachineInteractions(core, machines, machineListener), this);
        manager.registerEvents(new HopperListener(core, machines, containers), this);
        manager.registerEvents(new SolarPanelListener(core), this);
        // The turrets place through their own listener, which fires the same placement check; this lets that check through.
        manager.registerEvents(new PlaceableItemGuard(core, id ->
                processes.machine(id).isPresent() || drills.machine(id).isPresent() || TurretIds.ITEM.equals(id)
                        || TurretIds.LASER_ITEM.equals(id)), this);
        DrillTicker drillTicker = new DrillTicker(machines, holograms, deposits, depositSettings, drillYield, oreById, energy,
                getLogger());

        DepositScanner depositScanner = new DepositScanner(this, core, deposits, depositSettings,
                ScannerSettings.from(getConfig(), getLogger()));
        manager.registerEvents(depositScanner, this);

        GuideBook guide = new GuideBook(core, researchTree, processCatalog, oreDefinitions);
        CoreFactoryCommand command = new CoreFactoryCommand(this, core, achievements, new GeologyCommand(core, deposits, depositSettings),
                new ResearchCommand(core, research.service()), guide, energy, energyEffects, new ChartCommand(charts),
                new FriendsCommand(turrets.friends()), new TurretCommand(turrets.service(), turrets.registry()), depositScanner);
        if (getCommand("cf") != null) {
            getCommand("cf").setExecutor(command);
            getCommand("cf").setTabCompleter(command);
        }

        ticker = Bukkit.getScheduler().runTaskTimer(this, core::tick, 1L, 1L);
        saver = Bukkit.getScheduler().runTaskTimer(this, achievementStore::save, 6000L, 6000L);
        // A data reload (a datapack enabled or disabled) drops the recipes added through the API. Put them back.
        recipeWatch = Bukkit.getScheduler().runTaskTimer(this, () -> core.recipes().restoreIfMissing(), 40L, 40L);
        geigerWatch = Bukkit.getScheduler().runTaskTimer(this, new GeigerDisplay(core), 10L, 10L);
        resourcesWatch = Bukkit.getScheduler().runTaskTimer(this, resources::refreshOpen, 40L, 40L);
        energyTicker = Bukkit.getScheduler().runTaskTimer(this, () -> {
            energy.tick();
            logistics.tick();
        }, 1L, 1L);
        machineTicker = Bukkit.getScheduler().runTaskTimer(this,
                new MachineTicker(machines, processes, engine, drillTicker, sprites, holograms, energy,
                        core.settings().saveIntervalTicks()),
                1L, 1L);
        sprites.start();
        holograms.start();
        charts.start();
        concealment.start();
        getLogger().info("CoreFactory enabled: " + core.definitions().size() + " TNT types, "
                + core.placedTnts().size() + " placed.");
    }

    @Override
    public void onDisable() {
        if (turretTicker != null) {
            turretTicker.cancel();
            turretTicker = null;
        }
        if (turrets != null) {
            turrets.shutdown();
            turrets = null;
        }
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        if (recipeWatch != null) {
            recipeWatch.cancel();
            recipeWatch = null;
        }
        if (geigerWatch != null) {
            geigerWatch.cancel();
            geigerWatch = null;
        }
        if (energyTicker != null) {
            energyTicker.cancel();
            energyTicker = null;
        }
        if (logistics != null) {
            logistics.shutdown();
            logistics = null;
        }
        if (charts != null) {
            charts.shutdown();
            charts = null;
        }
        if (energy != null) {
            energy.shutdown();
            energy = null;
        }
        if (machineTicker != null) {
            machineTicker.cancel();
            machineTicker = null;
        }
        if (resourcesWatch != null) {
            resourcesWatch.cancel();
            resourcesWatch = null;
        }
        if (sprites != null) {
            sprites.stop();
            sprites = null;
        }
        if (holograms != null) {
            holograms.shutdown();
            holograms = null;
        }
        if (concealment != null) {
            concealment.stop();
            concealment = null;
        }
        if (machines != null) {
            machines.save();
            machines = null;
        }
        if (saver != null) {
            saver.cancel();
            saver = null;
        }
        if (achievementStore != null) {
            achievementStore.save();
        }
        if (core != null) {
            core.savePlaced();
            core.shutdown();
        }
        if (research != null) {
            research.close();
            research = null;
        }
        CoreFactoryProvider.set(null);
    }

    /** The level folder from server.properties, which the worlds load from. */
    private File levelFolder() {
        Properties properties = new Properties();
        File file = new File(Bukkit.getWorldContainer(), "server.properties");
        if (file.isFile()) {
            try (InputStream in = new FileInputStream(file)) {
                properties.load(in);
            } catch (IOException e) {
                getLogger().log(Level.WARNING, "Could not read server.properties.", e);
            }
        }
        return new File(Bukkit.getWorldContainer(), properties.getProperty("level-name", "world").trim());
    }

    /**
     * Writes the achievements as a datapack in the main world, so the Advancements screen shows them. The server reads
     * datapacks at startup, so a new datapack shows after one restart.
     */
    private void writeAdvancements() {
        if (Bukkit.getWorlds().isEmpty()) {
            getLogger().warning("No world is loaded, so the achievements datapack was not written.");
            return;
        }
        if (dataFormat == null) {
            // The world has loaded now, so its own bukkit data pack can be read, even on a first start.
            dataFormat = AchievementDatapack.dataFormat(levelFolder(), getClass().getClassLoader()).orElse(null);
        }
        if (dataFormat == null) {
            getLogger().warning("This server's data pack format is still unknown, so the achievements were not written.");
            return;
        }
        try {
            AchievementDatapack.write(Bukkit.getWorlds().get(0).getWorldFolder(), achievements.catalog(), dataFormat);
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Could not write the achievements datapack.", e);
            return;
        }
        if (Bukkit.getAdvancement(AchievementDatapack.root()) == null) {
            getLogger().info("Achievements are in the datapack " + AchievementDatapack.FOLDER
                    + ", but the server has not loaded it yet. Run in the console: datapack enable \"file/"
                    + AchievementDatapack.FOLDER + "\". If that is not enough, restart once.");
        }
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
        YamlConfiguration items = componentsOf(itemFile, new File(getDataFolder(), "materials.yml"));
        core.setItemComponentIds(componentIdsOf(itemFile));
        int count = core.reload(getConfig(), tnts, items, getDataFolder());
        if (achievements != null) {
            achievements.setCatalog(AchievementCatalog.build(core, researchTree, processCatalog, oreDefinitions));
        }
        return count;
    }

    public CoreFactoryCore core() {
        return core;
    }

    /**
     * One configuration holding the components of every file given: items.yml and materials.yml are read as one list.
     * A component id defined twice keeps its first definition and logs a warning.
     */
    private YamlConfiguration componentsOf(File... files) {
        YamlConfiguration merged = new YamlConfiguration();
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            ConfigurationSection source = YamlConfiguration.loadConfiguration(file).getConfigurationSection("components");
            if (source == null) {
                continue;
            }
            for (String id : source.getKeys(false)) {
                ConfigurationSection component = source.getConfigurationSection(id);
                if (component == null) {
                    continue;
                }
                if (merged.contains("components." + id)) {
                    getLogger().warning("Component '" + id + "' is defined in more than one file. The first one is used.");
                    continue;
                }
                merged.createSection("components." + id, component.getValues(true));
            }
        }
        return merged;
    }

    /** The ids of the components one file defines, or none when the file is missing. */
    private static Set<String> componentIdsOf(File file) {
        if (!file.isFile()) {
            return Set.of();
        }
        ConfigurationSection section = YamlConfiguration.loadConfiguration(file).getConfigurationSection("components");
        return section == null ? Set.of() : Set.copyOf(section.getKeys(false));
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
