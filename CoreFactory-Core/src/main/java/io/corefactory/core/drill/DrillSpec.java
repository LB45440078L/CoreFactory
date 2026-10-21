package io.corefactory.core.drill;

/**
 * The size and speed of one drill level.
 *
 * @param level      the level, from 1 to {@link DrillTable#MAX_LEVEL}
 * @param area       the side of the square the drill works in, in blocks
 * @param depth      how many layers the drill works down, counting its own layer
 * @param cycleTicks the ticks one cycle takes at full efficiency
 */
public record DrillSpec(int level, int area, int depth, int cycleTicks) {
}
