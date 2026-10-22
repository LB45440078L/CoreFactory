package io.corefactory.core.vertical;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.Dir;

import java.util.List;
import java.util.Objects;

/**
 * A tower: vertical belts at one x and z, contiguous in y, all with the same direction. The positions run from the bottom
 * belt to the top belt. Built by {@link VerticalTowers#towersOf}.
 *
 * <p>The input of a tower is the end opposite its output: the bottom for UP, the top for DOWN. A regular belt may also push
 * into any belt of the tower from one of its four horizontal faces. The level of a tower is the level of its output end
 * belt; it sets the carry speed and the capacity per block.
 */
public final class VerticalTower {

    private final List<Pos> positions;
    private final List<Belt> belts;

    /**
     * @param positions the belt positions, bottom to top, contiguous in y at one x and z
     * @param belts the belt at each position, in the same order
     * @throws IllegalArgumentException when the lists differ in length, are empty, are not one column of contiguous
     *         positions, or mix directions
     */
    VerticalTower(List<Pos> positions, List<Belt> belts) {
        if (positions.isEmpty() || positions.size() != belts.size()) {
            throw new IllegalArgumentException("a tower needs one belt for each of its positions");
        }
        Pos first = positions.get(0);
        VerticalDir direction = belts.get(0).direction();
        for (int i = 0; i < positions.size(); i++) {
            Pos position = positions.get(i);
            if (position.x() != first.x() || position.z() != first.z()) {
                throw new IllegalArgumentException("tower positions must share x and z: " + position);
            }
            if (i > 0 && position.y() != positions.get(i - 1).y() + 1) {
                throw new IllegalArgumentException("tower positions must be contiguous in y: " + position);
            }
            if (belts.get(i).direction() != direction) {
                throw new IllegalArgumentException("a tower has one direction: mixed at " + position);
            }
        }
        this.positions = List.copyOf(positions);
        this.belts = List.copyOf(belts);
    }

    /** The belt positions, bottom to top. The list cannot be changed. */
    public List<Pos> positions() {
        return positions;
    }

    /** The belts, bottom to top, in the same order as {@link #positions()}. The list cannot be changed. */
    public List<Belt> belts() {
        return belts;
    }

    /** The number of belts, which is also the tower's length in blocks. */
    public int size() {
        return positions.size();
    }

    /** The carry direction shared by every belt of the tower. */
    public VerticalDir direction() {
        return belts.get(0).direction();
    }

    /** The level of the output end belt, which sets the carry speed and capacity of the tower. */
    public int level() {
        return outputBelt().level();
    }

    /** The lowest belt. */
    public Pos bottom() {
        return positions.get(0);
    }

    /** The highest belt. */
    public Pos top() {
        return positions.get(positions.size() - 1);
    }

    /** Whether a position is one of the belts of this tower. */
    public boolean contains(Pos position) {
        return positions.contains(position);
    }

    /** The output end belt: the top for UP, the bottom for DOWN. */
    public Pos outputEnd() {
        return direction() == VerticalDir.UP ? top() : bottom();
    }

    /**
     * The port the output end pushes into: the block one beyond the output end in the carry direction, above the top for
     * UP and below the bottom for DOWN.
     */
    public Pos outputCell() {
        Pos end = outputEnd();
        return new Pos(end.x(), end.y() + direction().dy(), end.z());
    }

    /**
     * Whether a regular belt may push into this tower from a face of one of its belts. Any of the four horizontal faces of
     * any belt of the tower accepts; the top and bottom faces are the tower's own column, not a side.
     *
     * @param belt a belt position
     * @param side the horizontal face the item comes through
     * @return true when the belt is part of this tower
     */
    public boolean acceptsFrom(Pos belt, Dir side) {
        Objects.requireNonNull(side, "side");
        return contains(belt);
    }

    /**
     * The input cell for a side of a belt of this tower: the block next to that face, where the pushing regular belt sits.
     *
     * @throws IllegalArgumentException when the belt is not part of this tower
     */
    public Pos inputCell(Pos belt, Dir side) {
        if (!acceptsFrom(belt, side)) {
            throw new IllegalArgumentException("not a belt of this tower: " + belt);
        }
        return side.step(belt);
    }

    /**
     * The block index of a belt counted from the input end of the tower: 0 at the input end, {@link #size()} minus 1 at the
     * output end. This is the entry index for items that come in at that belt.
     *
     * @throws IllegalArgumentException when the belt is not part of this tower
     */
    public int carryBlock(Pos belt) {
        int index = positions.indexOf(belt);
        if (index < 0) {
            throw new IllegalArgumentException("not a belt of this tower: " + belt);
        }
        return direction() == VerticalDir.UP ? index : size() - 1 - index;
    }

    private Belt outputBelt() {
        return belts.get(direction() == VerticalDir.UP ? belts.size() - 1 : 0);
    }
}
