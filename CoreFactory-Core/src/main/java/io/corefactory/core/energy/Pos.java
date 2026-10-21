package io.corefactory.core.energy;

/**
 * A block position with no server types: the energy model reads only the three block coordinates.
 *
 * @param x block x
 * @param y block y
 * @param z block z
 */
public record Pos(int x, int y, int z) {

    /** The squared distance between two positions on x and z, in blocks. Exact, so range checks stay in integers. */
    public long horizontalDistanceSquared(Pos other) {
        long dx = (long) x - other.x;
        long dz = (long) z - other.z;
        return dx * dx + dz * dz;
    }

    /** The Euclidean distance between two positions on x and z, ignoring y, in blocks. */
    public double horizontalDistance(Pos other) {
        return Math.sqrt(horizontalDistanceSquared(other));
    }
}
