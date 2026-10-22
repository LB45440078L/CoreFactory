package io.corefactory.core.water;

import java.util.Locale;

/**
 * The text of the water holograms and messages. Pure: the numbers come in, the lines come out, with ampersand colour codes left for the
 * caller to colour.
 */
public final class WaterText {

    private WaterText() {
    }

    /**
     * A number for a line: a whole number with no decimals, anything else to one decimal.
     *
     * @param value the number, finite
     */
    public static String number(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    /**
     * The flow line of a pump: its level and the water it delivers into its network.
     *
     * @param level     the pump's level, 1 to 5
     * @param delivered units per second the pump puts into its network
     */
    public static String pumpFlow(int level, double delivered) {
        return "Pump L" + level + "  " + number(delivered) + " /s delivered";
    }

    /**
     * The source line of a pump: infinite while its connected water is at the minimum or more, else the count against the minimum.
     *
     * @param blocks  the connected water blocks counted, up to the cap
     * @param minimum the water blocks a source needs
     */
    public static String source(int blocks, int minimum) {
        if (blocks >= minimum) {
            return "Water source: infinite";
        }
        return "Source low: " + blocks + " blocks, needs " + minimum;
    }

    /**
     * The fuel line of a pump at levels 1 and 2: the items held, the fuel's name, and whether it burns.
     *
     * @param level   the pump's level
     * @param items   the fuel items held
     * @param running true when the pump burns now
     */
    public static String fuel(int level, int items, boolean running) {
        if (items <= 0) {
            return "Fuel: empty, stopped";
        }
        String name = level == 2 ? PumpFuel.DIESEL : "coal or charcoal";
        return "Fuel: " + items + " " + name + (running ? ", burning" : ", waiting");
    }

    /**
     * The power line of a pump at levels 3 to 5: the share of its demand it gets, and the demand in kW.
     *
     * @param satisfaction the share of its demand the network gives it, 0 to 1
     * @param demandW      the power the pump asks for, in W
     */
    public static String power(double satisfaction, int demandW) {
        if (demandW <= 0) {
            return "Power: none needed";
        }
        long percent = Math.round(Math.max(0.0, Math.min(1.0, satisfaction)) * 100.0);
        return "Power: " + percent + "% of " + number(demandW / 1000.0) + " kW";
    }

    /**
     * The refusal of a pump placed where its source is too small.
     *
     * @param blocks  the connected water blocks counted
     * @param minimum the water blocks a source needs
     */
    public static String sourceTooSmall(int blocks, int minimum) {
        return "A pump needs at least " + minimum + " water blocks connected to it. This one has " + blocks + ".";
    }

    /**
     * The line of a machine that takes water from a pipe: the units per second it receives against the units it asks for.
     *
     * @param delivered units per second the network gives the machine
     * @param requested units per second the machine asks for
     */
    public static String consumerLine(double delivered, double requested) {
        return "Water: " + number(delivered) + " of " + number(requested) + " /s";
    }
}
