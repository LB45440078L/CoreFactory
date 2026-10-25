package io.corefactory.core.artillery;

/**
 * A point in the world, in blocks. The artillery rules use plain numbers rather than the platform's location type, so they can
 * be tested without a server.
 *
 * @param x the x coordinate, in blocks
 * @param y the height, in blocks
 * @param z the z coordinate, in blocks
 */
public record Position(double x, double y, double z) {

    /** The straight-line distance to another point, in blocks. */
    public double distance(Position other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** The distance to another point on the ground plane, ignoring the height, in blocks. */
    public double horizontalDistance(Position other) {
        return Math.hypot(x - other.x, z - other.z);
    }
}
