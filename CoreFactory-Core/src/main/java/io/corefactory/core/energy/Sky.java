package io.corefactory.core.energy;

/**
 * The sky curves that drive the weather generators: the sun's irradiance over the day, the wind speed over the day and the
 * output of each. Every function is pure: the same inputs give the same output, so a world tick and a seed replay the same
 * wind and the same sun.
 */
public final class Sky {

    private static final long SPLITMIX_GAMMA = 0x9E3779B97F4A7C15L;
    private static final long SPLITMIX_A = 0xBF58476D1CE4E5B9L;
    private static final long SPLITMIX_B = 0x94D049BB133111EBL;
    private static final double UNIT = 0x1.0p-53;

    private Sky() {
    }

    /** The time of day of a world tick, from 0 to {@link EnergyTable#DAY_TICKS} - 1. */
    public static long dayTicks(long worldTick) {
        return Math.floorMod(worldTick, EnergyTable.DAY_TICKS);
    }

    /**
     * The sine of the sun's elevation at a time of day, from 0 to 1. The sun is at its highest at noon (6000) and sets at
     * 13000; it rises again at 23000. Its elevation grows linearly from 0 at dawn and dusk to 90 degrees at noon, so the
     * sine is a cosine of the time from noon. It is exactly 0 from 13000 to 23000.
     *
     * @param dayTicks the time of day; any value is folded into one day
     */
    public static double sunSine(long dayTicks) {
        long day = dayTicks(dayTicks);
        if (day >= EnergyTable.NIGHT_START_TICK && day < EnergyTable.NIGHT_END_TICK) {
            return 0.0;
        }
        long halfDay = EnergyTable.DAY_TICKS / 2;
        long fromNoon = Math.floorMod(day - EnergyTable.NOON_TICK + halfDay, EnergyTable.DAY_TICKS) - halfDay;
        long daylightHalfWidth = EnergyTable.NIGHT_START_TICK - EnergyTable.NOON_TICK;
        if (Math.abs(fromNoon) >= daylightHalfWidth) {
            return 0.0;
        }
        return Math.cos(Math.PI / 2 * fromNoon / daylightHalfWidth);
    }

    /**
     * The irradiance on a panel at a time of day, in W/m2: the peak irradiance times the sine of the sun's elevation.
     *
     * @param dayTicks the time of day
     */
    public static double irradianceWPerM2(long dayTicks) {
        return EnergyTable.IRRADIANCE_PEAK_W_M2 * sunSine(dayTicks);
    }

    /**
     * The output of one solar panel at an irradiance, in W: area times efficiency times irradiance.
     *
     * @param irradianceWPerM2 the irradiance, in W/m2; clamped to 0 .. the peak
     */
    public static double solarOutputW(double irradianceWPerM2) {
        double clamped = Math.min(EnergyTable.IRRADIANCE_PEAK_W_M2, Math.max(0.0, irradianceWPerM2));
        return EnergyTable.SOLAR_EFFICIENCY * EnergyTable.SOLAR_PANEL_AREA_M2 * clamped;
    }

    /**
     * The wind factor at a world tick, from 0 to 1: a fixed sine over one day, mixed with a noise that is seeded and
     * changes once every {@link EnergyTable#WIND_NOISE_CELL_TICKS} ticks.
     *
     * @param worldTick the world's full time
     * @param seed      the seed of the world's wind
     */
    public static double windFactor(long worldTick, long seed) {
        double phase = 2 * Math.PI * Math.floorMod(worldTick, EnergyTable.WIND_PERIOD_TICKS)
                / EnergyTable.WIND_PERIOD_TICKS;
        double sine = 0.5 + 0.5 * Math.sin(phase);
        long cell = Math.floorDiv(worldTick, EnergyTable.WIND_NOISE_CELL_TICKS);
        double noise = noise(seed, cell);
        double mixed = EnergyTable.WIND_SINE_WEIGHT * sine + EnergyTable.WIND_NOISE_WEIGHT * noise;
        return Math.min(1.0, Math.max(0.0, mixed));
    }

    /**
     * The wind speed at a world tick, in m/s: between the lowest and the highest speed of the daily curve.
     *
     * @param worldTick the world's full time
     * @param seed      the seed of the world's wind
     */
    public static double windSpeedMs(long worldTick, long seed) {
        return EnergyTable.WIND_MIN_SPEED_MS
                + (EnergyTable.WIND_MAX_SPEED_MS - EnergyTable.WIND_MIN_SPEED_MS) * windFactor(worldTick, seed);
    }

    /**
     * The power of a wind rotor at a wind speed, in W: 0.5 * air density * rotor area * v^3 * power coefficient, capped at
     * the rated output. It is 0 below the cut-in speed and above the cut-out speed.
     *
     * @param speedMs the wind speed, in m/s
     */
    public static double windPowerW(double speedMs) {
        if (speedMs < EnergyTable.WIND_CUT_IN_MS || speedMs > EnergyTable.WIND_CUT_OUT_MS) {
            return 0.0;
        }
        double area = Math.PI * EnergyTable.WIND_ROTOR_RADIUS_M * EnergyTable.WIND_ROTOR_RADIUS_M;
        double power = 0.5 * EnergyTable.AIR_DENSITY_KG_M3 * area * speedMs * speedMs * speedMs
                * EnergyTable.WIND_POWER_COEFFICIENT;
        return Math.min(EnergyTable.WIND_RATED_W, power);
    }

    /**
     * The output of a wind generator at a world tick, in W: 0 when its site is closed, otherwise the power at the wind speed.
     *
     * @param air       the air above the generator and its y
     * @param worldTick the world's full time
     * @param seed      the seed of the world's wind
     */
    public static double windOutputW(OpenAir air, long worldTick, long seed) {
        if (!air.open()) {
            return 0.0;
        }
        return windPowerW(windSpeedMs(worldTick, seed));
    }

    /** A hash of the seed and a cell, mapped to [0, 1). Equal inputs always give an equal value. */
    static double noise(long seed, long cell) {
        long z = seed + cell * SPLITMIX_GAMMA;
        z = (z ^ (z >>> 30)) * SPLITMIX_A;
        z = (z ^ (z >>> 27)) * SPLITMIX_B;
        z = z ^ (z >>> 31);
        return (z >>> 11) * UNIT;
    }
}
