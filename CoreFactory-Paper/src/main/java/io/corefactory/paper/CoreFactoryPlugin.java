package io.corefactory.paper;

import io.corefactory.api.CoreFactoryProvider;
import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.deposit.DepositSettings;
import io.corefactory.core.deposit.ScannerSettings;
import io.corefactory.core.placement.PlacedTntFile;
import io.corefactory.core.deposit.DepositStore;
import io.corefactory.core.deposit.DrillYield;
import io.corefactory.core.drill.DrillCatalog;
import io.corefactory.core.geology.GeologyService;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessEngine;
import io.corefactory.core.upgrade.LabLevels;
import io.corefactory.paper.machine.BarrelConcealment;
import io.corefactory.paper.machine.DrillTicker;
import io.corefactory.paper.machine.HopperListener;
import io.corefactory.paper.machine.ItemRefs;
import io.corefactory.paper.machine.MachineInteractions;
import io.corefactory.paper.machine.SolarPanelListener;
import io.corefactory.paper.machine.MachineListener;
import io.corefactory.paper.machine.MachineMenu;
import io.corefactory.paper.machine.MachineStore;
import io.corefactory.paper.machine.MachineTicker;
import io.corefactory.paper.machine.RefineryTicker;
import io.corefactory.paper.machine.DrillRates;
import io.corefactory.paper.machine.LabUpgradeRates;
import io.corefactory.paper.machine.DrillResources;
import io.corefactory.paper.ledger.DepositLedger;
import io.corefactory.paper.research.ResearchCommand;
import io.corefactory.paper.research.ResearchConsoleUi;
import io.corefactory.paper.chunks.ChunkKey;
import io.corefactory.paper.chunks.ChunkKeeper;
import io.corefactory.paper.chunks.ChunkTickets;
import io.corefactory.paper.command.CfCommand;
import io.corefactory.paper.command.CoreFactoryCommand;
import io.corefactory.paper.achievement.AchievementCatalog;
import io.corefactory.paper.achievement.AchievementDatapack;
import io.corefactory.paper.achievement.AchievementStore;
import io.corefactory.paper.achievement.AchievementTracker;
import io.corefactory.paper.geology.DepositChunkListener;
import io.corefactory.paper.geology.DepositGenerator;
import io.corefactory.paper.geology.GeologyCommand;
import io.corefactory.paper.effects.SpriteEffects;
import io.corefactory.paper.geology.GeologyListener;
import io.corefactory.paper.geology.GeologyWorker;
import io.corefactory.paper.guide.GuideBook;
import io.corefactory.paper.friends.FriendAccess;
import io.corefactory.paper.friends.FriendStore;
import io.corefactory.paper.friends.Friends;
import io.corefactory.paper.friends.FriendsCommand;
import io.corefactory.paper.gui.GuiListener;
import io.corefactory.paper.hook.ContainerProtection;
import io.corefactory.paper.hook.GriefPreventionGuard;
import io.corefactory.paper.hook.BukkitClientEffects;
import io.corefactory.paper.hook.WorldEditBlockEditor;
import io.corefactory.paper.hologram.HologramService;
import io.corefactory.paper.hook.WorldGuardGuard;
import io.corefactory.paper.listener.InteractionListener;
import io.corefactory.paper.listener.LockListener;
import io.corefactory.paper.lock.TntLocks;
import io.corefactory.paper.listener.DeviceBlast;
import io.corefactory.paper.listener.IgnitionListener;
import io.corefactory.paper.listener.PlaceableItemGuard;
import io.corefactory.paper.listener.PlacementListener;
import io.corefactory.paper.listener.RecipeRestoreListener;
import io.corefactory.paper.listener.ProtectionListener;
import io.corefactory.core.research.ResearchTree;
import io.corefactory.paper.config.DefaultsMerger;
import io.corefactory.paper.config.YamlMaps;
import io.corefactory.paper.energy.ChartCommand;
import io.corefactory.paper.energy.ChartService;
import io.corefactory.paper.energy.DeviceListener;
import io.corefactory.paper.energy.DeviceRegistry;
import io.corefactory.paper.energy.EnergyEffects;
import io.corefactory.paper.energy.EnergyPlacementListener;
import io.corefactory.paper.energy.EnergyService;
import io.corefactory.paper.energy.EnergyStore;
import io.corefactory.paper.logistics.LogisticsListener;
import io.corefactory.paper.logistics.LogisticsPlacement;
import io.corefactory.paper.logistics.LogisticsService;
import io.corefactory.paper.pack.BundledPack;
import io.corefactory.paper.radiation.GeigerDisplay;
import io.corefactory.paper.research.ResearchGateListener;
import io.corefactory.paper.artillery.ArtilleryIds;
import io.corefactory.paper.artillery.ArtilleryRuntime;
import io.corefactory.paper.research.ResearchRuntime;
import io.corefactory.paper.scanner.DepositScanner;
import io.corefactory.paper.turret.TurretCommand;
import io.corefactory.paper.turret.TurretIds;
import io.corefactory.paper.turret.TurretRuntime;
import io.corefactory.paper.oil.OilListener;
import io.corefactory.paper.oil.OilService;
import io.corefactory.paper.oil.OilStore;
import io.corefactory.paper.water.WaterListener;
import io.corefactory.paper.water.WaterService;
import io.corefactory.paper.water.WaterStore;
import io.corefactory.core.water.WaterPart;
import io.corefactory.core.water.WaterSettings;
import io.corefactory.core.oil.OilPart;
import io.corefactory.core.oil.OilSettings;
import io.corefactory.paper.vertical.VerticalListener;
import io.corefactory.paper.vertical.VerticalService;
import io.corefactory.paper.vertical.VerticalSettings;
import io.corefactory.paper.vertical.VerticalStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

