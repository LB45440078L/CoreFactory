package io.corefactory.core.oil;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.oil.OilJoinPlan.Device;
import io.corefactory.core.oil.OilJoinPlan.Join;
import io.corefactory.core.oil.OilJoinPlan.Kind;
import io.corefactory.core.water.Face;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.corefactory.core.oil.OilPlanFixture.dieselGenerator;
import static io.corefactory.core.oil.OilPlanFixture.dieselPump;
import static io.corefactory.core.oil.OilPlanFixture.petroleumDrill;
import static io.corefactory.core.oil.OilPlanFixture.petroleumGenerator;
import static io.corefactory.core.oil.OilPlanFixture.tube;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The other machines on the oil network, each on every horizontal face of one tube: a petroleum drill gives petroleum, a petroleum generator
 * and a diesel generator and a level II diesel pump take from the network. An ore drill is not a device of the network at all. Each device joins the
 * tube's network on whichever face it stands, and no device changes the fluid of a network it does not give into.
 */
class OilDeviceFacesTest {

    private static final Pos TUBE = new Pos(0, 64, 0);
    private static final List<Face> FACES = List.of(Face.NORTH, Face.SOUTH, Face.EAST, Face.WEST);

    /** A petroleum drill on any face of an empty tube gives petroleum into the tube's network, and the placement is allowed. */
    @Test
    void aPetroleumDrillGivesPetroleumOnEveryFace() {
        for (Face face : FACES) {
            Device drill = petroleumDrill(face.step(TUBE));
            List<OilJoinPlan.Tube> tubes = List.of(tube(TUBE, null));
            OilJoinPlan plan = OilJoinPlan.of(tubes, List.of(drill));
            int net = plan.networkOf(TUBE);

            assertThat(plan.networks().get(net).fluids()).as("face %s", face).containsExactly(Fluid.PETROLEUM);
            assertThat(plan.joinsAt(drill.anchor())).as("face %s", face).contains(new Join(net, Kind.SOURCE, Fluid.PETROLEUM));
            assertThat(OilJoinPlan.mixesIfDevicePlaced(tubes, List.of(), drill)).as("face %s", face).isFalse();
        }
    }

    /** A petroleum drill placed on a face of a tube that holds diesel is refused: the network would hold both fluids. */
    @Test
    void aPetroleumDrillIsRefusedOnAFaceOfADieselTubeAndAcceptedOnAPetroleumOne() {
        for (Face face : FACES) {
            Device drill = petroleumDrill(face.step(TUBE));

            assertThat(OilJoinPlan.mixesIfDevicePlaced(List.of(tube(TUBE, Fluid.DIESEL)), List.of(), drill))
                    .as("face %s", face).isTrue();
            assertThat(OilJoinPlan.mixesIfDevicePlaced(List.of(tube(TUBE, Fluid.PETROLEUM)), List.of(), drill))
                    .as("face %s", face).isFalse();
        }
    }

    /** A petroleum generator on any face takes petroleum from the tube's network, and gives nothing into it. */
    @Test
    void aPetroleumGeneratorTakesPetroleumOnEveryFace() {
        for (Face face : FACES) {
            Device generator = petroleumGenerator(face.step(TUBE));
            OilJoinPlan plan = OilJoinPlan.of(List.of(tube(TUBE, Fluid.PETROLEUM)), List.of(generator));
            int net = plan.networkOf(TUBE);

            assertThat(plan.joinsAt(generator.anchor())).as("face %s", face).contains(new Join(net, Kind.SINK, null));
            assertThat(plan.joinsAt(generator.anchor())).as("face %s", face).noneMatch(join -> join.kind() == Kind.SOURCE);
            assertThat(plan.networks().get(net).fluids()).as("face %s", face).containsExactly(Fluid.PETROLEUM);
        }
    }

    /**
     * A diesel generator on any face of a petroleum tube is joined to the network and draws nothing from it: the network stays petroleum,
     * and the generator is not a diesel source for it. Placing the generator is allowed, since it gives nothing into the network.
     */
    @Test
    void aDieselGeneratorOnAPetroleumTubeJoinsButGetsNoDiesel() {
        for (Face face : FACES) {
            Device generator = dieselGenerator(face.step(TUBE));
            List<OilJoinPlan.Tube> tubes = List.of(tube(TUBE, Fluid.PETROLEUM));
            OilJoinPlan plan = OilJoinPlan.of(tubes, List.of(generator));
            int net = plan.networkOf(TUBE);

            assertThat(plan.joinsAt(generator.anchor())).as("face %s", face).contains(new Join(net, Kind.SINK, null));
            assertThat(plan.networks().get(net).fluids()).as("face %s", face).containsExactly(Fluid.PETROLEUM);
            assertThat(OilJoinPlan.mixesIfDevicePlaced(tubes, List.of(), generator)).as("face %s", face).isFalse();
        }
    }

    /** A diesel generator on any face of an empty tube joins it, and the tube stays empty until a source gives it a fluid. */
    @Test
    void aDieselGeneratorOnAnEmptyTubeJoinsWithoutFluid() {
        for (Face face : FACES) {
            Device generator = dieselGenerator(face.step(TUBE));
            OilJoinPlan plan = OilJoinPlan.of(List.of(tube(TUBE, null)), List.of(generator));
            int net = plan.networkOf(TUBE);

            assertThat(plan.joinsAt(generator.anchor())).as("face %s", face).contains(new Join(net, Kind.SINK, null));
            assertThat(plan.networks().get(net).fluids()).as("face %s", face).isEmpty();
        }
    }

    /** A level II diesel pump on any face of a diesel tube takes diesel from it, and gives nothing into the network. */
    @Test
    void aDieselPumpTakesDieselOnEveryFace() {
        for (Face face : FACES) {
            Device pump = dieselPump(face.step(TUBE));
            OilJoinPlan plan = OilJoinPlan.of(List.of(tube(TUBE, Fluid.DIESEL)), List.of(pump));
            int net = plan.networkOf(TUBE);

            assertThat(plan.joinsAt(pump.anchor())).as("face %s", face).contains(new Join(net, Kind.SINK, null));
            assertThat(plan.networks().get(net).fluids()).as("face %s", face).containsExactly(Fluid.DIESEL);
        }
    }

}
