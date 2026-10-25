package io.corefactory.paper.energy;

import io.corefactory.core.energy.Accumulator;
import io.corefactory.core.energy.AccumulatorLevel;
import io.corefactory.core.energy.AccumulatorState;
import io.corefactory.core.energy.ChartHistory;
import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.EnergyNetwork;
import io.corefactory.core.energy.EnergyResult;
import io.corefactory.core.energy.EnergySnapshot;
import io.corefactory.core.energy.GeneratorOutput;
import io.corefactory.core.energy.GridRegistry;
import io.corefactory.core.energy.GridResult;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.energy.Pylon;
import io.corefactory.core.energy.PylonType;
import io.corefactory.core.energy.Wire;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ChartSamplesTest {

    private static final double DT_SECONDS = 0.05;

    /** One pylon with a generator and a consumer inside its square, an accumulator inside it and one outside it. */
    private static EnergySnapshot snapshot() {
        return new EnergySnapshot(
                List.of(new Pylon("p", new Pos(0, 64, 0), PylonType.SMALL)),
                List.of(new GeneratorOutput("g", new Pos(3, 70, 0), 5_000.0)),
                List.of(new Wire("w", "g", "p")),
                List.of(new Accumulator("a", new Pos(2, 66, 1), AccumulatorLevel.II),
                        new Accumulator("far", new Pos(40, 66, 40), AccumulatorLevel.I)),
                List.of(new AccumulatorState("a", 1_000.0, false)),
                List.of(new Consumer("c", new Pos(-2, 64, 2), 2_000.0)),
                GridRegistry.EMPTY);
    }

    @Test
    void theStorageOfAGridCountsOnlyTheAccumulatorsItsPylonsCover() {
        EnergySnapshot snapshot = snapshot();
        EnergyResult result = EnergyNetwork.solve(snapshot, DT_SECONDS);
        int gridId = result.grids().get(0).grid().id();
        Map<Integer, ChartSamples.Storage> storage = ChartSamples.storageByGrid(snapshot, result);
        assertThat(storage).containsOnlyKeys(gridId);
        ChartSamples.Storage grid = storage.get(gridId);
        assertThat(grid.capacityWh()).isEqualTo(AccumulatorLevel.II.capacityWh());
        assertThat(grid.storedWh()).isEqualTo(result.accumulatorStates().get(0).storedWh());
        assertThat(grid.storedWh()).isGreaterThan(1_000.0);
    }

    @Test
    void aSampleCopiesTheSupplyTheLoadTheSatisfactionAndTheStorage() {
        EnergyResult result = EnergyNetwork.solve(snapshot(), DT_SECONDS);
        GridResult grid = result.grids().get(0);
        ChartSamples.Storage storage = new ChartSamples.Storage(1_500.0, 20_000.0);
        ChartHistory.Sample sample = ChartSamples.sample(grid, storage);
        assertThat(sample.supplyW()).isEqualTo(5_000.0);
        assertThat(sample.loadW()).isEqualTo(2_000.0);
        assertThat(sample.satisfaction()).isCloseTo(1.0, within(1e-9));
        assertThat(sample.storedWh()).isEqualTo(1_500.0);
        assertThat(sample.capacityWh()).isEqualTo(20_000.0);
    }
}