/**
 * Entry point. Checks the hard dependencies, builds the core with its WorldEdit bridge, and wires listeners, the command and the per-tick
 * work.
 */
public final class CoreFactoryPlugin extends JavaPlugin {

    private CoreFactoryCore core;
    private ResearchTree researchTree;
    private ProcessCatalog processCatalog;
    private List<OreDefinition> oreDefinitions;
    private AchievementDatapack.DataFormat dataFormat;
    private AchievementStore achievementStore;
    private AchievementTracker achievements;
    private TickScheduler ticks;
    private ChunkKeeper chunkKeeper;
    private TurretRuntime turrets;
    private ArtilleryRuntime artillery;
    private ResearchRuntime research;
    private MachineStore machines;
    private SpriteEffects sprites;
    private HologramService holograms;
    private BarrelConcealment concealment;
    private EnergyService energy;
    private ChartService charts;
    private LogisticsService logistics;
    private VerticalService vertical;
    private ContainerProtection containers;
    private EnergyEffects energyEffects;
    private WaterService water;
    private OilService oil;

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
        CorePermissions.register(manager);
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
        researchTree = ResearchTree.parse(YamlMaps.loadFile(new File(getDataFolder(), "research.yml")), getLogger());
        Map<String, Object> processesFile = YamlMaps.loadFile(new File(getDataFolder(), "processes.yml"));
        processCatalog = ProcessCatalog.parse(processesFile, getLogger());
        oreDefinitions = OreDefinition.parse(YamlMaps.loadSection(new File(getDataFolder(), "ores.yml"), "ores"), getLogger());

