package io.corefactory.core.energy;

/** The verdict on a wire between two devices. {@link #OK} is the only verdict that makes a wire. */
public enum WireCheck {

    /** The wire may be made. */
    OK,

    /** Both ends are the same device. */
    SAME_DEVICE,

    /** One end is a consumer or an accumulator, which never take a wire. */
    NOT_WIRABLE,

    /** Both ends are generators: a generator never wires to another generator. */
    SAME_KIND,

    /** The ends are farther apart than {@link EnergyTable#WIRE_MAX_LENGTH} blocks on x and z. */
    TOO_LONG,

    /** The two ends are already joined by a wire. */
    DUPLICATE
}
