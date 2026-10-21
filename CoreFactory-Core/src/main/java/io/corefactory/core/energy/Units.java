package io.corefactory.core.energy;

import java.util.Locale;

/**
 * The electrical constants of the grids and the conversions and display text of the energy model. Power is in watts,
 * energy in watt-hours, voltage in volts, current in amperes and resistance in ohms.
 */
public final class Units {

    /** The nominal voltage of a grid, in V. */
    public static final double NOMINAL_VOLTAGE_V = 230.0;

    /** The resistance of one pylon contact, in ohms. */
    public static final double PYLON_CONTACT_OHM = 0.01;

    /** The resistance of one block of wire, in ohms. */
    public static final double WIRE_OHM_PER_BLOCK = 0.001;

    /** Seconds in one hour. */
    public static final double SECONDS_PER_HOUR = 3600.0;

    /** Joules in one watt-hour. */
    public static final double JOULES_PER_WATT_HOUR = 3600.0;

    private static final double WATTS_PER_KILOWATT = 1_000.0;
    private static final double WATTS_PER_MEGAWATT = 1_000_000.0;
    private static final double WATT_HOURS_PER_KILOWATT_HOUR = 1_000.0;
    private static final double WATT_HOURS_PER_MEGAWATT_HOUR = 1_000_000.0;

    private Units() {
    }

    /**
     * The energy a power gives over a time, in watt-hours.
     *
     * @param watts   the power, in W
     * @param seconds the time, in s
     */
    public static double wattHours(double watts, double seconds) {
        return watts * seconds / SECONDS_PER_HOUR;
    }

    /**
     * The power that moves an energy over a time, in watts.
     *
     * @param wattHours the energy, in Wh
     * @param seconds   the time, in s, positive
     */
    public static double watts(double wattHours, double seconds) {
        return wattHours * SECONDS_PER_HOUR / seconds;
    }

    /** The watt-hours of an energy given in joules. */
    public static double wattHoursFromJoules(double joules) {
        return joules / JOULES_PER_WATT_HOUR;
    }

    /** The joules of an energy given in watt-hours. */
    public static double joulesFromWattHours(double wattHours) {
        return wattHours * JOULES_PER_WATT_HOUR;
    }

    /**
     * The resistance of a grid's path: one pylon contact plus the wire of the longest generator path.
     *
     * @param wireBlocks the longest wire path from a generator to a pylon, in blocks
     */
    public static double resistanceOhm(double wireBlocks) {
        return PYLON_CONTACT_OHM + WIRE_OHM_PER_BLOCK * wireBlocks;
    }

    /**
     * The voltage at the load of a grid: V = V0 - I * R with the current taken once at the nominal voltage, I = P / V0.
     *
     * @param powerW        the power the grid delivers, in W, not negative
     * @param resistanceOhm the resistance of the grid's path, in ohms, not negative
     * @return the voltage in V, never below 0
     */
    public static double voltageV(double powerW, double resistanceOhm) {
        double estimate = NOMINAL_VOLTAGE_V - (powerW / NOMINAL_VOLTAGE_V) * resistanceOhm;
        return Math.max(0.0, estimate);
    }

    /**
     * The current of a power at a voltage, in A.
     *
     * @param powerW   the power, in W
     * @param voltageV the voltage, in V
     * @return the current; 0 when the voltage is 0
     */
    public static double currentA(double powerW, double voltageV) {
        return voltageV > 0 ? powerW / voltageV : 0.0;
    }

    /** A power in kW. */
    public static double kilowatts(double watts) {
        return watts / WATTS_PER_KILOWATT;
    }

    /** An energy in kWh. */
    public static double kilowattHours(double wattHours) {
        return wattHours / WATT_HOURS_PER_KILOWATT_HOUR;
    }

    /**
     * A power as text: W below 1000 W, kW below 1000 kW, MW above; for example "850 W", "2.8 kW", "1.2 MW".
     *
     * @param watts the power, in W
     */
    public static String powerText(double watts) {
        double magnitude = Math.abs(watts);
        if (magnitude < WATTS_PER_KILOWATT) {
            return format("%.0f W", watts);
        }
        if (magnitude < WATTS_PER_MEGAWATT) {
            return format("%.1f kW", watts / WATTS_PER_KILOWATT);
        }
        return format("%.1f MW", watts / WATTS_PER_MEGAWATT);
    }

    /**
     * An energy as text: Wh below 1000 Wh, kWh below 1000 kWh, MWh above; for example "850 Wh", "4.5 kWh", "1.2 MWh".
     *
     * @param wattHours the energy, in Wh
     */
    public static String energyText(double wattHours) {
        double magnitude = Math.abs(wattHours);
        if (magnitude < WATT_HOURS_PER_KILOWATT_HOUR) {
            return format("%.0f Wh", wattHours);
        }
        if (magnitude < WATT_HOURS_PER_MEGAWATT_HOUR) {
            return format("%.1f kWh", wattHours / WATT_HOURS_PER_KILOWATT_HOUR);
        }
        return format("%.1f MWh", wattHours / WATT_HOURS_PER_MEGAWATT_HOUR);
    }

    /** A voltage as text with one decimal, for example "230.0 V". */
    public static String voltageText(double voltageV) {
        return format("%.1f V", voltageV);
    }

    /** A current as text with one decimal, for example "12.4 A". */
    public static String currentText(double currentA) {
        return format("%.1f A", currentA);
    }

    /** A fraction as a percent with no decimals, for example "80 %" for 0.8. */
    public static String percentText(double fraction) {
        return format("%.0f %%", fraction * 100.0);
    }

    private static String format(String pattern, double value) {
        return String.format(Locale.ROOT, pattern, value);
    }
}
