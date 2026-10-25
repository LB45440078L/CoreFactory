package io.corefactory.core.scenario;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.oil.Fluid;
import io.corefactory.core.oil.FluidRules;
import io.corefactory.core.oil.FluidTank;
import io.corefactory.core.oil.OilModels;
import io.corefactory.core.oil.OilNetwork;
import io.corefactory.core.oil.OilSettings;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.WaterGrid;
import io.corefactory.core.water.WaterNode;
import io.corefactory.core.water.WaterOrientation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * The oil tube path on its own: a run of straight pipes, a corner in the horizontal plane, a corner in the vertical plane, a
 * vertical run, and the end. The tubes must join into one network, each must join on the faces of its neighbours, each must be
 * drawn with the model its shape asks for, and fluid must move from one end to the other.
 */
class OilTubePathTest {

    /**
     * The path from the west end: an east-west run of three pipes at z 0, a corner at (3, 64, 0) turning south, a corner at
     * (3, 64, 1) turning up, a vertical run of two pipes, and the last pipe that ends the path.
     */
    private static final List<Pos> PATH = List.of(
            at(0, 64, 0), at(1, 64, 0), at(2, 64, 0), at(3, 64, 0),
            at(3, 64, 1), at(3, 65, 1), at(3, 66, 1));

    private static Pos at(int x, int y, int z) {
        return new Pos(x, y, z);
    }

    private static WaterGrid path() {
        return WaterGrid.of(PATH.stream().map(WaterNode::pipe).toList());
    }

    /** The seven tubes make one network, and no other tube is in it. */
    @Test
    void theTubesOfThePathJoinIntoOneNetwork() {
        WaterGrid grid = path();

        assertThat(grid.networks()).hasSize(1);
        assertThat(grid.networks().get(0)).containsExactlyInAnyOrder(PATH.toArray(new Pos[0]));
    }

    /** Each tube joins on the faces that its neighbours on the path lie across, and on no other face. */
    @Test
    void eachTubeJoinsOnlyTheFacesOfItsNeighboursOnThePath() {
        WaterGrid grid = path();

        assertThat(grid.connectedFaces(at(0, 64, 0))).containsExactlyInAnyOrder(Face.EAST);
        assertThat(grid.connectedFaces(at(1, 64, 0))).containsExactlyInAnyOrder(Face.WEST, Face.EAST);
        assertThat(grid.connectedFaces(at(2, 64, 0))).containsExactlyInAnyOrder(Face.WEST, Face.EAST);
        assertThat(grid.connectedFaces(at(3, 64, 0))).containsExactlyInAnyOrder(Face.WEST, Face.SOUTH);
        assertThat(grid.connectedFaces(at(3, 64, 1))).containsExactlyInAnyOrder(Face.NORTH, Face.UP);
        assertThat(grid.connectedFaces(at(3, 65, 1))).containsExactlyInAnyOrder(Face.DOWN, Face.UP);
        assertThat(grid.connectedFaces(at(3, 66, 1))).containsExactlyInAnyOrder(Face.DOWN);
    }

