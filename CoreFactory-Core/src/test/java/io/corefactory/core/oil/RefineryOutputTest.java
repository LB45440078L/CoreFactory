package io.corefactory.core.oil;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.oil.OilJoinPlan.Join;
import io.corefactory.core.oil.OilJoinPlan.Kind;
import io.corefactory.core.oil.OilJoinPlan.Tube;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.corefactory.core.oil.OilPlanFixture.dieselGenerator;
import static io.corefactory.core.oil.OilPlanFixture.petroleumDrill;
import static io.corefactory.core.oil.OilPlanFixture.refinery;
import static io.corefactory.core.oil.OilPlanFixture.networkOf;
import static io.corefactory.core.oil.OilPlanFixture.tube;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The reported bug: a refinery takes petroleum from its west tube, and a diesel tube on its east face gets no diesel, so the diesel
 * generator beside it is never powered. The refinery's block used to join the tubes beside it as one node of the water grid, so the
 * east tube fell into the petroleum network, and the refinery gave diesel only through its north outlet.
 */
class RefineryOutputTest {

    private static final Pos REFINERY = new Pos(0, 64, 0);
    private static final Pos WEST = new Pos(-1, 64, 0);
    private static final Pos EAST = new Pos(1, 64, 0);
    private static final Pos OIL_DRILL = new Pos(-2, 64, 0);
    private static final Pos GENERATOR = new Pos(2, 64, 0);

    /** The west tube holds petroleum from its drill, the east tube is empty, and a diesel generator stands beside the east tube. */
    private static final Map<Pos, Tube> TUBES = Map.of(
            WEST, tube(WEST, Fluid.PETROLEUM),
            EAST, tube(EAST, null));

    private static OilJoinPlan plan() {
        return OilJoinPlan.of(TUBES.values(), List.of(
                refinery(REFINERY), petroleumDrill(OIL_DRILL), dieselGenerator(GENERATOR)));
    }

    /** The east tube is a network of its own: it is not the petroleum network the west tube feeds. */
    @Test
    void theEastTubeIsNotInThePetroleumNetwork() {
        OilJoinPlan plan = plan();

        assertThat(plan.networkOf(EAST)).isNotEqualTo(plan.networkOf(WEST));
    }

    /** The refinery takes petroleum from the west network, and gives diesel into the east network. */
    @Test
    void theRefineryTakesPetroleumFromTheWestAndGivesDieselToTheEast() {
        OilJoinPlan plan = plan();

        assertThat(plan.joinsAt(REFINERY)).contains(
                new Join(plan.networkOf(WEST), Kind.SINK, null),
                new Join(plan.networkOf(EAST), Kind.SOURCE, Fluid.DIESEL));
    }

    /** The east network takes the diesel, since it holds no petroleum, and the generator beside it draws the diesel back out. */
    @Test
    void theEastNetworkTakesDieselAndTheGeneratorDrawsIt() {
        OilJoinPlan plan = plan();
        OilNetwork east = networkOf(plan, TUBES, EAST);

        assertThat(east.supply(Fluid.DIESEL, 5.0)).isPositive();
        assertThat(east.draw(Fluid.DIESEL, 1.0)).isPositive();
    }
}
