package io.corefactory.core.logistics;

import io.corefactory.core.energy.EnergyTable;

/**
 * The single source of truth for the logistics numbers. Speeds are in blocks per second (a belt unit is one block long),
 * rates in items per second, power in watts (W) as in {@link EnergyTable}.
 */
public final class LogisticsTable {

    /** Items one belt unit holds at most. */
    public static final int BELT_CAPACITY = 4;

    /** Ticks a picker waits before the first move of each run of moves. */
    public static final int PICKER_DELAY_TICKS = 10;

    /** The picker delay in seconds, at full satisfaction. */
    public static final double PICKER_DELAY_SECONDS = PICKER_DELAY_TICKS * EnergyTable.SECONDS_PER_TICK;

    private LogisticsTable() {
    }

    /**
     * How far an item travels along a belt unit per second, at full satisfaction.
     *
     * @throws IllegalArgumentException for level IV or V, which a belt does not have
     */
    public static double beltSpeedBlocksPerSecond(LogisticsLevel level) {
        return switch (level) {
            case I -> 1.875;
            case II -> 3.75;
            case III -> 7.5;
            case IV, V -> throw noBelt(level);
        };
    }

    /**
     * The power a belt unit asks for, in W.
     *
     * @throws IllegalArgumentException for level IV or V, which a belt does not have
     */
    public static double beltPowerW(LogisticsLevel level) {
        return switch (level) {
            case I -> 200.0;
            case II -> 400.0;
            case III -> 800.0;
            case IV, V -> throw noBelt(level);
        };
    }

    /** The items a picker moves per second, at full satisfaction: 1, 2, 4, 8 and 16 from level I to level V. */
    public static double pickerItemsPerSecond(LogisticsLevel level) {
        return switch (level) {
            case I -> 1.0;
            case II -> 2.0;
            case III -> 4.0;
            case IV -> 8.0;
            case V -> 16.0;
        };
    }

    /** The power a picker asks for, in W: 100, 200, 400, 800 and 1600 from level I to level V. */
    public static double pickerPowerW(LogisticsLevel level) {
        return switch (level) {
            case I -> 100.0;
            case II -> 200.0;
            case III -> 400.0;
            case IV -> 800.0;
            case V -> 1600.0;
        };
    }

    /**
     * The items a splitter passes per second, at full satisfaction.
     *
     * @throws IllegalArgumentException for level IV or V, which a splitter does not have
     */
    public static double splitterItemsPerSecond(LogisticsLevel level) {
        return switch (level) {
            case I -> 2.0;
            case II -> 4.0;
            case III -> 8.0;
            case IV, V -> throw noSplitter(level);
        };
    }

    /**
     * The power a splitter asks for, in W.
     *
     * @throws IllegalArgumentException for level IV or V, which a splitter does not have
     */
    public static double splitterPowerW(LogisticsLevel level) {
        return switch (level) {
            case I -> 500.0;
            case II -> 1000.0;
            case III -> 1500.0;
            case IV, V -> throw noSplitter(level);
        };
    }

    /**
     * The items a belt duplex takes in per second, at full satisfaction, summed over its two outputs. Level I and II equal what a
     * full belt of that level delivers ({@link #BELT_CAPACITY} items a block times its speed: 7.5 and 15 items a second), so a
     * duplex keeps up with a line of its own level; level III is capped below a full level III belt (30 items a second).
     */
    public static double duplexItemsPerSecond(LogisticsLevel level) {
        return switch (level) {
            case I -> 7.5;
            case II -> 15.0;
            case III -> 22.5;
            case IV, V -> throw noDuplex(level);
        };
    }

    /** The power a belt duplex asks for, in W: 0.5 kW, 1 kW and 1.5 kW, as a splitter. */
    public static double duplexPowerW(LogisticsLevel level) {
        return switch (level) {
            case I -> 500.0;
            case II -> 1000.0;
            case III -> 1500.0;
            case IV, V -> throw noDuplex(level);
        };
    }

    private static IllegalArgumentException noBelt(LogisticsLevel level) {
        return new IllegalArgumentException("a belt has no level " + level);
    }

    private static IllegalArgumentException noSplitter(LogisticsLevel level) {
        return new IllegalArgumentException("a splitter has no level " + level);
    }

    private static IllegalArgumentException noDuplex(LogisticsLevel level) {
        return new IllegalArgumentException("a belt duplex has no level " + level);
    }
}
