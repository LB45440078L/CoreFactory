package io.corefactory.paper.turret;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.paper.TickScheduler;
import io.corefactory.paper.energy.DeviceRegistry;
import io.corefactory.paper.energy.EnergyService;
import io.corefactory.paper.friends.FriendAccess;
import io.corefactory.paper.config.YamlMaps;
import io.corefactory.paper.hologram.HologramService;
import io.corefactory.paper.machine.MachineStore;
import io.corefactory.paper.railgun.RailgunShots;
import io.corefactory.core.research.ResearchService;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Builds the turrets from their saved file and wires them to the plugin: the turrets join the energy network as consumers, their listener
 * is returned for registration, and their tick is registered with the plugin's tick scheduler. Call {@link #register} once after the energy
 * network and the hologram service are open, register {@link #listener()}, and call {@link #shutdown} when the plugin stops, before the
 * holograms close.
 */
public final class TurretRuntime {

    private final TurretRegistry registry;
    private final TurretService service;
    private final TurretListener listener;
    private final RailgunShots shots;

    private TurretRuntime(TurretRegistry registry, TurretService service, TurretListener listener, RailgunShots shots) {
        this.registry = registry;
        this.service = service;
        this.listener = listener;
        this.shots = shots;
    }

    /**
     * Reads the saved turrets and builds the runtime. The turrets are added to the energy network as a consumer source. Their entities are
     * made by the tick, for the chunks that are loaded.
     *
     * @param plugin   the plugin, whose configuration holds the {@code turrets} section and the claim integration switches
     * @param core     the core
     * @param energy   the energy network
     * @param devices  the energy devices, whose blocks a turret does not take
     * @param machines the placed machines
     * @param research the research progress
     * @param holograms the hologram service
     * @param friends  the friend rule, which lets a friend of the owner load, upgrade and break a turret
     * @param file     the store file, normally {@code turrets.yml} in the data folder
     * @param log      receives the warnings
     */
    public static TurretRuntime open(JavaPlugin plugin, CoreFactoryCore core, EnergyService energy, DeviceRegistry devices,
                                     MachineStore machines, ResearchService research, HologramService holograms, FriendAccess friends,
                                     File file, Logger log) {
        TurretStats stats = TurretStats.from(YamlMaps.toMap(plugin.getConfig()));
        TurretStore store = new TurretStore(file, log);
        TurretRegistry registry = new TurretRegistry(core, holograms, log);
        registry.load(store.read(), stats);
        registry.sweepLoaded();
        RailgunShots shots = new RailgunShots(core, stats.railgun());
        TurretService service = new TurretService(core, registry, energy, TurretProtection.of(plugin, plugin.getConfig(), log),
                friends, store, new Random(System.nanoTime()), stats, shots);
        energy.addConsumerSource(service);
        return new TurretRuntime(registry, service, new TurretListener(core, service, devices, machines, research, friends), shots);
    }

    /** The turrets' runtime: placement, loading and the tick. */
    public TurretService service() {
        return service;
    }

    /** The placed turrets, so the logistics ports can find a turret at a block. */
    public TurretRegistry registry() {
        return registry;
    }

    /** The listener of the turrets. Register it with the plugin manager. */
    public Listener listener() {
        return listener;
    }

    /**
     * Registers the actions that run every server tick: every turret, and the rail gun shots in flight.
     *
     * @param ticks the plugin's tick scheduler, which is started separately
     */
    public void register(TickScheduler ticks) {
        ticks.register("turrets", 1, service::tick);
        ticks.register("railgun-shots", 1, shots::tick);
    }

    /** Saves the turrets, releases their entities, and stops the rail gun shots in flight. Call when the plugin stops. */
    public void shutdown() {
        service.shutdown();
        shots.release();
    }
}
