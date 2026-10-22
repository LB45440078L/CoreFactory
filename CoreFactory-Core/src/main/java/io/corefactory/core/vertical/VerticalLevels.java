package io.corefactory.core.vertical;

/**
 * The per-level numbers of the vertical transport belt: the carry speed in blocks per second and the capacity in items
 * per block. Level 1 is the first entry of each array, as in the config keys vertical-belt.speed and vertical-belt.capacity.
 */
public final class VerticalLevels {

    /** The number of levels: I, II and III. */
    public static final int LEVELS = 3;

    private final double[] speeds;
    private final int[] capacities;

    private VerticalLevels(double[] speeds, int[] capacities) {
        this.speeds = speeds;
        this.capacities = capacities;
    }

    /**
     * Builds the table from the config lists. The arrays are copied.
     *
     * @param speeds carry speed per level in blocks per second, level 1 first; each finite and above zero
     * @param capacities items per block per level, level 1 first; each at least 1
     * @return the table
     * @throws IllegalArgumentException when an array is missing, has a length other than {@link #LEVELS}, or holds a value
     *         out of range
     */
    public static VerticalLevels of(double[] speeds, int[] capacities) {
        if (speeds == null || capacities == null) {
            throw new IllegalArgumentException("vertical belt speeds and capacities are required");
        }
        if (speeds.length != LEVELS) {
            throw new IllegalArgumentException("vertical belt speeds need " + LEVELS + " values: " + speeds.length);
        }
        if (capacities.length != LEVELS) {
            throw new IllegalArgumentException("vertical belt capacities need " + LEVELS + " values: " + capacities.length);
        }
        for (double speed : speeds) {
            if (!Double.isFinite(speed) || speed <= 0.0) {
                throw new IllegalArgumentException("vertical belt speed must be finite and above zero: " + speed);
            }
        }
        for (int capacity : capacities) {
            if (capacity < 1) {
                throw new IllegalArgumentException("vertical belt capacity must be at least 1: " + capacity);
            }
        }
        return new VerticalLevels(speeds.clone(), capacities.clone());
    }

    /**
     * The carry speed of a level.
     *
     * @param level 1 to {@link #LEVELS}
     * @return the speed in blocks per second
     * @throws IllegalArgumentException when the level is outside 1 to {@link #LEVELS}
     */
    public double speed(int level) {
        return speeds[index(level)];
    }

    /**
     * The capacity of a level.
     *
     * @param level 1 to {@link #LEVELS}
     * @return the items per block
     * @throws IllegalArgumentException when the level is outside 1 to {@link #LEVELS}
     */
    public int capacity(int level) {
        return capacities[index(level)];
    }

    private static int index(int level) {
        if (level < 1 || level > LEVELS) {
            throw new IllegalArgumentException("level must be 1 to " + LEVELS + ": " + level);
        }
        return level - 1;
    }
}
