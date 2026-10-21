package io.corefactory.spigot.turret;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.spigot.energy.DeviceRegistry;
import io.corefactory.spigot.energy.EnergyService;
import io.corefactory.spigot.friends.FriendStore;
import io.corefactory.spigot.friends.Friends;
import io.corefactory.spigot.hologram.HologramService;
import io.corefactory.spigot.machine.MachineStore;
import io.corefactory.core.research.ResearchService;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Builds the turrets from their saved file and wires them to the plugin: the turrets join the energy network as consumers, their listener
 * is returned for registration, and one scheduled task runs their tick. Call {@link #start} once after the energy network and the hologram
 * service are open, register {@link #listener()}, and call {@link #shutdown} when the plugin stops, before the holograms close.
 */
public final class TurretRuntime {

    private final TurretRegistry registry;
    private final TurretService service;
    private final Friends friends;
    private final TurretListener listener;

    private TurretRuntime(TurretRegistry registry, TurretService service, Friends friends, TurretListener listener) {
        this.registry = registry;
        this.service = service;
        this.friends = friends;
        this.listener = listener;
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
     * @param file     the store file, normally {@code turrets.yml} in the data folder
     * @param log      receives the warnings
     */
    public static TurretRuntime open(JavaPlugin plugin, CoreFactoryCore core, EnergyService energy, DeviceRegistry devices,
                                     MachineStore machines, ResearchService research, HologramService holograms, File file,
                                     Logger log) {
        TurretStats stats = TurretStats.from(plugin.getConfig());
        TurretStore store = new TurretStore(file, log);
        TurretRegistry registry = new TurretRegistry(core, holograms, log);
        registry.load(store.read(), stats);
        registry.sweepLoaded();
        Friends friends = new Friends(new FriendStore(new File(file.getAbsoluteFile().getParentFile(), "friends.yml"), log));
        TurretService service = new TurretService(core, registry, energy, TurretProtection.of(plugin, plugin.getConfig(), log),
                friends, store, new Random(System.nanoTime()), stats);
        energy.addConsumerSource(service);
        return new TurretRuntime(registry, service, friends, new TurretListener(core, service, devices, machines, research));
    }

    /** The turrets' runtime: placement, loading and the tick. */
    public TurretService service() {
        return service;
    }

    /** The internal friend lists, kept in friends.yml next to the turrets' file. */
    public Friends friends() {
        return friends;
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
     * Starts the one scheduled task that runs every turret each server tick.
     *
     * @return the task, to cancel when the plugin stops
     */
    public BukkitTask start(JavaPlugin plugin) {
        return Bukkit.getScheduler().runTaskTimer(plugin, service::tick, 1L, 1L);
    }

    /** Saves the turrets and releases their entities. Call when the plugin stops. */
    public void shutdown() {
        service.shutdown();
    }
}
