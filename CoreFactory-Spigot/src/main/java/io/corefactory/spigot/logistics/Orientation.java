package io.corefactory.spigot.logistics;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.BeltShape;
import io.corefactory.core.logistics.Dir;

/**
 * The turns of the logistics models, as yaw angles in degrees about the vertical axis. The belt and machine models face +Z
 * (their front) and -Z (their north face, the input side of a picker or a splitter). A rotation by {@code yaw} turns a model
 * vector (x, z) to ({@code x cos + z sin}, {@code -x sin + z cos}), the same rotation a display applies. Pure.
 *
 * <p>A straight belt flows toward its front (+Z). A corner's front (+Z) is its output, and its input is on a side: the +X edge
 * of the left corner and the -X edge of the right corner. These ports are the only place the corner models' layout is written.
 */
public final class Orientation {

    /** The model vector of a left corner's input side: its +X edge. */
    private static final double[] LEFT_CORNER_INPUT = {1, 0};

    /** The model vector of a right corner's input side: its -X edge. */
    private static final double[] RIGHT_CORNER_INPUT = {-1, 0};

    private Orientation() {
    }

    /** The yaw that turns a belt's front (+Z) to point along its flow. */
    public static double beltYaw(Dir flow) {
        double[] v = vectorOf(flow);
        return Math.toDegrees(Math.atan2(v[0], v[1]));
    }

    /**
     * The yaw of a corner's model, so its input port faces the side its items come from. The yaw is the one that turns the port's
     * model vector onto the side's vector, which also turns the model's front (+Z) to the output.
     *
     * @param shape the corner, {@link BeltShape#CORNER_LEFT} or {@link BeltShape#CORNER_RIGHT}
     * @param input the side the corner's items come from
     * @return the yaw in degrees
     */
    public static double cornerYaw(BeltShape shape, Dir input) {
        double[] port = shape == BeltShape.CORNER_LEFT ? LEFT_CORNER_INPUT : RIGHT_CORNER_INPUT;
        double[] v = vectorOf(input);
        return Math.toDegrees(Math.atan2(port[1], port[0]) - Math.atan2(v[1], v[0]));
    }

    /** The yaw that turns the north face (-Z) of a picker or splitter model to point toward a side. */
    public static double northYaw(Dir side) {
        double[] v = vectorOf(side);
        return Math.toDegrees(Math.atan2(-v[0], -v[1]));
    }

    /**
     * Applies the rotation of a yaw to a horizontal vector.
     *
     * @return the rotated {x, z}
     */
    public static double[] rotate(double yawDegrees, double x, double z) {
        double angle = Math.toRadians(yawDegrees);
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        return new double[] {x * cos + z * sin, -x * sin + z * cos};
    }

    /** The horizontal unit vector of a direction, as {x, z}. */
    public static double[] vectorOf(Dir dir) {
        Pos step = dir.step(new Pos(0, 0, 0));
        return new double[] {step.x(), step.z()};
    }
}
