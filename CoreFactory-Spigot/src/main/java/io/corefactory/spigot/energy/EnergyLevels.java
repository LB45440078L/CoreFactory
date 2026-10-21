package io.corefactory.spigot.energy;

import io.corefactory.core.energy.AccumulatorLevel;
import io.corefactory.core.energy.PylonType;

import java.util.Optional;

/** The levels of pylons and accumulators: how a level number maps to its size, and how a component id names one. Pure. */
public final class EnergyLevels {

    /** The component id prefix of pylons: pylon_1 to pylon_3. */
    public static final String PYLON = "pylon_";

    /** The component id prefix of accumulators: accumulator_1 to accumulator_3. */
    public static final String ACCUMULATOR = "accumulator_";

    private static final String[] ROMAN = {"I", "II", "III"};

    private EnergyLevels() {
    }

    /**
     * The pylon size of a level.
     *
     * @param level 1 (small), 2 (medium) or 3 (large)
     * @throws IllegalArgumentException for any other level
     */
    public static PylonType pylonType(int level) {
        return switch (level) {
            case 1 -> PylonType.SMALL;
            case 2 -> PylonType.MEDIUM;
            case 3 -> PylonType.LARGE;
            default -> throw new IllegalArgumentException("no pylon level " + level);
        };
    }

    /**
     * The accumulator level of a level number.
     *
     * @param level 1, 2 or 3
     * @throws IllegalArgumentException for any other level
     */
    public static AccumulatorLevel accumulatorLevel(int level) {
        return switch (level) {
            case 1 -> AccumulatorLevel.I;
            case 2 -> AccumulatorLevel.II;
            case 3 -> AccumulatorLevel.III;
            default -> throw new IllegalArgumentException("no accumulator level " + level);
        };
    }

    /** The roman numeral of a level: I, II or III. */
    public static String roman(int level) {
        if (level < 1 || level > ROMAN.length) {
            throw new IllegalArgumentException("no level " + level);
        }
        return ROMAN[level - 1];
    }

    /** The component id of a level, such as pylon_2 for a pylon of level 2. */
    public static String componentId(String prefix, int level) {
        return prefix + level;
    }

    /**
     * The level a component id names under a prefix, or empty when the id is not that prefix followed by 1 to 3.
     *
     * @param componentId the component id, such as pylon_2
     * @param prefix      {@link #PYLON} or {@link #ACCUMULATOR}
     */
    public static Optional<Integer> levelOf(String componentId, String prefix) {
        if (componentId == null || !componentId.startsWith(prefix)) {
            return Optional.empty();
        }
        String rest = componentId.substring(prefix.length());
        if (rest.length() != 1 || rest.charAt(0) < '1' || rest.charAt(0) > '3') {
            return Optional.empty();
        }
        return Optional.of(rest.charAt(0) - '0');
    }
}
