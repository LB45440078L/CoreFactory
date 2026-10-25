package io.corefactory.paper.artillery;

import org.bukkit.map.MapPalette;

/**
 * The loading tile of the Set Target map: a dark tile with a lighter band that sweeps across it from one corner to the other, drawn for a
 * chunk whose terrain is being read or generated. The band moves one step every {@link #STEP_TICKS} ticks. Pure, so the sweep is tested
 * without a server.
 */
final class LoadingAnimation {

    /** The ticks between two steps of the sweep: the map is drawn again at this cadence while a tile is loading. */
    static final int STEP_TICKS = 4;

    /** The half width of the band, in columns along the diagonal. */
    static final int BAND_HALF = 2;

    /** The steps of one sweep: the band starts just past the corner and leaves past the other one. */
    static final int STEPS = 30 + 2 * BAND_HALF + 1;

    private LoadingAnimation() {
    }

    /**
     * The sweep's step at a tick.
     *
     * @param tick the server tick, at least 0
     * @return the step, from 0 to {@link #STEPS} - 1
     */
    static int phase(long tick) {
        return (int) ((tick / STEP_TICKS) % STEPS);
    }

    /**
     * The colour of a column of a loading tile at a step: the band is the columns whose position along the diagonal from the chunk's lowest
     * corner is within {@link #BAND_HALF} of the step.
     *
     * @param x     the block x of the column
     * @param z     the block z of the column
     * @param phase the step, from {@link #phase}
     * @return the palette byte
     */
    static byte colour(int x, int z, int phase) {
        int diagonal = (x & 15) + (z & 15);
        return Math.abs(diagonal - (phase - BAND_HALF)) <= BAND_HALF ? MapPalette.LIGHT_GRAY : MapPalette.DARK_GRAY;
    }
}
