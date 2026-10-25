package io.corefactory.core.oil;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.oil.OilJoinPlan.Device;
import io.corefactory.core.oil.OilJoinPlan.Join;
import io.corefactory.core.oil.OilJoinPlan.Kind;
import io.corefactory.core.oil.OilJoinPlan.Tube;
import io.corefactory.core.water.WaterNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.corefactory.core.oil.OilPlanFixture.dieselGenerator;
import static io.corefactory.core.oil.OilPlanFixture.petroleumDrill;
import static io.corefactory.core.oil.OilPlanFixture.refinery;
import static io.corefactory.core.oil.OilPlanFixture.tube;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The join plan on its own: a merging device joins the networks it touches, a refinery keeps them apart, an empty intake stays empty
 * until a petroleum source is joined to it, and a placement is refused only when the network it joins would hold two fluids. The
 * merging cases are the cases the former OilJoins tests checked over the networks' fluid sets.
 */
class OilJoinPlanTest {

    private static final Pos WEST = new Pos(-1, 64, 0);
    private static final Pos EAST = new Pos(1, 64, 0);
    private static final Pos NORTH = new Pos(0, 64, -1);
    private static final Pos BETWEEN = new Pos(0, 64, 0);

    /** A merging device one block tall, which joins the tubes beside it and gives or takes the fluid given. */
    private static Device merger(Pos at, Fluid gives, Fluid takes) {
        return new Device(at, 1, gives, takes, false);
    }

    /** Two petroleum networks joined by a device: the device joins them into one network, which still holds petroleum only. */
    @Test
    void twoPetroleumNetworksJoin() {
        List<Tube> tubes = List.of(tube(WEST, Fluid.PETROLEUM), tube(EAST, Fluid.PETROLEUM));
        Device between = merger(BETWEEN, null, Fluid.PETROLEUM);

        assertThat(OilJoinPlan.mixesIfDevicePlaced(tubes, List.of(), between)).isFalse();
        OilJoinPlan plan = OilJoinPlan.of(tubes, List.of(between));
        assertThat(plan.networkOf(WEST)).isEqualTo(plan.networkOf(EAST));
        assertThat(plan.networks().get(plan.networkOf(WEST)).fluids()).containsExactly(Fluid.PETROLEUM);
    }

    /** A petroleum network and a diesel network: a device touching both would join them, so it is refused. */
    @Test
    void aPetroleumAndADieselNetworkAreRefused() {
        List<Tube> tubes = List.of(tube(WEST, Fluid.PETROLEUM), tube(EAST, Fluid.DIESEL));

        assertThat(OilJoinPlan.mixesIfDevicePlaced(tubes, List.of(), merger(BETWEEN, null, null))).isTrue();
    }

    /** An empty network joins a network of either fluid. */
    @Test
    void anEmptyNetworkJoinsAnyFluid() {
        assertThat(OilJoinPlan.mixesIfDevicePlaced(List.of(tube(WEST, null), tube(EAST, Fluid.PETROLEUM)), List.of(),
                merger(BETWEEN, null, null))).isFalse();
        assertThat(OilJoinPlan.mixesIfDevicePlaced(List.of(tube(WEST, null), tube(EAST, Fluid.DIESEL)), List.of(),
                merger(BETWEEN, null, null))).isFalse();
    }

    /** A device that touches no network joins nothing and is allowed. */
    @Test
    void aDeviceTouchingNoNetworkJoinsNothing() {
        assertThat(OilJoinPlan.mixesIfDevicePlaced(List.of(), List.of(), merger(BETWEEN, Fluid.PETROLEUM, null))).isFalse();
    }

    /** Three networks: two of petroleum and one of diesel, around one device, mix. */
    @Test
    void aThirdNetworkOfTheOtherFluidIsRefused() {
        List<Tube> tubes = List.of(tube(WEST, Fluid.PETROLEUM), tube(EAST, Fluid.PETROLEUM), tube(NORTH, Fluid.DIESEL));

        assertThat(OilJoinPlan.mixesIfDevicePlaced(tubes, List.of(), merger(BETWEEN, null, null))).isTrue();
    }

    /** A tube placed between a petroleum tube and a diesel tube would join them, and is refused. */
    @Test
    void aTubeBetweenPetroleumAndDieselIsRefused() {
        List<Tube> tubes = List.of(tube(WEST, Fluid.PETROLEUM), tube(EAST, Fluid.DIESEL));

        assertThat(OilJoinPlan.mixesIfTubePlaced(tubes, List.of(), BETWEEN)).isTrue();
        assertThat(OilJoinPlan.mixesIfTubePlaced(List.of(tube(WEST, Fluid.PETROLEUM), tube(EAST, null)), List.of(), BETWEEN)).isFalse();
    }

    /** A tube beside a refinery joins no network through it: the refinery keeps its networks apart, so the tube does not mix them. */
    @Test
    void aTubeBesideARefineryDoesNotJoinItsNetworks() {
        List<Tube> tubes = List.of(tube(WEST, Fluid.PETROLEUM), tube(EAST, Fluid.DIESEL));

        assertThat(OilJoinPlan.mixesIfTubePlaced(tubes, List.of(refinery(BETWEEN)), NORTH)).isFalse();
        OilJoinPlan plan = OilJoinPlan.of(tubes, List.of(refinery(BETWEEN)));
        assertThat(plan.networkOf(WEST)).isNotEqualTo(plan.networkOf(EAST));
    }

