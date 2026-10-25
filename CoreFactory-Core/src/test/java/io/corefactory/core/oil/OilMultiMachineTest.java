package io.corefactory.core.oil;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.oil.OilJoinPlan.Device;
import io.corefactory.core.oil.OilJoinPlan.Join;
import io.corefactory.core.oil.OilJoinPlan.Kind;
import io.corefactory.core.oil.OilJoinPlan.Tube;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.corefactory.core.oil.OilPlanFixture.dieselGenerator;
import static io.corefactory.core.oil.OilPlanFixture.petroleumDrill;
import static io.corefactory.core.oil.OilPlanFixture.refinery;
import static io.corefactory.core.oil.OilPlanFixture.tube;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two or more machines on one oil network: two refineries on one petroleum network, a drill and a refinery sharing a tube, and two
 * refineries giving diesel into one tube. Each layout is built from the placements the plugin accepts, so the rules hold for it.
 */
class OilMultiMachineTest {

    /** Two refineries on one petroleum network: both take petroleum from it, and each gives diesel into its own output. */
    @Test
    void twoRefineriesOnOnePetroleumNetworkEachGiveDieselToTheirOwnOutput() {
        Pos intake = new Pos(0, 64, 0);
        Pos westRefinery = new Pos(-1, 64, 0);
        Pos eastRefinery = new Pos(1, 64, 0);
        Pos westOutput = new Pos(-2, 64, 0);
        Pos eastOutput = new Pos(2, 64, 0);
        Map<Pos, Tube> tubes = tubesOf(tube(intake, Fluid.PETROLEUM), tube(westOutput, null), tube(eastOutput, null));
        List<Device> devices = List.of(
                petroleumDrill(new Pos(0, 64, -1)),
                refinery(westRefinery),
                refinery(eastRefinery),
                dieselGenerator(new Pos(-3, 64, 0)),
                dieselGenerator(new Pos(3, 64, 0)));
        OilJoinPlan plan = OilJoinPlan.of(tubes.values(), devices);
        int petroleum = plan.networkOf(intake);

        assertThat(plan.joinsAt(westRefinery))
                .contains(new Join(petroleum, Kind.SINK, null), new Join(plan.networkOf(westOutput), Kind.SOURCE, Fluid.DIESEL));
        assertThat(plan.joinsAt(eastRefinery))
                .contains(new Join(petroleum, Kind.SINK, null), new Join(plan.networkOf(eastOutput), Kind.SOURCE, Fluid.DIESEL));
        assertThat(plan.networks().get(petroleum).fluids()).containsExactly(Fluid.PETROLEUM);
        assertThat(plan.networks()).allMatch(OilJoinPlan.Network::allowed);
    }

    /** A drill and a refinery sharing a tube: the drill's petroleum is the refinery's intake, and the refinery's diesel leaves by the west tube. */
    @Test
    void aDrillAndARefineryShareATube() {
        Pos intake = new Pos(0, 64, 0);
        Pos refineryAt = new Pos(-1, 64, 0);
        Pos output = new Pos(-2, 64, 0);
        Map<Pos, Tube> tubes = tubesOf(tube(intake, null), tube(output, null));
        Device drill = petroleumDrill(new Pos(0, 64, -1));
        OilJoinPlan plan = OilJoinPlan.of(tubes.values(), List.of(drill, refinery(refineryAt), dieselGenerator(new Pos(-3, 64, 0))));
        int petroleum = plan.networkOf(intake);

        assertThat(plan.networks().get(petroleum).fluids()).containsExactly(Fluid.PETROLEUM);
        assertThat(plan.joinsAt(refineryAt)).contains(
                new Join(petroleum, Kind.SINK, null),
                new Join(plan.networkOf(output), Kind.SOURCE, Fluid.DIESEL));
        assertThat(plan.networks()).allMatch(OilJoinPlan.Network::allowed);
    }

    /**
     * Two refineries giving into one diesel tube, each with its own petroleum intake. The diesel tube is an output of both, and each intake
     * takes petroleum from its own network only.
     */
    @Test
    void twoRefineriesGiveDieselIntoOneTube() {
        Pos westIntake = new Pos(-2, 64, 0);
        Pos eastIntake = new Pos(2, 64, 0);
        Pos shared = new Pos(0, 64, 0);
        Pos westRefinery = new Pos(-1, 64, 0);
        Pos eastRefinery = new Pos(1, 64, 0);
        Map<Pos, Tube> tubes = tubesOf(tube(westIntake, Fluid.PETROLEUM), tube(shared, null), tube(eastIntake, Fluid.PETROLEUM));
        List<Device> devices = List.of(
                refinery(westRefinery),
                refinery(eastRefinery),
                dieselGenerator(new Pos(0, 64, -1)));
        OilJoinPlan plan = OilJoinPlan.of(tubes.values(), devices);
        int sharedNet = plan.networkOf(shared);

        assertThat(plan.joinsAt(westRefinery)).contains(
                new Join(plan.networkOf(westIntake), Kind.SINK, null), new Join(sharedNet, Kind.SOURCE, Fluid.DIESEL));
        assertThat(plan.joinsAt(eastRefinery)).contains(
                new Join(plan.networkOf(eastIntake), Kind.SINK, null), new Join(sharedNet, Kind.SOURCE, Fluid.DIESEL));
        assertThat(plan.joinsAt(westRefinery)).doesNotContain(new Join(sharedNet, Kind.SINK, null));
        assertThat(plan.joinsAt(eastRefinery)).doesNotContain(new Join(sharedNet, Kind.SINK, null));
        assertThat(plan.networks()).allMatch(OilJoinPlan.Network::allowed);
    }

    /**
     * A drill placed on the output tube of a refinery is refused: it would join the diesel to petroleum. The same drill on the intake tube
     * is accepted.
     */
    @Test
    void aDrillIsRefusedOnARefineryOutputAndAcceptedOnItsIntake() {
        Pos intake = new Pos(-1, 64, 0);
        Pos output = new Pos(1, 64, 0);
        List<Tube> tubes = new ArrayList<>(List.of(tube(intake, null), tube(output, Fluid.DIESEL)));
        List<Device> devices = List.of(refinery(new Pos(0, 64, 0)), dieselGenerator(new Pos(2, 64, 0)));

        assertThat(OilJoinPlan.mixesIfDevicePlaced(tubes, devices, petroleumDrill(new Pos(1, 64, -1)))).isTrue();
        assertThat(OilJoinPlan.mixesIfDevicePlaced(tubes, devices, petroleumDrill(new Pos(-1, 64, -1)))).isFalse();
    }

    private static Map<Pos, Tube> tubesOf(Tube... tubes) {
        Map<Pos, Tube> byPos = new LinkedHashMap<>();
        for (Tube tube : tubes) {
            byPos.put(tube.pos(), tube);
        }
        return byPos;
    }
}
