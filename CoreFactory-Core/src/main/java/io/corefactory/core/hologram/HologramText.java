package io.corefactory.core.hologram;

import io.corefactory.core.drill.DrillKind;
import io.corefactory.core.drill.DrillSpec;
import io.corefactory.core.drill.DrillStatus;
import io.corefactory.core.drill.DrillText;
import io.corefactory.core.process.MachineState;

import java.util.List;

/**
 * The text of a machine's hologram, one string per line of the hologram. A lab tool shows three lines and a drill five.
 * Each line is short, and the pure string work uses ampersand colour codes, as the messages in config.yml do, so it has no
 * Bukkit types.
 */
public final class HologramText {

    /** The number of segments in a machine's bar. */
    public static final int BAR_WIDTH = 20;

    /** The lines of a lab tool's hologram: the process, its bar, and its percentage with the time left. */
    public static final int MACHINE_LINES = 3;

    /** The lines of a drill's hologram: its name and level, its power, its rate or status, its deposit, and its storage. */
    public static final int DRILL_LINES = 5;

    private static final String FILLED = "&a";
    private static final String EMPTY = "&8";
    private static final String SEGMENT = "■";
    private static final String IDLE = "&7idle";
    private static final String DOT = "  ·  ";
    private static final double PERCENT_EPSILON = 1e-9;

    private HologramText() {
    }

    /** The fraction clamped to 0 to 1. NaN counts as 0. */
    public static double clamp(double fraction) {
        if (Double.isNaN(fraction) || fraction < 0.0) {
            return 0.0;
        }
        return Math.min(1.0, fraction);
    }

    /** The share of a pass that has run: progress over total ticks, clamped. Zero when the total is not positive. */
    public static double fraction(int progress, int total) {
        if (total <= 0) {
            return 0.0;
        }
        return clamp((double) progress / total);
    }

    /**
     * A bar of {@code width} segments: filled segments in green, then empty ones in dark gray. The filled count is the
     * fraction times the width, rounded to the nearest segment. A width below one is read as one.
     */
    public static String progressBar(double fraction, int width) {
        int size = Math.max(1, width);
        int filled = (int) Math.round(clamp(fraction) * size);
        return FILLED + SEGMENT.repeat(filled) + EMPTY + SEGMENT.repeat(size - filled);
    }

    /**
     * The percentage of a fraction, such as {@code 29%}. It is truncated rather than rounded, so a pass that is not yet
     * finished never reads 100%.
     */
    public static String percent(double fraction) {
        return (int) Math.floor(clamp(fraction) * 100 + PERCENT_EPSILON) + "%";
    }

    /**
     * The name of a process, from its id: {@code crush_uraninite} reads {@code Crush uraninite}.
     */
    public static String processName(String processId) {
        String spaced = processId.replace('_', ' ');
        return spaced.isEmpty() ? spaced : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    /**
     * The lines of a lab tool's hologram. While a process runs: its name and the process, its bar against the process's total
     * ticks, and the percentage with the seconds left. When idle: the name, an empty bar and {@code idle}.
     *
     * @param name       the machine's display name, with colour codes
     * @param state      the machine's state
     * @param totalTicks the ticks one pass of the running process takes
     */
    public static List<String> forMachine(String name, MachineState state, int totalTicks) {
        if (state.processId() == null) {
            return List.of(name, progressBar(0.0, BAR_WIDTH), IDLE);
        }
        double fraction = fraction(state.progress(), totalTicks);
        int remaining = Math.max(0, totalTicks - state.progress());
        String left = remaining == 0 ? "finishing" : "about " + (remaining + 19) / 20 + " s";
        return List.of(
                "&f" + processName(state.processId()),
                progressBar(fraction, BAR_WIDTH),
                "&f" + percent(fraction) + "&7" + DOT + left);
    }

    /**
     * The lines of a drill's hologram: its level and kind, its power, its rate while it runs or else its status, its deposit,
     * and its storage.
     *
     * @param kind           the drill's kind
     * @param spec           the drill's level
     * @param efficiency     the drill's efficiency this moment, the share of its demand the grid gives it
     * @param status         the drill's status
     * @param itemsPerSecond the units the drill yields per second now, shown while it runs
     * @param depositLine    the deposit line from {@link DrillText#deposit}
     * @param stored         the items in its output slots
     * @param capacity       the most items its output slots can hold
     */
    public static List<String> drillLines(DrillKind kind, DrillSpec spec, double efficiency, DrillStatus status,
                                          double itemsPerSecond, String depositLine, int stored, int capacity) {
        String state = status == DrillStatus.RUNNING
                ? "&7Extracting &f" + DrillText.rate(itemsPerSecond)
                : "&7" + status.text(kind);
        return List.of(
                "&bL" + spec.level() + " " + kind.label(),
                "&7Power: &e" + DrillText.power(efficiency),
                state,
                depositLine,
                "&7Stored &f" + stored + "&7/" + capacity);
    }
}
