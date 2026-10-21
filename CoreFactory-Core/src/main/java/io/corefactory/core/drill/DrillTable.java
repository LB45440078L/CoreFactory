package io.corefactory.core.drill;

import java.util.List;

/**
 * The single source of the drill levels: the area, the depth and the cycle length of each. Every other part of the
 * drill code reads its numbers from here. The output slot count of every drill is here too.
 *
 * <p>Level 1 works a 10 by 10 square down 48 layers with a 600 tick cycle; each level up is wider, deeper and faster,
 * until level 5 works 40 by 40 down 280 layers with a 120 tick cycle. Level 5 reaches from a surface drill to the
 * bottom of the world.
 */
public final class DrillTable {

    /** The highest drill level. */
    public static final int MAX_LEVEL = 5;

    /** The output slots of every drill, laid out after its fuel slot in the drill's barrel. */
    public static final int OUTPUT_SLOTS = 9;

    private static final List<DrillSpec> LEVELS = List.of(
            new DrillSpec(1, 10, 48, 600),
            new DrillSpec(2, 14, 96, 480),
            new DrillSpec(3, 20, 144, 360),
            new DrillSpec(4, 28, 200, 240),
            new DrillSpec(5, 40, 280, 120));

    private DrillTable() {
    }

    /**
     * The spec of a level.
     *
     * @throws IllegalArgumentException when the level is outside 1 to {@link #MAX_LEVEL}
     */
    public static DrillSpec level(int level) {
        if (level < 1 || level > MAX_LEVEL) {
            throw new IllegalArgumentException("A drill level is 1 to " + MAX_LEVEL + ", not " + level + ".");
        }
        return LEVELS.get(level - 1);
    }

    /** Every level, from 1 to {@link #MAX_LEVEL}. */
    public static List<DrillSpec> levels() {
        return LEVELS;
    }
}
