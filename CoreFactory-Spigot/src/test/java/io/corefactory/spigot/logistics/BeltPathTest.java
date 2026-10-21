package io.corefactory.spigot.logistics;

import io.corefactory.core.logistics.BeltShape;
import io.corefactory.core.logistics.Dir;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BeltPathTest {

    private static void assertOffset(double[] offset, double x, double z) {
        assertThat(offset[0]).isCloseTo(x, within(1e-9));
        assertThat(offset[1]).isCloseTo(z, within(1e-9));
    }

    @Test
    void aStraightUnitRunsFromItsBackEdgeToItsFrontEdge() {
        assertOffset(BeltPath.offsetOn(BeltShape.STRAIGHT, Dir.EAST, Dir.WEST, 0.0), -0.5, 0.0);
        assertOffset(BeltPath.offsetOn(BeltShape.STRAIGHT, Dir.EAST, Dir.WEST, 0.5), 0.0, 0.0);
        assertOffset(BeltPath.offsetOn(BeltShape.STRAIGHT, Dir.EAST, Dir.WEST, 1.0), 0.5, 0.0);
    }

    @Test
    void aCornerRunsFromItsInputEdgeThroughTheCentreToItsOutputEdge() {
        double[] entry = BeltPath.offsetOn(BeltShape.CORNER_LEFT, Dir.EAST, Dir.NORTH, 0.0);
        double[] middle = BeltPath.offsetOn(BeltShape.CORNER_LEFT, Dir.EAST, Dir.NORTH, 0.5);
        double[] exit = BeltPath.offsetOn(BeltShape.CORNER_LEFT, Dir.EAST, Dir.NORTH, 1.0);
        assertThat(entry[0]).isCloseTo(0.0, within(1e-9));
        assertThat(entry[1]).isCloseTo(-0.5, within(1e-9));
        assertThat(middle[0]).isCloseTo(0.0, within(1e-9));
        assertThat(middle[1]).isCloseTo(0.0, within(1e-9));
        assertThat(exit[0]).isCloseTo(0.5, within(1e-9));
        assertThat(exit[1]).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void aPositionOutsideTheUnitIsClamped() {
        assertOffset(BeltPath.offsetOn(BeltShape.STRAIGHT, Dir.SOUTH, Dir.NORTH, 2.0), 0.0, 0.5);
        assertOffset(BeltPath.offsetOn(BeltShape.STRAIGHT, Dir.SOUTH, Dir.NORTH, -1.0), 0.0, -0.5);
    }

    @Test
    void eachShapeHasItsOwnModelAndTheRightCornerIsNotTheLeftOne() {
        assertThat(BeltPath.modelKeyOf("belt_1", BeltShape.STRAIGHT)).isEqualTo("belt/belt_1");
        assertThat(BeltPath.modelKeyOf("belt_1", BeltShape.CORNER_LEFT)).isEqualTo("belt/belt_1/corner");
        assertThat(BeltPath.modelKeyOf("belt_1", BeltShape.CORNER_RIGHT)).isEqualTo("belt/belt_1/corner_right");
    }

    /** The horizontal {x, z} that a belt display's transform turns the model vector (x, 0, z) to. */
    private static double[] displayed(Transformation transform, double x, double z) {
        Vector3f turned = transform.getLeftRotation().transform(new Vector3f((float) x, 0f, (float) z));
        return new double[] {turned.x, turned.z};
    }

    /** The same check as {@link #assertOffset}, with a tolerance for the single-precision display rotation. */
    private static void assertDisplayed(double[] turned, double x, double z) {
        assertThat(turned[0]).isCloseTo(x, within(1e-6));
        assertThat(turned[1]).isCloseTo(z, within(1e-6));
    }

    @Test
    void aStraightBeltDisplayFacesItsFlowInEveryDirection() {
        for (Dir flow : Dir.values()) {
            Transformation transform = LogisticsEntities.beltTransform(flow, BeltShape.STRAIGHT, flow.opposite());
            double[] expected = Orientation.vectorOf(flow);
            assertDisplayed(displayed(transform, 0, 1), expected[0], expected[1]);
        }
    }

    @Test
    void aLeftCornerUsesTheCornerModelAndARightCornerTheRightCornerModel() {
        for (Dir input : Dir.values()) {
            Dir travel = input.opposite();
            assertCornerDisplay(BeltShape.CORNER_LEFT, "belt/belt_1/corner", input, travel.turnLeft());
            assertCornerDisplay(BeltShape.CORNER_RIGHT, "belt/belt_1/corner_right", input, travel.turnRight());
        }
    }

    /** A corner's display takes its items in on the input side and turns them out toward its output, with its model chosen. */
    private static void assertCornerDisplay(BeltShape shape, String model, Dir input, Dir output) {
        String where = shape + " from " + input + " to " + output;
        assertThat(BeltPath.modelKeyOf("belt_1", shape)).as(where).isEqualTo(model);
        Transformation transform = LogisticsEntities.beltTransform(output, shape, input);
        double port = shape == BeltShape.CORNER_LEFT ? 1 : -1;
        double[] in = displayed(transform, port, 0);
        assertDisplayed(in, Orientation.vectorOf(input)[0], Orientation.vectorOf(input)[1]);
        double[] front = displayed(transform, 0, 1);
        assertDisplayed(front, Orientation.vectorOf(output)[0], Orientation.vectorOf(output)[1]);
    }

    @Test
    void noBeltDisplayIsMirrored() {
        for (BeltShape shape : BeltShape.values()) {
            for (Dir input : Dir.values()) {
                Transformation transform = LogisticsEntities.beltTransform(Dir.EAST, shape, input);
                assertThat(transform.getScale().x).as(shape + " from " + input).isEqualTo(1f);
                assertThat(transform.getScale().y).as(shape + " from " + input).isEqualTo(1f);
                assertThat(transform.getScale().z).as(shape + " from " + input).isEqualTo(1f);
            }
        }
    }
}
