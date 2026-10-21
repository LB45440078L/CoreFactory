package io.corefactory.spigot.geology;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.drill.DrillRules;
import io.corefactory.core.drill.PetroleumVolume;
import io.corefactory.core.geology.BlockSampler;
import io.corefactory.core.geology.DepositVolumes;
import io.corefactory.core.geology.GeologyRegistry;
import io.corefactory.core.geology.GeologyService;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.geology.Placement;
import io.corefactory.core.geology.RetrofitQueue;
import io.corefactory.core.ledger.ChunkBudgets;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Lays ore veins into chunks on the main thread. The listener queues each new chunk when it generates, and retrofit
 * queues old ones; {@link #drain} takes a budget of chunks per tick and applies the plan to each of them.
 *
 * <p>A chunk is laid once. When it is laid, its ore counts go into the chunk budgets, and a chunk that has a budget, or
 * that an earlier version laid, is never laid again: a forced retrofit skips it, so mined ores are not re-created.
 */
public final class GeologyWorker {

    private final CoreFactoryCore core;
    private final GeologyService service;
    private final RetrofitQueue queue;
    private final ChunkBudgets budgets;
    private final Material[] hosts;
    private final Logger log;

    public GeologyWorker(CoreFactoryCore core, GeologyService service, int chunksPerTick, ChunkBudgets budgets,
                         Logger log) {
        this.core = core;
        this.service = service;
        this.queue = new RetrofitQueue(chunksPerTick);
        this.budgets = budgets;
        this.log = log;
        this.hosts = new Material[service.ores().size()];
        for (int index = 0; index < hosts.length; index++) {
            OreDefinition ore = service.ore(index);
            Material host = Material.matchMaterial(ore.host());
            if (host == null || !host.isBlock()) {
                log.warning("ores.yml: '" + ore.id() + "' host " + ore.host() + " is not a block; it is not placed.");
            } else {
                hosts[index] = host;
            }
            if (!core.componentDefinitions().containsKey(ore.drop())) {
                log.warning("ores.yml: '" + ore.id() + "' drops unknown component '" + ore.drop() + "'.");
            }
        }
    }

    public GeologyService service() {
        return service;
    }

    public RetrofitQueue queue() {
        return queue;
    }

    /** The chunk budgets, which say which chunks were laid and what each ore had in them. */
    public ChunkBudgets budgets() {
        return budgets;
    }

    public Logger log() {
        return log;
    }

    /** True when the ore at this index is placed as this material. */
    public boolean hostMatches(int oreIndex, Material material) {
        return oreIndex >= 0 && oreIndex < hosts.length && hosts[oreIndex] == material;
    }

    /** The material the ore at this index is placed as, or null when its host is not a block. */
    public Material host(int oreIndex) {
        return hosts[oreIndex];
    }

    /**
     * Queues a chunk for ore placement.
     *
     * @return true when the chunk was not already waiting
     */
    public boolean enqueue(World world, int chunkX, int chunkZ, boolean force) {
        if (service.ores().isEmpty()) {
            return false;
        }
        return queue.offer(world.getName(), chunkX, chunkZ, force);
    }

    /**
     * Applies the plan to up to the budget of queued chunks. Call from the main thread.
     *
     * @return the number of chunks taken from the queue
     */
    public int drain() {
        List<RetrofitQueue.Entry> batch = queue.take();
        for (RetrofitQueue.Entry entry : batch) {
            try {
                populate(entry);
            } catch (RuntimeException e) {
                log.warning("Could not lay ores in chunk " + entry.chunk().chunkX() + ", " + entry.chunk().chunkZ()
                        + " of " + entry.chunk().world() + ": " + e);
            }
        }
        return batch.size();
    }

    /** Applies every queued chunk now. Used on shutdown, so new chunks do not wait for the next start. */
    public void drainAll() {
        while (!queue.isEmpty()) {
            drain();
        }
    }

    private void populate(RetrofitQueue.Entry entry) {
        World world = Bukkit.getWorld(entry.chunk().world());
        if (world == null) {
            return;
        }
        int chunkX = entry.chunk().chunkX();
        int chunkZ = entry.chunk().chunkZ();
        if (!world.isChunkLoaded(chunkX, chunkZ) && !world.isChunkGenerated(chunkX, chunkZ)) {
            return;
        }
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        if (!entry.force() && OreChunks.marker(chunk) >= GeologyService.CURRENT_VERSION) {
            return;
        }
        if (budgets.isBudgeted(world.getName(), chunkX, chunkZ)) {
            if (entry.force()) {
                log.info("Retrofit skipped chunk " + chunkX + ", " + chunkZ + " of " + world.getName()
                        + ": it has an ore budget, so its ores are not placed again.");
            }
            return;
        }
        if (entry.force() && OreChunks.marker(chunk) > 0) {
            log.info("Retrofit skipped chunk " + chunkX + ", " + chunkZ + " of " + world.getName()
                    + ": an earlier version laid it, so its ores are not placed again.");
            return;
        }
        List<Placement> plan = service.plan(world.getSeed(), chunkX, chunkZ, sampler(world), entry.force());
        apply(world, chunk, plan, entry.force());
    }

    /**
     * Sets each planned block to its host and records it. A block is changed only while it is still a replace material of its
     * ore; with {@code force}, its own host counts too. Everything else in the chunk is left alone. The chunk's budget is
     * written last, from the registry as it now stands, so the planned counts match the ores that are in the chunk.
     */
    private void apply(World world, Chunk chunk, List<Placement> plan, boolean force) {
        List<Placement> laid = new ArrayList<>();
        for (Placement p : plan) {
            Material host = hosts[p.oreIndex()];
            if (host == null) {
                continue;
            }
            Block block = world.getBlockAt(p.x(), p.y(), p.z());
            if (!service.accepts(p.oreIndex(), block.getType().name(), force)) {
                continue;
            }
            block.setType(host, false);
            laid.add(p);
        }
        int[] registry = GeologyRegistry.merge(OreChunks.registry(chunk), laid);
        OreChunks.storeRegistry(chunk, registry);
        int[] volumes = OreChunks.volumes(chunk);
        int deposit = service.indexOf(DrillRules.DEPOSIT);
        for (Placement p : laid) {
            if (p.oreIndex() == deposit) {
                volumes = DepositVolumes.put(volumes, p.x(), p.y(), p.z(), fullVolume(deposit));
            }
        }
        OreChunks.storeVolumes(chunk, volumes);
        OreChunks.setMarker(chunk, GeologyService.CURRENT_VERSION);
        budgets.plant(world.getName(), chunk.getX(), chunk.getZ(), planned(registry, chunk.getX(), chunk.getZ()));
    }

    /**
     * The planned count of each ore in a chunk: its blocks, or for a deposit the volume of its blocks. Every ore id is in the
     * map, with 0 for an ore the chunk does not hold.
     */
    private Map<String, Long> planned(int[] registry, int chunkX, int chunkZ) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (OreDefinition ore : service.ores()) {
            counts.put(ore.id(), 0L);
        }
        for (Placement p : GeologyRegistry.decode(registry, chunkX, chunkZ)) {
            OreDefinition ore = service.ore(p.oreIndex());
            long units = ore.id().equals(DrillRules.DEPOSIT) ? fullVolume(p.oreIndex()) : 1;
            counts.merge(ore.id(), units, Long::sum);
        }
        return counts;
    }

    /** The volume one deposit block holds when it is laid. */
    private int fullVolume(int oreIndex) {
        return PetroleumVolume.volumeOf(service.ore(oreIndex).volume());
    }

    private static BlockSampler sampler(World world) {
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();
        return (x, y, z) -> y < minY || y >= maxY ? "AIR" : world.getBlockAt(x, y, z).getType().name();
    }

    /** Drops the ore's component at the block, the given number of items. */
    public void dropFor(Block block, OreDefinition ore, int amount) {
        core.createComponentItem(ore.drop(), amount).ifPresent(item ->
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), item));
    }
}
