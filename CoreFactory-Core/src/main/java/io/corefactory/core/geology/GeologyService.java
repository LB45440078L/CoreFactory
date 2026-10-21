package io.corefactory.core.geology;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The ores of the server and the plan for a chunk. Holds the parsed ore list (its order gives the ore indices the
 * registry stores), plans every ore of a chunk in that order, and applies the overlap rule: an ore planned earlier
 * keeps a block, so no block is claimed twice.
 */
public final class GeologyService {

    /** The marker version a chunk carries once its ores are laid. Bump it to re-plan every chunk. */
    public static final int CURRENT_VERSION = 1;

    private record Cell(int x, int y, int z) {
    }

    private final List<OreDefinition> ores;

    /**
     * @param ores the ores in file order; at most 256, since a registry entry stores the index in 8 bits
     * @throws IllegalArgumentException when there are more than 256 ores
     */
    public GeologyService(List<OreDefinition> ores) {
        if (ores.size() > 256) {
            throw new IllegalArgumentException("at most 256 ores are supported, got " + ores.size());
        }
        this.ores = List.copyOf(ores);
    }

    /** The ores, in index order. */
    public List<OreDefinition> ores() {
        return ores;
    }

    /** The ore at an index. */
    public OreDefinition ore(int index) {
        return ores.get(index);
    }

    /** The index of the ore with this id, or -1 when no ore has it. */
    public int indexOf(String id) {
        for (int index = 0; index < ores.size(); index++) {
            if (ores.get(index).id().equals(id)) {
                return index;
            }
        }
        return -1;
    }

    /** The host material names of every ore. */
    public Set<String> hostMaterials() {
        Set<String> hosts = new HashSet<>();
        for (OreDefinition ore : ores) {
            hosts.add(ore.host());
        }
        return hosts;
    }

    /** Plans the chunk with the ore's own replace set. */
    public List<Placement> plan(long seed, int chunkX, int chunkZ, BlockSampler sampler) {
        return plan(seed, chunkX, chunkZ, sampler, false);
    }

    /**
     * Plans every ore of the chunk, in index order, and drops any placement that an earlier ore already claimed.
     *
     * @param includeHosts when true, each ore may also plan into its own host block. Retrofit uses this to record
     *                     ores that were already placed; the other ores' hosts are never claimed
     * @return the placements, each block at most once
     */
    public List<Placement> plan(long seed, int chunkX, int chunkZ, BlockSampler sampler, boolean includeHosts) {
        Set<Cell> claimed = new HashSet<>();
        List<Placement> out = new ArrayList<>();
        for (int index = 0; index < ores.size(); index++) {
            OreDefinition ore = includeHosts ? ores.get(index).withHostReplaceable() : ores.get(index);
            for (Placement p : VeinPlanner.plan(seed, chunkX, chunkZ, index, ore, sampler)) {
                if (claimed.add(new Cell(p.x(), p.y(), p.z()))) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    /**
     * True when the ore may replace a block of this material. With {@code includeHost}, its own host counts too.
     */
    public boolean accepts(int oreIndex, String material, boolean includeHost) {
        OreDefinition ore = ores.get(oreIndex);
        return ore.replace().contains(material) || (includeHost && ore.host().equals(material));
    }
}
