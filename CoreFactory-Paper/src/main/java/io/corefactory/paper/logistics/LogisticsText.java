package io.corefactory.paper.logistics;

import io.corefactory.core.energy.Units;
import io.corefactory.core.logistics.LogisticsLevel;

import java.util.List;
import java.util.Locale;

/** The text of the logistics holograms, with ampersand colour codes. Pure. */
public final class LogisticsText {

    private LogisticsText() {
    }

    /** The name of a device at a level, such as {@code Picker II}. */
    public static String name(LogisticsIds.Kind kind, LogisticsLevel level) {
        return kind.title() + " " + roman(level);
    }

    /** The roman numeral of a level: I, II or III. */
    public static String roman(LogisticsLevel level) {
        return switch (level) {
            case I -> "I";
            case II -> "II";
            case III -> "III";
        };
    }

    /**
     * The status of a picker: moving at its rate while items pass, waiting when its output is full, waiting when no item is at
     * its input.
     *
     * @param moved        true when it moved an item this tick or recently
     * @param blocked      true when an item waits at its input and its output refuses it
     * @param itemsPerSecond the rate it moves at now, at full satisfaction times its satisfaction
     */
    public static String pickerStatus(boolean moved, boolean blocked, double itemsPerSecond) {
        if (blocked) {
            return "&cwaiting: output full";
        }
        if (moved) {
            return "&amoving " + String.format(Locale.ROOT, "%.1f", itemsPerSecond) + "/s";
        }
        return "&7waiting: no item";
    }

    /** The status of a splitter: flowing while items pass, blocked while it holds an item it cannot deliver, idle otherwise. */
    public static String splitterStatus(boolean moved, boolean blocked) {
        if (blocked) {
            return "&cblocked";
        }
        if (moved) {
            return "&aflowing";
        }
        return "&7idle";
    }

    /** The lines of a picker's hologram: its name and its status. */
    public static List<String> pickerLines(LogisticsLevel level, double demandW, String status) {
        return List.of("&6" + name(LogisticsIds.Kind.PICKER, level) + "  &7" + Units.powerText(demandW), status);
    }

    /** The lines of a splitter's hologram: its name, the counts of its two lists, and its status. */
    public static List<String> splitterLines(LogisticsLevel level, double demandW, int left, int right, String status) {
        return List.of("&6" + name(LogisticsIds.Kind.SPLITTER, level) + "  &7" + Units.powerText(demandW),
                "&7Left: &f" + left + " &7Right: &f" + right, status);
    }
}
