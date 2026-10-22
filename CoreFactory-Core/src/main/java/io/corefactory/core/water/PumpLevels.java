package io.corefactory.core.water;

import java.util.OptionalInt;

/**
 * The levels of a pump, 1 to 5. A kit named {@code water_pump_upgrade_N} takes the pump to level N, one step at a time. Levels 1 and 2
 * burn fuel and levels 3 to 5 draw power. Pure: the caller reads the item and the owner.
 */
public final class PumpLevels {

    /** The highest pump level. */
    public static final int MAX_LEVEL = 5;

    /** The prefix of an upgrade kit's component id, such as {@code water_pump_upgrade_3}. */
    public static final String KIT_PREFIX = "water_pump_upgrade_";

    /** The first level that draws power instead of burning fuel. */
    public static final int FIRST_POWERED_LEVEL = 3;

    private PumpLevels() {
    }

    /**
     * The level a kit makes, read from its component id.
     *
     * @param componentId the component id; null is no kit
     * @return the level, 2 to 5, or empty when the id is not a pump kit
     */
    public static OptionalInt kitLevel(String componentId) {
        if (componentId == null || !componentId.startsWith(KIT_PREFIX)) {
            return OptionalInt.empty();
        }
        try {
            int level = Integer.parseInt(componentId.substring(KIT_PREFIX.length()));
            return level >= 2 && level <= MAX_LEVEL ? OptionalInt.of(level) : OptionalInt.empty();
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    /**
     * The level a kit takes a pump to. Only the next level is allowed: a kit for a level more than one step up, or a level the pump
     * already has, is refused.
     *
     * @param current  the pump's level, 1 to 5
     * @param kitLevel the level the kit makes
     * @return the new level, or empty when the kit is refused
     */
    public static OptionalInt upgrade(int current, int kitLevel) {
        if (current < 1 || current >= MAX_LEVEL || kitLevel != current + 1) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(kitLevel);
    }

    /** The message a refused kit gets, naming the reason. */
    public static String refusal(int current, int kitLevel) {
        if (current >= MAX_LEVEL) {
            return "This pump is already at its top level.";
        }
        if (kitLevel <= current) {
            return "This pump is already level " + current + " or higher.";
        }
        if (kitLevel > current + 1) {
            return "Fit the level " + (current + 1) + " kit first. This kit needs the pump at level " + (kitLevel - 1) + ".";
        }
        return "This kit does not fit this pump.";
    }

    /** The model key of a pump at a level: {@code water_pump} for level 1, {@code water_pump_N} above it. */
    public static String modelKey(int level) {
        return level <= 1 ? WaterPart.PUMP.componentId() : WaterPart.PUMP.componentId() + "_" + level;
    }

    /** True when a pump at this level draws power rather than burning fuel. */
    public static boolean powered(int level) {
        return level >= FIRST_POWERED_LEVEL;
    }
}
