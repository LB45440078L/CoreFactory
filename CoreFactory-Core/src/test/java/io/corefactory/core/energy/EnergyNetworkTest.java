package io.corefactory.core.energy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class EnergyNetworkTest {

    private static final double DT = 1.0;
    private static final double HOUR = 3600.0;

    /** A small builder for one tick's devices, with plain block coordinates. */
    private static final class World {
        private final List<Pylon> pylons = new ArrayList<>();
        private final List<GeneratorOutput> generators = new ArrayList<>();
        private final List<Wire> wires = new ArrayList<>();
        private final List<Accumulator> accumulators = new ArrayList<>();
        private final List<AccumulatorState> states = new ArrayList<>();
        private final List<Consumer> consumers = new ArrayList<>();
        private GridRegistry grids = GridRegistry.EMPTY;

        World pylon(String id, int x, int z, PylonType type) {
            pylons.add(new Pylon(id, new Pos(x, 64, z), type));
            return this;
        }

        World generator(String id, int x, int z, double outputW) {
            generators.add(new GeneratorOutput(id, new Pos(x, 70, z), outputW));
            return this;
        }

        World wire(String from, String to) {
            wires.add(new Wire(from + ">" + to, from, to));
            return this;
        }

        World accumulator(String id, int x, int z, AccumulatorLevel level) {
            accumulators.add(new Accumulator(id, new Pos(x, 66, z), level));
            return this;
        }

        World state(String id, double storedWh, boolean discharging) {
            states.add(new AccumulatorState(id, storedWh, discharging));
            return this;
        }

        World states(List<AccumulatorState> given) {
            states.clear();
            states.addAll(given);
            return this;
        }

        World consumer(String id, int x, int z, double demandW) {
            consumers.add(new Consumer(id, new Pos(x, 66, z), demandW));
            return this;
        }

        World criticalConsumer(String id, int x, int z, double demandW) {
            consumers.add(new Consumer(id, new Pos(x, 66, z), demandW, true));
            return this;
        }

        World grids(GridRegistry registry) {
            this.grids = registry;
            return this;
        }

        EnergySnapshot snapshot() {
            return new EnergySnapshot(pylons, generators, wires, accumulators, states, consumers, grids);
        }

        EnergySnapshot reversedSnapshot() {
            List<Pylon> p = new ArrayList<>(pylons);
            List<GeneratorOutput> g = new ArrayList<>(generators);
            List<Wire> w = new ArrayList<>(wires);
            List<Accumulator> a = new ArrayList<>(accumulators);
            List<AccumulatorState> s = new ArrayList<>(states);
            List<Consumer> c = new ArrayList<>(consumers);
            Collections.reverse(p);
            Collections.reverse(g);
            Collections.reverse(w);
            Collections.reverse(a);
            Collections.reverse(s);
            Collections.reverse(c);
            return new EnergySnapshot(p, g, w, a, s, c, grids);
        }
    }

    private static EnergyResult solve(World world) {
        return EnergyNetwork.solve(world.snapshot(), DT);
    }

    private static EnergyResult solve(World world, double dt) {
        return EnergyNetwork.solve(world.snapshot(), dt);
    }

    private static GridResult gridOf(EnergyResult result, String pylonId) {
        return result.grids().stream()
                .filter(grid -> grid.grid().pylonIds().contains(pylonId))
                .findFirst()
                .orElseThrow();
    }

    private static ConsumerResult consumerOf(EnergyResult result, String id) {
        return result.consumers().stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
    }

    private static AccumulatorResult accumulatorOf(EnergyResult result, String id) {
        return result.accumulators().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void aConsumerInRangeOfAPylonWithAGeneratorIsFullySupplied() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 10_000).wire("g1", "p1")
                .consumer("c1", 2, 0, 4_000);
        EnergyResult result = solve(world);
        ConsumerResult consumer = consumerOf(result, "c1");
        assertThat(consumer.absorbedW()).isEqualTo(4_000);
        assertThat(consumer.satisfaction()).isEqualTo(1.0);
        assertThat(consumer.fromGridsW()).isEqualTo(4_000);
        assertThat(consumer.fromStorageW()).isZero();
        GridResult grid = gridOf(result, "p1");
        assertThat(grid.deliveredW()).isEqualTo(4_000);
        assertThat(grid.loadW()).isEqualTo(4_000);
        assertThat(grid.satisfaction()).isEqualTo(1.0);
    }

    @Test
    void theVoltageAtTheLoadFallsByTheCurrentTimesTheGridResistance() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 10_000).wire("g1", "p1")
                .consumer("c1", 2, 0, 4_000);
        EnergyResult result = solve(world);
        double resistance = 0.01 + 0.001 * 3;
        double voltage = 230.0 - (4_000 / 230.0) * resistance;
        assertThat(consumerOf(result, "c1").voltageV()).isCloseTo(voltage, within(1e-9));
        assertThat(gridOf(result, "p1").voltageV()).isCloseTo(voltage, within(1e-9));
        assertThat(gridOf(result, "p1").currentA()).isCloseTo(4_000 / voltage, within(1e-9));
    }

    @Test
    void aConsumerShortOfSupplyGetsWhatIsThereAndAPartialSatisfaction() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 2_000).wire("g1", "p1")
                .consumer("c1", 2, 0, 4_000);
        ConsumerResult consumer = consumerOf(solve(world), "c1");
        assertThat(consumer.absorbedW()).isEqualTo(2_000);
        assertThat(consumer.satisfaction()).isCloseTo(0.5, within(1e-12));
    }

    @Test
    void aConsumerOutsideEveryRangeAbsorbsNothingAndHasNoVoltage() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 10_000).wire("g1", "p1")
                .consumer("c1", 10, 0, 4_000);
        ConsumerResult consumer = consumerOf(solve(world), "c1");
        assertThat(consumer.absorbedW()).isZero();
        assertThat(consumer.satisfaction()).isZero();
        assertThat(consumer.voltageV()).isZero();
    }

    @Test
    void aConsumerOnTheEdgeOfTheSquareIsCoveredAndOneBlockBeyondIsNot() {
        World onEdge = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 10_000).wire("g1", "p1")
                .consumer("c1", 5, -5, 1_000);
        assertThat(consumerOf(solve(onEdge), "c1").absorbedW()).isEqualTo(1_000);
        World beyond = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 10_000).wire("g1", "p1")
                .consumer("c1", 6, 0, 1_000);
        assertThat(consumerOf(solve(beyond), "c1").absorbedW()).isZero();
    }

    @Test
    void aZeroDemandConsumerIsFullySatisfiedAndTakesNothing() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 10_000).wire("g1", "p1")
                .consumer("c1", 2, 0, 0);
        ConsumerResult consumer = consumerOf(solve(world), "c1");
        assertThat(consumer.satisfaction()).isEqualTo(1.0);
        assertThat(consumer.absorbedW()).isZero();
        assertThat(gridOf(solve(world), "p1").deliveredW()).isZero();
    }

    @Test
    void aGridDeliversAtMostItsCapacityEvenWithMoreSupply() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 50_000).wire("g1", "p1")
                .consumer("c1", 2, 0, 30_000);
        EnergyResult result = solve(world);
        GridResult grid = gridOf(result, "p1");
        assertThat(grid.grid().supplyW()).isEqualTo(50_000);
        assertThat(grid.grid().capacityW()).isEqualTo(23_000);
        assertThat(grid.deliveredW()).isEqualTo(23_000);
        assertThat(consumerOf(result, "c1").absorbedW()).isEqualTo(23_000);
        assertThat(consumerOf(result, "c1").satisfaction()).isCloseTo(23.0 / 30.0, within(1e-12));
    }

    @Test
    void overlappingGridsSplitTheDemandInProportionToTheirFreePower() {
        World world = new World()
                .pylon("pA", 0, 0, PylonType.SMALL).generator("gA", -1, 0, 3_000).wire("gA", "pA")
                .pylon("pB", 4, 0, PylonType.SMALL).generator("gB", 5, 0, 1_000).wire("gB", "pB")
                .consumer("c1", 2, 0, 2_000);
        EnergyResult result = solve(world);
        assertThat(gridOf(result, "pA").deliveredW()).isCloseTo(1_500, within(1e-9));
        assertThat(gridOf(result, "pB").deliveredW()).isCloseTo(500, within(1e-9));
        assertThat(consumerOf(result, "c1").absorbedW()).isCloseTo(2_000, within(1e-9));
        assertThat(consumerOf(result, "c1").satisfaction()).isCloseTo(1.0, within(1e-12));
    }

    @Test
    void overlappingGridsCannotGiveMoreThanTheSumOfTheirFreePower() {
        World world = new World()
                .pylon("pA", 0, 0, PylonType.SMALL).generator("gA", -1, 0, 3_000).wire("gA", "pA")
                .pylon("pB", 4, 0, PylonType.SMALL).generator("gB", 5, 0, 1_000).wire("gB", "pB")
                .consumer("c1", 2, 0, 6_000);
        EnergyResult result = solve(world);
        assertThat(gridOf(result, "pA").deliveredW()).isCloseTo(3_000, within(1e-9));
        assertThat(gridOf(result, "pB").deliveredW()).isCloseTo(1_000, within(1e-9));
        assertThat(consumerOf(result, "c1").absorbedW()).isCloseTo(4_000, within(1e-9));
        assertThat(consumerOf(result, "c1").satisfaction()).isCloseTo(4.0 / 6.0, within(1e-12));
    }

    @Test
    void aConsumerCoveredByTwoPylonsOfOneGridCountsTheGridOnce() {
        World world = new World()
                .pylon("p1", 0, 0, PylonType.SMALL).pylon("p2", 4, 0, PylonType.SMALL).wire("p1", "p2")
                .generator("g1", -1, 0, 2_000).wire("g1", "p1")
                .consumer("c1", 2, 0, 2_000);
        EnergyResult result = solve(world);
        assertThat(result.grids()).hasSize(1);
        assertThat(consumerOf(result, "c1").absorbedW()).isEqualTo(2_000);
    }

    @Test
    void aPylonWireJoinsTwoPylonsIntoOneGridWithTheSumOfTheirCapacityAndSupply() {
        World world = new World()
                .pylon("p1", 0, 0, PylonType.SMALL).pylon("p2", 12, 0, PylonType.MEDIUM).wire("p1", "p2")
                .generator("g1", -1, 0, 1_000).wire("g1", "p1")
                .generator("g2", 13, 0, 2_000).wire("g2", "p2");
        EnergyResult result = solve(world);
        assertThat(result.grids()).hasSize(1);
        GridResult grid = result.grids().get(0);
        assertThat(grid.grid().pylonIds()).containsExactly("p1", "p2");
        assertThat(grid.grid().generatorIds()).containsExactly("g1", "g2");
        assertThat(grid.grid().supplyW()).isEqualTo(3_000);
        assertThat(grid.grid().capacityW()).isEqualTo(23_000 + 92_000);
    }

    @Test
    void aGeneratorWiredToTwoPylonsJoinsThemIntoOneGrid() {
        World world = new World()
                .pylon("p1", 0, 0, PylonType.SMALL).pylon("p2", 20, 0, PylonType.SMALL)
                .generator("g1", 10, 0, 1_000).wire("g1", "p1").wire("g1", "p2");
        EnergyResult result = solve(world);
        assertThat(result.grids()).hasSize(1);
        assertThat(result.grids().get(0).grid().pylonIds()).containsExactly("p1", "p2");
    }

    @Test
    void aGeneratorWithNoWireContributesNothing() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 2, 0, 5_000)
                .consumer("c1", 2, 0, 1_000);
        EnergyResult result = solve(world);
        assertThat(consumerOf(result, "c1").absorbedW()).isZero();
        assertThat(gridOf(result, "p1").grid().supplyW()).isZero();
    }

    @Test
    void aWireBetweenTwoGeneratorsIsIgnored() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 1_000).wire("g1", "p1")
                .generator("g2", 4, 0, 9_000).wire("g2", "g1")
                .consumer("c1", 2, 0, 5_000);
        EnergyResult result = solve(world);
        assertThat(gridOf(result, "p1").grid().supplyW()).isEqualTo(1_000);
        assertThat(consumerOf(result, "c1").absorbedW()).isEqualTo(1_000);
    }

    @Test
    void aWireLongerThanTheLimitIsIgnoredBySolver() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 25, 0, 5_000).wire("g1", "p1")
                .consumer("c1", 2, 0, 1_000);
        EnergyResult result = solve(world);
        assertThat(consumerOf(result, "c1").absorbedW()).isZero();
        assertThat(result.grids().get(0).grid().generatorIds()).isEmpty();
    }

    @Test
    void theSupplyOfAGridIsTheSumOfItsGenerators() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 1_000).wire("g1", "p1")
                .generator("g2", -3, 0, 2_000).wire("g2", "p1");
        assertThat(gridOf(solve(world), "p1").grid().supplyW()).isEqualTo(3_000);
    }

    @Test
    void anEmptyNetworkGivesEmptyResults() {
        EnergyResult result = solve(new World());
        assertThat(result.grids()).isEmpty();
        assertThat(result.consumers()).isEmpty();
        assertThat(result.accumulators()).isEmpty();
        assertThat(result.accumulatorStates()).isEmpty();
    }

    @Test
    void consumersDrawInIdOrderSoAnEarlierConsumerTakesTheSupplyFirst() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 3_000).wire("g1", "p1")
                .consumer("a", 1, 0, 2_000)
                .consumer("b", 2, 0, 2_000);
        EnergyResult result = solve(world);
        assertThat(consumerOf(result, "a").absorbedW()).isCloseTo(2_000, within(1e-9));
        assertThat(consumerOf(result, "b").absorbedW()).isCloseTo(1_000, within(1e-9));
    }

    @Test
    void anAccumulatorChargesFromTheSurplusAtItsPowerLimit() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 10_000).wire("g1", "p1")
                .accumulator("acc", 1, 0, AccumulatorLevel.I);
        EnergyResult result = solve(world, HOUR);
        AccumulatorResult acc = accumulatorOf(result, "acc");
        assertThat(acc.chargedWh()).isCloseTo(5_000, within(1e-9));
        assertThat(acc.dischargedWh()).isZero();
        assertThat(acc.state().storedWh()).isCloseTo(4_500, within(1e-9));
        assertThat(acc.state().discharging()).isFalse();
        assertThat(acc.activated()).isFalse();
    }

    @Test
    void theChargeIsOnlyTheSurplusLeftAfterTheConsumersDraw() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 6_000).wire("g1", "p1")
                .consumer("c1", 2, 0, 4_000)
                .accumulator("acc", 1, 0, AccumulatorLevel.I);
        AccumulatorResult acc = accumulatorOf(solve(world, HOUR), "acc");
        assertThat(acc.chargedWh()).isCloseTo(2_000, within(1e-9));
        assertThat(acc.state().storedWh()).isCloseTo(1_800, within(1e-9));
    }

    @Test
    void theChargeIsLimitedByTheFreeSpaceAndTheRoundTripLoss() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 10_000).wire("g1", "p1")
                .accumulator("acc", 1, 0, AccumulatorLevel.I)
                .state("acc", 4_000, false);
        AccumulatorResult acc = accumulatorOf(solve(world, HOUR), "acc");
        assertThat(acc.chargedWh()).isCloseTo(1_000 / 0.9, within(1e-9));
        assertThat(acc.state().storedWh()).isCloseTo(5_000, within(1e-9));
    }

    @Test
    void aFullAccumulatorTakesNothing() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 10_000).wire("g1", "p1")
                .accumulator("acc", 1, 0, AccumulatorLevel.I)
                .state("acc", 5_000, false);
        AccumulatorResult acc = accumulatorOf(solve(world, HOUR), "acc");
        assertThat(acc.chargedWh()).isZero();
        assertThat(acc.state().storedWh()).isCloseTo(5_000, within(1e-9));
    }

    @Test
    void levelThreeChargesAtItsSixtyKilowattLimit() {
        World world = new World().pylon("p1", 0, 0, PylonType.LARGE)
                .generator("g1", 3, 0, 100_000).wire("g1", "p1")
                .accumulator("acc", 1, 0, AccumulatorLevel.III);
        AccumulatorResult acc = accumulatorOf(solve(world, HOUR), "acc");
        assertThat(acc.chargedWh()).isCloseTo(60_000, within(1e-9));
        assertThat(acc.state().storedWh()).isCloseTo(54_000, within(1e-9));
    }

    @Test
    void levelFiveChargesAtItsEightHundredKilowattLimit() {
        World world = new World().pylon("p1", 0, 0, PylonType.COLOSSAL)
                .generator("g1", 3, 0, 1_000_000).wire("g1", "p1")
                .accumulator("acc", 1, 0, AccumulatorLevel.V);
        AccumulatorResult acc = accumulatorOf(solve(world, HOUR), "acc");
        assertThat(acc.chargedWh()).isCloseTo(800_000, within(1e-9));
        assertThat(acc.state().storedWh()).isCloseTo(720_000, within(1e-9));
    }

    @Test
    void levelFourChargesAtItsTwoHundredKilowattLimit() {
        World world = new World().pylon("p1", 0, 0, PylonType.HUGE)
                .generator("g1", 3, 0, 500_000).wire("g1", "p1")
                .accumulator("acc", 1, 0, AccumulatorLevel.IV);
        AccumulatorResult acc = accumulatorOf(solve(world, HOUR), "acc");
        assertThat(acc.chargedWh()).isCloseTo(200_000, within(1e-9));
        assertThat(acc.state().storedWh()).isCloseTo(180_000, within(1e-9));
    }

    @Test
    void aColossalPylonAddsItsTwoPointThreeMegawattsToTheGridCapacity() {
        World world = new World().pylon("p1", 0, 0, PylonType.COLOSSAL)
                .generator("g1", 3, 0, 3_000_000).wire("g1", "p1");
        GridResult grid = gridOf(solve(world), "p1");
        assertThat(grid.grid().supplyW()).isEqualTo(3_000_000);
        assertThat(grid.grid().capacityW()).isEqualTo(2_300_000);
    }

    @Test
    void theChargeScalesWithTheLengthOfTheTick() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 10_000).wire("g1", "p1")
                .accumulator("acc", 1, 0, AccumulatorLevel.I);
        AccumulatorResult acc = accumulatorOf(solve(world, 0.05), "acc");
        assertThat(acc.chargedWh()).isCloseTo(5_000 * 0.05 / HOUR, within(1e-12));
    }

    @Test
    void anAccumulatorOutsideEveryRangeDoesNotCharge() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 10_000).wire("g1", "p1")
                .accumulator("acc", 50, 0, AccumulatorLevel.I);
        AccumulatorResult acc = accumulatorOf(solve(world, HOUR), "acc");
        assertThat(acc.chargedWh()).isZero();
        assertThat(acc.state().storedWh()).isZero();
    }

    @Test
    void aShortConsumerDischargesIntoItsAccumulatorAtTheLevelLimit() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .consumer("c1", 2, 0, 4_000)
                .accumulator("acc", 1, 0, AccumulatorLevel.I)
                .state("acc", 5_000, false);
        EnergyResult result = solve(world, HOUR);
        ConsumerResult consumer = consumerOf(result, "c1");
        assertThat(consumer.absorbedW()).isCloseTo(4_000, within(1e-9));
        assertThat(consumer.satisfaction()).isCloseTo(1.0, within(1e-12));
        assertThat(consumer.fromGridsW()).isZero();
        assertThat(consumer.fromStorageW()).isCloseTo(4_000, within(1e-9));
        AccumulatorResult acc = accumulatorOf(result, "acc");
        assertThat(acc.dischargedWh()).isCloseTo(4_000, within(1e-9));
        assertThat(acc.state().storedWh()).isCloseTo(1_000, within(1e-9));
        assertThat(acc.state().discharging()).isTrue();
        assertThat(acc.activated()).isTrue();
    }

    @Test
    void dischargeNeverExceedsTheLevelPowerLimitEvenForAHugeShortfall() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .consumer("c1", 2, 0, 10_000)
                .accumulator("acc", 1, 0, AccumulatorLevel.I)
                .state("acc", 5_000, false);
        EnergyResult result = solve(world, HOUR);
        assertThat(consumerOf(result, "c1").absorbedW()).isCloseTo(5_000, within(1e-9));
        assertThat(consumerOf(result, "c1").satisfaction()).isCloseTo(0.5, within(1e-12));
    }

    @Test
    void dischargeIsLimitedByWhatIsStored() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .consumer("c1", 2, 0, 4_000)
                .accumulator("acc", 1, 0, AccumulatorLevel.I)
                .state("acc", 1_000, false);
        EnergyResult result = solve(world, HOUR);
        assertThat(consumerOf(result, "c1").absorbedW()).isCloseTo(1_000, within(1e-9));
        assertThat(accumulatorOf(result, "acc").state().storedWh()).isZero();
    }

    @Test
    void dischargeStopsAtTheShortfallOfTheGrid() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 1_000).wire("g1", "p1")
                .consumer("c1", 2, 0, 2_000)
                .accumulator("acc", 1, 0, AccumulatorLevel.I)
                .state("acc", 5_000, false);
        EnergyResult result = solve(world, HOUR);
        ConsumerResult consumer = consumerOf(result, "c1");
        assertThat(consumer.fromGridsW()).isCloseTo(1_000, within(1e-9));
        assertThat(consumer.fromStorageW()).isCloseTo(1_000, within(1e-9));
        assertThat(consumer.absorbedW()).isCloseTo(2_000, within(1e-9));
        assertThat(accumulatorOf(result, "acc").dischargedWh()).isCloseTo(1_000, within(1e-9));
    }

    @Test
    void anAccumulatorDischargesOnlyToConsumersOfItsGrid() {
        World world = new World()
                .pylon("pA", 0, 0, PylonType.SMALL)
                .pylon("pB", 20, 0, PylonType.SMALL)
                .consumer("c1", 20, 0, 1_000)
                .accumulator("acc", -3, 0, AccumulatorLevel.I)
                .state("acc", 5_000, false);
        EnergyResult result = solve(world, HOUR);
        assertThat(consumerOf(result, "c1").absorbedW()).isZero();
        assertThat(accumulatorOf(result, "acc").dischargedWh()).isZero();
        assertThat(accumulatorOf(result, "acc").state().storedWh()).isCloseTo(5_000, within(1e-9));
    }

    @Test
    void activationIsTrueOnlyOnTheTickDischargeStarts() {
        List<AccumulatorState> states = List.of(AccumulatorState.empty("acc"));
        boolean[] activations = new boolean[4];
        boolean[] discharging = new boolean[4];
        double[] demand = {2_000, 2_000, 0, 2_000};
        for (int tick = 0; tick < demand.length; tick++) {
            World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                    .consumer("c1", 2, 0, demand[tick])
                    .accumulator("acc", 1, 0, AccumulatorLevel.I)
                    .states(states);
            if (tick == 0) {
                world.states(List.of(new AccumulatorState("acc", 5_000, false)));
            }
            EnergyResult result = solve(world, DT);
            AccumulatorResult acc = accumulatorOf(result, "acc");
            activations[tick] = acc.activated();
            discharging[tick] = acc.state().discharging();
            states = result.accumulatorStates();
        }
        assertThat(activations).containsExactly(true, false, false, true);
        assertThat(discharging).containsExactly(true, true, false, true);
    }

    @Test
    void theDischargingFlagIsKeptForTheNextTickAndClearedByCharging() {
        World discharge = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .consumer("c1", 2, 0, 1_000)
                .accumulator("acc", 1, 0, AccumulatorLevel.I)
                .state("acc", 5_000, false);
        AccumulatorState afterDischarge = accumulatorOf(solve(discharge, DT), "acc").state();
        assertThat(afterDischarge.discharging()).isTrue();
        World charge = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 10_000).wire("g1", "p1")
                .accumulator("acc", 1, 0, AccumulatorLevel.I)
                .states(List.of(afterDischarge));
        AccumulatorResult acc = accumulatorOf(solve(charge, DT), "acc");
        assertThat(acc.chargedWh()).isPositive();
        assertThat(acc.state().discharging()).isFalse();
        assertThat(acc.activated()).isFalse();
    }

    @Test
    void chargingAndDischargingNeverHappenInTheSameTick() {
        World world = new World()
                .pylon("pA", 0, 0, PylonType.SMALL).generator("gA", 3, 0, 10_000).wire("gA", "pA")
                .pylon("pB", 10, 0, PylonType.SMALL)
                .consumer("c1", 10, 0, 4_000)
                .accumulator("acc", 5, 0, AccumulatorLevel.I)
                .state("acc", 5_000, false);
        AccumulatorResult acc = accumulatorOf(solve(world, HOUR), "acc");
        assertThat(acc.dischargedWh()).isPositive();
        assertThat(acc.chargedWh()).isZero();
    }

    @Test
    void theSameSnapshotSolvedTwiceGivesTheSameResult() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 4_000).wire("g1", "p1")
                .consumer("c1", 2, 0, 6_000)
                .accumulator("acc", 1, 0, AccumulatorLevel.I)
                .state("acc", 2_000, false);
        assertThat(solve(world)).isEqualTo(solve(world));
    }

    @Test
    void theSameDevicesGivenInAnyOrderGiveTheSameResult() {
        World world = new World()
                .pylon("pA", 0, 0, PylonType.SMALL).generator("gA", -1, 0, 3_000).wire("gA", "pA")
                .pylon("pB", 4, 0, PylonType.SMALL).generator("gB", 5, 0, 1_000).wire("gB", "pB")
                .consumer("c1", 2, 0, 2_000).consumer("c2", 3, 0, 3_000)
                .accumulator("acc", 2, 0, AccumulatorLevel.I).state("acc", 3_000, false);
        EnergyResult forward = EnergyNetwork.solve(world.snapshot(), DT);
        EnergyResult backward = EnergyNetwork.solve(world.reversedSnapshot(), DT);
        assertThat(backward).isEqualTo(forward);
    }

    @Test
    void aSnapshotRefusesDuplicateIds() {
        assertThatThrownBy(() -> new World().pylon("p1", 0, 0, PylonType.SMALL)
                .pylon("p1", 9, 9, PylonType.SMALL).snapshot())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aCriticalPumpIsServedBeforeAHungryConsumerWhateverItsId() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 20_000_000).wire("g1", "p1")
                .consumer("a-rail", 1, 0, 8_000_000)
                .criticalConsumer("pump", 2, 0, 50_000);
        ConsumerResult pump = consumerOf(solve(world), "pump");
        assertThat(pump.absorbedW()).isCloseTo(23_000, within(1e-9));
        assertThat(pump.satisfaction()).isCloseTo(0.46, within(1e-12));
        assertThat(consumerOf(solve(world), "a-rail").absorbedW()).isZero();
    }

    @Test
    void theOrderOfCriticalConsumersIsStillTheirIdOrder() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .generator("g1", 3, 0, 3_000).wire("g1", "p1")
                .criticalConsumer("b", 2, 0, 2_000)
                .criticalConsumer("a", 1, 0, 2_000);
        EnergyResult result = solve(world);
        assertThat(consumerOf(result, "a").absorbedW()).isCloseTo(2_000, within(1e-9));
        assertThat(consumerOf(result, "b").absorbedW()).isCloseTo(1_000, within(1e-9));
    }

    @Test
    void aSnapshotRefusesAStateForAnUnknownAccumulator() {
        assertThatThrownBy(() -> new World().state("ghost", 1.0, false).snapshot())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anAccumulatorWithoutAGivenStateStartsEmpty() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .accumulator("acc", 1, 0, AccumulatorLevel.I);
        assertThat(world.snapshot().accumulatorStates()).containsExactly(AccumulatorState.empty("acc"));
    }

    @Test
    void aStoredAmountAboveCapacityIsClampedToCapacity() {
        World world = new World().pylon("p1", 0, 0, PylonType.SMALL)
                .accumulator("acc", 1, 0, AccumulatorLevel.I)
                .state("acc", 9_999, false);
        assertThat(accumulatorOf(solve(world), "acc").state().storedWh()).isEqualTo(5_000);
    }

    @Test
    void theTickLengthMustBePositiveAndFinite() {
        World world = new World();
        assertThatThrownBy(() -> EnergyNetwork.solve(world.snapshot(), 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnergyNetwork.solve(world.snapshot(), Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theResultListsAreUnmodifiable() {
        EnergyResult result = solve(new World().pylon("p1", 0, 0, PylonType.SMALL));
        assertThatThrownBy(() -> result.grids().add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.consumers().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aJoinKeepsTheSmallerGridIdAndItsColour() {
        World apart = new World()
                .pylon("a", 0, 0, PylonType.SMALL)
                .pylon("b", 20, 0, PylonType.SMALL);
        EnergyResult first = solve(apart);
        assertThat(first.gridIds().idOf("a")).isEqualTo(0);
        assertThat(first.gridIds().idOf("b")).isEqualTo(1);

        World joined = new World()
                .pylon("a", 0, 0, PylonType.SMALL)
                .pylon("b", 20, 0, PylonType.SMALL)
                .wire("a", "b")
                .grids(first.gridIds());
        EnergyResult second = solve(joined);
        assertThat(second.grids()).hasSize(1);
        assertThat(second.grids().get(0).grid().id()).isEqualTo(0);
        assertThat(second.grids().get(0).grid().colour()).isEqualTo(GridPalette.colour(0));
    }

    @Test
    void aSplitKeepsTheIdOnTheComponentWithTheSmallestPylonAndGivesAFreshIdToTheRest() {
        World joined = new World()
                .pylon("a", 0, 0, PylonType.SMALL)
                .pylon("b", 20, 0, PylonType.SMALL)
                .wire("a", "b");
        EnergyResult first = solve(joined);
        assertThat(first.gridIds().idOf("a")).isEqualTo(0);
        assertThat(first.gridIds().idOf("b")).isEqualTo(0);

        World split = new World()
                .pylon("a", 0, 0, PylonType.SMALL)
                .pylon("b", 20, 0, PylonType.SMALL)
                .grids(first.gridIds());
        EnergyResult second = solve(split);
        assertThat(second.gridIds().idOf("a")).isEqualTo(0);
        assertThat(second.gridIds().idOf("b")).isEqualTo(1);
        assertThat(gridOf(second, "b").grid().colour()).isEqualTo(GridPalette.colour(1));
    }

    @Test
    void resultsStayWithinTheirBoundsOverALongRun() {
        Random random = new Random(7);
        List<AccumulatorState> states = List.of(AccumulatorState.empty("acc1"), AccumulatorState.empty("acc2"));
        GridRegistry grids = GridRegistry.EMPTY;
        double[] accumulatorCapacity = {
            AccumulatorLevel.I.capacityWh(), AccumulatorLevel.II.capacityWh()
        };
        for (int tick = 0; tick < 300; tick++) {
            World world = new World()
                    .pylon("p1", 0, 0, PylonType.SMALL)
                    .pylon("p2", 12, 0, PylonType.MEDIUM)
                    .wire("p1", "p2")
                    .pylon("p3", 40, 40, PylonType.LARGE)
                    .generator("g1", 3, 0, random.nextDouble() * 20_000).wire("g1", "p1")
                    .generator("g2", 40, 44, random.nextDouble() * 90_000).wire("g2", "p3")
                    .accumulator("acc1", 2, 1, AccumulatorLevel.I)
                    .accumulator("acc2", 41, 40, AccumulatorLevel.II)
                    .states(states)
                    .consumer("c1", 1, 0, random.nextDouble() * 20_000)
                    .consumer("c2", 9, 2, random.nextDouble() * 90_000)
                    .consumer("c3", 43, 41, random.nextDouble() * 15_000)
                    .consumer("c4", 100, 100, 4_000)
                    .grids(grids);
            double dt = 1.0 + random.nextInt(120);
            EnergyResult result = solve(world, dt);
            assertThat(result).isEqualTo(solve(world, dt));

            for (ConsumerResult consumer : result.consumers()) {
                assertThat(consumer.absorbedW()).isBetween(0.0, consumer.demandW() + 1e-9);
                assertThat(consumer.satisfaction()).isBetween(0.0, 1.0);
            }
            for (GridResult grid : result.grids()) {
                assertThat(grid.deliveredW()).isBetween(0.0, grid.grid().deliverableW() + 1e-9);
                assertThat(grid.satisfaction()).isBetween(0.0, 1.0);
                assertThat(grid.voltageV()).isBetween(0.0, 230.0);
                assertThat(grid.currentA()).isNotNegative();
            }
            for (int a = 0; a < result.accumulators().size(); a++) {
                AccumulatorResult acc = result.accumulators().get(a);
                assertThat(acc.state().storedWh()).isBetween(0.0, accumulatorCapacity[a] + 1e-9);
                assertThat(acc.chargedWh()).isNotNegative();
                assertThat(acc.dischargedWh()).isNotNegative();
                assertThat(acc.chargedWh() > 0 && acc.dischargedWh() > 0).isFalse();
            }
            states = result.accumulatorStates();
            grids = result.gridIds();
        }
    }
}
