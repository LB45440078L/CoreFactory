package io.corefactory.core.water;

import io.corefactory.core.energy.Pos;

/**
 * One of the six faces of a block, named by the direction from the block to the neighbour across it. NORTH is
 * {@code -z}, SOUTH is {@code +z}, EAST is {@code +x}, WEST is {@code -x}, UP is {@code +y} and DOWN is {@code -y}.
 */
public enum Face {
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    EAST(1, 0, 0),
    WEST(-1, 0, 0),
    UP(0, 1, 0),
    DOWN(0, -1, 0);

    private final int dx;
    private final int dy;
    private final int dz;

    Face(int dx, int dy, int dz) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
    }

    /** The face on the other side of the block. */
    public Face opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
            case UP -> DOWN;
            case DOWN -> UP;
        };
    }

    /** The position of the neighbour across this face of {@code pos}. */
    public Pos step(Pos pos) {
        return new Pos(pos.x() + dx, pos.y() + dy, pos.z() + dz);
    }

    /** True for NORTH, SOUTH, EAST and WEST, the faces of the horizontal plane. */
    public boolean horizontal() {
        return this != UP && this != DOWN;
    }
}
