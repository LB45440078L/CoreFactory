package io.corefactory.spigot.logistics;

import io.corefactory.core.logistics.BeltShape;
import io.corefactory.core.logistics.Dir;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class OrientationTest {

    @ParameterizedTest
    @EnumSource(Dir.class)
    void aBeltTurnedByItsYawHasItsFrontAlongItsFlow(Dir flow) {
        double[] front = Orientation.rotate(Orientation.beltYaw(flow), 0, 1);
        double[] expected = Orientation.vectorOf(flow);
        assertThat(front[0]).isCloseTo(expected[0], within(1e-9));
        assertThat(front[1]).isCloseTo(expected[1], within(1e-9));
    }

    /**
     * For every input side and both turns: the left corner's input port (+X) turns onto the input side, its front (+Z) turns onto the
     * output, which is the left of the item's travel; the right corner's input port (-X) does the same, with the output on the right.
     */
    @Test
    void aCornerTurnedByItsYawHasItsInputOnItsInputSideAndItsFrontOnItsTurn() {
        for (Dir input : Dir.values()) {
            Dir travel = input.opposite();
            assertCorner(BeltShape.CORNER_LEFT, new double[] {1, 0}, input, travel.turnLeft());
            assertCorner(BeltShape.CORNER_RIGHT, new double[] {-1, 0}, input, travel.turnRight());
        }
    }

    private static void assertCorner(BeltShape shape, double[] port, Dir input, Dir output) {
        double yaw = Orientation.cornerYaw(shape, input);
        double[] in = Orientation.rotate(yaw, port[0], port[1]);
        double[] front = Orientation.rotate(yaw, 0, 1);
        String where = shape + " from " + input;
        assertThat(in[0]).as(where + " input x").isCloseTo(Orientation.vectorOf(input)[0], within(1e-9));
        assertThat(in[1]).as(where + " input z").isCloseTo(Orientation.vectorOf(input)[1], within(1e-9));
        assertThat(front[0]).as(where + " output x").isCloseTo(Orientation.vectorOf(output)[0], within(1e-9));
        assertThat(front[1]).as(where + " output z").isCloseTo(Orientation.vectorOf(output)[1], within(1e-9));
    }

    /**
     * The corner tops followed from the texture to the world. A corner's arc joins the middle of its texture's right edge, where its
     * items come in, to the middle of its bottom edge, where they leave (BeltArt). The up face's uv puts its first pair at the model's
     * (+X, +Z) corner and its second at (-X, -Z), so the left corner's uv {16, 16, 0, 0} puts the texture's right edge on +X, the
     * input of a left corner, and the right corner's uv {0, 16, 16, 0} puts it on -X, the input of a right corner. The bottom edge is
     * the front (+Z). ResourcePackBuilderTest pins these uvs. For every input and both turns, the arc must enter on the input side
     * and leave on the output side.
     */
    @Test
    void aCornerArcEntersOnItsInputSideAndLeavesOnItsOutput() {
        for (Dir input : Dir.values()) {
            Dir travel = input.opposite();
            assertArc(BeltShape.CORNER_LEFT, new double[] {16, 16, 0, 0}, input, travel.turnLeft());
            assertArc(BeltShape.CORNER_RIGHT, new double[] {0, 16, 16, 0}, input, travel.turnRight());
        }
    }

    private static void assertArc(BeltShape shape, double[] uv, Dir input, Dir output) {
        double yaw = Orientation.cornerYaw(shape, input);
        double[] entryModel = topTexel(uv, 16, 8);
        double[] exitModel = topTexel(uv, 8, 16);
        double[] entry = Orientation.rotate(yaw, entryModel[0], entryModel[1]);
        double[] exit = Orientation.rotate(yaw, exitModel[0], exitModel[1]);
        String where = shape + " from " + input;
        assertThat(entry[0]).as(where + " entry x").isCloseTo(Orientation.vectorOf(input)[0], within(1e-9));
        assertThat(entry[1]).as(where + " entry z").isCloseTo(Orientation.vectorOf(input)[1], within(1e-9));
        assertThat(exit[0]).as(where + " exit x").isCloseTo(Orientation.vectorOf(output)[0], within(1e-9));
        assertThat(exit[1]).as(where + " exit z").isCloseTo(Orientation.vectorOf(output)[1], within(1e-9));
    }

    /** The model {x, z} of a texel (u, v) of an up face with uv {x1, y1, x2, y2}: x1 and y1 at +1, x2 and y2 at -1. */
    private static double[] topTexel(double[] uv, double u, double v) {
        return new double[] {1 - 2 * (u - uv[0]) / (uv[2] - uv[0]), 1 - 2 * (v - uv[1]) / (uv[3] - uv[1])};
    }

    @ParameterizedTest
    @EnumSource(Dir.class)
    void aPickerTurnedByItsYawHasItsNorthFaceToward(Dir side) {
        double[] north = Orientation.rotate(Orientation.northYaw(side), 0, -1);
        double[] expected = Orientation.vectorOf(side);
        assertThat(north[0]).isCloseTo(expected[0], within(1e-9));
        assertThat(north[1]).isCloseTo(expected[1], within(1e-9));
    }

    @Test
    void theVectorsOfTheFourDirectionsAreTheCompassVectors() {
        assertThat(Orientation.vectorOf(Dir.NORTH)).containsExactly(0, -1);
        assertThat(Orientation.vectorOf(Dir.EAST)).containsExactly(1, 0);
        assertThat(Orientation.vectorOf(Dir.SOUTH)).containsExactly(0, 1);
        assertThat(Orientation.vectorOf(Dir.WEST)).containsExactly(-1, 0);
    }
}
