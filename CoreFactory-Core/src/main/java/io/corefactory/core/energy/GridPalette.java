package io.corefactory.core.energy;

/** The fixed palette of grid colours. A grid's colour is taken from its id once, when the grid is made. */
public final class GridPalette {

    /** The number of colours in the palette. */
    public static final int SIZE = 16;

    private static final int[] COLOURS = {
        0xE6194B, 0x3CB44B, 0xFFE119, 0x4363D8,
        0xF58231, 0x911EB4, 0x46F0F0, 0xF032E6,
        0xBCF60C, 0xFABEBE, 0x008080, 0xE6BEFF,
        0x9A6324, 0xFFFAC8, 0x800000, 0xAAFFC3
    };

    private GridPalette() {
    }

    /**
     * The colour of a grid id: the palette entry at the id modulo the palette size, as 0xRRGGBB.
     *
     * @param gridId the grid's id, not negative
     */
    public static int colour(int gridId) {
        return COLOURS[Math.floorMod(gridId, SIZE)];
    }
}
