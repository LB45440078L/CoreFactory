package io.corefactory.core.energy;

import java.util.List;

/**
 * A grid: a connected group of pylons and generators, joined by valid wires, with its persistent id and colour. Its supply
 * is the output of its generators and its capacity the capacity of its pylons.
 *
 * @param id            the persistent id, see {@link GridRegistry}
 * @param colour        the colour, as 0xRRGGBB, taken from {@link GridPalette#colour(int)} when the grid is made
 * @param pylonIds      the ids of its pylons, in id order
 * @param generatorIds  the ids of its generators, in id order
 * @param supplyW       the sum of its generators' output, in W
 * @param capacityW     the sum of its pylons' capacities, in W
 * @param resistanceOhm the resistance of its path, in ohms
 */
public record Grid(
        int id,
        int colour,
        List<String> pylonIds,
        List<String> generatorIds,
        double supplyW,
        double capacityW,
        double resistanceOhm) {

    public Grid {
        Checks.nonNegative(id, "grid id");
        pylonIds = List.copyOf(pylonIds);
        generatorIds = List.copyOf(generatorIds);
        Checks.finiteNonNegative(supplyW, "grid supply");
        Checks.finiteNonNegative(capacityW, "grid capacity");
        Checks.finiteNonNegative(resistanceOhm, "grid resistance");
    }

    /** The most power the grid can deliver this tick, in W: its supply, capped by its capacity. */
    public double deliverableW() {
        return Math.min(supplyW, capacityW);
    }
}
