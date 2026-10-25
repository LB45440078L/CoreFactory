package io.corefactory.paper.energy;

import io.corefactory.core.energy.Accumulator;
import io.corefactory.core.energy.AccumulatorState;
import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.EnergySnapshot;
import io.corefactory.core.energy.GeneratorOutput;
import io.corefactory.core.energy.GridRegistry;
import io.corefactory.core.energy.Pylon;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.energy.Wire;
import io.corefactory.paper.energy.StoredEnergy.StoredAccumulator;
import io.corefactory.paper.energy.StoredEnergy.StoredPylon;
import io.corefactory.paper.energy.StoredEnergy.StoredWire;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The mapping from stored devices to the entries of an {@link EnergySnapshot}. Pure: the service reads the Bukkit devices into
 * these records, and the snapshot is built here, so the mapping is tested without a server.
 */
public final class EnergySnapshots {

    private EnergySnapshots() {
    }

    /** The core pylon of a stored pylon. */
    public static Pylon pylon(StoredPylon stored) {
        return new Pylon(stored.id(), stored.pos(), EnergyLevels.pylonType(stored.level()));
    }

    /** The core accumulator of a stored accumulator. */
    public static Accumulator accumulator(StoredAccumulator stored) {
        return new Accumulator(stored.id(), stored.pos(), EnergyLevels.accumulatorLevel(stored.level()));
    }

    /** The state the solver starts a stored accumulator from: the Wh it holds and whether it was discharging. */
    public static AccumulatorState state(StoredAccumulator stored) {
        return new AccumulatorState(stored.id(), Math.max(0.0, stored.stored()), stored.discharging());
    }

    /** The grid registry the solver starts from: the grid id of each pylon that has one, and the first id never issued. */
    public static GridRegistry grids(List<StoredPylon> pylons, int nextGridId) {
        Map<String, Integer> byPylon = new HashMap<>();
        int next = Math.max(0, nextGridId);
        for (StoredPylon pylon : pylons) {
            if (pylon.grid() >= 0) {
                byPylon.put(pylon.id(), pylon.grid());
                next = Math.max(next, pylon.grid() + 1);
            }
        }
        return new GridRegistry(byPylon, next);
    }

    /** The core wire of a stored wire. */
    public static Wire wire(StoredWire stored) {
        return new Wire(stored.id(), stored.from(), stored.to());
    }

    /**
     * The id of a consumer at a block: the world and the three block coordinates. Machines and the service use the same id,
     * so a satisfaction is looked up by the block of the machine.
     */
    public static String consumerId(UUID world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    /**
     * The snapshot of one world's tick: its pylons, accumulators, generators, wires and consumers.
     *
     * @param pylons       the stored pylons of the world
     * @param accumulators the stored accumulators of the world
     * @param wires        the stored wires between the devices of the world
     * @param consumers    the consumers of the world, with their demand in W
     * @param generators   the generators of the world with their output this tick, in W
     * @param grids        the grid ids of the previous tick
     */
    public static EnergySnapshot build(List<StoredPylon> pylons, List<StoredAccumulator> accumulators, List<StoredWire> wires,
                                       List<Consumer> consumers, List<GeneratorOutput> generators, GridRegistry grids) {
        return new EnergySnapshot(
                pylons.stream().map(EnergySnapshots::pylon).toList(),
                generators,
                wires.stream().map(EnergySnapshots::wire).toList(),
                accumulators.stream().map(EnergySnapshots::accumulator).toList(),
                accumulators.stream().map(EnergySnapshots::state).toList(),
                consumers,
                grids);
    }

    /** A consumer at a block with its demand in W. */
    public static Consumer consumer(UUID world, Pos pos, double demandW) {
        return new Consumer(consumerId(world, pos.x(), pos.y(), pos.z()), pos, demandW);
    }
}
