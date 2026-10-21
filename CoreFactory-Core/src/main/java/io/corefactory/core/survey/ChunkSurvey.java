package io.corefactory.core.survey;

import io.corefactory.core.geology.GeologyRegistry;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.geology.Placement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Summarises the planned ore placements of one chunk for the survey scanner. Pure logic: it takes the placements the
 * planner gives and reports, per column of 16 x 16 blocks, which ore dominates it and how many of its blocks that ore
 * has there, plus per-ore totals and depth ranges.
 */
public final class ChunkSurvey {

    /** The side of a chunk in blocks. */
    public static final int SIZE = 16;

    /** The ore index of a column that has no planned block. */
    public static final int NONE = -1;

    /**
     * The dominant ore of one column.
     *
     * @param ore   index of the ore with the most planned blocks in the column, or {@link #NONE}
     * @param count planned blocks of that ore in the column; 0 when the column is empty
     */
    public record Cell(int ore, int count) {
    }

    /**
     * The planned blocks of one ore in the chunk.
     *
     * @param ore     index of the ore in the ore list
     * @param planned planned blocks of the ore
     * @param minY    lowest planned y
     * @param maxY    highest planned y
     */
    public record OreTotal(int ore, int planned, int minY, int maxY) {
    }

    /**
     * The survey of one chunk.
     *
     * @param chunkX     chunk x
     * @param chunkZ     chunk z
     * @param cells      the 16 x 16 columns in row-major order: the column at local (x, z) is at index z * 16 + x
     * @param totals     one entry per ore with at least one planned block, in ore index order
     * @param placements the number of planned blocks
     */
    public record Survey(int chunkX, int chunkZ, List<Cell> cells, List<OreTotal> totals, int placements) {

        public Survey {
            cells = List.copyOf(cells);
            totals = List.copyOf(totals);
        }

        /** The column at local x and z, each in 0..15. */
        public Cell cell(int x, int z) {
            return cells.get(z * SIZE + x);
        }
    }

    private ChunkSurvey() {
    }

    /**
     * Summarises the placements of one chunk.
     *
     * @param chunkX     chunk x
     * @param chunkZ     chunk z
     * @param placements the planned blocks; each must lie in the chunk and name an ore of {@code ores}
     * @param ores       the ores in index order, which give the indices a tie is broken by
     * @return the survey; an empty chunk has every column at {@link #NONE}
     * @throws IllegalArgumentException when a placement lies outside the chunk or names no ore
     */
    public static Survey survey(int chunkX, int chunkZ, List<Placement> placements, List<OreDefinition> ores) {
        int oreCount = ores.size();
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int columns = SIZE * SIZE;
        int[] counts = new int[columns * oreCount];
        int[] planned = new int[oreCount];
        int[] minY = new int[oreCount];
        int[] maxY = new int[oreCount];
        Arrays.fill(minY, Integer.MAX_VALUE);
        Arrays.fill(maxY, Integer.MIN_VALUE);
        for (Placement p : placements) {
            int ore = p.oreIndex();
            if (ore < 0 || ore >= oreCount) {
                throw new IllegalArgumentException("placement names ore index " + ore + ", outside 0.." + (oreCount - 1));
            }
            int x = p.x() - baseX;
            int z = p.z() - baseZ;
            if (x < 0 || x >= SIZE || z < 0 || z >= SIZE) {
                throw new IllegalArgumentException("placement at " + p.x() + ", " + p.y() + ", " + p.z()
                        + " is outside chunk " + chunkX + ", " + chunkZ);
            }
            counts[(z * SIZE + x) * oreCount + ore]++;
            planned[ore]++;
            minY[ore] = Math.min(minY[ore], p.y());
            maxY[ore] = Math.max(maxY[ore], p.y());
        }
        List<Cell> cells = new ArrayList<>(columns);
        for (int column = 0; column < columns; column++) {
            int best = NONE;
            int bestCount = 0;
            for (int ore = 0; ore < oreCount; ore++) {
                int count = counts[column * oreCount + ore];
                if (count > bestCount) {
                    best = ore;
                    bestCount = count;
                }
            }
            cells.add(new Cell(best, bestCount));
        }
        List<OreTotal> totals = new ArrayList<>();
        for (int ore = 0; ore < oreCount; ore++) {
            if (planned[ore] > 0) {
                totals.add(new OreTotal(ore, planned[ore], minY[ore], maxY[ore]));
            }
        }
        return new Survey(chunkX, chunkZ, cells, totals, placements.size());
    }

    /**
     * Counts the planned blocks that are still registered in the chunk, per ore index. A planned block that was mined or
     * blown up has left the registry and is not counted; a registered block counts only for the ore it is registered as.
     *
     * @param planned  the planned blocks of the chunk
     * @param registry the packed registry of the chunk, or null for none
     * @param chunkX   chunk x
     * @param chunkZ   chunk z
     * @return the count per ore index, for each ore with at least one planned block still registered
     */
    public static Map<Integer, Integer> stillInGround(List<Placement> planned, int[] registry, int chunkX, int chunkZ) {
        Set<Placement> registered = new HashSet<>(GeologyRegistry.decode(registry, chunkX, chunkZ));
        Map<Integer, Integer> counts = new HashMap<>();
        for (Placement p : planned) {
            if (registered.contains(p)) {
                counts.merge(p.oreIndex(), 1, Integer::sum);
            }
        }
        return counts;
    }
}
