package io.corefactory.core.turret;

import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.ConsumerResult;
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
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The power share of two level V lasers on one grid, solved by the real {@link EnergyNetwork}. Each laser asks for its level's demand
 * (120 kW) while it holds a target. The grid delivers the lesser of its generators' output and its pylons' capacity, and the solver
 * gives the consumers the power in id order, so the first turret takes what it can and the second takes the rest.
 */
class LaserPowerShareTest {

    private static final double LEVEL_FIVE_DEMAND_W = TurretLevels.LASER.level(5).powerDemandW();
    private static final String FIRST = "turret-a";
    private static final String SECOND = "turret-b";

    /** One pylon, one generator of the given output, and the two turrets in its range. */
    private static EnergyResult grid(PylonType pylon, double supplyW) {
        List<Pylon> pylons = List.of(new Pylon("p1", new Pos(0, 64, 0), pylon));
        List<GeneratorOutput> generators = List.of(new GeneratorOutput("g1", new Pos(3, 70, 0), supplyW));
        List<Wire> wires = List.of(new Wire("g1>p1", "g1", "p1"));
        List<Consumer> consumers = List.of(
                new Consumer(FIRST, new Pos(1, 66, 0), LEVEL_FIVE_DEMAND_W),
                new Consumer(SECOND, new Pos(2, 66, 0), LEVEL_FIVE_DEMAND_W));
        EnergySnapshot snapshot = new EnergySnapshot(pylons, generators, wires, List.of(), List.of(), consumers, GridRegistry.EMPTY);
        return EnergyNetwork.solve(snapshot, 1.0);
    }

    private static ConsumerResult consumer(EnergyResult result, String id) {
        return result.consumers().stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void theLevelFiveDemandIsOneHundredTwentyKilowatts() {
        assertThat(LEVEL_FIVE_DEMAND_W).isEqualTo(120_000.0);
    }

    @Test
    void aFullySuppliedPairOfTurretsGetsItsFullDemand() {
        // A large pylon carries 368 kW, so 240 kW of supply reaches both turrets.
        EnergyResult result = grid(PylonType.LARGE, 240_000.0);

        assertThat(consumer(result, FIRST).satisfaction()).isEqualTo(1.0);
        assertThat(consumer(result, SECOND).satisfaction()).isEqualTo(1.0);
        assertThat(FireRate.intervalTicks(4, consumer(result, FIRST).satisfaction())).isEqualTo(OptionalInt.of(4));
    }

    @Test
    void sixtyKilowattsOfSupplyGivesTheFirstTurretHalfItsDemandAndTheSecondNone() {
        EnergyResult result = grid(PylonType.MEDIUM, 60_000.0);

        assertThat(consumer(result, FIRST).fromGridsW()).isEqualTo(60_000.0);
        assertThat(consumer(result, FIRST).satisfaction()).isCloseTo(0.5, within(1e-12));
        assertThat(consumer(result, SECOND).fromGridsW()).isZero();
        assertThat(consumer(result, SECOND).satisfaction()).isZero();
    }

    @Test
    void thatSatisfactionFiresTheFirstTurretEveryEightTicksAndTheSecondNotAtAll() {
        EnergyResult result = grid(PylonType.MEDIUM, 60_000.0);

        assertThat(FireRate.intervalTicks(4, consumer(result, FIRST).satisfaction())).isEqualTo(OptionalInt.of(8));
        assertThat(FireRate.intervalTicks(4, consumer(result, SECOND).satisfaction())).isEmpty();
    }

    @Test
    void theGridReadsAQuarterOfItsDemandAtSixtyKilowatts() {
        EnergyResult result = grid(PylonType.MEDIUM, 60_000.0);
        GridResult grid = result.grids().get(0);

        assertThat(grid.loadW()).isEqualTo(240_000.0);
        assertThat(grid.deliveredW()).isEqualTo(60_000.0);
        assertThat(grid.satisfaction()).isCloseTo(0.25, within(1e-12));
    }

    @Test
    void aSmallPylonCapsTheWholePairAtTwentyThreeKilowatts() {
        // The generator gives 500 kW, but one small pylon carries only 100 A at 230 V, which is 23 kW.
        EnergyResult result = grid(PylonType.SMALL, 500_000.0);

        assertThat(result.grids().get(0).deliveredW()).isEqualTo(23_000.0);
        assertThat(consumer(result, FIRST).satisfaction()).isCloseTo(23_000.0 / 120_000.0, within(1e-12));
        assertThat(FireRate.intervalTicks(4, consumer(result, FIRST).satisfaction())).isEqualTo(OptionalInt.of(21));
        assertThat(consumer(result, SECOND).satisfaction()).isZero();
    }

    @Test
    void theLaserDamageAtFullSupplyIsFourTimesTheSixtyKilowattDamage() {
        double full = 2 * LaserCannonModel.damagePerSecond(4, 1.0, 4, 18.0);
        double sixtyKilowatts = LaserCannonModel.damagePerSecond(4, 0.5, 4, 18.0)
                + LaserCannonModel.damagePerSecond(4, 0.0, 4, 18.0);

        assertThat(full).isEqualTo(180.0);
        assertThat(sixtyKilowatts).isEqualTo(45.0);
        assertThat(full / sixtyKilowatts).isEqualTo(4.0);
    }

    @Test
    void theSmallPylonGivesAboutTenTimesTheSlowdown() {
        double supplied = 18.0 * LaserCannonModel.TICKS_PER_SECOND / 21.0;

        assertThat(180.0 / supplied).isCloseTo(10.5, within(0.05));
    }
}
