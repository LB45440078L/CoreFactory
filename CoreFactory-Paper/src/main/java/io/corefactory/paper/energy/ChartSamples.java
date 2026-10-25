package io.corefactory.paper.energy;

import io.corefactory.core.energy.Accumulator;
import io.corefactory.core.energy.ChartHistory;
import io.corefactory.core.energy.EnergyResult;
import io.corefactory.core.energy.EnergySnapshot;
import io.corefactory.core.energy.GridResult;
import io.corefactory.core.energy.Pylon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The storage of each grid and the sample a grid gives one tick. An accumulator counts toward every grid whose pylon covers it,
 * once per grid. Pure.
 */
public final class ChartSamples {

    /**
     * The energy stored in a grid's accumulators and their capacity, in Wh.
     *
     * @param storedWh   the energy held after the tick
     * @param capacityWh the most the accumulators hold
     */
    public record Storage(double storedWh, double capacityWh) {
    }

    private ChartSamples() {
    }

    /**
     * The storage of every grid of a solved tick.
     *
     * @param snapshot the snapshot the tick was solved from
     * @param result   the result of that tick
     * @return the storage by grid id, for every grid of the result
     */
    public static Map<Integer, Storage> storageByGrid(EnergySnapshot snapshot, EnergyResult result) {
        Map<Integer, Storage> byGrid = new HashMap<>();
        List<Accumulator> accumulators = snapshot.accumulators();
        for (GridResult grid : result.grids()) {
            Set<String> pylonIds = Set.copyOf(grid.grid().pylonIds());
            List<Pylon> pylons = snapshot.pylons().stream().filter(pylon -> pylonIds.contains(pylon.id())).toList();
            double stored = 0.0;
            double capacity = 0.0;
            for (int a = 0; a < accumulators.size(); a++) {
                Accumulator accumulator = accumulators.get(a);
                if (pylons.stream().anyMatch(pylon -> pylon.covers(accumulator.pos()))) {
                    stored += result.accumulators().get(a).state().storedWh();
                    capacity += accumulator.level().capacityWh();
                }
            }
            byGrid.put(grid.grid().id(), new Storage(stored, capacity));
        }
        return byGrid;
    }

    /**
     * The sample of a grid for one tick: its supply and load in W, its satisfaction, and its storage.
     *
     * @param grid    the grid's result of the tick
     * @param storage the grid's storage
     */
    public static ChartHistory.Sample sample(GridResult grid, Storage storage) {
        return new ChartHistory.Sample(grid.grid().supplyW(), grid.loadW(), grid.satisfaction(), storage.storedWh(),
                storage.capacityWh());
    }
}
