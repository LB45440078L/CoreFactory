package io.corefactory.paper.oil;

import io.corefactory.core.oil.Fluid;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Two oil networks join only when their fluids agree: one kind between them, or none. */
class OilJoinsTest {

    @Test
    void twoPetroleumNetworksJoin() {
        assertThat(OilJoins.mayJoinAll(List.of(EnumSet.of(Fluid.PETROLEUM), EnumSet.of(Fluid.PETROLEUM)))).isTrue();
    }

    @Test
    void aPetroleumAndADieselNetworkAreRefused() {
        assertThat(OilJoins.mayJoinAll(List.of(EnumSet.of(Fluid.PETROLEUM), EnumSet.of(Fluid.DIESEL)))).isFalse();
    }

    @Test
    void anEmptyNetworkJoinsAnyFluid() {
        Set<Fluid> empty = EnumSet.noneOf(Fluid.class);

        assertThat(OilJoins.mayJoinAll(List.of(empty, EnumSet.of(Fluid.PETROLEUM)))).isTrue();
        assertThat(OilJoins.mayJoinAll(List.of(EnumSet.of(Fluid.DIESEL), empty))).isTrue();
        assertThat(OilJoins.mayJoinAll(List.of(empty, empty))).isTrue();
    }

    @Test
    void aTubeTouchingNothingJoins() {
        assertThat(OilJoins.mayJoinAll(List.of())).isTrue();
    }

    @Test
    void aThirdNetworkOfTheOtherFluidIsRefusedAfterTwoOfOneKind() {
        assertThat(OilJoins.mayJoinAll(List.of(
                EnumSet.of(Fluid.PETROLEUM), EnumSet.of(Fluid.PETROLEUM), EnumSet.of(Fluid.DIESEL)))).isFalse();
    }
}