        WorldEditBlockEditor blockEditor = new WorldEditBlockEditor(getLogger());
        if (!getServer().getWorlds().isEmpty()) {
            blockEditor.verify(getServer().getWorlds().get(0));
        }
        core = new CoreFactoryCore(this, blockEditor, new BukkitClientEffects());
        ticks = new TickScheduler(getLogger());
        sprites = new SpriteEffects(this, ticks, getConfig().getBoolean("effects.enabled", true),
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
        manager.registerEvents(new RecipeRestoreListener(core), this);
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
        DepositSettings depositSettings = DepositSettings.from(configData(), getLogger());
        DrillYield drillYield = DrillRates.read(getConfig().getDoubleList("drill-rates"), getLogger());
        LabLevels labLevels = LabUpgradeRates.read(getConfig().getDoubleList("machine-upgrades.speed"), getLogger());
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
        holograms = new HologramService(getConfig().getBoolean("holograms.enabled", true),
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
        energyEffects = new EnergyEffects(this, ticks);
        energy = new EnergyService(machines, devices, new EnergyStore(new File(getDataFolder(), "energy.yml"), getLogger()),
                energyEffects, getConfig().getInt("energy.save-interval-ticks", 300),
                getConfig().getInt("energy.consumers.lab-demand", 4000), getConfig().getInt("energy.consumers.drill-demand", 15000),
                getLogger());
        energy.load();
        manager.registerEvents(new DeviceListener(core, devices, energy), this);
        Friends friends = new Friends(new FriendStore(new File(getDataFolder(), "friends.yml"), getLogger()));
        FriendAccess friendAccess = new FriendAccess(friends);
        turrets = TurretRuntime.open(this, core, energy, devices, machines, research.service(), holograms, friendAccess,
                new File(getDataFolder(), "turrets.yml"), getLogger());
        manager.registerEvents(turrets.listener(), this);
        turrets.register(ticks);
        artillery = ArtilleryRuntime.open(this, core, energy, devices, machines, turrets.registry(), research.service(), friendAccess,
                new File(getDataFolder(), "artillery.yml"), getLogger());
        manager.registerEvents(artillery.listener(), this);
        manager.registerEvents(artillery.centerListener(), this);
        manager.registerEvents(artillery.targetingListener(), this);
        artillery.register(ticks);
        containers = ContainerProtection.of(this, getConfig(), getLogger());
        vertical = new VerticalService(core, itemRefs, new VerticalStore(new File(getDataFolder(), "vertical.yml"), getLogger()),
                VerticalSettings.from(getConfig(), getLogger()), getLogger());
        logistics = new LogisticsService(core, machines, devices, energy, holograms, turrets.registry(), itemRefs, containers,
                vertical, new File(getDataFolder(), "logistics.yml"), getConfig().getInt("logistics.max-item-displays", 2000),
                getConfig().getInt("logistics.save-interval-ticks", 300), getLogger());
        logistics.load();
        energy.addConsumerSource(logistics);
        water = new WaterService(core, machines, devices, energy, holograms,
                new WaterStore(new File(getDataFolder(), "water.yml"), getLogger()), WaterSettings.from(configData()), getLogger());
        water.load();
        energy.addConsumerSource(water);
        energy.useWater(water);
        oil = new OilService(core, machines, devices, water, new OilStore(new File(getDataFolder(), "oil.yml"), getLogger()),
                OilSettings.from(configData()), getLogger());
        oil.load();
        energy.useOil(oil);
        water.useOil(oil);
        manager.registerEvents(new EnergyPlacementListener(core, devices, energy, machines, research.service(), oil), this);
        manager.registerEvents(new WaterListener(core, water, oil, machines, devices, research.service(), friendAccess), this);
        manager.registerEvents(new OilListener(core, oil, water, machines, devices, research.service()), this);
        charts = new ChartService(energy, holograms, ticks, getConfig().getInt("charts.charts-per-grid", 1),
                getConfig().getInt("charts.charts-total", 5), getConfig().getInt("charts.refresh-ticks", 10), getLogger());
        manager.registerEvents(charts, this);
        manager.registerEvents(new LogisticsListener(core, logistics, machines, devices,
                new LogisticsPlacement(core, logistics, research.service()), itemRefs), this);
        manager.registerEvents(new VerticalListener(core, vertical, logistics, machines, research.service(), friendAccess), this);
        DeviceBlast deviceBlast = new DeviceBlast(core, machines, devices, energy, logistics, water, oil, vertical);
        manager.registerEvents(deviceBlast, this);
        core.addDetonationObserver(deviceBlast);
        MachineListener machineListener = new MachineListener(core, processes, drills, machines, devices, achievements, oil, friendAccess);
        manager.registerEvents(machineListener, this);
        concealment = new BarrelConcealment(this, ticks, core.clientEffects(), machines,
                getConfig().getBoolean("concealment.enabled", true), getConfig().getDouble("concealment.range", 48.0));
        manager.registerEvents(concealment, this);
        manager.registerEvents(new MachineInteractions(core, machines, machineListener), this);
        manager.registerEvents(new HopperListener(core, machines, containers), this);
        manager.registerEvents(new SolarPanelListener(core), this);
        // The turrets and the artillery place through their own listeners, which fire the same placement check; this lets that check through.
        manager.registerEvents(new PlaceableItemGuard(core, id ->
                processes.machine(id).isPresent() || drills.machine(id).isPresent() || TurretIds.ITEM.equals(id)
                        || TurretIds.LASER_ITEM.equals(id) || TurretIds.RAILGUN_ITEM.equals(id)
                        || ArtilleryIds.isItem(id) || ArtilleryIds.isCommandCenter(id)
                        || WaterPart.ofComponent(id).isPresent()
                        || OilPart.ofComponent(id).isPresent()), this);
        DrillTicker drillTicker = new DrillTicker(machines, holograms, deposits, depositSettings, drillYield, oreById, energy,
                oil, getLogger());

        DepositScanner depositScanner = new DepositScanner(ticks, core, deposits, depositSettings,
                ScannerSettings.from(configData(), getLogger()));
        manager.registerEvents(depositScanner, this);

        GuideBook guide = new GuideBook(core, researchTree, processCatalog, oreDefinitions);
        CoreFactoryCommand command = new CoreFactoryCommand(this, core, achievements, guide, energy, energyEffects, depositScanner,
                holograms);
        LiteralCommandNode<CommandSourceStack> cfCommand = new CfCommand(command, new GeologyCommand(core, deposits, depositSettings),
                new ResearchCommand(core, research.service()), new ChartCommand(charts), new FriendsCommand(friends),
                new TurretCommand(turrets.service(), turrets.registry())).node();
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar()
                .register(cfCommand, "CoreFactory administration, TNT catalogue and placed-TNT configuration.", List.of("corefactory")));

        ticks.register("core", 1, core::tick);
        ticks.register("achievement-save", 6000, achievementStore::save);
        ticks.register("geiger", 10, new GeigerDisplay(core));
        ticks.register("resources", 40, resources::refreshOpen);
        ticks.register("oil", 1, oil::tick);
        ticks.register("energy", 1, () -> {
            energy.tick();
            logistics.tick();
        });
        ticks.register("water", 1, water::tick);
        ticks.register("machines", 1, new MachineTicker(machines, processes, engine, drillTicker, sprites, holograms, energy,
                labLevels, core.settings().saveIntervalTicks(), water, new RefineryTicker(oil)));
        chunkKeeper = new ChunkKeeper(ChunkTickets.of(this), uid -> Bukkit.getWorld(uid) != null, getLogger(),
                getConfig().getBoolean("chunks.keep-loaded", true), getConfig().getInt("chunks.max-forced", 256));
        ticks.register("chunks", 0, 100, () -> chunkKeeper.reconcile(deviceChunks(devices)));
        ticks.start(this);
        sprites.start();
        holograms.start();
        water.start();
        oil.start();
        charts.start();
        concealment.start();
        getLogger().info("CoreFactory enabled: " + core.definitions().size() + " TNT types, "
                + core.placedTnts().size() + " placed.");
    }

