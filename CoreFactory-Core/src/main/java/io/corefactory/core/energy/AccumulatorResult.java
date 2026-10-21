package io.corefactory.core.energy;

/**
 * What one accumulator did in one tick, and the state to keep for the next tick.
 *
 * @param id            the accumulator's id
 * @param chargedWh     the energy taken from the grid this tick, in Wh, before the round-trip loss
 * @param dischargedWh  the energy given out this tick, in Wh
 * @param activated     true only on the tick it starts discharging after being idle
 * @param state         the accumulator's state after this tick: stored Wh and whether it discharged
 */
public record AccumulatorResult(
        String id,
        double chargedWh,
        double dischargedWh,
        boolean activated,
        AccumulatorState state) {
}
