package io.corefactory.paper.energy;

import io.corefactory.core.energy.ChartMode;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The rules of the grid charts: the name of a chart, the limits on how many there are, and the order its mode cycles in. Pure, so
 * the rules are tested without a server.
 */
public final class ChartRules {

    /** The longest name of a chart. */
    public static final int MAX_NAME_LENGTH = 24;

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,24}");

    /** What a new chart is refused for, or that it is allowed. */
    public enum Verdict {
        /** The chart may be made. */
        OK,
        /** The grid already has as many charts as the limit per grid allows. */
        GRID_FULL,
        /** The server already has as many charts as the limit allows. */
        SERVER_FULL
    }

    private ChartRules() {
    }

    /** True when the name is 1 to 24 letters, digits or underscores. */
    public static boolean validName(String name) {
        return name != null && NAME.matcher(name).matches();
    }

    /**
     * Whether a new chart may be made on a grid.
     *
     * @param chartsOnGrid  the charts the grid already has
     * @param chartsTotal   the charts the server already has
     * @param perGridLimit  the most charts one grid may have
     * @param totalLimit    the most charts the server may have
     */
    public static Verdict canCreate(int chartsOnGrid, int chartsTotal, int perGridLimit, int totalLimit) {
        if (chartsOnGrid >= perGridLimit) {
            return Verdict.GRID_FULL;
        }
        if (chartsTotal >= totalLimit) {
            return Verdict.SERVER_FULL;
        }
        return Verdict.OK;
    }

    /** The mode after a click: satisfaction, then production, then storage, then satisfaction again. */
    public static ChartMode next(ChartMode mode) {
        return switch (mode) {
            case SATISFACTION -> ChartMode.PRODUCTION;
            case PRODUCTION -> ChartMode.STORAGE;
            case STORAGE -> ChartMode.SATISFACTION;
        };
    }

    /** The mode a command word names: satisfaction, production or storage, in any case. */
    public static Optional<ChartMode> parseMode(String word) {
        if (word == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ChartMode.valueOf(word.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** The word a mode is written and named by in commands: satisfaction, production or storage. */
    public static String word(ChartMode mode) {
        return mode.name().toLowerCase(Locale.ROOT);
    }
}
