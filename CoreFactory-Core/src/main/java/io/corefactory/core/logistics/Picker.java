package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;

import java.util.Objects;

/**
 * A picker (inserter). It takes one item from its input cell, {@code pos + inDir}, and moves it to its output cell,
 * {@code pos + outDir}. The two cells are opposite each other.
 *
 * @param id    the picker's id, unique among pickers
 * @param pos   the picker's block
 * @param inDir the direction from the picker to its input cell
 * @param outDir the direction from the picker to its output cell, the opposite of {@code inDir}
 * @param level the picker's level, which sets its rate and power
 */
public record Picker(String id, Pos pos, Dir inDir, Dir outDir, LogisticsLevel level) {

    public Picker {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("picker id must not be blank");
        }
        Objects.requireNonNull(pos, "picker pos");
        Objects.requireNonNull(inDir, "picker input direction");
        Objects.requireNonNull(outDir, "picker output direction");
        Objects.requireNonNull(level, "picker level");
        if (outDir != inDir.opposite()) {
            throw new IllegalArgumentException("picker output must face away from its input: " + inDir + " / " + outDir);
        }
    }

    /** The cell the picker takes from. */
    public Pos inputCell() {
        return inDir.step(pos);
    }

    /** The cell the picker gives to. */
    public Pos outputCell() {
        return outDir.step(pos);
    }
}
