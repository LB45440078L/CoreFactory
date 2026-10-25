package io.corefactory.paper.artillery;

/**
 * The view of a Command Center's map: the point on the ground at its centre, and the zoom level. The map is {@link #PIXELS} by
 * {@link #PIXELS} pixels, and a pixel is a number of blocks set by the zoom level and the map radius (see {@link #blocksPerPixel}). The
 * view maps a pixel to the ground and back, moves its centre, and keeps its zoom and its centre inside their limits. Pure, so the mapping
 * is tested without a server.
 *
 * @param centreX  the x of the view's centre on the ground, in blocks
 * @param centreZ  the z of the view's centre on the ground, in blocks
 * @param zoomIndex the index of the zoom level in the configured list, from the closest level at 0
 */
public record TargetView(double centreX, double centreZ, int zoomIndex) {

    /** The map's width and height, in pixels. */
    public static final int PIXELS = 128;

    /** The half of the map in pixels: the pixel index that the centre falls on. */
    static final double HALF = PIXELS / 2.0;

    /** The fraction of the view's span that one arrow button moves the centre by. */
    public static final double STEP_FRACTION = 0.25;

    /**
     * The blocks one pixel covers at a zoom level. The zoom level is in blocks per pixel at the default map radius, and the map radius scales
     * it: the view spans {@code 2 x mapRadius x zoom} blocks.
     *
     * @param zoomLevel    the zoom level, in blocks per pixel at the default map radius, above 0
     * @param mapRadiusBlocks the blocks from the centre to the edge of the map at zoom 1, above 0
     * @return the blocks per pixel
     */
    public static double blocksPerPixel(double zoomLevel, double mapRadiusBlocks) {
        return zoomLevel * mapRadiusBlocks / HALF;
    }

    /**
     * The x of the ground at a pixel column, at the centre of the pixel.
     *
     * @param px          the pixel column, from 0 to {@link #PIXELS} - 1
     * @param blocksPerPixel the blocks per pixel
     */
    public double worldX(double px, double blocksPerPixel) {
        return centreX + (px + 0.5 - HALF) * blocksPerPixel;
    }

    /**
     * The z of the ground at a pixel row, at the centre of the pixel.
     *
     * @param py          the pixel row, from 0 to {@link #PIXELS} - 1
     * @param blocksPerPixel the blocks per pixel
     */
    public double worldZ(double py, double blocksPerPixel) {
        return centreZ + (py + 0.5 - HALF) * blocksPerPixel;
    }

    /**
     * The pixel column that holds a ground x. A point outside the map gives a column outside 0 to {@link #PIXELS} - 1.
     *
     * @param x              the x on the ground, in blocks
     * @param blocksPerPixel the blocks per pixel
     */
    public int pixelX(double x, double blocksPerPixel) {
        return (int) Math.floor((x - centreX) / blocksPerPixel + HALF);
    }

    /**
     * The pixel row that holds a ground z. A point outside the map gives a row outside 0 to {@link #PIXELS} - 1.
     *
     * @param z              the z on the ground, in blocks
     * @param blocksPerPixel the blocks per pixel
     */
    public int pixelZ(double z, double blocksPerPixel) {
        return (int) Math.floor((z - centreZ) / blocksPerPixel + HALF);
    }

    /** The span of the view across, in blocks: the width of the map at the given blocks per pixel. */
    public static double spanBlocks(double blocksPerPixel) {
        return PIXELS * blocksPerPixel;
    }

    /**
     * The view one zoom step closer: one fewer block per pixel, held at the closest level.
     *
     * @param levels the number of zoom levels
     */
    public TargetView zoomedIn(int levels) {
        return new TargetView(centreX, centreZ, clampZoom(zoomIndex - 1, levels));
    }

    /**
     * The view one zoom step wider: one more block per pixel, held at the widest level.
     *
     * @param levels the number of zoom levels
     */
    public TargetView zoomedOut(int levels) {
        return new TargetView(centreX, centreZ, clampZoom(zoomIndex + 1, levels));
    }

    /** The view with its centre moved by a number of blocks on each axis. */
    public TargetView moved(double dx, double dz) {
        return new TargetView(centreX + dx, centreZ + dz, zoomIndex);
    }

    /**
     * The view with its centre held inside a rectangle on the ground: the world border, in blocks.
     *
     * @param minX the least x of the centre
     * @param maxX the greatest x of the centre
     * @param minZ the least z of the centre
     * @param maxZ the greatest z of the centre
     */
    public TargetView clampedTo(double minX, double maxX, double minZ, double maxZ) {
        return new TargetView(clamp(centreX, minX, maxX), clamp(centreZ, minZ, maxZ), zoomIndex);
    }

    /**
     * The zoom index held inside the levels.
     *
     * @param index  the index asked for
     * @param levels the number of levels, at least 1
     */
    static int clampZoom(int index, int levels) {
        return Math.max(0, Math.min(levels - 1, index));
    }

    /**
     * A value held inside a range. A range that is empty, from a minimum above the maximum, holds the value at the minimum.
     *
     * @param value the value
     * @param min   the least value
     * @param max   the greatest value
     */
    static double clamp(double value, double min, double max) {
        if (min > max) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
