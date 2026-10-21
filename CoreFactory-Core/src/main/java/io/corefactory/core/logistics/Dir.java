package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;

/**
 * A horizontal direction for belts, pickers and splitters. North is toward negative z and east toward positive x.
 *
 * <p>The yaw convention is Minecraft's: 0 is south, 90 is west, 180 is north and 270 is east. A player's look is split
 * into four 90 degree sectors, each centred on its cardinal: the sector of a cardinal runs from 45 degrees before it up
 * to, but not including, 45 degrees after it. So a yaw of exactly 45, 135, 225 or 315 belongs to the cardinal with the
 * larger yaw (west, north, east and south).
 */
public enum Dir {

    /** Toward negative z. */
    NORTH(0, -1),

    /** Toward positive x. */
    EAST(1, 0),

    /** Toward positive z. */
    SOUTH(0, 1),

    /** Toward negative x. */
    WEST(-1, 0);

    private final int dx;
    private final int dz;

    Dir(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    /** The reverse direction. */
    public Dir opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
        };
    }

    /** The direction 90 degrees to the left of this one, seen from above. */
    public Dir turnLeft() {
        return switch (this) {
            case NORTH -> WEST;
            case WEST -> SOUTH;
            case SOUTH -> EAST;
            case EAST -> NORTH;
        };
    }

    /** The direction 90 degrees to the right of this one, seen from above. */
    public Dir turnRight() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }

    /** The block one step in this direction from {@code from}, on the same y level. */
    public Pos step(Pos from) {
        return new Pos(from.x() + dx, from.y(), from.z() + dz);
    }

    /**
     * The flow of a belt a player places while looking at {@code yawDegrees}: the opposite of the horizontal look, so
     * looking east places a belt that flows west.
     *
     * @param yawDegrees the player's yaw in Minecraft's convention; any finite value, normalised to one turn
     * @return the flow direction of the new belt
     */
    public static Dir fromPlacement(double yawDegrees) {
        if (!Double.isFinite(yawDegrees)) {
            throw new IllegalArgumentException("yaw must be finite: " + yawDegrees);
        }
        double yaw = yawDegrees % 360.0;
        if (yaw < 0) {
            yaw += 360.0;
        }
        int sector = (int) Math.floor((yaw + 45.0) / 90.0) % 4;
        Dir look = switch (sector) {
            case 0 -> SOUTH;
            case 1 -> WEST;
            case 2 -> NORTH;
            default -> EAST;
        };
        return look.opposite();
    }
}
