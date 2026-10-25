package io.corefactory.core.water;

import io.corefactory.core.hologram.Tone;

/**
 * The state a pump's status line names: that it pumps, or the one thing that stops it. A pump never idles with nothing to do, so every
 * stopped state is red, and a pumping one is green.
 */
public enum PumpStatus {

    /** The pump pushes water into its network. Green. */
    PUMPING("&aPumping", Tone.WORKING),

    /** The source counts below the minimum. Red. */
    NO_SOURCE("&cNo water source", Tone.STUCK),

    /** A powered pump has no grid covering it. Red. */
    NO_GRID("&cNo power grid", Tone.STUCK),

    /** A powered pump has a source and a grid, but the grid gives it no power. Red. */
    NO_POWER("&cNo power", Tone.STUCK),

    /** A level 1 pump holds no fuel item. Red. */
    NO_FUEL("&cNo fuel", Tone.STUCK),

    /** A level 2 pump does not get the diesel it burns from the oil network. Red. */
    NO_DIESEL("&cNo diesel", Tone.STUCK);

    private final String label;
    private final Tone tone;

    PumpStatus(String label, Tone tone) {
        this.label = label;
        this.tone = tone;
    }

    /** The word the status line shows, with its colour code. */
    public String label() {
        return label;
    }

    /** The background of the pump's hologram in this state. */
    public Tone tone() {
        return tone;
    }
}
