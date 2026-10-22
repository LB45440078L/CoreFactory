package io.corefactory.core.vertical;

import java.util.Objects;

/**
 * One vertical transport belt: its carry direction and its level, from 1 to {@link VerticalLevels#LEVELS}.
 *
 * @param direction the carry direction; a tower shares one direction
 * @param level the belt level, 1 (I), 2 (II) or 3 (III)
 */
public record Belt(VerticalDir direction, int level) {

    /**
     * Checks the parts.
     *
     * @throws NullPointerException when the direction is null
     * @throws IllegalArgumentException when the level is outside 1 to {@link VerticalLevels#LEVELS}
     */
    public Belt {
        Objects.requireNonNull(direction, "direction");
        if (level < 1 || level > VerticalLevels.LEVELS) {
            throw new IllegalArgumentException("level must be 1 to " + VerticalLevels.LEVELS + ": " + level);
        }
    }

    /** The same belt with another direction. */
    public Belt withDirection(VerticalDir newDirection) {
        return new Belt(newDirection, level);
    }
}
