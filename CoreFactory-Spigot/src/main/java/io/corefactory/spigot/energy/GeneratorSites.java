package io.corefactory.spigot.energy;

import io.corefactory.core.energy.Generator;
import io.corefactory.core.energy.OpenAir;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

/**
 * Reads the site of a generator from the world: the air above it for wind, and whether it stands in water for nuclear. The
 * counts are the core's: six blocks above for the air, and the five positions above and beside for the water. Only chunks that are
 * loaded are read, so the callers check that first.
 */
public final class GeneratorSites {

    /** The blocks above a generator that are counted as air: the core stops counting at this many. */
    static final int ABOVE_SCAN = 6;

    private GeneratorSites() {
    }

    /**
     * The open-air input of a wind generator: the air blocks among the six above it, and its y.
     *
     * @param block the generator's block
     */
    public static OpenAir openAir(Location block) {
        World world = block.getWorld();
        int air = 0;
        for (int i = 1; i <= ABOVE_SCAN; i++) {
            if (world.getBlockAt(block.getBlockX(), block.getBlockY() + i, block.getBlockZ()).getType().isAir()) {
                air++;
            }
        }
        return new OpenAir(air, block.getBlockY());
    }

    /**
     * Whether a nuclear generator stands fully under water: its own block is water, and no other block above it or beside it is.
     *
     * @param block the generator's block
     */
    public static boolean underwater(Location block) {
        World world = block.getWorld();
        int x = block.getBlockX();
        int y = block.getBlockY();
        int z = block.getBlockZ();
        boolean ownWater = world.getBlockAt(x, y, z).getType() == Material.WATER;
        int notWater = 0;
        int[][] around = {{0, 1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int[] offset : around) {
            if (world.getBlockAt(x + offset[0], y + offset[1], z + offset[2]).getType() != Material.WATER) {
                notWater++;
            }
        }
        return Generator.isUnderwater(ownWater, notWater);
    }
}
