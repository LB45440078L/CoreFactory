package io.corefactory.core.energy;

/**
 * The single source of truth for the energy numbers: pylon ranges and maximum currents, wires, accumulator levels, the
 * output and fuel of every generator, and the demand of the lab tools and drills. Power is in watts (W), energy in
 * watt-hours (Wh) unless a name says joules (J), current in amperes (A), time in server ticks (20 per second).
 */
public final class EnergyTable {

    /** Ticks in a second of the server clock. */
    public static final int TICKS_PER_SECOND = 20;

    /** Seconds in one tick. */
    public static final double SECONDS_PER_TICK = 1.0 / TICKS_PER_SECOND;

    /** Ticks in one in-game day. */
    public static final long DAY_TICKS = 24_000L;

    /** The day time of noon, where the sun is highest. */
    public static final long NOON_TICK = 6_000L;

    /** The day time where the night starts. */
    public static final long NIGHT_START_TICK = 13_000L;

    /** The day time where the night ends. */
    public static final long NIGHT_END_TICK = 23_000L;

    /** Half-side of a level I pylon's range square, in blocks. */
    public static final int SMALL_HALF_SIDE = 5;

    /** Maximum current of a level I pylon, in A. */
    public static final double SMALL_MAX_CURRENT_A = 100.0;

    /** Half-side of a level II pylon's range square, in blocks. */
    public static final int MEDIUM_HALF_SIDE = 9;

    /** Maximum current of a level II pylon, in A. */
    public static final double MEDIUM_MAX_CURRENT_A = 400.0;

    /** Half-side of a level III pylon's range square, in blocks. */
    public static final int LARGE_HALF_SIDE = 15;

    /** Maximum current of a level III pylon, in A. */
    public static final double LARGE_MAX_CURRENT_A = 1600.0;

    /** Longest wire, in horizontal blocks. */
    public static final int WIRE_MAX_LENGTH = 24;

    /** Capacity of accumulator level I, in Wh. */
    public static final double ACCUMULATOR_I_CAPACITY_WH = 5_000.0;

    /** Charge and discharge power of accumulator level I, in W. */
    public static final double ACCUMULATOR_I_POWER_W = 5_000.0;

    /** Capacity of accumulator level II, in Wh. */
    public static final double ACCUMULATOR_II_CAPACITY_WH = 20_000.0;

    /** Charge and discharge power of accumulator level II, in W. */
    public static final double ACCUMULATOR_II_POWER_W = 20_000.0;

    /** Capacity of accumulator level III, in Wh. */
    public static final double ACCUMULATOR_III_CAPACITY_WH = 80_000.0;

    /** Charge and discharge power of accumulator level III, in W. */
    public static final double ACCUMULATOR_III_POWER_W = 60_000.0;

    /** The share of the energy taken in that is stored when an accumulator charges: the round trip efficiency. */
    public static final double ACCUMULATOR_ROUND_TRIP = 0.9;

    /** Rated output of a wind generator, in W. */
    public static final double WIND_RATED_W = 3_000.0;

    /** Lowest wind speed of the daily curve, in m/s. */
    public static final double WIND_MIN_SPEED_MS = 3.0;

    /** Highest wind speed of the daily curve, in m/s. */
    public static final double WIND_MAX_SPEED_MS = 12.0;

    /** Wind speed below which a wind generator gives nothing, in m/s. */
    public static final double WIND_CUT_IN_MS = 3.0;

    /** Wind speed above which a wind generator gives nothing, in m/s. */
    public static final double WIND_CUT_OUT_MS = 25.0;

    /** Density of air, in kg/m3. */
    public static final double AIR_DENSITY_KG_M3 = 1.225;

    /** Radius of a wind rotor, in m. */
    public static final double WIND_ROTOR_RADIUS_M = 2.0;

    /** Power coefficient of a wind rotor. */
    public static final double WIND_POWER_COEFFICIENT = 0.35;

    /** Air blocks needed above a wind generator for it to work. */
    public static final int WIND_MIN_OPEN_BLOCKS = 6;

    /** Lowest y at which a wind generator works. */
    public static final int WIND_MIN_Y = 96;

    /** Ticks in one period of the wind's daily sine. */
    public static final long WIND_PERIOD_TICKS = DAY_TICKS;

    /** Ticks over which the wind's noise stays constant before it changes. */
    public static final long WIND_NOISE_CELL_TICKS = 200L;

    /** Weight of the daily sine in the wind factor. */
    public static final double WIND_SINE_WEIGHT = 0.6;

    /** Weight of the noise in the wind factor. */
    public static final double WIND_NOISE_WEIGHT = 0.4;

    /** Area of one solar panel, in m2. */
    public static final double SOLAR_PANEL_AREA_M2 = 1.6;

    /** Efficiency of one solar panel. */
    public static final double SOLAR_EFFICIENCY = 0.2;

    /** Irradiance at the sun's peak, in W/m2. */
    public static final double IRRADIANCE_PEAK_W_M2 = 1_000.0;

    /** Rated output of a charcoal generator while burning, in W. */
    public static final double CHARCOAL_RATED_W = 25_000.0;

    /** Lower heating value of one charcoal item, in J: 0.1 kg at 30 MJ/kg. */
    public static final double CHARCOAL_FUEL_J = 3.0e6;

    /** Efficiency of a charcoal generator. */
    public static final double CHARCOAL_EFFICIENCY = 0.25;

    /** Rated output of a petroleum generator while burning, in W. */
    public static final double PETROLEUM_RATED_W = 80_000.0;

    /** Lower heating value of one petroleum item, in J: 1 L at 0.85 kg/L and 43 MJ/kg. */
    public static final double PETROLEUM_FUEL_J = 36.6e6;

    /** Efficiency of a petroleum generator. */
    public static final double PETROLEUM_EFFICIENCY = 0.35;

    /** Rated output of a diesel generator while burning, in W. */
    public static final double DIESEL_RATED_W = 150_000.0;

    /** Lower heating value of one diesel item, in J: 1 L at 0.84 kg/L and 43 MJ/kg. */
    public static final double DIESEL_FUEL_J = 36.1e6;

    /** Efficiency of a diesel generator. */
    public static final double DIESEL_EFFICIENCY = 0.40;

    /** Rated output of a nuclear generator while burning underwater, in W. */
    public static final double NUCLEAR_RATED_W = 2_000_000.0;

    /** Electric energy of one nuclear fuel rod, in Wh. It is already electric, so no efficiency is applied to it. */
    public static final double NUCLEAR_ROD_WH = 356_000_000.0;

    /** Power a lab tool asks for, in W. */
    public static final double LAB_TOOL_DEMAND_W = 4_000.0;

    private static final double[] DRILL_DEMAND_W = {15_000.0, 25_000.0, 40_000.0, 60_000.0, 90_000.0};

    private EnergyTable() {
    }

    /**
     * The power a drill of a level asks for.
     *
     * @param level the drill's level, from 1 to 5
     * @return the demand in W
     * @throws IllegalArgumentException when the level is out of range
     */
    public static double drillDemandW(int level) {
        if (level < 1 || level > DRILL_DEMAND_W.length) {
            throw new IllegalArgumentException("drill level must be from 1 to " + DRILL_DEMAND_W.length + ": " + level);
        }
        return DRILL_DEMAND_W[level - 1];
    }
}
