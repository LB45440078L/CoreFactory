package io.corefactory.core.energy;

import java.util.Objects;

/**
 * An accumulator device. It has no wire: it connects by sitting inside at least one pylon square.
 *
 * @param id    the stable id of the accumulator
 * @param pos   the accumulator's block
 * @param level the accumulator's level
 */
public record Accumulator(String id, Pos pos, AccumulatorLevel level) {

    public Accumulator {
        Checks.id(id, "accumulator id");
        Objects.requireNonNull(pos, "accumulator pos");
        Objects.requireNonNull(level, "accumulator level");
    }
}
