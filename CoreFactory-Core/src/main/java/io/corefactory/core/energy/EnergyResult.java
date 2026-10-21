package io.corefactory.core.energy;

import java.util.List;

/**
 * The result of one tick of the energy network. Every list is in id order.
 *
 * @param grids        one entry per grid, in grid id order
 * @param consumers    one entry per consumer, in id order
 * @param accumulators one entry per accumulator, with its new state
 * @param gridIds      the grid ids after this tick; keep these for the next tick
 */
public record EnergyResult(
        List<GridResult> grids,
        List<ConsumerResult> consumers,
        List<AccumulatorResult> accumulators,
        GridRegistry gridIds) {

    public EnergyResult {
        grids = List.copyOf(grids);
        consumers = List.copyOf(consumers);
        accumulators = List.copyOf(accumulators);
    }

    /** The accumulator states after this tick, in the order of the accumulators; keep these for the next tick. */
    public List<AccumulatorState> accumulatorStates() {
        return accumulators.stream().map(AccumulatorResult::state).toList();
    }
}