    @Override
    public void onDisable() {
        if (ticks != null) {
            ticks.cancel();
            ticks = null;
        }
        if (chunkKeeper != null) {
            chunkKeeper.release();
            chunkKeeper = null;
        }
        if (artillery != null) {
            artillery.shutdown();
            artillery = null;
        }
        if (turrets != null) {
            turrets.shutdown();
            turrets = null;
        }
        if (oil != null) {
            oil.shutdown();
            oil = null;
        }
        if (water != null) {
            water.shutdown();
            water = null;
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
        Map<String, Object> tnts = YamlMaps.loadFile(tntFile);
        Map<String, Object> items = componentsOf(itemFile, new File(getDataFolder(), "materials.yml"));
        core.setItemComponentIds(componentIdsOf(itemFile));
        int count = core.reload(configData(), tnts, items);
        core.setPlacedFile(placedTntFile());
        if (achievements != null) {
            achievements.setCatalog(AchievementCatalog.build(core, researchTree, processCatalog, oreDefinitions));
        }
        return count;
    }

    public CoreFactoryCore core() {
        return core;
    }

    /** The whole of config.yml as the plain data Core reads. */
    private Map<String, Object> configData() {
        return YamlMaps.toMap(getConfig());
    }

    /**
     * The file where the placed TNT registry is kept: the name config.yml gives, inside the data folder.
     */
    private PlacedTntFile placedTntFile() {
        File file = new File(getDataFolder(), core.settings().dataFileName());
        return new PlacedTntFile() {
            @Override
            public Map<String, Object> read() {
                return YamlMaps.loadFile(file);
            }

            @Override
            public void write(Map<String, Object> data) {
                try {
                    YamlMaps.writeFile(file, data);
                } catch (IOException e) {
                    getLogger().log(Level.SEVERE, "Could not save placed TNT to " + file.getName(), e);
                }
            }
        };
    }

    /**
     * One configuration holding the components of every file given: items.yml and materials.yml are read as one list.
     * A component id defined twice keeps its first definition and logs a warning.
     */
    private Map<String, Object> componentsOf(File... files) {
        Map<String, Object> components = new LinkedHashMap<>();
        for (File file : files) {
            Map<String, Object> source = YamlMaps.loadSection(file, "components");
            if (source == null) {
                continue;
            }
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                if (!(entry.getValue() instanceof Map<?, ?>)) {
                    continue;
                }
                if (components.containsKey(entry.getKey())) {
                    getLogger().warning("Component '" + entry.getKey() + "' is defined in more than one file. The first one is used.");
                    continue;
                }
                components.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.of("components", components);
    }

    /** The ids of the components one file defines, or none when the file is missing. */
    private static Set<String> componentIdsOf(File file) {
        Map<String, Object> section = YamlMaps.loadSection(file, "components");
        return section == null ? Set.of() : Set.copyOf(section.keySet());
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

    /**
     * The chunks that hold a working device: those of the machines and drills, the energy devices, the pumps, the oil tubes, the turrets and
     * the logistics parts. The chunk keeper keeps these loaded, within its cap. The artillery pieces and Command Centers hold their chunks
     * outside the keeper, so the cap is not spent on them.
     */
    private Set<ChunkKey> deviceChunks(DeviceRegistry devices) {
        Set<ChunkKey> needed = new HashSet<>(machines.chunkKeys());
        needed.addAll(devices.chunkKeys());
        needed.addAll(water.chunkKeys());
        needed.addAll(oil.chunkKeys());
        needed.addAll(turrets.registry().chunkKeys());
        needed.addAll(logistics.chunkKeys());
        return needed;
    }

    private void saveResourceIfMissing(String name) {
        if (!new File(getDataFolder(), name).exists()) {
            saveResource(name, false);
        }
    }
}
