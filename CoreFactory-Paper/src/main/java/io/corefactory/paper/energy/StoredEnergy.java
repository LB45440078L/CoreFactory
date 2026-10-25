package io.corefactory.paper.energy;

import io.corefactory.core.energy.ChartMode;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.water.Face;

import java.util.List;
import java.util.UUID;

/**
 * The saved state of the energy devices, wires and charts, as {@code energy.yml} holds it. Pure: it has no Bukkit types, so the
 * file format is tested without a server. A device whose world is not loaded keeps its record until that world loads.
 *
 * @param pylons       the pylons, with their grid ids
 * @param accumulators the accumulators, with their stored energy and discharging flag
 * @param generators   the generators, with their kind, fuel and the time left and the energy made
 * @param wires        the wires, by the ids of their two devices
 * @param nextGridId   the first grid id never issued; no later grid takes an id below it
 * @param charts       the grid charts
 */
public record StoredEnergy(List<StoredPylon> pylons, List<StoredAccumulator> accumulators, List<StoredGenerator> generators,
                           List<StoredWire> wires, int nextGridId, List<StoredChart> charts) {

    /** The grid id of a pylon that has none yet: it gets one at the next solve. */
    public static final int NO_GRID = -1;

    public StoredEnergy {
        pylons = List.copyOf(pylons);
        accumulators = List.copyOf(accumulators);
        generators = List.copyOf(generators);
        wires = List.copyOf(wires);
        charts = List.copyOf(charts);
        if (nextGridId < 0) {
            throw new IllegalArgumentException("next grid id must not be negative: " + nextGridId);
        }
    }

    /** Pylons, generators and wires with no grid ids and no charts. */
    public StoredEnergy(List<StoredPylon> pylons, List<StoredAccumulator> accumulators, List<StoredGenerator> generators,
                        List<StoredWire> wires) {
        this(pylons, accumulators, generators, wires, 0, List.of());
    }

    /** Pylons, accumulators and wires with no generators, as the files of earlier phases held them. */
    public StoredEnergy(List<StoredPylon> pylons, List<StoredAccumulator> accumulators, List<StoredWire> wires) {
        this(pylons, accumulators, List.of(), wires, 0, List.of());
    }

    /** No devices, no wires and no charts. */
    public static StoredEnergy empty() {
        return new StoredEnergy(List.of(), List.of(), List.of(), List.of(), 0, List.of());
    }

    /**
     * A pylon.
     *
     * @param id    the device id
     * @param world the id of the world the pylon stands in
     * @param pos   the block
     * @param level the pylon level, 1 to 5
     * @param grid  the grid id the pylon had at the last save, or {@link #NO_GRID}
     */
    public record StoredPylon(String id, UUID world, Pos pos, int level, int grid) {

        /** A pylon with no grid id yet. */
        public StoredPylon(String id, UUID world, Pos pos, int level) {
            this(id, world, pos, level, NO_GRID);
        }
    }

    /**
     * An accumulator.
     *
     * @param id          the device id
     * @param world       the id of the world the accumulator stands in
     * @param pos         the block
     * @param level       the accumulator level, 1 to 5
     * @param stored      the energy held, in Wh
     * @param discharging true when it gave energy out in the last tick
     */
    public record StoredAccumulator(String id, UUID world, Pos pos, int level, double stored, boolean discharging) {
    }

    /**
     * A generator.
     *
     * @param id               the device id
     * @param world            the id of the world the generator stands in
     * @param pos              the block
     * @param kind             the generator kind
     * @param fuel             the fuel items kept, 0 to 64; always 0 for wind and solar
     * @param remainingSeconds the seconds left on the fuel item that is burning, 0 when none
     * @param totalWh          the energy the generator has made since it was placed, in Wh
     * @param front            the horizontal face its screen is on, or null for a generator saved before fronts were kept
     */
    public record StoredGenerator(String id, UUID world, Pos pos, GeneratorKind kind, int fuel, double remainingSeconds,
                                  double totalWh, Face front) {

        /** A generator with no front kept, as the files before fronts held them. */
        public StoredGenerator(String id, UUID world, Pos pos, GeneratorKind kind, int fuel, double remainingSeconds,
                               double totalWh) {
            this(id, world, pos, kind, fuel, remainingSeconds, totalWh, null);
        }
    }

    /**
     * A wire between two devices.
     *
     * @param id   the wire id
     * @param from the id of one device
     * @param to   the id of the other device
     */
    public record StoredWire(String id, String from, String to) {
    }

    /**
     * A chart hologram of a grid.
     *
     * @param name  the name, unique among the charts
     * @param world the id of the world the chart stands in
     * @param pos   the block its top line stands above
     * @param grid  the grid id the chart shows
     * @param mode  the mode it shows
     */
    public record StoredChart(String name, UUID world, Pos pos, int grid, ChartMode mode) {

        /** The same chart showing another grid. */
        public StoredChart withGrid(int grid) {
            return new StoredChart(name, world, pos, grid, mode);
        }

        /** The same chart in another mode. */
        public StoredChart withMode(ChartMode mode) {
            return new StoredChart(name, world, pos, grid, mode);
        }
    }
}
