package io.corefactory.core.energy;

import java.util.Objects;

/**
 * A pylon: a member of a grid and the centre of a range square. Pylons are joined to each other or to generators by wire,
 * and a grid's pylons together carry the grid's power.
 *
 * @param id   the stable id of the pylon, unique among the pylons of a snapshot
 * @param pos  the pylon's block; the square is centred on its x and z
 * @param type the pylon's size
 */
public record Pylon(String id, Pos pos, PylonType type) {

    public Pylon {
        Checks.id(id, "pylon id");
        Objects.requireNonNull(pos, "pylon pos");
        Objects.requireNonNull(type, "pylon type");
    }

    /**
     * True when the position lies inside the range square, edges included. The square runs from x - R to x + R and from
     * z - R to z + R with R the half-side. The y of the position is ignored.
     */
    public boolean covers(Pos position) {
        int r = type.halfSide();
        return Math.abs((long) position.x() - pos.x()) <= r && Math.abs((long) position.z() - pos.z()) <= r;
    }

    /** The maximum current of this pylon, in A. */
    public double maxCurrentA() {
        return type.maxCurrentA();
    }

    /** The capacity of this pylon, in W. */
    public double capacityW() {
        return type.capacityW();
    }
}
