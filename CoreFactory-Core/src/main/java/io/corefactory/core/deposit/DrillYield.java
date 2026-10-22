package io.corefactory.core.deposit;

/**
 * The output rate of a drill at each level, in items per second, from a table the caller supplies. The table is copied
 * and checked once: every rate is finite and at least 0, and a higher level never yields less than a lower one.
 */
public final class DrillYield {

    private final double[] itemsPerSecond;

    /**
     * @param itemsPerSecondByLevel the rate of level 1 first, then level 2, and so on; at least one level
     * @throws IllegalArgumentException when the table is empty, holds a rate that is negative or not finite, or falls
     *                                  as the level rises
     */
    public DrillYield(double[] itemsPerSecondByLevel) {
        if (itemsPerSecondByLevel == null || itemsPerSecondByLevel.length == 0) {
            throw new IllegalArgumentException("A drill yield table needs at least one level");
        }
        for (int i = 0; i < itemsPerSecondByLevel.length; i++) {
            double rate = itemsPerSecondByLevel[i];
            if (!Double.isFinite(rate) || rate < 0) {
                throw new IllegalArgumentException("Level " + (i + 1) + " must yield a finite rate of 0 or more, not " + rate);
            }
            if (i > 0 && rate < itemsPerSecondByLevel[i - 1]) {
                throw new IllegalArgumentException("Level " + (i + 1) + " yields less than level " + i);
            }
        }
        this.itemsPerSecond = itemsPerSecondByLevel.clone();
    }

    /** The highest level the table covers. */
    public int maxLevel() {
        return itemsPerSecond.length;
    }

    /**
     * The rate of a level before any satisfaction is applied.
     *
     * @throws IllegalArgumentException when the level is outside 1 to {@link #maxLevel()}
     */
    public double itemsPerSecond(int level) {
        if (level < 1 || level > itemsPerSecond.length) {
            throw new IllegalArgumentException("A drill level is 1 to " + itemsPerSecond.length + ", not " + level);
        }
        return itemsPerSecond[level - 1];
    }
}
