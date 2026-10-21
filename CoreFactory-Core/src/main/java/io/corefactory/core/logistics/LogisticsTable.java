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

    /** How far an item travels along a belt unit per second, at full satisfaction. */
    public static double beltSpeedBlocksPerSecond(LogisticsLevel level) {
        return switch (level) {
            case I -> 1.875;
            case II -> 3.75;
            case III -> 7.5;
        };
    }

    /** The power a belt unit asks for, in W. */
    public static double beltPowerW(LogisticsLevel level) {
        return switch (level) {
            case I -> 200.0;
            case II -> 400.0;
            case III -> 800.0;
        };
    }

    /** The items a picker moves per second, at full satisfaction. */
    public static double pickerItemsPerSecond(LogisticsLevel level) {
        return switch (level) {
            case I -> 1.0;
            case II -> 2.0;
            case III -> 4.0;
        };
    }

    /** The power a picker asks for, in W. */
    public static double pickerPowerW(LogisticsLevel level) {
        return switch (level) {
            case I -> 100.0;
            case II -> 200.0;
            case III -> 400.0;
        };
    }

    /** The items a splitter passes per second, at full satisfaction. */
    public static double splitterItemsPerSecond(LogisticsLevel level) {
        return switch (level) {
            case I -> 2.0;
            case II -> 4.0;
            case III -> 8.0;
        };
    }

    /** The power a splitter asks for, in W. */
    public static double splitterPowerW(LogisticsLevel level) {
        return switch (level) {
            case I -> 500.0;
            case II -> 1000.0;
            case III -> 1500.0;
        };
    }
}
