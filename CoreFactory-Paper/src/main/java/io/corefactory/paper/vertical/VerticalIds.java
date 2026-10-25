package io.corefactory.paper.vertical;

import java.util.OptionalInt;

/** The component ids of the vertical transport belts: vertical_belt_1 to vertical_belt_3, one for each level I to III. Pure. */
public final class VerticalIds {

    /** The id prefix shared by the three levels. */
    public static final String PREFIX = "vertical_belt_";

    /** The highest level. */
    public static final int MAX_LEVEL = 3;

    private VerticalIds() {
    }

    /**
     * The component id of a level, such as {@code vertical_belt_2}.
     *
     * @throws IllegalArgumentException when the level is outside 1 to {@link #MAX_LEVEL}
     */
    public static String componentId(int level) {
        if (level < 1 || level > MAX_LEVEL) {
            throw new IllegalArgumentException("level must be 1 to " + MAX_LEVEL + ": " + level);
        }
        return PREFIX + level;
    }

    /** The level a component id names, or empty when the id is not a vertical belt. */
    public static OptionalInt levelOf(String componentId) {
        if (componentId == null || !componentId.startsWith(PREFIX) || componentId.length() != PREFIX.length() + 1) {
            return OptionalInt.empty();
        }
        char digit = componentId.charAt(PREFIX.length());
        if (digit < '1' || digit > '0' + MAX_LEVEL) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(digit - '0');
    }

    /**
     * The roman numeral of a level: I, II or III.
     *
     * @throws IllegalArgumentException when the level is outside 1 to {@link #MAX_LEVEL}
     */
    public static String roman(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> throw new IllegalArgumentException("level must be 1 to " + MAX_LEVEL + ": " + level);
        };
    }
}
