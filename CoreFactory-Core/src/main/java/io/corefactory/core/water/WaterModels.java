package io.corefactory.core.water;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The model ids of the water system. Every orientation of a pipe, a splitter and a pump has a model of its own, with the orientation
 * baked into its geometry, so a placed block is drawn with no display rotation at all: the model's faces are the world's faces. The
 * resource pack generates these models with these ids (see the art of the water system), and a test checks that every id here exists there.
 *
 * <p>Naming: faces are lower-case, {@code north}, {@code south}, {@code east}, {@code west}, {@code up}, {@code down}. A pair of faces
 * is named with the one earlier in {@link Face} first. Pure.
 */
public final class WaterModels {

    /** The hub of a junction, which is the same in every direction. */
    public static final String HUB = "water_pipe_hub";

    /** The pump's levels, one to five. */
    public static final int LEVELS = PumpLevels.MAX_LEVEL;

    private WaterModels() {
    }

    /**
     * The model of a pipe from the faces it connects on: a straight run, a bend between its two faces, or the junction hub, which is the
     * model {@link WaterOrientation#pipe} gives, named.
     *
     * @param connections the faces where the pipe joins a neighbour
     */
    public static String pipe(Set<Face> connections) {
        WaterOrientation.PipeModel model = WaterOrientation.pipe(connections);
        return switch (model.shape()) {
            case STRAIGHT -> straight(model.frame().x());
            case BEND -> {
                List<Face> pair = connections.stream().sorted().toList();
                yield bend(pair.get(0), pair.get(1));
            }
            case JUNCTION -> HUB;
        };
    }

    /** The model of a straight pipe along an axis: {@code water_pipe} along east-west, {@code water_pipe_z} along north-south, {@code water_pipe_y} up and down. */
    public static String straight(Face along) {
        return switch (along) {
            case EAST, WEST -> "water_pipe";
            case NORTH, SOUTH -> "water_pipe_z";
            case UP, DOWN -> "water_pipe_y";
        };
    }

    /** The model of a bend joining two faces that are not opposite, such as {@code water_pipe_bend_south_west}. */
    public static String bend(Face a, Face b) {
        requireBend(a, b);
        Face first = a.ordinal() < b.ordinal() ? a : b;
        Face second = first == a ? b : a;
        return "water_pipe_bend_" + token(first) + "_" + token(second);
    }

    /** The model of one arm of a junction, such as {@code water_pipe_arm_east}. */
    public static String arm(Face face) {
        return "water_pipe_arm_" + token(face);
    }

    /**
     * The model of a T splitter: its input face, and the face of its outputs that comes first in {@link Face}, such as
     * {@code water_splitter_t_up_east}, which has its input from above and outputs to the east and west. A T with the other pair of
     * outputs is the same model with the other output named, so the first one names the pair.
     *
     * @param input  the face water enters from
     * @param output one of the two outputs, on a line across the input
     */
    public static String tee(Face input, Face output) {
        if (input == output || input == output.opposite()) {
            throw new IllegalArgumentException("a T's outputs lie across its input, got " + input + " and " + output);
        }
        Face first = output.ordinal() < output.opposite().ordinal() ? output : output.opposite();
        return "water_splitter_t_" + token(input) + "_" + token(first);
    }

    /**
     * The model of a cross splitter, named by the plane its arms lie in: {@code water_splitter_x_xz} horizontally, {@code xy} with
     * arms east-west and up-down, {@code zy} with arms north-south and up-down.
     *
     * @param arm    one of the arms
     * @param across the arm at a right angle to it
     */
    public static String cross(Face arm, Face across) {
        if (axis(arm) == axis(across)) {
            throw new IllegalArgumentException("a cross's arms lie across each other, got " + arm + " and " + across);
        }
        int a = axis(arm);
        int b = axis(across);
        String plane = (a == 0 || b == 0) && (a == 2 || b == 2) ? "xz"
                : (a == 0 || b == 0) ? "xy" : "zy";
        return "water_splitter_x_" + plane;
    }

    /**
     * The model of a pump at a level with its front toward a horizontal face, such as {@code water_pump_3_north}. Level four and five
     * also have an upper block, named by {@link #pumpTop}.
     *
     * @param level the pump's level, 1 to 5
     * @param front the horizontal face the front looks toward
     */
    public static String pump(int level, Face front) {
        requireLevel(level);
        requireHorizontal(front);
        return "water_pump_" + level + "_" + token(front);
    }

    /** The model of the upper block of a tall pump, which is the lower model's id with {@code _top} added. */
    public static String pumpTop(int level, Face front) {
        return pump(level, front) + "_top";
    }

    /** Every model id of the water system, in the order the resource pack lists them. Pure. */
    public static List<String> all() {
        List<String> ids = new ArrayList<>();
        ids.add("water_pipe");
        ids.add("water_pipe_z");
        ids.add("water_pipe_y");
        ids.add(HUB);
        for (Face a : Face.values()) {
            for (Face b : Face.values()) {
                if (a.ordinal() < b.ordinal() && a != b.opposite()) {
                    ids.add(bend(a, b));
                }
            }
        }
        for (Face face : Face.values()) {
            ids.add(arm(face));
        }
        for (Face input : Face.values()) {
            for (Face output : Face.values()) {
                if (output != input && output != input.opposite() && output.ordinal() < output.opposite().ordinal()) {
                    ids.add(tee(input, output));
                }
            }
        }
        ids.add("water_splitter_x_xz");
        ids.add("water_splitter_x_xy");
        ids.add("water_splitter_x_zy");
        for (int level = 1; level <= LEVELS; level++) {
            for (Face front : new Face[] {Face.NORTH, Face.SOUTH, Face.EAST, Face.WEST}) {
                ids.add(pump(level, front));
                if (PumpLevels.height(level) > 1) {
                    ids.add(pumpTop(level, front));
                }
            }
        }
        return List.copyOf(ids);
    }

    private static String token(Face face) {
        return face.name().toLowerCase(Locale.ROOT);
    }

    /** The axis of a face: 0 for east-west, 1 for up-down, 2 for north-south. */
    private static int axis(Face face) {
        return switch (face) {
            case EAST, WEST -> 0;
            case UP, DOWN -> 1;
            case NORTH, SOUTH -> 2;
        };
    }

    private static void requireBend(Face a, Face b) {
        if (a == b || a == b.opposite()) {
            throw new IllegalArgumentException("a bend joins two faces that are not opposite, got " + a + " and " + b);
        }
    }

    private static void requireLevel(int level) {
        if (level < 1 || level > LEVELS) {
            throw new IllegalArgumentException("pump level must be 1 to " + LEVELS + ", got " + level);
        }
    }

    private static void requireHorizontal(Face face) {
        if (!face.horizontal()) {
            throw new IllegalArgumentException("a pump's front is horizontal, got " + face);
        }
    }
}
