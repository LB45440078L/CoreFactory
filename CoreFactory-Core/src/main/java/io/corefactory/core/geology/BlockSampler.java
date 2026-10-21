package io.corefactory.core.geology;

/**
 * Reads the material name at a block position. The planner uses it so that it never depends on Bukkit: the Spigot
 * module passes the world, tests pass a map.
 */
@FunctionalInterface
public interface BlockSampler {

    /** The material name (for example {@code STONE}) at the position. */
    String materialAt(int x, int y, int z);
}
