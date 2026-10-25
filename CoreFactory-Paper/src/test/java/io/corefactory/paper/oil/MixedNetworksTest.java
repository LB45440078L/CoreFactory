package io.corefactory.paper.oil;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.oil.Fluid;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A saved network that holds two fluids keeps its majority kind; a tie keeps petroleum; each network is judged on its own. */
class MixedNetworksTest {

    private static final String PETROLEUM = Fluid.PETROLEUM.id();
    private static final String DIESEL = Fluid.DIESEL.id();

    @Test
    void aPureNetworkIsUntouched() {
        List<MixedNetworks.Tube> tubes = List.of(
                new MixedNetworks.Tube(new Pos(0, 0, 0), PETROLEUM),
                new MixedNetworks.Tube(new Pos(1, 0, 0), PETROLEUM));

        assertThat(MixedNetworks.decide(tubes, List.of(List.of(new Pos(0, 0, 0), new Pos(1, 0, 0))))).isEmpty();
    }

    @Test
    void aMixedNetworkWithAMajorityEmptiesItsMinorityTubesOnly() {
        Pos diesel = new Pos(2, 0, 0);
        List<MixedNetworks.Tube> tubes = List.of(
                new MixedNetworks.Tube(new Pos(0, 0, 0), PETROLEUM),
                new MixedNetworks.Tube(new Pos(1, 0, 0), PETROLEUM),
                new MixedNetworks.Tube(diesel, DIESEL),
                new MixedNetworks.Tube(new Pos(3, 0, 0), PETROLEUM));

        List<MixedNetworks.Mixed> mixed = MixedNetworks.decide(tubes, List.of(List.of(
                new Pos(0, 0, 0), new Pos(1, 0, 0), diesel, new Pos(3, 0, 0))));

        assertThat(mixed).containsExactly(new MixedNetworks.Mixed(new Pos(0, 0, 0), PETROLEUM, DIESEL, List.of(diesel)));
    }

    @Test
    void aTieEmptiesDiesel() {
        Pos petroleum = new Pos(0, 0, 0);
        Pos diesel = new Pos(1, 0, 0);
        List<MixedNetworks.Tube> tubes = List.of(
                new MixedNetworks.Tube(petroleum, PETROLEUM),
                new MixedNetworks.Tube(diesel, DIESEL));

        List<MixedNetworks.Mixed> mixed = MixedNetworks.decide(tubes, List.of(List.of(petroleum, diesel)));

        assertThat(mixed).containsExactly(new MixedNetworks.Mixed(petroleum, PETROLEUM, DIESEL, List.of(diesel)));
    }

    @Test
    void twoSeparateNetworksAreJudgedSeparately() {
        Pos purePetroleum = new Pos(0, 0, 0);
        Pos purePetroleumNext = new Pos(1, 0, 0);
        Pos mixedDiesel = new Pos(5, 0, 0);
        Pos mixedDieselNext = new Pos(6, 0, 0);
        Pos mixedPetroleum = new Pos(7, 0, 0);
        List<MixedNetworks.Tube> tubes = List.of(
                new MixedNetworks.Tube(purePetroleum, PETROLEUM),
                new MixedNetworks.Tube(purePetroleumNext, PETROLEUM),
                new MixedNetworks.Tube(mixedDiesel, DIESEL),
                new MixedNetworks.Tube(mixedDieselNext, DIESEL),
                new MixedNetworks.Tube(mixedPetroleum, PETROLEUM));

        List<MixedNetworks.Mixed> mixed = MixedNetworks.decide(tubes, List.of(
                List.of(purePetroleum, purePetroleumNext),
                List.of(mixedDiesel, mixedDieselNext, mixedPetroleum)));

        assertThat(mixed).containsExactly(
                new MixedNetworks.Mixed(mixedDiesel, DIESEL, PETROLEUM, List.of(mixedPetroleum)));
    }

    @Test
    void anEmptyTubeDoesNotCountAsAFluid() {
        Pos petroleum = new Pos(0, 0, 0);
        Pos diesel = new Pos(1, 0, 0);
        List<MixedNetworks.Tube> tubes = List.of(
                new MixedNetworks.Tube(petroleum, PETROLEUM),
                new MixedNetworks.Tube(new Pos(2, 0, 0), null),
                new MixedNetworks.Tube(new Pos(3, 0, 0), null),
                new MixedNetworks.Tube(diesel, DIESEL));
        List<Pos> network = List.of(petroleum, new Pos(2, 0, 0), new Pos(3, 0, 0), diesel);

        List<MixedNetworks.Mixed> mixed = MixedNetworks.decide(tubes, List.of(network));

        assertThat(mixed).containsExactly(new MixedNetworks.Mixed(petroleum, PETROLEUM, DIESEL, List.of(diesel)));
    }

    @Test
    void aNetworkOfOneFluidAndEmptyTubesIsUntouched() {
        List<MixedNetworks.Tube> tubes = List.of(
                new MixedNetworks.Tube(new Pos(0, 0, 0), DIESEL),
                new MixedNetworks.Tube(new Pos(1, 0, 0), null));

        assertThat(MixedNetworks.decide(tubes, List.of(List.of(new Pos(0, 0, 0), new Pos(1, 0, 0))))).isEmpty();
    }
}
