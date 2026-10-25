package io.corefactory.core.energy;

import io.corefactory.core.oil.Fluid;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class EnergyTableTest {

    @Test
    void aTickIsOneTwentiethOfASecond() {
        assertThat(EnergyTable.TICKS_PER_SECOND).isEqualTo(20);
        assertThat(EnergyTable.SECONDS_PER_TICK).isCloseTo(0.05, within(1e-12));
    }

    @Test
    void pylonRangesAndCurrentsMatchTheContract() {
        assertThat(PylonType.SMALL.halfSide()).isEqualTo(5);
        assertThat(PylonType.MEDIUM.halfSide()).isEqualTo(9);
        assertThat(PylonType.LARGE.halfSide()).isEqualTo(15);
        assertThat(PylonType.SMALL.maxCurrentA()).isEqualTo(100.0);
        assertThat(PylonType.MEDIUM.maxCurrentA()).isEqualTo(400.0);
        assertThat(PylonType.LARGE.maxCurrentA()).isEqualTo(1600.0);
        assertThat(PylonType.HUGE.halfSide()).isEqualTo(25);
        assertThat(PylonType.COLOSSAL.halfSide()).isEqualTo(40);
        assertThat(PylonType.HUGE.maxCurrentA()).isEqualTo(4_000.0);
        assertThat(PylonType.COLOSSAL.maxCurrentA()).isEqualTo(10_000.0);
    }

    @Test
    void pylonCapacitiesAreTheCurrentsAtTheNominalVoltage() {
        assertThat(PylonType.SMALL.capacityW()).isEqualTo(23_000.0);
        assertThat(PylonType.MEDIUM.capacityW()).isEqualTo(92_000.0);
        assertThat(PylonType.LARGE.capacityW()).isEqualTo(368_000.0);
        assertThat(PylonType.HUGE.capacityW()).isEqualTo(920_000.0);
        assertThat(PylonType.COLOSSAL.capacityW()).isEqualTo(2_300_000.0);
    }

    @Test
    void accumulatorLevelsMatchTheContract() {
        assertThat(AccumulatorLevel.I.capacityWh()).isEqualTo(5_000.0);
        assertThat(AccumulatorLevel.II.capacityWh()).isEqualTo(20_000.0);
        assertThat(AccumulatorLevel.III.capacityWh()).isEqualTo(80_000.0);
        assertThat(AccumulatorLevel.I.powerW()).isEqualTo(5_000.0);
        assertThat(AccumulatorLevel.II.powerW()).isEqualTo(20_000.0);
        assertThat(AccumulatorLevel.III.powerW()).isEqualTo(60_000.0);
        assertThat(AccumulatorLevel.IV.capacityWh()).isEqualTo(320_000.0);
        assertThat(AccumulatorLevel.V.capacityWh()).isEqualTo(1_280_000.0);
        assertThat(AccumulatorLevel.IV.powerW()).isEqualTo(200_000.0);
        assertThat(AccumulatorLevel.V.powerW()).isEqualTo(800_000.0);
        assertThat(EnergyTable.ACCUMULATOR_ROUND_TRIP).isEqualTo(0.9);
    }

    @Test
    void ratedOutputsMatchTheContract() {
        assertThat(GeneratorKind.WIND.ratedW()).isEqualTo(3_000.0);
        assertThat(GeneratorKind.SOLAR.ratedW()).isCloseTo(320.0, within(1e-9));
        assertThat(GeneratorKind.CHARCOAL.ratedW()).isEqualTo(25_000.0);
        assertThat(GeneratorKind.PETROLEUM.ratedW()).isEqualTo(80_000.0);
        assertThat(GeneratorKind.DIESEL.ratedW()).isEqualTo(150_000.0);
        assertThat(GeneratorKind.NUCLEAR.ratedW()).isEqualTo(2_000_000.0);
    }

    @Test
    void burnTimesPerItemMatchTheContract() {
        assertThat(GeneratorKind.CHARCOAL.burnSeconds()).isCloseTo(30.0, within(1e-9));
        assertThat(GeneratorKind.PETROLEUM.burnSeconds()).isCloseTo(150.5, within(0.01));
        assertThat(GeneratorKind.DIESEL.burnSeconds()).isCloseTo(96.32, within(0.01));
        assertThat(GeneratorKind.NUCLEAR.burnSeconds()).isCloseTo(178.0 * 3600.0, within(1e-6));
    }

    @Test
    void aFluidKindDrawsLitresAtItsRatedOutputOverTheEnergyOfALitre() {
        assertThat(GeneratorKind.PETROLEUM.litresPerSecond())
                .isEqualTo(GeneratorKind.PETROLEUM.ratedW() / GeneratorKind.PETROLEUM.electricJoulesPerItem());
        assertThat(GeneratorKind.DIESEL.litresPerSecond())
                .isEqualTo(GeneratorKind.DIESEL.ratedW() / GeneratorKind.DIESEL.electricJoulesPerItem());
        assertThat(GeneratorKind.PETROLEUM.litresPerSecond()).isCloseTo(1.0 / 150.5, within(1e-5));
    }

    @Test
    void onlyPetroleumAndDieselBurnAFluid() {
        assertThat(GeneratorKind.PETROLEUM.fluid()).contains(Fluid.PETROLEUM);
        assertThat(GeneratorKind.DIESEL.fluid()).contains(Fluid.DIESEL);
        for (GeneratorKind kind : GeneratorKind.values()) {
            if (kind != GeneratorKind.PETROLEUM && kind != GeneratorKind.DIESEL) {
                assertThat(kind.fluid()).as(kind.name()).isEmpty();
                assertThat(kind.litresPerSecond()).as(kind.name()).isZero();
            }
        }
    }

    @Test
    void onlyTheFourFuelKindsBurn() {
        assertThat(GeneratorKind.values()).hasSize(6);
        assertThat(GeneratorKind.WIND.burnsFuel()).isFalse();
        assertThat(GeneratorKind.SOLAR.burnsFuel()).isFalse();
        assertThat(GeneratorKind.WIND.burnSeconds()).isZero();
        assertThat(GeneratorKind.SOLAR.burnSeconds()).isZero();
        assertThat(GeneratorKind.CHARCOAL.burnsFuel()).isTrue();
        assertThat(GeneratorKind.PETROLEUM.burnsFuel()).isTrue();
        assertThat(GeneratorKind.DIESEL.burnsFuel()).isTrue();
        assertThat(GeneratorKind.NUCLEAR.burnsFuel()).isTrue();
    }

    @Test
    void theElectricEnergyOfAnItemIsItsFuelEnergyTimesItsEfficiency() {
        assertThat(GeneratorKind.CHARCOAL.electricJoulesPerItem()).isCloseTo(3.0e6 * 0.25, within(1e-6));
        assertThat(GeneratorKind.PETROLEUM.electricJoulesPerItem()).isCloseTo(Fluid.PETROLEUM.joulesPerLitre() * 0.35, within(1e-6));
        assertThat(GeneratorKind.DIESEL.electricJoulesPerItem()).isCloseTo(Fluid.DIESEL.joulesPerLitre() * 0.40, within(1e-6));
        assertThat(GeneratorKind.NUCLEAR.electricJoulesPerItem()).isCloseTo(356e6 * 3600.0, within(1e-3));
    }

    @Test
    void labToolAndDrillDemandsMatchTheContract() {
        assertThat(EnergyTable.LAB_TOOL_DEMAND_W).isEqualTo(4_000.0);
        assertThat(EnergyTable.drillDemandW(1)).isEqualTo(15_000.0);
        assertThat(EnergyTable.drillDemandW(2)).isEqualTo(25_000.0);
        assertThat(EnergyTable.drillDemandW(3)).isEqualTo(40_000.0);
        assertThat(EnergyTable.drillDemandW(4)).isEqualTo(60_000.0);
        assertThat(EnergyTable.drillDemandW(5)).isEqualTo(90_000.0);
    }

    @ParameterizedTest
    @EnumSource(value = GeneratorKind.class, names = {"WIND", "SOLAR"})
    void windAndSolarNeedNoFuel(GeneratorKind kind) {
        assertThat(kind.electricJoulesPerItem()).isZero();
    }

    @Test
    void aDrillLevelOutOfRangeIsRefused() {
        assertThatThrownBy(() -> EnergyTable.drillDemandW(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EnergyTable.drillDemandW(6)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theWireLimitIsTwentyFourBlocks() {
        assertThat(EnergyTable.WIRE_MAX_LENGTH).isEqualTo(24);
    }
}
