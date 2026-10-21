package io.corefactory.core.geology;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

/**
 * Plans the veins of one ore in one chunk. Pure logic: the same seed, chunk and ore index always give the same
 * placements, on any machine, as long as the sampler reports the same materials.
 *
 * <p>Each chunk and ore has its own random stream, seeded by SplitMix64 mixing of the inputs. The stream first decides
 * how many veins the chunk gets, then for each vein picks a centre inside the chunk and grows a blob by a random walk
 * over the six neighbours. A vein never leaves its chunk, so the placements always fit the registry encoding.
 */
public final class VeinPlanner {

    private static final long GAMMA = 0x9E3779B97F4A7C15L;
    private static final int[][] STEPS = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private VeinPlanner() {
    }

    /**
     * Plans the veins of one ore in one chunk. Only blocks whose material is in the ore's replace set are taken; a
     * vein whose centre is not replaceable is dropped.
     *
     * @param seed     the world seed
     * @param chunkX   chunk x
     * @param chunkZ   chunk z
     * @param oreIndex the ore's index, which also selects its random stream
     * @param ore      the ore's definition
     * @param sampler  the materials of the world
     * @return the placements, without duplicates, in the order they were grown
     */
    public static List<Placement> plan(long seed, int chunkX, int chunkZ, int oreIndex, OreDefinition ore,
                                       BlockSampler sampler) {
        SplittableRandom random = new SplittableRandom(streamSeed(seed, chunkX, chunkZ, oreIndex));
        int veins = veinCount(ore.veinsPerChunk(), random);
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        int height = ore.maxY() - ore.minY() + 1;
        Set<Placement> taken = new LinkedHashSet<>();
        for (int vein = 0; vein < veins; vein++) {
            int x = baseX + random.nextInt(16);
            int y = ore.minY() + random.nextInt(height);
            int z = baseZ + random.nextInt(16);
            int target = ore.veinSizeMin() + random.nextInt(ore.veinSizeMax() - ore.veinSizeMin() + 1);
            growVein(random, new Placement(x, y, z, oreIndex), target, ore, sampler, baseX, baseZ, taken);
        }
        return List.copyOf(taken);
    }

    private static void growVein(SplittableRandom random, Placement centre, int target, OreDefinition ore,
                                 BlockSampler sampler, int baseX, int baseZ, Set<Placement> taken) {
        Placement current;
        int grown = 0;
        if (taken.contains(centre)) {
            current = centre;
        } else if (ore.replace().contains(sampler.materialAt(centre.x(), centre.y(), centre.z()))) {
            taken.add(centre);
            current = centre;
            grown = 1;
        } else {
            return;
        }
        for (int attempt = 0; grown < target && attempt < target * 8; attempt++) {
            int[] step = STEPS[random.nextInt(STEPS.length)];
            int x = current.x() + step[0];
            int y = current.y() + step[1];
            int z = current.z() + step[2];
            if (x < baseX || x > baseX + 15 || z < baseZ || z > baseZ + 15 || y < ore.minY() || y > ore.maxY()) {
                continue;
            }
            Placement next = new Placement(x, y, z, centre.oreIndex());
            if (taken.contains(next)) {
                current = next;
            } else if (ore.replace().contains(sampler.materialAt(x, y, z))) {
                taken.add(next);
                current = next;
                grown++;
            }
        }
    }

    /** The integer part of the expected count, plus one more with probability equal to the fraction. */
    static int veinCount(double expected, SplittableRandom random) {
        double whole = Math.floor(expected);
        int count = (int) whole;
        if (random.nextDouble() < expected - whole) {
            count++;
        }
        return count;
    }

    /** The seed of the random stream of one ore in one chunk. */
    static long streamSeed(long seed, int chunkX, int chunkZ, int oreIndex) {
        long h = mix(seed);
        h = mix(h + ((((long) chunkX) << 32) | (chunkZ & 0xFFFFFFFFL)));
        return mix(h + (oreIndex + 1) * GAMMA);
    }

    /** The SplitMix64 finaliser. */
    static long mix(long value) {
        long z = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
