package io.corefactory.core.deposit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Plans the deposits of a world from its seed, with no stored state. The surface plane is cut into square cells of
 * {@link DepositSettings#cellSize()} blocks. Each cell holds at most one deposit, and every choice about it (whether it
 * holds one, its ore, its radius, its centre and its amount) is a 64-bit hash of the seed, the world and the cell index.
 * The same inputs always give the same deposits, so a chunk can be planned alone, in any order, and again after a restart.
 *
 * <p>The centre of a deposit lies in its cell at least its radius from each edge, so the whole circle lies inside the
 * cell. A deposit can still cross into a neighbouring chunk, so {@link #planChunk} returns every deposit whose circle
 * touches the chunk's footprint, whichever cell it was planned in. Within a call the deposits come in cell order, by cell
 * x and then cell z.
 */
public final class DepositPlanner {

    /** The side of a chunk, in blocks. */
    public static final int CHUNK_SIZE = 16;

    private static final long GAMMA = 0x9E3779B97F4A7C15L;

    private static final int PRESENCE = 0;
    private static final int ORE_KIND = 1;
    private static final int ORE_CHOICE = 2;
    private static final int RADIUS = 3;
    private static final int CENTRE_X = 4;
    private static final int CENTRE_Z = 5;
    private static final int AMOUNT = 6;

    private DepositPlanner() {
    }

    /**
     * The deposits whose circle intersects the 16 by 16 footprint of a chunk.
     *
     * @param seed       the world seed
     * @param world      the world name
     * @param chunkX     the chunk x; the footprint starts at block {@code chunkX * 16}
     * @param chunkZ     the chunk z; the footprint starts at block {@code chunkZ * 16}
     * @param solidOres  the ore ids a deposit may hold when it is not oil; at least one
     * @param oilOre     the id of the oil deposit
     * @param settings   the planning constants
     * @return the deposits that touch the chunk, each planned with its full initial amount
     * @throws IllegalArgumentException when {@code solidOres} is empty or holds a null
     */
    public static List<Deposit> planChunk(long seed, String world, int chunkX, int chunkZ, List<String> solidOres,
                                          String oilOre, DepositSettings settings) {
        checkInputs(world, solidOres, oilOre, settings);
        int minX = chunkX * CHUNK_SIZE;
        int minZ = chunkZ * CHUNK_SIZE;
        int maxX = minX + CHUNK_SIZE - 1;
        int maxZ = minZ + CHUNK_SIZE - 1;
        int reach = settings.maxRadius();
        int cellMinX = Math.floorDiv(minX - reach, settings.cellSize());
        int cellMaxX = Math.floorDiv(maxX + reach, settings.cellSize());
        int cellMinZ = Math.floorDiv(minZ - reach, settings.cellSize());
        int cellMaxZ = Math.floorDiv(maxZ + reach, settings.cellSize());

        List<Deposit> found = new ArrayList<>();
        for (int cellX = cellMinX; cellX <= cellMaxX; cellX++) {
            for (int cellZ = cellMinZ; cellZ <= cellMaxZ; cellZ++) {
                Optional<Deposit> deposit = cell(seed, world, cellX, cellZ, solidOres, oilOre, settings);
                if (deposit.isPresent() && touches(deposit.get(), minX, minZ, maxX, maxZ)) {
                    found.add(deposit.get());
                }
            }
        }
        return List.copyOf(found);
    }

    /** The deposit planned in one cell, or empty when the cell holds none. */
    static Optional<Deposit> cell(long seed, String world, int cellX, int cellZ, List<String> solidOres, String oilOre,
                                  DepositSettings settings) {
        long hash = cellHash(seed, world, cellX, cellZ);
        if (unit(hash, PRESENCE) >= settings.density()) {
            return Optional.empty();
        }
        String ore = unit(hash, ORE_KIND) < settings.oilShare()
                ? oilOre
                : solidOres.get(pick(hash, ORE_CHOICE, solidOres.size()));
        int radius = settings.minRadius() + pick(hash, RADIUS, settings.maxRadius() - settings.minRadius() + 1);
        int span = settings.cellSize() - 2 * radius + 1;
        int centreX = cellX * settings.cellSize() + radius + pick(hash, CENTRE_X, span);
        int centreZ = cellZ * settings.cellSize() + radius + pick(hash, CENTRE_Z, span);
        long amount = logUniformAmount(hash, settings);
        return Optional.of(new Deposit(hash, world, centreX, centreZ, radius, ore, amount, amount));
    }

    private static boolean touches(Deposit deposit, int minX, int minZ, int maxX, int maxZ) {
        long nearestX = Math.max(minX, Math.min(deposit.centreX(), maxX));
        long nearestZ = Math.max(minZ, Math.min(deposit.centreZ(), maxZ));
        long dx = deposit.centreX() - nearestX;
        long dz = deposit.centreZ() - nearestZ;
        return dx * dx + dz * dz <= (long) deposit.radius() * deposit.radius();
    }

    private static long logUniformAmount(long hash, DepositSettings settings) {
        double low = Math.log(settings.minAmount());
        double high = Math.log(settings.maxAmount());
        double exponent = low + unit(hash, AMOUNT) * (high - low);
        long amount = Math.round(Math.exp(exponent));
        return Math.max(settings.minAmount(), Math.min(settings.maxAmount(), amount));
    }

    private static long cellHash(long seed, String world, int cellX, int cellZ) {
        long worldHash = mix(seed ^ mix(world.hashCode()));
        return mix(worldHash + (((long) cellX) << 32 | (cellZ & 0xFFFFFFFFL)));
    }

    /** A uniform value from 0 inclusive to 1 exclusive, for one draw of a cell. */
    private static double unit(long hash, int draw) {
        return (mix(hash + (draw + 1) * GAMMA) >>> 11) * 0x1.0p-53;
    }

    /** A uniform index from 0 to {@code bound - 1}, for one draw of a cell. */
    private static int pick(long hash, int draw, int bound) {
        return Math.min(bound - 1, (int) (unit(hash, draw) * bound));
    }

    private static long mix(long value) {
        long z = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static void checkInputs(String world, List<String> solidOres, String oilOre, DepositSettings settings) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(oilOre, "oilOre");
        Objects.requireNonNull(settings, "settings");
        if (solidOres == null || solidOres.isEmpty()) {
            throw new IllegalArgumentException("A deposit needs at least one solid ore to choose from");
        }
        for (String ore : solidOres) {
            Objects.requireNonNull(ore, "solid ore id");
        }
    }
}
