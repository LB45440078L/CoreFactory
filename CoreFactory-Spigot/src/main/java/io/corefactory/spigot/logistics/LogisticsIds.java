package io.corefactory.spigot.logistics;

import io.corefactory.core.logistics.LogisticsLevel;

import java.util.Optional;

/** The component ids of the belts, pickers and splitters: belt_1 to belt_3, picker_1 to picker_3, splitter_1 to splitter_3. Pure. */
public final class LogisticsIds {

    /** The three kinds of logistics device. */
    public enum Kind {
        /** A belt unit, one block of conveyor. */
        BELT("belt", "Belt"),
        /** A picker, which moves one item from its input to its output. */
        PICKER("picker", "Picker"),
        /** A splitter, which sends items straight, left or right by its filter. */
        SPLITTER("splitter", "Splitter");

        private final String prefix;
        private final String title;

        Kind(String prefix, String title) {
            this.prefix = prefix;
            this.title = title;
        }

        /** The component id prefix, such as {@code belt}. */
        public String prefix() {
            return prefix;
        }

        /** The name shown to players, such as {@code Belt}. */
        public String title() {
            return title;
        }
    }

    /**
     * A logistics device named by a component id.
     *
     * @param kind  the kind
     * @param level the level
     */
    public record Device(Kind kind, LogisticsLevel level) {
    }

    private LogisticsIds() {
    }

    /** The component id of a kind at a level, such as {@code picker_2}. */
    public static String componentId(Kind kind, LogisticsLevel level) {
        return kind.prefix() + "_" + (level.ordinal() + 1);
    }

    /** Whether a component id is a picker or a splitter: a device that is placed against the side of a machine or a generator. */
    public static boolean takesSideAim(String componentId) {
        return of(componentId).filter(device -> device.kind() != Kind.BELT).isPresent();
    }

    /** The device a component id names, or empty when the id is not a belt, picker or splitter. */
    public static Optional<Device> of(String componentId) {
        if (componentId == null) {
            return Optional.empty();
        }
        for (Kind kind : Kind.values()) {
            String start = kind.prefix() + "_";
            if (componentId.startsWith(start) && componentId.length() == start.length() + 1) {
                char digit = componentId.charAt(start.length());
                if (digit >= '1' && digit <= '3') {
                    return Optional.of(new Device(kind, LogisticsLevel.values()[digit - '1']));
                }
            }
        }
        return Optional.empty();
    }
}
