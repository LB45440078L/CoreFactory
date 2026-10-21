package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A splitter. Items enter from the back cell, {@code pos - inDir}, which must be a belt flowing in {@code inDir}. The
 * splitter has three outputs: straight ({@code pos + inDir}), left and right. An item whose identity is in
 * {@code rightSet} goes right, one in {@code leftSet} goes left, and every other item goes straight.
 *
 * @param id       the splitter's id, unique among splitters
 * @param pos      the splitter's block
 * @param inDir    the direction items travel through the splitter
 * @param leftSet  the items sent to the left output
 * @param rightSet the items sent to the right output
 * @param level    the splitter's level, which sets its throughput and power
 * @throws IllegalArgumentException when an item is on both lists
 */
public record Splitter(
        String id,
        Pos pos,
        Dir inDir,
        Set<Item> leftSet,
        Set<Item> rightSet,
        LogisticsLevel level) {

    public Splitter {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("splitter id must not be blank");
        }
        Objects.requireNonNull(pos, "splitter pos");
        Objects.requireNonNull(inDir, "splitter input direction");
        Objects.requireNonNull(level, "splitter level");
        leftSet = Set.copyOf(Objects.requireNonNull(leftSet, "left list"));
        rightSet = Set.copyOf(Objects.requireNonNull(rightSet, "right list"));
        for (Item item : leftSet) {
            if (rightSet.contains(item)) {
                throw new IllegalArgumentException("item on both splitter lists: " + item.id());
            }
        }
    }

    /** The cell items enter from. */
    public Pos backCell() {
        return inDir.opposite().step(pos);
    }

    /** The cell of the straight output. */
    public Pos straightCell() {
        return inDir.step(pos);
    }

    /** The cell of the left output. */
    public Pos leftCell() {
        return inDir.turnLeft().step(pos);
    }

    /** The cell of the right output. */
    public Pos rightCell() {
        return inDir.turnRight().step(pos);
    }

    /** The three output cells: straight, left and right. */
    public List<Pos> outputCells() {
        return List.of(straightCell(), leftCell(), rightCell());
    }

    /** The output cell an item of this identity goes to. */
    public Pos outputCell(Item item) {
        if (rightSet.contains(item)) {
            return rightCell();
        }
        if (leftSet.contains(item)) {
            return leftCell();
        }
        return straightCell();
    }
}
