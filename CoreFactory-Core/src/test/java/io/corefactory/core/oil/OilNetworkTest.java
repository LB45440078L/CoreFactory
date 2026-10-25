package io.corefactory.core.oil;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class OilNetworkTest {

    @Test
    void aSupplySplitsOverTheTubesInProportionToTheirFreeSpace() {
        FluidTank small = new FluidTank(100.0);
        FluidTank large = new FluidTank(300.0);
        large.accept(Fluid.PETROLEUM, 100.0);
        OilNetwork network = new OilNetwork(List.of(small, large));

        assertThat(network.supply(Fluid.PETROLEUM, 60.0)).isCloseTo(60.0, within(1e-9));

        assertThat(small.litres()).isCloseTo(20.0, within(1e-9));
        assertThat(large.litres()).isCloseTo(140.0, within(1e-9));
    }

    @Test
    void aSupplyLargerThanTheRoomFillsEveryTubeAndTakesOnlyTheRoom() {
        FluidTank first = new FluidTank(10.0);
        FluidTank second = new FluidTank(10.0);
        OilNetwork network = new OilNetwork(List.of(first, second));

        assertThat(network.supply(Fluid.DIESEL, 50.0)).isCloseTo(20.0, within(1e-9));

        assertThat(first.space()).isZero();
        assertThat(second.space()).isZero();
        assertThat(network.supply(Fluid.DIESEL, 5.0)).isZero();
    }

    @Test
    void aSecondKindIsRefusedAndTheTanksAreLeftAsTheyWere() {
        FluidTank tube = new FluidTank(100.0);
        OilNetwork network = new OilNetwork(List.of(tube));
        network.supply(Fluid.PETROLEUM, 30.0);

        assertThat(network.supply(Fluid.DIESEL, 10.0)).isZero();

        assertThat(network.kinds()).containsExactly(Fluid.PETROLEUM);
        assertThat(network.litres()).isCloseTo(30.0, within(1e-9));
    }

    @Test
    void anEmptyNetworkTakesAnyKindOfFluid() {
        OilNetwork network = new OilNetwork(List.of(new FluidTank(50.0)));

        assertThat(network.supply(Fluid.DIESEL, 20.0)).isCloseTo(20.0, within(1e-9));
        assertThat(network.kinds()).containsExactly(Fluid.DIESEL);
    }

    @Test
    void aNetworkEmptiedOfOneKindTakesTheOther() {
        OilNetwork network = new OilNetwork(List.of(new FluidTank(50.0)));
        network.supply(Fluid.PETROLEUM, 10.0);
        network.draw(Fluid.PETROLEUM, 10.0);

        assertThat(network.supply(Fluid.DIESEL, 10.0)).isCloseTo(10.0, within(1e-9));
        assertThat(network.kinds()).containsExactly(Fluid.DIESEL);
    }

    @Test
    void aFullNetworkTakesNothing() {
        OilNetwork network = new OilNetwork(List.of(new FluidTank(10.0)));
        network.supply(Fluid.PETROLEUM, 10.0);

        assertThat(network.supply(Fluid.PETROLEUM, 5.0)).isZero();
    }

    @Test
    void aDrawTakesProportionallyToWhatEachTubeHolds() {
        FluidTank first = new FluidTank(100.0);
        FluidTank second = new FluidTank(100.0);
        first.accept(Fluid.PETROLEUM, 30.0);
        second.accept(Fluid.PETROLEUM, 10.0);
        OilNetwork network = new OilNetwork(List.of(first, second));

        assertThat(network.draw(Fluid.PETROLEUM, 20.0)).isCloseTo(20.0, within(1e-9));

        assertThat(first.litres()).isCloseTo(15.0, within(1e-9));
        assertThat(second.litres()).isCloseTo(5.0, within(1e-9));
    }

    @Test
    void aDrawLargerThanTheHoldingEmptiesEveryTubeThatHoldsTheFluid() {
        FluidTank first = new FluidTank(100.0);
        FluidTank second = new FluidTank(100.0);
        first.accept(Fluid.DIESEL, 30.0);
        second.accept(Fluid.DIESEL, 10.0);
        OilNetwork network = new OilNetwork(List.of(first, second));

        assertThat(network.draw(Fluid.DIESEL, 100.0)).isCloseTo(40.0, within(1e-9));

        assertThat(network.kinds()).isEmpty();
        assertThat(network.litres()).isZero();
    }

    @Test
    void aDrawOfAFluidTheNetworkDoesNotHoldGivesNothing() {
        OilNetwork network = new OilNetwork(List.of(new FluidTank(100.0)));
        network.supply(Fluid.PETROLEUM, 10.0);

        assertThat(network.draw(Fluid.DIESEL, 5.0)).isZero();
        assertThat(network.litres()).isCloseTo(10.0, within(1e-9));
    }

    @Test
    void kindsIsEmptyForAnEmptyNetworkAndHoldsTheFluidOtherwise() {
        OilNetwork network = new OilNetwork(List.of(new FluidTank(100.0), new FluidTank(100.0)));
        assertThat(network.kinds()).isEmpty();

        network.supply(Fluid.DIESEL, 1.0);

        assertThat(network.kinds()).containsExactly(Fluid.DIESEL);
    }

    @Test
    void litresSumsTheTanksOfTheNetwork() {
        FluidTank first = new FluidTank(100.0);
        FluidTank second = new FluidTank(100.0);
        first.accept(Fluid.PETROLEUM, 12.5);
        second.accept(Fluid.PETROLEUM, 7.5);

        assertThat(new OilNetwork(List.of(first, second)).litres()).isCloseTo(20.0, within(1e-9));
    }

    @Test
    void aNetworkWithNoTubesHoldsAndTakesNothing() {
        OilNetwork network = new OilNetwork(List.of());

        assertThat(network.litres()).isZero();
        assertThat(network.kinds()).isEmpty();
        assertThat(network.supply(Fluid.PETROLEUM, 5.0)).isZero();
        assertThat(network.draw(Fluid.PETROLEUM, 5.0)).isZero();
    }

    @Test
    void aNetworkHoldingTwoKindsIsRefused() {
        FluidTank petroleum = new FluidTank(100.0);
        FluidTank diesel = new FluidTank(100.0);
        petroleum.accept(Fluid.PETROLEUM, 1.0);
        diesel.accept(Fluid.DIESEL, 1.0);

        assertThatThrownBy(() -> new OilNetwork(List.of(petroleum, diesel))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNegativeOrInfiniteAmountIsRefused() {
        OilNetwork network = new OilNetwork(List.of(new FluidTank(100.0)));

        assertThatThrownBy(() -> network.supply(Fluid.PETROLEUM, -1.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> network.draw(Fluid.PETROLEUM, Double.POSITIVE_INFINITY)).isInstanceOf(IllegalArgumentException.class);
    }
}
