package io.corefactory.core.energy;

/** The kinds of device the energy network knows. Only generators and pylons take a wire. */
public enum DeviceKind {

    /** A pylon: the root of a line. Takes a wire from a generator. */
    PYLON(true),

    /** A generator: a source. Takes a wire to a pylon. */
    GENERATOR(true),

    /** An accumulator: connects by pylon range, never by wire. */
    ACCUMULATOR(false),

    /** A consumer: draws power from the lines whose range covers it, never by wire. */
    CONSUMER(false);

    private final boolean wirable;

    DeviceKind(boolean wirable) {
        this.wirable = wirable;
    }

    /** True when a device of this kind may be an end of a wire. */
    public boolean wirable() {
        return wirable;
    }
}
