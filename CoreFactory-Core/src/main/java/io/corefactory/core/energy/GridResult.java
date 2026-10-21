package io.corefactory.core.energy;

/**
 * What one grid did in one tick.
 *
 * @param grid            the grid: its id, colour, devices and its supply, capacity and resistance
 * @param deliveredW      the power the grid's pylons delivered to consumers this tick, in W; never more than the supply or
 *                        the capacity
 * @param fromStorageW    the part of the consumers' storage draw attributed to this grid, in W
 * @param loadW           the demand attributed to this grid, in W: each consumer's demand split equally between the grids
 *                        that cover it
 * @param satisfaction    the delivered and storage power over the load, from 0 to 1; 1 when the load is 0
 * @param voltageV        the voltage at the grid's load, in V
 * @param currentA        the current of the delivered power at that voltage, in A
 */
public record GridResult(
        Grid grid,
        double deliveredW,
        double fromStorageW,
        double loadW,
        double satisfaction,
        double voltageV,
        double currentA) {
}
