package io.corefactory.paper.artillery;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.artillery.ArtilleryFiringRules;
import io.corefactory.core.artillery.ArtillerySettings;
import io.corefactory.core.artillery.CommandCenterSettings;
import io.corefactory.core.research.ResearchService;
import io.corefactory.paper.TickScheduler;
import io.corefactory.paper.chunks.ChunkTickets;
import io.corefactory.paper.config.YamlMaps;
import io.corefactory.paper.energy.DeviceRegistry;
import io.corefactory.paper.energy.EnergyService;
import io.corefactory.paper.friends.FriendAccess;
import io.corefactory.paper.machine.MachineStore;
import io.corefactory.paper.turret.TurretRegistry;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Builds the artillery pieces and the Command Centers from their saved file and wires them to the plugin: the pieces join the energy network
 * as consumers, their listeners are returned for registration, and their ticks are registered with the plugin's tick scheduler. Call
 * {@link #register} once after the energy network is open, register {@link #listener()} and {@link #centerListener()}, and call
 * {@link #shutdown} when the plugin stops.
 */
public final class ArtilleryRuntime {

    private final CommandCenterService commandCenters;
    private final ArtilleryService service;
    private final ArtilleryRegistry registry;
    private final ArtilleryHazards hazards;
    private final ArtilleryListener listener;
    private final CommandCenterListener centerListener;
    private final ListeningMode targeting;

    private ArtilleryRuntime(CommandCenterService commandCenters, ArtilleryService service, ArtilleryRegistry registry,
                             ArtilleryHazards hazards, ArtilleryListener listener, CommandCenterListener centerListener,
                             ListeningMode targeting) {
        this.commandCenters = commandCenters;
        this.service = service;
        this.registry = registry;
        this.hazards = hazards;
        this.listener = listener;
        this.centerListener = centerListener;
        this.targeting = targeting;
    }

    /**
     * Reads the settings and the saved pieces and centers, and builds the runtime. The pieces are added to the energy network as a consumer
     * source.
     *
     * @param plugin   the plugin, whose configuration holds the {@code artillery} and {@code command-center} sections and whose data folder
     *                 holds {@code tnts.yml}
     * @param core     the core
     * @param energy   the energy network
     * @param devices  the energy devices, whose blocks a piece or a center may not take
     * @param machines the placed machines
     * @param turrets  the turrets, whose blocks a piece or a center may not take
     * @param research the research progress
     * @param friends  the friend rule, which lets a friend of the owner use a piece or a center
     * @param file     the store file, normally {@code artillery.yml} in the data folder
     * @param log      receives the warnings
     */
    public static ArtilleryRuntime open(JavaPlugin plugin, CoreFactoryCore core, EnergyService energy, DeviceRegistry devices,
                                        MachineStore machines, TurretRegistry turrets, ResearchService research, FriendAccess friends,
                                        File file, Logger log) {
        Map<String, Object> config = YamlMaps.toMap(plugin.getConfig());
        ArtillerySettings settings = ArtillerySettings.from(config, YamlMaps.loadFile(new File(plugin.getDataFolder(), "tnts.yml")));
        CommandCenterSettings centerSettings = CommandCenterSettings.from(config);
        ArtilleryStore store = new ArtilleryStore(file, log);
        ArtilleryStore.Saved saved = store.read();
        ArtilleryRegistry registry = new ArtilleryRegistry(core, settings, log);
        registry.load(saved.pieces());
        registry.sweepLoaded();
        CommandCenterRegistry centers = new CommandCenterRegistry(core, log, centerSettings.mapWallSize());
        centers.load(saved.centers());
        centers.sweepLoaded();
        ArtilleryHazards hazards = new ArtilleryHazards(settings.presentation());
        ArtilleryImpacts impacts = new ArtilleryImpacts(core, settings, new ArtilleryFiringRules(settings), hazards);
        ArtilleryChunks chunks = new ArtilleryChunks(ChunkTickets.of(plugin), plugin.getConfig().getBoolean("chunks.keep-loaded", true));
        ArtilleryService service = new ArtilleryService(core, registry, centers, energy, settings, store, impacts, chunks,
                saved.antimatterShots(), new Random(System.nanoTime()), core.settings().saveIntervalTicks(), log);
        service.flights().sweepLoaded();
        energy.addConsumerSource(service);
        CommandCenterService commandCenters = new CommandCenterService(core, service, centers, energy, centerSettings);
        TerrainLoader terrains = new TerrainLoader(new ChunkTerrainSource(), new TerrainCache(centerSettings.terrainCacheChunks()),
                new TerrainBudget(centerSettings.terrainReadsInFlight(), centerSettings.terrainGenerationsInFlight()));
        ListeningMode targeting = new ListeningMode(commandCenters, centerSettings, terrains, log);
        CommandCenterDialogs dialogs = new CommandCenterDialogs(commandCenters, targeting);
        return new ArtilleryRuntime(commandCenters, service, registry, hazards,
                new ArtilleryListener(core, service, devices, machines, turrets, research, friends),
                new CommandCenterListener(core, commandCenters, devices, machines, turrets, research, dialogs, targeting, friends),
                targeting);
    }

    /** The pieces' runtime: placement, power, shots. */
    public ArtilleryService service() {
        return service;
    }

    /** The Command Centers' runtime: placement, the artillery they command, the safety check, the screen. */
    public CommandCenterService commandCenters() {
        return commandCenters;
    }

    /** The listener of the pieces. Register it with the plugin manager. */
    public Listener listener() {
        return listener;
    }

    /** The listener of the Command Centers. Register it with the plugin manager. */
    public Listener centerListener() {
        return centerListener;
    }

    /** The listener of the Set Target mode, which traces a player's view onto the map. Register it with the plugin manager. */
    public Listener targetingListener() {
        return targeting;
    }

    /**
     * Registers the actions that run the pieces: the piece tick every server tick, the shots in flight every tick, the clouds and fires every
     * few ticks, and the Command Centers' screens, refreshed every few ticks as their settings say. The chunks of the pieces and the centers
     * are held once now, and then with each refresh of the pieces.
     *
     * @param ticks the plugin's tick scheduler, which is started separately
     */
    public void register(TickScheduler ticks) {
        service.holdChunks();
        ticks.register("artillery", 1, service::tick);
        ticks.register("artillery-shots", 1, service::tickShots);
        ticks.register("artillery-hazards", ArtilleryHazards.STEP_TICKS, hazards::tick);
        ticks.register("command-center", 1, commandCenters::tick);
        targeting.attach(ticks);
    }

    /** Saves the pieces and the centers, clears the clouds' light, and releases their entities. Call when the plugin stops. */
    public void shutdown() {
        targeting.shutdown();
        hazards.shutdown();
        service.shutdown();
    }
}
