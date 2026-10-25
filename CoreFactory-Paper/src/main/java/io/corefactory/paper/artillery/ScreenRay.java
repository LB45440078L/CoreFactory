package io.corefactory.paper.artillery;

import io.corefactory.paper.artillery.CommandCenterGeometry.Screen;

import java.util.Optional;

/**
 * The player's view ray, cut by the Command Center's screen. The screen is a flat rectangle on a plane of constant z, and it faces north, so a
 * ray reads the screen only when it travels south, into the plane from the north side. A ray parallel to the plane, one that points away
 * from it, or one that meets it outside the rectangle, hits nothing. Pure, so the intersection is tested without a server.
 */
public final class ScreenRay {

    /** The least z speed of a ray that is not parallel to the plane. */
    static final double PARALLEL_EPSILON = 1e-9;

    private ScreenRay() {
    }

    /**
     * The point where a ray meets a plane of constant z, from the north side.
     *
     * @param ex     the x of the eye, in blocks
     * @param ey     the y of the eye, in blocks
     * @param ez     the z of the eye, in blocks
     * @param dx     the x of the ray's direction; it need not be a unit vector
     * @param dy     the y of the ray's direction
     * @param dz     the z of the ray's direction
     * @param planeZ the plane's z, in blocks
     * @return the point {x, y}, or empty when the ray is parallel to the plane, points away from it, or meets it behind the eye
     */
    public static Optional<double[]> hitPlane(double ex, double ey, double ez, double dx, double dy, double dz, double planeZ) {
        if (!(dz > PARALLEL_EPSILON)) {
            return Optional.empty();
        }
        double t = (planeZ - ez) / dz;
        if (!(t > 0)) {
            return Optional.empty();
        }
        return Optional.of(new double[] {ex + dx * t, ey + dy * t});
    }

    /**
     * The map pixel under a ray: the point where the ray meets the screen's plane, taken inside the screen's rectangle, in the map's pixels.
     * The top row is row 0.
     *
     * @param ex     the x of the eye, in blocks
     * @param ey     the y of the eye, in blocks
     * @param ez     the z of the eye, in blocks
     * @param dx     the x of the ray's direction
     * @param dy     the y of the ray's direction
     * @param dz     the z of the ray's direction
     * @param blockX the x of the command center's block corner
     * @param blockY the y of the command center's block corner
     * @param blockZ the z of the command center's block corner
     * @param screen the screen's rectangle, in blocks from the block's corner
     * @return the pixel {column, row}, or empty when the ray meets the screen on no point
     */
    public static Optional<int[]> pixelUnder(double ex, double ey, double ez, double dx, double dy, double dz, int blockX,
                                             int blockY, int blockZ, Screen screen) {
        Optional<double[]> hit = hitPlane(ex, ey, ez, dx, dy, dz, blockZ + screen.z());
        if (hit.isEmpty()) {
            return Optional.empty();
        }
        double left = blockX + screen.x0();
        double bottom = blockY + screen.y0();
        double x = hit.get()[0];
        double y = hit.get()[1];
        if (x < left || x > left + screen.width() || y < bottom || y > bottom + screen.height()) {
            return Optional.empty();
        }
        int px = pixelIndex((x - left) / screen.width());
        int py = pixelIndex((bottom + screen.height() - y) / screen.height());
        return Optional.of(new int[] {px, py});
    }

    /** The pixel index of a fraction across the screen, from 0 to 1, clamped so the last pixel is the one at the far edge. */
    static int pixelIndex(double fraction) {
        int index = (int) Math.floor(fraction * TargetView.PIXELS);
        return Math.max(0, Math.min(TargetView.PIXELS - 1, index));
    }
}
