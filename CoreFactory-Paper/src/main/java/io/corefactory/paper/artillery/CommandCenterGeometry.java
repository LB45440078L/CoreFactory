package io.corefactory.paper.artillery;

/**
 * Where the Command Center's screen goes, in blocks from the corner of its block. The screen is a flat rectangle on the north face of the
 * command center's pedestal, in the resource pack's model units: 16 units to a block, with the front of the model toward north. The text
 * display of the screen stands a little in front of that plane, and the click target is an interaction a little larger than the screen.
 * Pure, so the numbers are tested without a server; {@link CommandCenterRegistry} turns them into entities.
 *
 * <p>The screen's rectangle is the resource pack's {@code ArtilleryArt.SCREEN}, which the plugin does not compile against. A test checks
 * that the two agree.
 */
public final class CommandCenterGeometry {

    /** The screen in model units, as {@code ArtilleryArt.SCREEN}: x0, y0, x1, y1, and the plane z the screen lies on. */
    static final double[] SCREEN_MODEL_UNITS = {4.5, 4.5, 11.5, 9.75, 2};

    /** The model units in one block. */
    static final double UNITS_PER_BLOCK = 16;

    /** The width of one pixel of text at text scale 1, in blocks. The same as the device screens. */
    public static final double PIXEL_BLOCKS = 0.025;

    /** The line width of the screen's text, in pixels. At the text scale it spans the screen's width. */
    public static final int LINE_WIDTH = 200;

    /** How far the text stands in front of the screen's plane, toward the viewer, in blocks. */
    static final double TEXT_IN_FRONT_BLOCKS = 0.005;

    /** The margin the click target keeps around the screen on every side, in blocks. */
    static final double CLICK_MARGIN_BLOCKS = 0.02;

    /** The screen's rectangle, in blocks from the corner of the block. */
    public record Screen(double x0, double y0, double x1, double y1, double z) {

        /** The screen's width, in blocks. */
        public double width() {
            return x1 - x0;
        }

        /** The screen's height, in blocks. */
        public double height() {
            return y1 - y0;
        }

        /** The middle of the screen across the block, in blocks. */
        public double centreX() {
            return (x0 + x1) / 2;
        }

        /** The middle of the screen above the block's floor, in blocks. */
        public double centreY() {
            return (y0 + y1) / 2;
        }
    }

    private CommandCenterGeometry() {
    }

    /** The screen's rectangle in blocks. */
    public static Screen screen() {
        double[] m = SCREEN_MODEL_UNITS;
        return new Screen(m[0] / UNITS_PER_BLOCK, m[1] / UNITS_PER_BLOCK, m[2] / UNITS_PER_BLOCK, m[3] / UNITS_PER_BLOCK,
                m[4] / UNITS_PER_BLOCK);
    }

    /**
     * The text scale that makes {@link #LINE_WIDTH} pixels span the screen's width. A text's pixel is {@link #PIXEL_BLOCKS} at scale 1.
     */
    public static float textScale() {
        return (float) (screen().width() / (LINE_WIDTH * PIXEL_BLOCKS));
    }

    /** The height above the block's floor of the text's origin, which is the middle of the screen. */
    public static double textY() {
        return screen().centreY();
    }

    /** The distance of the text's origin from the block's north face, in blocks: on the plane, moved toward the viewer. */
    public static double textZ() {
        return screen().z() - TEXT_IN_FRONT_BLOCKS;
    }

    /** The width of the click target, in blocks: the screen and its margin on each side. */
    public static double clickWidth() {
        return screen().width() + 2 * CLICK_MARGIN_BLOCKS;
    }

    /** The height of the click target, in blocks: the screen and its margin on each side. */
    public static double clickHeight() {
        return screen().height() + 2 * CLICK_MARGIN_BLOCKS;
    }

    /** The height above the block's floor of the click target's bottom, in blocks. */
    public static double clickBottomY() {
        return screen().y0() - CLICK_MARGIN_BLOCKS;
    }

    /** The distance of the click target from the block's north face, in blocks: on the screen's plane. */
    public static double clickZ() {
        return screen().z();
    }
}
