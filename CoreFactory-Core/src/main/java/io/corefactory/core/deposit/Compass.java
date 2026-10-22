package io.corefactory.core.deposit;

/**
 * The compass point of an offset in blocks. North is toward negative z and east toward positive x, as in the game. The point is
 * the nearest of the eight, so an offset exactly between two points takes the one clockwise from north.
 */
public final class Compass {

    /** The word for an offset of zero: the point is where you stand. */
    public static final String HERE = "here";

    private static final String[] POINTS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    private Compass() {
    }

    /**
     * The compass point of an offset, such as {@code NE} for an offset east and north of the origin.
     *
     * @param dx the offset in x, from the origin to the target
     * @param dz the offset in z, from the origin to the target
     * @return one of N, NE, E, SE, S, SW, W, NW, or {@link #HERE} for a zero offset
     */
    public static String point(long dx, long dz) {
        if (dx == 0 && dz == 0) {
            return HERE;
        }
        double bearing = Math.toDegrees(Math.atan2(dx, -dz));
        long sector = Math.round(bearing / 45.0);
        return POINTS[Math.floorMod(sector, POINTS.length)];
    }
}
