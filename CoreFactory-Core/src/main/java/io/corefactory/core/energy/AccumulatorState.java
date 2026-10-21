package io.corefactory.core.energy;

/**
 * The mutable part of an accumulator, kept by the caller between ticks.
 *
 * @param id          the accumulator's id
 * @param storedWh    the energy held, in Wh, not negative
 * @param discharging true when the accumulator gave energy out in the previous tick; used to detect activation
 */
public record AccumulatorState(String id, double storedWh, boolean discharging) {

    public AccumulatorState {
        Checks.id(id, "accumulator id");
        Checks.finiteNonNegative(storedWh, "stored energy");
    }

    /** An accumulator with nothing stored that was not discharging. */
    public static AccumulatorState empty(String id) {
        return new AccumulatorState(id, 0.0, false);
    }
}
