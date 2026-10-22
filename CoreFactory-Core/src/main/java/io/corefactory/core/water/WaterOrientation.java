package io.corefactory.core.water;

import io.corefactory.core.energy.Pos;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * The turns of the water models. A model is drawn along one axis, or as a bend in the horizontal plane, or as a T or cross in the
 * horizontal plane; the placed block is turned to match its connections. Yaws are degrees about the vertical axis, quarter turns
 * only, and rolls are degrees about the north-south axis, for the vertical pipe.
 *
 * <p>The turns follow the display transform: a yaw of {@code a} maps a horizontal direction {@code (x, z)} to
 * {@code (x cos a + z sin a, -x sin a + z cos a)}. Pure: the caller applies the degrees to the display.
 */
public final class WaterOrientation {

    /** The shape of a pipe's model: a straight run on one axis, or a bend. */
    public enum PipeShape {
        /** Straight along the east-west axis: the model as drawn. */
        STRAIGHT_X,
        /** Straight along the north-south axis: the model turned a quarter by yaw. */
        STRAIGHT_Z,
        /** Straight up and down: the model rolled a quarter. */
        STRAIGHT_Y,
        /** A 90 degree bend in the horizontal plane. */
        BEND
    }

    /**
     * How to draw a pipe: its shape, and its yaw and roll in degrees.
     *
     * @param shape the shape of the model
     * @param yaw   the yaw about the vertical axis, a multiple of 90
     * @param roll  the roll about the north-south axis, 0 or 90
     */
    public record PipeModel(PipeShape shape, int yaw, int roll) {
    }

    private WaterOrientation() {
    }

    /**
     * The model of a pipe from the faces it connects on. A corner of two horizontal faces with no vertical connection is a bend. Otherwise
     * the pipe is straight on the axis with the most connections, and on the first axis in the order east-west, north-south, up-down when
     * the counts tie. A pipe with no connection is straight on the east-west axis.
     *
     * @param connections the faces where the pipe joins a neighbour
     */
    public static PipeModel pipe(Set<Face> connections) {
        Objects.requireNonNull(connections, "connections");
        int alongX = count(connections, Face.EAST, Face.WEST);
        int alongZ = count(connections, Face.NORTH, Face.SOUTH);
        int alongY = count(connections, Face.UP, Face.DOWN);
        EnumSet<Face> horizontal = EnumSet.noneOf(Face.class);
        for (Face face : connections) {
            if (face.horizontal()) {
                horizontal.add(face);
            }
        }
        if (alongY == 0 && horizontal.size() == 2 && !isOpposite(horizontal)) {
            return new PipeModel(PipeShape.BEND, bendYaw(horizontal), 0);
        }
        PipeShape shape = PipeShape.STRAIGHT_X;
        int best = alongX;
        if (alongZ > best) {
            shape = PipeShape.STRAIGHT_Z;
            best = alongZ;
        }
        if (alongY > best) {
            shape = PipeShape.STRAIGHT_Y;
        }
        return switch (shape) {
            case STRAIGHT_X -> new PipeModel(shape, 0, 0);
            case STRAIGHT_Z -> new PipeModel(shape, 90, 0);
            case STRAIGHT_Y -> new PipeModel(shape, 0, 90);
            case BEND -> throw new IllegalStateException("a bend is not a straight run");
        };
    }

    /**
     * The yaw that turns a bend's model so its open ends are the two given faces. The model's open ends are west and south.
     *
     * @param ends the two horizontal faces the bend joins
     * @return the yaw, 0, 90, 180 or 270
     * @throws IllegalArgumentException when the faces are not a horizontal corner
     */
    public static int bendYaw(Set<Face> ends) {
        if (ends.size() != 2 || isOpposite(ends) || !ends.stream().allMatch(Face::horizontal)) {
            throw new IllegalArgumentException("a bend joins two horizontal faces that are not opposite, got " + ends);
        }
        for (int yaw = 0; yaw < 360; yaw += 90) {
            EnumSet<Face> turned = EnumSet.of(turn(Face.WEST, yaw), turn(Face.SOUTH, yaw));
            if (turned.equals(EnumSet.copyOf(ends))) {
                return yaw;
            }
        }
        throw new IllegalStateException("no quarter turn joins " + ends);
    }

    /**
     * The yaw that turns a model's horizontal face to a target face.
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
