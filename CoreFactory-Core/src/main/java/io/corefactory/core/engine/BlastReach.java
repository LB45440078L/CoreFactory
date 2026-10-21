package io.corefactory.core.engine;

/**
 * The reach of a blast on the things it does not carve: the 3D distance from the blast centre to a position, and whether a
 * protected block spares what stands on it. Pure geometry, so the platform decides which positions to test.
 */
public final class BlastReach {

    private BlastReach() {
    }

    /**
     * True when the point lies within the sphere of {@code radius} around the origin. A point exactly on the sphere is inside.
     * A negative radius reaches nothing.
     */
    public static boolean within(double ox, double oy, double oz, double x, double y, double z, double radius) {
        if (radius < 0) {
            return false;
        }
        double dx = x - ox;
        double dy = y - oy;
        double dz = z - oz;
        return dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    /** A device is destroyed when the blast reaches it and no protection spares its block. */
    public static boolean destroyed(boolean reached, boolean protectedBlock) {
        return reached && !protectedBlock;
    }
}
