package io.corefactory.paper.artillery;

import io.corefactory.paper.artillery.CommandCenterGeometry.Screen;

import java.util.Optional;

/**
 * The wall of item frames that shows a Command Center's map. The wall is a square of {@code tiles} by {@code tiles} item frames, one block
 * each, standing in front of the center's screen on the north side: the frames face north, and each hangs on a barrier block one block
 * further south. The wall is centred across the center's block, and its lowest row lies one block above it, so the wall stands on top of
 * the center and never in the ground. Its frames stand two blocks north of the center's block, so the barriers lie directly in front of
 * its north face.
 *
 * <p>The map is one canvas of {@code tiles x 128} pixels by {@code tiles x 128}, and each frame holds one tile of it. The viewer's left is
 * the wall's pixel column 0, and the top row is the wall's row 0. The ray of a player reads the wall on the plane of the frames' front faces,
 * and the pixel under the ray is read from the canvas. Pure, so the frame positions, the tiles and the hits are tested without a server.
 *
 * @param x0    the x of the frames' westmost block column
 * @param y0    the y of the frames' lowest block row
 * @param z0    the z of the frames' block layer; the barriers lie in the next layer south
 * @param tiles the side of the wall, in frames: 3 or 5
 */
public record WallLayout(int x0, int y0, int z0, int tiles) {

    /** The blocks north of the center's block at which the frames stand. The barriers lie in the block between, one block from the face. */
    static final int FRAME_DISTANCE = 2;

    /** The thickness of an item frame's plate in blocks: its front stands this far in front of the block it hangs on. */
    static final double PLATE_BLOCKS = 1.0 / 16.0;

    /**
     * The wall of a center: centred across the center's block, with its lowest row one block above it and its frames two blocks north of it.
     * The wall never reaches the ground, whatever its side: a wall of five rows centred on the screen would sink two blocks into the ground.
     *
     * @param screen the screen's rectangle, for the wall's centre across the block
     * @param blockX the x of the center's block
     * @param blockY the y of the center's block
     * @param blockZ the z of the center's block
     * @param tiles  the side of the wall, in frames
     * @return the layout
     */
    public static WallLayout of(Screen screen, int blockX, int blockY, int blockZ, int tiles) {
        int x0 = blockX + (int) Math.round(screen.centreX() - tiles / 2.0);
        int y0 = blockY + 1;
        return new WallLayout(x0, y0, blockZ - FRAME_DISTANCE, tiles);
    }

    public WallLayout {
        if (tiles < 1) {
            throw new IllegalArgumentException("the wall needs at least one frame a side");
        }
    }

    /** The side of the canvas in pixels: the tiles' pixels across. */
    public int pixels() {
        return tiles * TargetView.PIXELS;
    }

    /** The number of item frames: the wall's frames, one for each tile. */
    public int frames() {
        return tiles * tiles;
    }

    /** The block layer of the barriers, which lie directly behind the frames. */
    public int backingZ() {
        return z0 + 1;
    }

    /** The plane of the frames' front faces, in blocks from the world's origin. A ray reads the wall on this plane. */
    public double planeZ() {
        return backingZ() - PLATE_BLOCKS;
    }

    /** The column of a frame in the wall, counted from the viewer's left. */
    public int column(int index) {
        return index % tiles;
    }

    /** The row of a frame in the wall, counted from the top. */
    public int row(int index) {
        return index / tiles;
    }

    /** The x of a frame's block. The viewer's left is the east, so the columns run from east to west. */
    public int frameX(int index) {
        return x0 + tiles - 1 - column(index);
    }

    /** The y of a frame's block. The top row is the highest. */
    public int frameY(int index) {
        return y0 + tiles - 1 - row(index);
    }

    /** The index of the tile of the canvas that holds a pixel: the tiles are numbered row by row, from the top left. */
    public int tileOf(int px, int py) {
        return (py / TargetView.PIXELS) * tiles + px / TargetView.PIXELS;
    }

    /** True when a block is one of the wall's frame blocks or one of its barriers. */
    public boolean covers(int x, int y, int z) {
        return x >= x0 && x < x0 + tiles && y >= y0 && y < y0 + tiles && (z == z0 || z == backingZ());
    }

    /** The least chunk x the wall reaches. */
    public int minChunkX() {
        return x0 >> 4;
    }

    /** The greatest chunk x the wall reaches. */
    public int maxChunkX() {
        return (x0 + tiles - 1) >> 4;
    }

    /** The least chunk z the wall reaches. */
    public int minChunkZ() {
        return z0 >> 4;
    }

    /** The greatest chunk z the wall reaches. */
    public int maxChunkZ() {
        return backingZ() >> 4;
    }

    /** True when a chunk holds any of the wall's blocks. */
    public boolean touchesChunk(int chunkX, int chunkZ) {
        return chunkX >= minChunkX() && chunkX <= maxChunkX() && chunkZ >= minChunkZ() && chunkZ <= maxChunkZ();
    }

    /**
     * The canvas pixel under a point of the wall's plane. The top left of the wall is pixel 0, 0, as the viewer sees it.
     *
     * @param x the x of the point, in blocks
     * @param y the y of the point, in blocks
     * @return the pixel {column, row}, or empty when the point lies outside the wall
     */
    public Optional<int[]> pixelAt(double x, double y) {
        double right = x0 + tiles;
        double top = y0 + tiles;
        if (x < x0 || x > right || y < y0 || y > top) {
            return Optional.empty();
        }
        return Optional.of(new int[] {pixelIndex((right - x) / tiles), pixelIndex((top - y) / tiles)});
    }

    /**
     * The canvas pixel under a player's view: the point where the ray meets the wall's plane, taken on the wall.
     *
     * @param ex the x of the eye, in blocks
     * @param ey the y of the eye
     * @param ez the z of the eye
     * @param dx the x of the ray's direction
     * @param dy the y of the ray's direction
     * @param dz the z of the ray's direction
     * @return the pixel {column, row}, or empty when the ray meets the wall on no point
     */
    public Optional<int[]> pixelUnder(double ex, double ey, double ez, double dx, double dy, double dz) {
        return ScreenRay.hitPlane(ex, ey, ez, dx, dy, dz, planeZ()).flatMap(hit -> pixelAt(hit[0], hit[1]));
    }

    /**
     * The button under a canvas pixel. The buttons are laid out on the 128-pixel map, and the canvas is that map scaled up by the number of
     * tiles, so the pixel is divided by the tiles to find the button.
     *
     * @param px the canvas column
     * @param py the canvas row
     * @return the button, or empty when the pixel is on no button or outside the canvas
     */
    public Optional<TargetButton> buttonAt(int px, int py) {
        if (px < 0 || py < 0 || px >= pixels() || py >= pixels()) {
            return Optional.empty();
        }
        return TargetButton.at(px / tiles, py / tiles);
    }

    /** The pixel index of a fraction across the wall, from 0 to 1, clamped so the last pixel is the one at the far edge. */
    private int pixelIndex(double fraction) {
        int index = (int) Math.floor(fraction * pixels());
        return Math.max(0, Math.min(pixels() - 1, index));
    }
}
