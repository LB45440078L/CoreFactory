package io.corefactory.core.energy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Everything the solver reads for one tick. The lists are copied and sorted by id, so the same devices given in any order
 * make the same snapshot. Each accumulator has exactly one state in the list, in the same order as the accumulators; an
 * accumulator without a given state starts empty.
 *
 * @param pylons            the pylons; ids unique
 * @param generators        the generators with their output for this tick; ids unique
 * @param wires             the wires; ids unique
 * @param accumulators      the accumulators; ids unique
 * @param accumulatorStates the states of accumulators, by id; each id must name an accumulator; ids unique
 * @param consumers         the consumers; ids unique
 * @param grids             the grid ids of the previous tick, see {@link GridRegistry}; null means none
 */
public record EnergySnapshot(
        List<Pylon> pylons,
        List<GeneratorOutput> generators,
        List<Wire> wires,
        List<Accumulator> accumulators,
        List<AccumulatorState> accumulatorStates,
        List<Consumer> consumers,
        GridRegistry grids) {

    public EnergySnapshot {
        pylons = Checks.sortedUnique(pylons, Pylon::id, "pylon");
        generators = Checks.sortedUnique(generators, GeneratorOutput::id, "generator");
        wires = Checks.sortedUnique(wires, Wire::id, "wire");
        accumulators = Checks.sortedUnique(accumulators, Accumulator::id, "accumulator");
        consumers = Checks.sortedUnique(consumers, Consumer::id, "consumer");
        List<AccumulatorState> given = Checks.sortedUnique(accumulatorStates, AccumulatorState::id, "accumulator state");
        Set<String> accumulatorIds = new HashSet<>();
        for (Accumulator accumulator : accumulators) {
            accumulatorIds.add(accumulator.id());
        }
        Map<String, AccumulatorState> byId = new HashMap<>();
        for (AccumulatorState state : given) {
            if (!accumulatorIds.contains(state.id())) {
                throw new IllegalArgumentException("state for unknown accumulator: " + state.id());
            }
            byId.put(state.id(), state);
        }
        accumulatorStates = accumulators.stream()
                .map(accumulator -> Objects.requireNonNullElseGet(byId.get(accumulator.id()),
                        () -> AccumulatorState.empty(accumulator.id())))
                .toList();
        grids = Objects.requireNonNullElse(grids, GridRegistry.EMPTY);
    }
}
