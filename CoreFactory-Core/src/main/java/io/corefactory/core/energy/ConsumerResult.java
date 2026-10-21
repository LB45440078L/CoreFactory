package io.corefactory.core.energy;

/**
 * What one consumer received in one tick.
 *
 * @param id             the consumer's id
 * @param demandW        the power the consumer asked for, in W
 * @param absorbedW      the power it received, in W; never more than the demand
 * @param satisfaction   absorbed over demand, from 0 to 1; 1 when the demand is 0
 * @param voltageV       the voltage at its covering grid, in V; 0 when no grid covers it
 * @param fromGridsW     the part of the absorbed power that came from grids, in W
 * @param fromStorageW   the part that came from accumulators, in W
 */
public record ConsumerResult(
        String id,
        double demandW,
        double absorbedW,
        double satisfaction,
        double voltageV,
        double fromGridsW,
        double fromStorageW) {
}
