package io.corefactory.spigot.energy;

import io.corefactory.core.energy.GridPalette;

/** The names of the grid colours, in the order of {@link GridPalette}. Pure. */
public final class GridNames {

    private static final String[] NAMES = {
        "red", "green", "yellow", "blue", "orange", "purple", "cyan", "pink",
        "lime", "rose", "teal", "lavender", "brown", "beige", "maroon", "mint"
    };

    private GridNames() {
    }

    /**
     * The name of a grid's colour: the name at the grid's colour index, which is its id modulo the palette size.
     *
     * @param gridId the grid's id, not negative
     */
    public static String colourName(int gridId) {
        return NAMES[Math.floorMod(gridId, GridPalette.SIZE)];
    }
}
