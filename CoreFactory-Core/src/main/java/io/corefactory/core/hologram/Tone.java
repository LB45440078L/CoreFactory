package io.corefactory.core.hologram;

/**
 * The state a hologram's background shows: a device that works, one that is stuck, or one with nothing to say. The server draws the
 * background in the matching colour.
 */
public enum Tone {

    /** No background: an idle device, or a device whose state says nothing about working. */
    NEUTRAL,

    /** The device works as it should: green. */
    WORKING,

    /** The device is stuck or refused: no power, no fuel, no room, no target. Red. */
    STUCK
}
