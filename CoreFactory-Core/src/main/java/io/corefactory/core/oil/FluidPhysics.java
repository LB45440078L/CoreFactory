package io.corefactory.core.oil;

import java.util.Locale;

/**
 * The physics of the fluids: how fast a generator of a given power burns a fluid, how much energy a litre holds, and how volumes read in
 * the holograms and the menus. A litre is a cubic decimetre, so a cubic metre is a thousand litres. Pure.
 */
public final class FluidPhysics {

    /** Litres in one cubic metre. */
    public static final double LITRES_PER_CUBIC_METRE = 1000.0;

    /** Joules in one watt hour. */
    static final double JOULES_PER_WATT_HOUR = 3600.0;

    private FluidPhysics() {
    }

    /**
     * The electrical energy a litre of the fluid gives at an efficiency: the energy of the litre times the efficiency, in watt hours.
     *
     * @param fuel       the fluid burnt
     * @param efficiency the share of the fuel's energy that becomes electricity, above 0 and up to 1
     */
    public static double watthoursPerLitre(Fluid fuel, double efficiency) {
        requireEfficiency(efficiency);
        return fuel.joulesPerLitre() * efficiency / JOULES_PER_WATT_HOUR;
    }

    /**
     * The litres a generator burns each second to give a steady power: power over the electrical energy of a litre, per second.
     *
     * @param fuel       the fluid burnt
     * @param watts      the electrical power the generator gives, finite and 0 or more
     * @param efficiency the share of the fuel's energy that becomes electricity, above 0 and up to 1
     * @return the litres per second, 0 for no power
     */
    public static double litresPerSecond(Fluid fuel, double watts, double efficiency) {
        requireEfficiency(efficiency);
        if (!Double.isFinite(watts) || watts < 0) {
            throw new IllegalArgumentException("power must be finite and 0 or more, got " + watts);
        }
        return watts / (fuel.joulesPerLitre() * efficiency);
    }

    /**
     * A volume for a player to read: litres under a thousand, cubic metres from there.
     *
     * @param litres the volume, finite and 0 or more
     * @return for example {@code 850 L} or {@code 1.25 m3}
     */
    public static String volume(double litres) {
        requireFinite(litres);
        if (litres < LITRES_PER_CUBIC_METRE) {
            return String.format(Locale.ROOT, "%.0f L", litres);
        }
        return String.format(Locale.ROOT, "%.2f m3", litres / LITRES_PER_CUBIC_METRE);
    }

    /**
     * A flow for a player to read, in litres per second, or in cubic metres per hour from a thousand litres per second.
     *
     * @param litresPerSecond the flow, finite and 0 or more
     * @return for example {@code 4.0 L/s} or {@code 1.20 m3/h}
     */
    public static String rate(double litresPerSecond) {
        requireFinite(litresPerSecond);
        if (litresPerSecond < 0.1) {
            return String.format(Locale.ROOT, "%.4f L/s", litresPerSecond);
        }
        if (litresPerSecond < LITRES_PER_CUBIC_METRE) {
            return String.format(Locale.ROOT, "%.2f L/s", litresPerSecond);
        }
        double cubicMetresPerHour = litresPerSecond * 3600.0 / LITRES_PER_CUBIC_METRE;
        return String.format(Locale.ROOT, "%.2f m3/h", cubicMetresPerHour);
    }

    private static void requireEfficiency(double efficiency) {
        if (!Double.isFinite(efficiency) || efficiency <= 0 || efficiency > 1) {
            throw new IllegalArgumentException("efficiency must be above 0 and up to 1, got " + efficiency);
        }
    }

    private static void requireFinite(double value) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException("value must be finite and 0 or more, got " + value);
        }
    }
}
