package io.corefactory.core.oil;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.oil.OilJoinPlan.Device;
import io.corefactory.core.oil.OilJoinPlan.Join;
import io.corefactory.core.oil.OilJoinPlan.Kind;
import io.corefactory.core.oil.OilJoinPlan.Tube;
import io.corefactory.core.water.Face;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.corefactory.core.oil.OilPlanFixture.dieselGenerator;
import static io.corefactory.core.oil.OilPlanFixture.networkOf;
import static io.corefactory.core.oil.OilPlanFixture.petroleumDrill;
import static io.corefactory.core.oil.OilPlanFixture.refinery;
import static io.corefactory.core.oil.OilPlanFixture.tube;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every combination of the refinery's four horizontal faces. A face holds nothing, a petroleum tube (a petroleum drill joined to it) or a
 * diesel tube (a diesel generator joined to it, or no generator). Each occupied face also varies: a diesel generator may stand beside a
 * petroleum tube too, and a tube's tank may start full of its fluid or empty. That is 81 combinations of the three face states, each with
 * every generator and fill variant: 9 choices a face, 9 to the fourth, 6561 layouts. Each layout is checked against the rules: a petroleum
 * face is an intake; a diesel face gets diesel only when it holds diesel or a diesel generator draws from it; no network holds both fluids;
 * the refinery never takes from a diesel network nor gives into a petroleum one; an empty intake never becomes diesel.
 */
class RefineryFaceCombinationsTest {

    private static final Pos ANCHOR = new Pos(0, 64, 0);
    private static final List<Face> FACES = List.of(Face.NORTH, Face.SOUTH, Face.EAST, Face.WEST);

    /** One face's layout: its state, whether a diesel generator stands beside its tube, and whether its tank starts full. */
    private record Layout(State state, boolean generator, boolean full) {

        /** The layout as the failure message names it, such as {@code EAST:DIESEL+generator+full}. */
        String name(Face face) {
            if (state == State.NONE) {
                return face + ":NONE";
            }
            return face + ":" + state + (generator ? "+generator" : "") + (full ? "+full" : "");
        }
    }

    /** The three states of a face: nothing, a petroleum tube, or a diesel tube. */
    private enum State { NONE, PETROLEUM, DIESEL }

    /** Every layout a face may take: nothing, or one of the two tubes with and without a generator and with and without a full tank. */
    private static List<Layout> options() {
        List<Layout> options = new ArrayList<>();
        options.add(new Layout(State.NONE, false, false));
        for (State state : List.of(State.PETROLEUM, State.DIESEL)) {
            for (boolean generator : new boolean[] {false, true}) {
                for (boolean full : new boolean[] {false, true}) {
                    options.add(new Layout(state, generator, full));
                }
            }
        }
        return options;
    }

    @Test
    void everyLayoutOfTheFourFacesFollowsTheRules() {
        List<Layout> options = options();
        int total = (int) Math.pow(options.size(), FACES.size());
        int checked = 0;
        for (int code = 0; code < total; code++) {
            List<Layout> layouts = new ArrayList<>();
            int rest = code;
            for (int face = 0; face < FACES.size(); face++) {
                layouts.add(options.get(rest % options.size()));
                rest /= options.size();
            }
            check(layouts);
            checked++;
        }
        assertThat(checked).isEqualTo(6561);
    }

    /** Builds one layout's tubes and devices, and checks the joins the plan gives the refinery and every network. */
    private static void check(List<Layout> layouts) {
        Map<Pos, Tube> tubes = new LinkedHashMap<>();
        List<Device> devices = new ArrayList<>();
        devices.add(refinery(ANCHOR));
        StringBuilder label = new StringBuilder();
        for (int i = 0; i < FACES.size(); i++) {
            Face face = FACES.get(i);
            Layout layout = layouts.get(i);
            label.append(layout.name(face)).append(' ');
            if (layout.state() == State.NONE) {
                continue;
            }
            Pos at = face.step(ANCHOR);
            Fluid fluid = layout.state() == State.PETROLEUM ? Fluid.PETROLEUM : Fluid.DIESEL;
            tubes.put(at, tube(at, layout.full() ? fluid : null));
            if (layout.state() == State.PETROLEUM) {
                devices.add(petroleumDrill(face.step(at)));
                if (layout.generator()) {
                    devices.add(dieselGenerator(new Pos(at.x(), at.y() + 1, at.z())));
                }
            } else if (layout.generator()) {
                devices.add(dieselGenerator(face.step(at)));
            }
        }
        String faces = label.toString().trim();
        OilJoinPlan plan = OilJoinPlan.of(tubes.values(), devices);
        List<Join> refinery = plan.joinsAt(ANCHOR);

        for (OilJoinPlan.Network network : plan.networks()) {
            assertThat(network.allowed()).as("(c) no network holds both fluids, layout %s, network %s", faces, network.id()).isTrue();
        }

        for (int i = 0; i < FACES.size(); i++) {
            Face face = FACES.get(i);
            Layout layout = layouts.get(i);
            if (layout.state() == State.NONE) {
                continue;
            }
            Pos at = face.step(ANCHOR);
            int net = plan.networkOf(at);
            Set<Fluid> held = plan.networks().get(net).fluids();
            String where = "layout " + faces + ", face " + face;
            if (layout.state() == State.PETROLEUM) {
                assertThat(held).as("(e) a petroleum face holds petroleum from its drill, %s", where)
                        .contains(Fluid.PETROLEUM);
                assertThat(refinery).as("(a) a petroleum face is an intake, %s", where)
                        .contains(new Join(net, Kind.SINK, null));
                assertThat(refinery).as("(e) an intake never gives diesel, %s", where)
                        .doesNotContain(new Join(net, Kind.SOURCE, Fluid.DIESEL));
            } else {
                boolean output = layout.generator() || layout.full();
                assertThat(refinery).as("(d) a diesel face is never an intake, %s", where)
                        .doesNotContain(new Join(net, Kind.SINK, null));
                if (output) {
                    assertThat(refinery).as("(b) a diesel face with a consumer or diesel gets diesel, %s", where)
                            .contains(new Join(net, Kind.SOURCE, Fluid.DIESEL));
                    OilNetwork network = networkOf(plan, tubes, at);
                    assertThat(network.supply(Fluid.DIESEL, 5.0)).as("(b) the network takes diesel, %s", where).isPositive();
                    if (layout.generator()) {
                        assertThat(network.draw(Fluid.DIESEL, 1.0)).as("(b) the generator draws diesel, %s", where).isPositive();
                    }
                } else {
                    assertThat(refinery).as("an empty diesel face with no consumer is not an output yet, %s", where)
                            .doesNotContain(new Join(net, Kind.SOURCE, Fluid.DIESEL));
                }
            }
        }

        for (Join join : refinery) {
            Set<Fluid> held = plan.networks().get(join.network()).fluids();
            if (join.kind() == Kind.SINK) {
                assertThat(held).as("(d) the refinery takes petroleum only from a petroleum network, layout %s", faces)
                        .contains(Fluid.PETROLEUM).doesNotContain(Fluid.DIESEL);
            } else if (join.kind() == Kind.SOURCE) {
                assertThat(held).as("(d) the refinery gives diesel only into a network without petroleum, layout %s", faces)
                        .contains(Fluid.DIESEL).doesNotContain(Fluid.PETROLEUM);
            }
        }
    }
}
