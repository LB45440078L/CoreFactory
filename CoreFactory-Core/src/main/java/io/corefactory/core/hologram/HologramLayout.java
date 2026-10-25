package io.corefactory.core.hologram;

import java.util.List;

/**
 * The layout rules of a hologram: its text size, the spacing of its lines, where it stands above a device, and how far a player sees one.
 * A hologram is anchored by its bottom line and grows upward, so a device of any height never reaches into its text. Pure.
 */
public final class HologramLayout {

    /** The size of the text, as a fraction of the game's default. A hologram is drawn at half size. */
    public static final float SCALE = 0.5f;

    /** The distance in blocks between one text line and the next, at {@link #SCALE}. */
    public static final double LINE_SPACING = 0.25 * SCALE;

    /** The distance in blocks from the top of a device to the centre of a hologram's bottom line. */
    public static final double GAP = 0.3;

    /** The view distance, in blocks, that a text display's view range of 1.0 covers in the game. */
    public static final double BLOCKS_PER_VIEW_RANGE = 64.0;

    private static final float MIN_VIEW_RANGE = 0.05f;

    private HologramLayout() {
    }

    /**
     * How far above the bottom line the centre of a text display of this many lines stands. A text display is positioned by its centre,
     * so the centre sits half the stack's height above the bottom line.
     *
     * @param lines the number of lines, at least 1
     * @return the offset in blocks, zero or positive
     */
    public static double centreOffset(int lines) {
        return (Math.max(1, lines) - 1) * LINE_SPACING / 2.0;
    }

    /**
     * The view range of a text display that a player sees from the given distance: the distance in blocks over the game's 64, between a
     * small minimum and 1.0.
     *
     * @param viewDistanceBlocks the distance a player sees a hologram from, in blocks
     * @return the view range, from {@value #MIN_VIEW_RANGE} to 1.0
     */
    public static float viewRange(double viewDistanceBlocks) {
        double range = viewDistanceBlocks / BLOCKS_PER_VIEW_RANGE;
        return (float) Math.max(MIN_VIEW_RANGE, Math.min(1.0, range));
    }

    /**
     * The text of a hologram as one string, its lines separated by a newline.
     *
     * @param lines the text of each line, top first
     */
    public static String joined(List<String> lines) {
        return String.join("\n", lines);
    }
}
