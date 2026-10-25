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
     * The title of a pump's hologram and menu: its name and its level, such as {@code Water pump Level II}.
     *
     * @param level the pump's level, 1 to 5
     */
    public static String title(int level) {
        return "&fWater pump &7Level " + roman(level);
    }

    /**
     * The flow line: the water a pump puts into its network now, against what it can give, such as {@code Flow 4 of 8 /s}.
     *
     * @param delivered units per second the pump puts into its network
     * @param capacity  units per second the pump can give at its level
     */
    public static String flow(double delivered, double capacity) {
        return "&bFlow  &f" + number(delivered) + " &7of " + number(capacity) + " /s";
    }

    /**
     * The source line: infinite when the connected water reaches the minimum, else the count against the minimum.
     *
     * @param blocks  the connected water blocks counted, up to the cap
     * @param minimum the water blocks a source needs
     */
    public static String source(int blocks, int minimum) {
        if (blocks >= minimum) {
            return "&7Source  &aInfinite";
        }
        return "&7Source  &c" + blocks + " &7of " + minimum + " blocks";
    }

    /**
     * The fuel line of a pump at levels 1 and 2: at level 1 the items held and whether one burns; at level 2 the diesel it draws from
     * the oil network and whether it runs.
     *
     * @param level   the pump's level
     * @param items   the fuel items held, the one burning now included; not read at level 2
     * @param running true when the pump burns now
     */
    public static String fuel(int level, int items, boolean running) {
        String state = running ? "  &a· burning" : "  &e· waiting";
        if (PumpFuel.drawsFluid(level)) {
            return "&7Fuel  &fdiesel from the oil network" + state;
        }
        if (items <= 0) {
            return "&7Fuel  &cEmpty";
        }
        return "&7Fuel  &f" + items + " coal or charcoal" + state;
    }

    /**
     * The power line of a pump at levels 3 to 5: the share of its demand it gets, and the demand in kW.
     *
     * @param satisfaction the share of its demand the network gives it, 0 to 1
     * @param demandW      the power the pump asks for, in W
     */
    public static String power(double satisfaction, int demandW) {
        if (demandW <= 0) {
            return "&7Power  &7none needed";
        }
        double share = Math.max(0.0, Math.min(1.0, satisfaction));
        String colour = share >= 1.0 ? "&a" : share > 0.0 ? "&e" : "&c";
        long percent = Math.round(share * 100.0);
        return "&7Power  " + colour + percent + "% &7of " + number(demandW / 1000.0) + " kW";
    }

    /** The power line of a pump with no grid covering it. */
    public static String noGrid() {
        return "&7Power  &cNo grid";
    }

    /** The power line of a powered pump with no water source yet: it waits for one. */
    public static String noSource() {
        return "&7Power  &7waiting for a source";
    }

    /**
     * The status line of a pump: the state it is in, such as {@code Status  Pumping} or {@code Status  No fuel}.
     *
     * @param status the pump's state
     */
    public static String status(PumpStatus status) {
        return "&7Status  " + status.label();
    }

    /**
     * The line of a machine that takes water from a pipe: the units per second it receives against the units it asks for.
     *
     * @param delivered units per second the network gives the machine
     * @param requested units per second the machine asks for
     */
    public static String consumerLine(double delivered, double requested) {
        return "&7Water  &b" + number(delivered) + " &7of &f" + number(requested) + " /s";
    }

    /** The Roman numeral of a level, I to V. */
    public static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(level);
        };
    }
}
