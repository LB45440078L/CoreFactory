package io.corefactory.core.railgun;

/**
 * The geometry of a rail gun's helix: the frame across the direction of travel, and the offset of a point on a helix around the path. Pure.
 * The coordinates are world space, in blocks: x east, y up, z south.
 */
public final class RailgunHelix {

    /** A full turn, in radians. */
    static final double TWO_PI = 2.0 * Math.PI;

    /** Below this length a horizontal vector counts as zero, so the path runs straight up or down. */
    private static final double DEGENERATE = 1e-9;

    private RailgunHelix() {
    }

    /**
     * The offset of a point on a helix from the path, at an angle around the path. The frame is built from the unit tangent {@code t} of the
     * path: {@code u} is horizontal and across the direction of travel, {@code v} is {@code t} crossed with {@code u}, which is roughly up.
     * The offset is {@code radius * (cos(angle) * u + sin(angle) * v)}, so it is {@code radius} long and lies across the path. Where the path
     * runs straight up or down, there is no horizontal direction across it, and {@code u} is the x axis instead.
     *
     * @param tx     the x of the unit tangent of the path, the direction of travel
     * @param ty     the y of the unit tangent
     * @param tz     the z of the unit tangent
     * @param angle  the angle around the path, in radians
     * @param radius the helix radius, in blocks
     * @param out    receives the offset as x, y and z; at least 3 long
     */
    public static void offset(double tx, double ty, double tz, double angle, double radius, double[] out) {
        double ux = -tz;
        double uz = tx;
        double horizontal = Math.hypot(ux, uz);
        if (horizontal < DEGENERATE) {
            ux = 1.0;
            uz = 0.0;
        } else {
            ux /= horizontal;
            uz /= horizontal;
        }
        // v = t x u, with u = (ux, 0, uz).
        double vx = ty * uz;
        double vy = tz * ux - tx * uz;
        double vz = -ty * ux;
        double c = Math.cos(angle) * radius;
        double s = Math.sin(angle) * radius;
        out[0] = c * ux + s * vx;
        out[1] = s * vy;
        out[2] = c * uz + s * vz;
    }

    /**
     * The angle of a helix at a distance along the path: the turns it makes, times a full turn.
     *
     * @param arcBlocks    the distance along the path from the barrel, in blocks
     * @param turnsPerBlock the turns of the helix for each block
     * @return the angle, in radians
     */
    public static double phase(double arcBlocks, double turnsPerBlock) {
        return TWO_PI * turnsPerBlock * arcBlocks;
    }
}