    /** A refinery is never refused: it does not merge the networks it touches. */
    @Test
    void aRefineryIsNeverRefused() {
        List<Tube> tubes = List.of(tube(WEST, Fluid.PETROLEUM), tube(EAST, Fluid.DIESEL));

        assertThat(OilJoinPlan.mixesIfDevicePlaced(tubes, List.of(), refinery(BETWEEN))).isFalse();
    }

    /** Every tube joins its own network, and a block that holds no tube has no network. */
    @Test
    void aTubeJoinsItsOwnNetworkAndOtherBlocksHaveNone() {
        OilJoinPlan plan = OilJoinPlan.of(List.of(tube(WEST, null), tube(EAST, null)), List.of());

        assertThat(plan.joinsAt(WEST)).containsExactly(new Join(plan.networkOf(WEST), Kind.TUBE, null));
        assertThat(plan.networkOf(BETWEEN)).isEqualTo(-1);
        assertThat(plan.joinsAt(BETWEEN)).isEmpty();
    }

    /** An empty tube beside a refinery, with no petroleum source and no consumer on its network, is neither intake nor output. */
    @Test
    void anEmptyTubeBesideARefineryWithNoSourceOrConsumerIsNotJoined() {
        OilJoinPlan plan = OilJoinPlan.of(List.of(tube(WEST, null)), List.of(refinery(BETWEEN)));

        assertThat(plan.joinsAt(BETWEEN)).isEmpty();
    }

    /**
     * The empty intake the refinery must not turn into diesel: while no drill stands on it, its tube is neutral even though the refinery
     * has an output, so a petroleum drill placed on it later is accepted and the tube becomes an intake.
     */
    @Test
    void anEmptyIntakeIsNotMadeDieselSoADrillCanStillBePlacedOnIt() {
        Device refinery = refinery(BETWEEN);
        Device output = dieselGenerator(new Pos(2, 64, 0));
        List<Tube> tubes = List.of(tube(WEST, null), tube(EAST, null));

        OilJoinPlan before = OilJoinPlan.of(tubes, List.of(refinery, output));
        assertThat(before.joinsAt(BETWEEN)).contains(new Join(before.networkOf(EAST), Kind.SOURCE, Fluid.DIESEL));
        assertThat(before.joinsAt(BETWEEN)).noneMatch(join -> join.network() == before.networkOf(WEST));
        assertThat(OilJoinPlan.mixesIfDevicePlaced(tubes, List.of(refinery, output), petroleumDrill(new Pos(-2, 64, 0)))).isFalse();

        OilJoinPlan after = OilJoinPlan.of(tubes, List.of(refinery, output, petroleumDrill(new Pos(-2, 64, 0))));
        assertThat(after.joinsAt(BETWEEN)).contains(new Join(after.networkOf(WEST), Kind.SINK, null));
        assertThat(after.joinsAt(BETWEEN)).doesNotContain(new Join(after.networkOf(WEST), Kind.SOURCE, Fluid.DIESEL));
    }

    /** A tube holding petroleum with no drill is still an intake: the refinery takes what it holds. */
    @Test
    void aFilledPetroleumTubeIsAnIntakeWithoutADrill() {
        OilJoinPlan plan = OilJoinPlan.of(List.of(tube(WEST, Fluid.PETROLEUM)), List.of(refinery(BETWEEN)));

        assertThat(plan.joinsAt(BETWEEN)).containsExactly(new Join(plan.networkOf(WEST), Kind.SINK, null));
    }

    /** A tube holding diesel is an output of the refinery even without a consumer: it keeps the diesel it holds. */
    @Test
    void aFilledDieselTubeIsAnOutputWithoutAConsumer() {
        OilJoinPlan plan = OilJoinPlan.of(List.of(tube(EAST, Fluid.DIESEL)), List.of(refinery(BETWEEN)));

        assertThat(plan.joinsAt(BETWEEN)).containsExactly(new Join(plan.networkOf(EAST), Kind.SOURCE, Fluid.DIESEL));
    }

    /** A network with a petroleum source is an intake, never an output, whatever diesel generator stands on it. */
    @Test
    void aNetworkWithAPetroleumSourceIsNeverAnOutput() {
        OilJoinPlan plan = OilJoinPlan.of(List.of(tube(EAST, null)),
                List.of(refinery(BETWEEN), petroleumDrill(new Pos(1, 64, -1)), dieselGenerator(new Pos(2, 64, 0))));

        assertThat(plan.joinsAt(BETWEEN)).doesNotContain(new Join(plan.networkOf(EAST), Kind.SOURCE, Fluid.DIESEL));
        assertThat(plan.joinsAt(BETWEEN)).contains(new Join(plan.networkOf(EAST), Kind.SINK, null));
        assertThat(plan.networks().get(plan.networkOf(EAST)).fluids()).containsExactly(Fluid.PETROLEUM);
    }

    /** Two tubes may not share a block, a tube's node must stand in its block, and a device's height and roles are checked. */
    @Test
    void theInputsAreChecked() {
        assertThatThrownBy(() -> OilJoinPlan.of(List.of(tube(WEST, null), tube(WEST, null)), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Tube(WEST, WaterNode.pipe(EAST), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Device(BETWEEN, 0, null, null, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Device(BETWEEN, 1, Fluid.DIESEL, null, true)).isInstanceOf(IllegalArgumentException.class);
    }
}
