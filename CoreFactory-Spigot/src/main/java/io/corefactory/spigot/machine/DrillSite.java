package io.corefactory.spigot.machine;

import io.corefactory.core.drill.DrillBox;
import io.corefactory.core.geology.DepositVolumes;
import io.corefactory.core.geology.GeologyRegistry;
import io.corefactory.core.geology.GeologyService;
import io.corefactory.core.geology.Placement;
import io.corefactory.spigot.geology.OreChunks;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

/** Reads what a drill can see: the registered blocks of its box, grouped by ore, from the chunks that are loaded. */
final class DrillSite {

    private DrillSite() {
    }

    /** The key of a drill in the mining ledger: its world name and block position. */
    static String keyOf(Location site) {
        String world = site.getWorld() == null ? "" : site.getWorld().getName();
        return world + ":" + site.getBlockX() + ":" + site.getBlockY() + ":" + site.getBlockZ();
    }

    /** Every registered block the box touches, in loaded chunks only. Chunks that are not loaded are left out. */
    static List<Placement> registeredIn(World world, DrillBox box) {
        List<Placement> found = new ArrayList<>();
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                Chunk chunk = world.getChunkAt(cx, cz);
                for (Placement placement : GeologyRegistry.decode(OreChunks.registry(chunk), cx, cz)) {
                    if (box.contains(placement)) {
                        found.add(placement);
                    }
                }
            }
        }
        return found;
    }

    /**
     * The registered blocks inside the box whose ore the predicate takes, grouped by ore id.
     *
     * @param targets which ore indices the drill's kind may take
     */
    static Map<String, List<Placement>> byOre(World world, DrillBox box, GeologyService service, IntPredicate targets) {
        Map<String, List<Placement>> byOre = new LinkedHashMap<>();
        for (Placement placement : registeredIn(world, box)) {
            if (targets.test(placement.oreIndex())) {
                byOre.computeIfAbsent(service.ore(placement.oreIndex()).id(), id -> new ArrayList<>()).add(placement);
            }
        }
        return byOre;
    }

    /**
     * What is in the box per ore id: the number of blocks, or for a deposit the volume left in its blocks.
     *
     * @param deposit     the id of the petroleum deposit ore
     * @param depositFull the volume a deposit holds when it has no stored volume
     */
    static Map<String, Long> inRange(World world, DrillBox box, GeologyService service, IntPredicate targets,
                                     String deposit, int depositFull) {
        Map<String, Long> inRange = new LinkedHashMap<>();
        for (Map.Entry<String, List<Placement>> entry : byOre(world, box, service, targets).entrySet()) {
            if (entry.getKey().equals(deposit)) {
                long volume = 0;
                for (Placement p : entry.getValue()) {
                    Chunk chunk = world.getChunkAt(p.x() >> 4, p.z() >> 4);
                    volume += DepositVolumes.get(OreChunks.volumes(chunk), p.x(), p.y(), p.z(), depositFull);
                }
                inRange.put(entry.getKey(), volume);
            } else {
                inRange.put(entry.getKey(), (long) entry.getValue().size());
            }
        }
        return inRange;
    }
}
