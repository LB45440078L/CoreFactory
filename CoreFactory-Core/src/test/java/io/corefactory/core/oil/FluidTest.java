package io.corefactory.core.oil;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FluidTest {

    @Test
    void aFluidIsFoundByItsId() {
        assertThat(Fluid.ofId("petroleum")).contains(Fluid.PETROLEUM);
        assertThat(Fluid.ofId("diesel")).contains(Fluid.DIESEL);
        assertThat(Fluid.ofId("water")).isEmpty();
    }

    @Test
    void aLitreOfPetroleumHoldsThirtyFourPointFourMegajoules() {
        assertThat(Fluid.PETROLEUM.joulesPerLitre()).isEqualTo(34.4e6);
    }

    @Test
    void theElectricalEnergyOfALitreFollowsFromItsEfficiency() {
        double wattHours = FluidPhysics.watthoursPerLitre(Fluid.PETROLEUM, 0.35);

        assertThat(wattHours).isCloseTo(34.4e6 * 0.35 / 3600.0, org.assertj.core.api.Assertions.within(1e-6));
    }

    @Test
    void aGeneratorOfOneKilowattBurnsAboutThreeTenthsOfALitreAnHourAtThirtyFivePercent() {
        double perSecond = FluidPhysics.litresPerSecond(Fluid.PETROLEUM, 1000.0, 0.35);

        assertThat(perSecond * 3600.0).isCloseTo(1000.0 * 3600.0 / (34.4e6 * 0.35), org.assertj.core.api.Assertions.within(1e-9));
        assertThat(perSecond * 3600.0).isCloseTo(0.3, org.assertj.core.api.Assertions.within(0.01));
    }

    @Test
    void noPowerBurnsNothing() {
        assertThat(FluidPhysics.litresPerSecond(Fluid.DIESEL, 0.0, 0.35)).isZero();
    }

    @Test
    void anEfficiencyOutsideZeroToOneIsRefused() {
        assertThatThrownBy(() -> FluidPhysics.watthoursPerLitre(Fluid.DIESEL, 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FluidPhysics.litresPerSecond(Fluid.DIESEL, 1.0, 1.5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aVolumeReadsInLitresUnderAThousandAndInCubicMetresFromThere() {
        assertThat(FluidPhysics.volume(850.0)).isEqualTo("850 L");
        assertThat(FluidPhysics.volume(1250.0)).isEqualTo("1.25 m3");
        assertThat(FluidPhysics.volume(0.0)).isEqualTo("0 L");
    }

    @Test
    void aFlowReadsInLitresPerSecondAndInCubicMetresPerHourFromAThousand() {
        assertThat(FluidPhysics.rate(4.0)).isEqualTo("4.00 L/s");
        assertThat(FluidPhysics.rate(1000.0)).isEqualTo("3600.00 m3/h");
    }

    @Test
    void aSmallFlowKeepsItsDigitsSoItDoesNotReadAsZero() {
        assertThat(FluidPhysics.rate(0.0083)).isEqualTo("0.0083 L/s");
        assertThat(FluidPhysics.rate(0.0)).isEqualTo("0.0000 L/s");
    }

    @Test
    void aTankTakesOnlyTheFluidItHoldsAndNeverMixes() {
        FluidTank tank = new FluidTank(100.0);

        assertThat(tank.accept(Fluid.PETROLEUM, 60.0)).isEqualTo(60.0);
        assertThat(tank.accept(Fluid.DIESEL, 10.0)).isZero();
        assertThat(tank.fluid()).contains(Fluid.PETROLEUM);
        assertThat(tank.litres()).isEqualTo(60.0);
    }

    @Test
    void aTankStopsAtItsCapacity() {
        FluidTank tank = new FluidTank(100.0);

        assertThat(tank.accept(Fluid.DIESEL, 150.0)).isEqualTo(100.0);
        assertThat(tank.space()).isZero();
        assertThat(tank.accept(Fluid.DIESEL, 5.0)).isZero();
    }

    @Test
    void aTankGivesOutNoMoreThanItHoldsAndEmptiesItsKind() {
        FluidTank tank = new FluidTank(100.0);
        tank.accept(Fluid.PETROLEUM, 30.0);

        assertThat(tank.draw(Fluid.DIESEL, 10.0)).isZero();
        assertThat(tank.draw(Fluid.PETROLEUM, 50.0)).isEqualTo(30.0);
        assertThat(tank.fluid()).isEqualTo(Optional.empty());
        assertThat(tank.litres()).isZero();
    }

    @Test
    void aTankWhichIsEmptiedTakesAnyKindAgain() {
        FluidTank tank = new FluidTank(100.0);
        tank.accept(Fluid.PETROLEUM, 10.0);
        tank.draw(Fluid.PETROLEUM, 10.0);

        assertThat(tank.accept(Fluid.DIESEL, 20.0)).isEqualTo(20.0);
        assertThat(tank.fluid()).contains(Fluid.DIESEL);
    }

    @Test
    void aTankRefusesANonPositiveCapacityAndANegativeAmount() {
        assertThatThrownBy(() -> new FluidTank(0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FluidTank(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FluidTank(10.0).accept(Fluid.DIESEL, -1.0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNetworkWithOneKindOfFluidIsAllowedAndTwoKindsAreNot() {
        assertThat(FluidRules.allowed(List.of())).isTrue();
        assertThat(FluidRules.allowed(List.of(Fluid.PETROLEUM, Fluid.PETROLEUM))).isTrue();
        assertThat(FluidRules.allowed(List.of(Fluid.PETROLEUM, Fluid.DIESEL))).isFalse();
    }

    @Test
    void petroleumAndDieselNetworksMayNotBeJoined() {
        assertThat(FluidRules.mayJoin(EnumSet.of(Fluid.PETROLEUM), EnumSet.of(Fluid.DIESEL))).isFalse();
        assertThat(FluidRules.mayJoin(EnumSet.of(Fluid.PETROLEUM), EnumSet.noneOf(Fluid.class))).isTrue();
        assertThat(FluidRules.mayJoin(EnumSet.noneOf(Fluid.class), EnumSet.noneOf(Fluid.class))).isTrue();
        assertThat(FluidRules.mayJoin(EnumSet.of(Fluid.DIESEL), EnumSet.of(Fluid.DIESEL))).isTrue();
    }
}
