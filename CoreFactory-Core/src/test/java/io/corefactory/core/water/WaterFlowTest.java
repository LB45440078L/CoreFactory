package io.corefactory.core.water;

import io.corefactory.core.energy.Pos;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class WaterFlowTest {

    private static final double[] FLOW = {2, 4, 8, 12, 16};

    private static final Pos PUMP = at(0, 0);
    private static final Pos PIPE_A = at(1, 0);
    private static final Pos PIPE_B = at(2, 0);
    private static final Pos EAST_CONSUMER = at(3, 0);
    private static final Pos NORTH_CONSUMER = at(1, -1);

    private static Pos at(int x, int z) {
        return new Pos(x, 64, z);
    }

    /** Pump, two pipes in a row, a consumer at the end of the row and a consumer on the first pipe's north face. */
    private static WaterGrid network() {
        return WaterGrid.of(List.of(
                WaterNode.pump(PUMP),
                WaterNode.pipe(PIPE_A),
                WaterNode.pipe(PIPE_B),
                WaterNode.consumer(EAST_CONSUMER, EnumSet.allOf(Face.class)),
                WaterNode.consumer(NORTH_CONSUMER, EnumSet.allOf(Face.class))));
    }

    @Test
    void oneConsumerRequestingThreeFromAPumpOfEightGetsThree() {
        WaterGrid grid = network();
        WaterFlow.Result result = WaterFlow.solve(grid, FLOW,
                Map.of(PUMP, new WaterFlow.PumpSupply(3, true)),
                Map.of(EAST_CONSUMER, 3.0));

        assertThat(result.consumerDelivered().get(EAST_CONSUMER)).isEqualTo(3.0);
        assertThat(result.consumerDelivered().get(NORTH_CONSUMER)).isEqualTo(0.0);
        assertThat(result.pumpOutput().get(PUMP)).isEqualTo(3.0);
        assertThat(result.deliveredIn(grid.networkOf(PUMP))).isEqualTo(3.0);
    }

    @Test
    void twoConsumersRequestingSixEachShareEightEqually() {
        WaterFlow.Result result = WaterFlow.solve(network(), FLOW,
                Map.of(PUMP, new WaterFlow.PumpSupply(3, true)),
                Map.of(EAST_CONSUMER, 6.0, NORTH_CONSUMER, 6.0));

        assertThat(result.consumerDelivered().get(EAST_CONSUMER)).isEqualTo(4.0);
        assertThat(result.consumerDelivered().get(NORTH_CONSUMER)).isEqualTo(4.0);
        assertThat(result.pumpOutput().get(PUMP)).isEqualTo(8.0);
    }

    @Test
    void aSmallRequestIsServedInFullBeforeTheLargerShare() {
        WaterFlow.Result result = WaterFlow.solve(network(), FLOW,
                Map.of(PUMP, new WaterFlow.PumpSupply(3, true)),
                Map.of(EAST_CONSUMER, 10.0, NORTH_CONSUMER, 2.0));

        assertThat(result.consumerDelivered().get(NORTH_CONSUMER)).isEqualTo(2.0);
        assertThat(result.consumerDelivered().get(EAST_CONSUMER)).isEqualTo(6.0);
        assertThat(result.pumpOutput().get(PUMP)).isEqualTo(8.0);
    }

    @Test
    void pumpOutputIsItsFlowShareOfTheDeliveredWaterAndNeverMoreThanItsFlow() {
        WaterGrid grid = WaterGrid.of(List.of(
                WaterNode.pump(PUMP),
                WaterNode.pump(at(1, 1)),
                WaterNode.pipe(PIPE_A),
                WaterNode.pipe(PIPE_B),
                WaterNode.consumer(EAST_CONSUMER, EnumSet.allOf(Face.class))));
        WaterFlow.Result result = WaterFlow.solve(grid, FLOW,
                Map.of(PUMP, new WaterFlow.PumpSupply(1, true), at(1, 1), new WaterFlow.PumpSupply(3, true)),
                Map.of(EAST_CONSUMER, 12.0));

        assertThat(result.consumerDelivered().get(EAST_CONSUMER)).isCloseTo(10.0, within(1e-9));
        assertThat(result.pumpOutput().get(PUMP)).isCloseTo(2.0, within(1e-9));
        assertThat(result.pumpOutput().get(at(1, 1))).isCloseTo(8.0, within(1e-9));
        assertThat(result.networkSupply().get(0)).isCloseTo(10.0, within(1e-9));
    }

    @Test
    void aPumpBelowItsSourceMinimumSuppliesNothing() {
        WaterSource.BlockProbe strip = (x, y, z) -> y == 64 && z == 0 && x >= 0 && x < 5;
        boolean sourceOk = WaterSource.hasSource(0, 64, 0, strip, 100);
        assertThat(sourceOk).isFalse();

        WaterFlow.Result result = WaterFlow.solve(network(), FLOW,
                Map.of(PUMP, new WaterFlow.PumpSupply(3, sourceOk)),
                Map.of(EAST_CONSUMER, 3.0));

        assertThat(result.consumerDelivered().get(EAST_CONSUMER)).isEqualTo(0.0);
        assertThat(result.pumpOutput().get(PUMP)).isEqualTo(0.0);
    }

    @Test
    void aPumpWithoutFuelOrPowerSuppliesNothing() {
        WaterFlow.Result result = WaterFlow.solve(network(), FLOW,
                Map.of(PUMP, new WaterFlow.PumpSupply(5, false)),
                Map.of(EAST_CONSUMER, 3.0));

        assertThat(result.consumerDelivered().get(EAST_CONSUMER)).isEqualTo(0.0);
        assertThat(result.pumpOutput().get(PUMP)).isEqualTo(0.0);
    }

    @Test
    void networksDoNotShareWater() {
        WaterGrid grid = WaterGrid.of(List.of(
                WaterNode.pump(PUMP),
                WaterNode.pipe(PIPE_A),
                WaterNode.pipe(PIPE_B),
                WaterNode.consumer(EAST_CONSUMER, EnumSet.allOf(Face.class)),
                WaterNode.pipe(at(9, 9)),
                WaterNode.consumer(at(9, 10), EnumSet.allOf(Face.class))));
        WaterFlow.Result result = WaterFlow.solve(grid, FLOW,
                Map.of(PUMP, new WaterFlow.PumpSupply(3, true)),
                Map.of(EAST_CONSUMER, 3.0, at(9, 10), 5.0));

        assertThat(result.consumerDelivered().get(EAST_CONSUMER)).isEqualTo(3.0);
        assertThat(result.consumerDelivered().get(at(9, 10))).isEqualTo(0.0);
        assertThat(result.deliveredIn(grid.networkOf(at(9, 9)))).isEqualTo(0.0);
    }

    @Test
    void nuclearGeneratorRunsOnAPipeWhoseNetworkDeliversWater() {
        Pos generator = EAST_CONSUMER;
        WaterGrid grid = network();

        WaterFlow.Result flowing = WaterFlow.solve(grid, FLOW,
                Map.of(PUMP, new WaterFlow.PumpSupply(1, true)),
                Map.of(NORTH_CONSUMER, 1.0));
        assertThat(WaterRules.nuclearMayRun(false, false, grid.pipeAttached(generator),
                flowing.deliveredIn(grid.networkOf(generator)))).isTrue();

        WaterFlow.Result idle = WaterFlow.solve(grid, FLOW,
                Map.of(PUMP, new WaterFlow.PumpSupply(1, false)),
                Map.of(NORTH_CONSUMER, 1.0));
        assertThat(WaterRules.nuclearMayRun(false, false, grid.pipeAttached(generator),
                idle.deliveredIn(grid.networkOf(generator)))).isFalse();
    }

    @Test
    void levelWithoutFlowIsRefused() {
        assertThatThrownBy(() -> WaterFlow.solve(network(), FLOW,
                Map.of(PUMP, new WaterFlow.PumpSupply(6, true)), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shareWaterGivesFullRequestsWhenTheSupplyCoversThem() {
        assertThat(WaterFlow.shareWater(8.0, new double[] {3.0})).containsExactly(3.0);
    }

    @Test
    void shareWaterFillsTheSmallestRequestFirst() {
        assertThat(WaterFlow.shareWater(8.0, new double[] {2.0, 10.0})).containsExactly(2.0, 6.0);
    }
}