    /**
     * The shapes come from the faces: a corner is a bend and a run is straight. The corner at (3, 64, 0) lies in the horizontal
     * plane, since both its faces are horizontal; the one at (3, 64, 1) lies in the vertical plane, since it joins up.
     */
    @Test
    void theBendModelIsChosenForTheCornersAndTheStraightModelForTheStraightRuns() {
        WaterGrid grid = path();

        assertThat(WaterOrientation.pipe(grid.connectedFaces(at(0, 64, 0))).shape())
                .isEqualTo(WaterOrientation.PipeShape.STRAIGHT);
        assertThat(WaterOrientation.pipe(grid.connectedFaces(at(1, 64, 0))).shape())
                .isEqualTo(WaterOrientation.PipeShape.STRAIGHT);
        assertThat(WaterOrientation.pipe(grid.connectedFaces(at(2, 64, 0))).shape())
                .isEqualTo(WaterOrientation.PipeShape.STRAIGHT);
        assertThat(WaterOrientation.pipe(grid.connectedFaces(at(3, 64, 0))).shape())
                .isEqualTo(WaterOrientation.PipeShape.BEND);
        assertThat(WaterOrientation.pipe(grid.connectedFaces(at(3, 64, 1))).shape())
                .isEqualTo(WaterOrientation.PipeShape.BEND);
        assertThat(WaterOrientation.pipe(grid.connectedFaces(at(3, 65, 1))).shape())
                .isEqualTo(WaterOrientation.PipeShape.STRAIGHT);
        assertThat(WaterOrientation.pipe(grid.connectedFaces(at(3, 66, 1))).shape())
                .isEqualTo(WaterOrientation.PipeShape.STRAIGHT);

        assertThat(grid.connectedFaces(at(3, 64, 0))).allMatch(Face::horizontal);
        assertThat(grid.connectedFaces(at(3, 64, 1))).contains(Face.UP);

        assertThat(OilModels.pipe(grid.connectedFaces(at(0, 64, 0)))).isEqualTo("oil_pipe");
        assertThat(OilModels.pipe(grid.connectedFaces(at(1, 64, 0)))).isEqualTo("oil_pipe");
        assertThat(OilModels.pipe(grid.connectedFaces(at(2, 64, 0)))).isEqualTo("oil_pipe");
        assertThat(OilModels.pipe(grid.connectedFaces(at(3, 64, 0)))).isEqualTo("oil_pipe_bend_south_west");
        assertThat(OilModels.pipe(grid.connectedFaces(at(3, 64, 1)))).isEqualTo("oil_pipe_bend_north_up");
        assertThat(OilModels.pipe(grid.connectedFaces(at(3, 65, 1)))).isEqualTo("oil_pipe_y");
        assertThat(OilModels.pipe(grid.connectedFaces(at(3, 66, 1)))).isEqualTo("oil_pipe_y");
    }

    /**
     * Fluid supplied at the west end is in the tank of every tube of the network, the east end included: the supply splits over the
     * seven tanks evenly, since they are equal, and a draw at the east end takes from all of them in proportion to what each holds.
     */
    @Test
    void fluidSuppliedAtOneEndIsDrawnAtTheOtherThroughTheTubes() {
        WaterGrid grid = path();
        assertThat(grid.networks()).hasSize(1);
        double capacity = OilSettings.from(ScenarioSupport.bundled("config.yml")).tubeCapacityLitres();
        assertThat(capacity).isEqualTo(100.0);

        List<FluidTank> tanks = new ArrayList<>();
        for (int i = 0; i < PATH.size(); i++) {
            tanks.add(new FluidTank(capacity));
        }
        OilNetwork network = new OilNetwork(tanks);
        FluidTank west = tanks.get(0);
        FluidTank east = tanks.get(tanks.size() - 1);

        assertThat(network.supply(Fluid.PETROLEUM, 150.0)).isCloseTo(150.0, within(1e-9));
        assertThat(west.litres()).isCloseTo(150.0 / PATH.size(), within(1e-9));
        assertThat(east.litres()).isCloseTo(150.0 / PATH.size(), within(1e-9));

        assertThat(network.draw(Fluid.PETROLEUM, 100.0)).isCloseTo(100.0, within(1e-9));
        assertThat(network.litres()).isCloseTo(50.0, within(1e-9));
        assertThat(east.litres()).isCloseTo(50.0 / PATH.size(), within(1e-9));

        assertThat(network.supply(Fluid.DIESEL, 10.0)).isZero();
        assertThat(network.supply(Fluid.PETROLEUM, 1000.0)).isCloseTo(650.0, within(1e-9));
        assertThat(network.litres()).isCloseTo(7 * capacity, within(1e-9));
    }

    /** Two pipe runs with a gap between them are two networks, and a petroleum tube and a diesel tube may not join. */
    @Test
    void aPetroleumTubeAndADieselTubeAreNotJoinedAndMayNotBe() {
        WaterGrid apart = WaterGrid.of(List.of(
                WaterNode.pipe(at(0, 64, 0)), WaterNode.pipe(at(1, 64, 0)),
                WaterNode.pipe(at(3, 64, 0)), WaterNode.pipe(at(4, 64, 0))));

        assertThat(apart.networks()).hasSize(2);
        assertThat(FluidRules.mayJoin(EnumSet.of(Fluid.PETROLEUM), EnumSet.of(Fluid.DIESEL))).isFalse();
        assertThat(FluidRules.mayJoin(EnumSet.of(Fluid.PETROLEUM), EnumSet.of(Fluid.PETROLEUM))).isTrue();
        assertThat(FluidRules.allowed(List.of(Fluid.PETROLEUM, Fluid.DIESEL))).isFalse();
    }
}
