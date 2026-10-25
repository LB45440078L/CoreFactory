package io.corefactory.core.water;

import io.corefactory.core.energy.Pos;

import java.util.Objects;
import java.util.Set;

/**
 * The turns of the water models. A pipe is drawn as a straight run or as a bend, in any plane; a splitter is drawn in the plane of
 * its input and its outputs; a pump is drawn with its front toward the player. A pipe or splitter is turned by a {@link WaterFrame},
 * which maps the model's own axes to world faces, so a model can lie in a horizontal or a vertical plane alike. A pump is turned by
 * a yaw, since its front is always horizontal.
 *
 * <p>The yaw of {@code a} maps a horizontal direction {@code (x, z)} to {@code (x cos a + z sin a, -x sin a + z cos a)}. Pure: the
 * caller applies the frame or the degrees to the display.
 */
public final class WaterOrientation {

    /** The shape of a pipe's model: a straight run along the frame's X axis, or a bend in the frame's X and Z plane. */
    public enum PipeShape {
        /** A straight run. */
        STRAIGHT,
        /** A 90 degree bend. */
        BEND,
        /**
         * Three or more connections: a hub in the middle with one arm to each connected face. The arms are drawn one by one, so every
         * connection shows.
         */
        JUNCTION
    }

    /**
     * How to draw a pipe: its shape, and the frame that turns its model.
     *
     * @param shape the shape of the model
     * @param frame the turn of the model
     */
    public record PipeModel(PipeShape shape, WaterFrame frame) {
    }

    private WaterOrientation() {
    }

    /**
     * The model of a pipe from the faces it connects on. Three or more connections make a junction, which draws every arm. Two connections
     * that are not opposite make a bend, in the plane they span, vertical or horizontal. Otherwise the pipe is straight on the axis of its
     * two connections, or along the east-west axis when it has fewer than two. A pipe with no connection is straight on the east-west axis.
     *
     * @param connections the faces where the pipe joins a neighbour
     */
    public static PipeModel pipe(Set<Face> connections) {
        Objects.requireNonNull(connections, "connections");
        if (connections.size() >= 3) {
            return new PipeModel(PipeShape.JUNCTION, WaterFrame.identity());
        }
        if (connections.size() == 2 && !isOpposite(connections)) {
            var pair = connections.stream().sorted().toList();
            return new PipeModel(PipeShape.BEND, WaterFrame.bend(pair.get(0), pair.get(1)));
        }
        int alongX = count(connections, Face.EAST, Face.WEST);
        int alongZ = count(connections, Face.NORTH, Face.SOUTH);
        int alongY = count(connections, Face.UP, Face.DOWN);
        Face along = Face.EAST;
        int best = alongX;
        if (alongZ > best) {
            along = Face.SOUTH;
            best = alongZ;
        }
        if (alongY > best) {
            along = Face.UP;
        }
        return new PipeModel(PipeShape.STRAIGHT, WaterFrame.straight(along));
    }

    /**
     * The default output of a T whose input is horizontal: the face across the input that the model's +Z points to. An input on the
     * east or west line gives south; an input on the north or south line gives east.
     *
     * @param input the horizontal input face
     * @throws IllegalArgumentException when the input is vertical
     */
    public static Face defaultOutput(Face input) {
        if (!input.horizontal()) {
            throw new IllegalArgumentException("a default output needs a horizontal input, got " + input);
        }
        return input == Face.EAST || input == Face.WEST ? Face.SOUTH : Face.EAST;
    }

    /**
     * The yaw that turns a model's horizontal face to a target face. Used for the pump, whose front is horizontal.
     *
     * @param modelFace the face the model has at its unturned position, horizontal
     * @param target    the face it must reach, horizontal
     * @return the yaw, 0, 90, 180 or 270
     * @throws IllegalArgumentException when either face is vertical
     */
    public static int yawOf(Face modelFace, Face target) {
        if (!modelFace.horizontal() || !target.horizontal()) {
            throw new IllegalArgumentException("a yaw turns horizontal faces only");
        }
        for (int yaw = 0; yaw < 360; yaw += 90) {
            if (turn(modelFace, yaw) == target) {
                return yaw;
            }
        }
        throw new IllegalStateException("no quarter turn takes " + modelFace + " to " + target);
    }

    /**
     * The side a splitter's input is on, when it is placed by a player: the side facing the player, opposite to where the player looks.
     * A splitter placed while the player looks east takes its input from the west.
     *
     * @param playerYaw the player's yaw in degrees, as the server reports it
     */
    public static Face inputFromLook(double playerYaw) {
        double radians = Math.toRadians(playerYaw);
        return dominantFace(Math.sin(radians), 0.0, -Math.cos(radians));
    }

    /**
     * The horizontal face the player looks toward, from the player's yaw: the face a vertical splitter's outputs lie across.
     *
     * @param playerYaw the player's yaw in degrees, as the server reports it
     */
    public static Face lookFace(double playerYaw) {
        return inputFromLook(playerYaw).opposite();
    }

    /**
     * The face of a block a direction points through: the axis with the largest component, vertical first on a tie, then east-west.
     *
     * @param dx the east component
     * @param dy the up component
     * @param dz the south component
     */
    public static Face dominantFace(double dx, double dy, double dz) {
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);
        if (ay >= ax && ay >= az) {
            return dy >= 0 ? Face.UP : Face.DOWN;
        }
        if (ax >= az) {
            return dx >= 0 ? Face.EAST : Face.WEST;
        }
        return dz >= 0 ? Face.SOUTH : Face.NORTH;
    }

    /**
     * Turns a face about the vertical axis by a yaw of a multiple of 90 degrees. A vertical face stays as it is.
     *
     * @param face the face to turn
     * @param yaw  the yaw in degrees, a multiple of 90; any sign
     */
    public static Face turn(Face face, int yaw) {
        if (!face.horizontal()) {
            return face;
        }
        int quarter = Math.floorMod(yaw, 360) / 90;
        int cos = quarter == 0 ? 1 : quarter == 2 ? -1 : 0;
        int sin = quarter == 1 ? 1 : quarter == 3 ? -1 : 0;
        Pos vector = face.step(new Pos(0, 0, 0));
        int x = vector.x() * cos + vector.z() * sin;
        int z = -vector.x() * sin + vector.z() * cos;
        for (Face candidate : Face.values()) {
            Pos step = candidate.step(new Pos(0, 0, 0));
            if (candidate.horizontal() && step.x() == x && step.z() == z) {
                return candidate;
            }
        }
        throw new IllegalStateException("no horizontal face points to " + x + ", " + z);
    }

    private static int count(Set<Face> faces, Face a, Face b) {
        return (faces.contains(a) ? 1 : 0) + (faces.contains(b) ? 1 : 0);
    }

    private static boolean isOpposite(Set<Face> pair) {
        return pair.size() == 2 && pair.stream().allMatch(face -> pair.contains(face.opposite()));
    }
}
