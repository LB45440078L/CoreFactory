package io.corefactory.core.water;

import io.corefactory.core.energy.Pos;

import java.util.Arrays;
import java.util.Objects;

/**
 * The turn of a water model in the world. A model is drawn in its own frame: its +X runs along a straight pipe, or along a
 * splitter's input, its +Y is its up, and its +Z is the second arm of a bend or a splitter. The frame says which world face
 * each of those axes points to. The frame is right-handed, so the model's X crossed with its Y is its Z, and a model can be
 * turned to any pair of faces without being mirrored. Pure.
 *
 * @param x the world face the model's +X points to
 * @param y the world face the model's +Y points to
 * @param z the world face the model's +Z points to
 * @throws IllegalArgumentException when two axes share a line, or the axes are not right-handed
 */
public record WaterFrame(Face x, Face y, Face z) {

    public WaterFrame {
        Objects.requireNonNull(x, "x");
        Objects.requireNonNull(y, "y");
        Objects.requireNonNull(z, "z");
        if (axis(x) == axis(y) || axis(y) == axis(z) || axis(x) == axis(z)) {
            throw new IllegalArgumentException("the axes of a frame must be three different lines, got " + x + ", " + y + ", " + z);
        }
        if (!Arrays.equals(cross(vector(x), vector(y)), vector(z))) {
            throw new IllegalArgumentException("a frame must be right-handed, got " + x + ", " + y + ", " + z);
        }
    }

    /** The frame of an unturned model: X east, Y up, Z south. */
    public static WaterFrame identity() {
        return new WaterFrame(Face.EAST, Face.UP, Face.SOUTH);
    }

    /**
     * The frame with the given X and Z axes. The Y axis follows from them, so the frame is right-handed.
     *
     * @param x the world face the model's +X points to
     * @param z the world face the model's +Z points to, on another line than x
     */
    public static WaterFrame of(Face x, Face z) {
        Objects.requireNonNull(x, "x");
        Objects.requireNonNull(z, "z");
        Face y = faceOf(cross(vector(z), vector(x)));
        return new WaterFrame(x, y, z);
    }

    /**
     * A straight run along a face. A horizontal run is turned so that its model's up is the world up, and a vertical run keeps
     * its model's Z to the south.
     *
     * @param along the face the pipe runs toward at its +X end
     */
    public static WaterFrame straight(Face along) {
        Objects.requireNonNull(along, "along");
        Face z = along.horizontal() ? (along == Face.EAST || along == Face.WEST ? Face.SOUTH : Face.EAST) : Face.SOUTH;
        return upright(along, z);
    }

    /**
     * A 90 degree bend joining two faces. The bend is an arc, which is the same shape from either end, so the frame is the
     * one whose up is the world up when the bend lies in the horizontal plane.
     *
     * @param a the first face the bend joins; not opposite {@code b}
     * @param b the second face the bend joins
     * @throws IllegalArgumentException when the faces are opposite or the same
     */
    public static WaterFrame bend(Face a, Face b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        if (axis(a) == axis(b)) {
            throw new IllegalArgumentException("a bend joins two faces on different lines, got " + a + " and " + b);
        }
        WaterFrame first = of(a.opposite(), b);
        WaterFrame second = of(b.opposite(), a);
        return first.y() == Face.UP || second.y() != Face.UP ? first : second;
    }

    /**
     * A T splitter: its input is the model's -X, and its two outputs are the model's +Z and -Z, so the outputs are the faces on
     * either side of the output given. The model is turned so its up is the world up when the splitter lies in a horizontal plane.
     *
     * @param input  the face water enters from
     * @param output one of the two faces the water leaves to, on a line across the input
     */
    public static WaterFrame tee(Face input, Face output) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        if (axis(input) == axis(output)) {
            throw new IllegalArgumentException("a T's outputs lie across its input, got " + input + " and " + output);
        }
        return upright(input.opposite(), output);
    }

    /**
     * A cross splitter: its four arms are the model's X and Z, on the line of the given faces.
     *
     * @param arm    one of the arms
     * @param across the arm at a right angle to it
     */
    public static WaterFrame cross(Face arm, Face across) {
        Objects.requireNonNull(arm, "arm");
        Objects.requireNonNull(across, "across");
        if (axis(arm) == axis(across)) {
            throw new IllegalArgumentException("a cross's arms lie across each other, got " + arm + " and " + across);
        }
        return upright(arm, across);
    }

    /** The world face a model's canonical face points to, under this frame. */
    public Face map(Face canonical) {
        Pos step = canonical.step(new Pos(0, 0, 0));
        int[] sum = new int[3];
        addScaled(sum, vector(x), step.x());
        addScaled(sum, vector(y), step.y());
        addScaled(sum, vector(z), step.z());
        return faceOf(sum);
    }

    /** The frame with the same X and Z, and with Y pointing up when the axes allow it. */
    private static WaterFrame upright(Face x, Face z) {
        WaterFrame frame = of(x, z);
        return frame.y() == Face.DOWN ? of(x, z.opposite()) : frame;
    }

    private static int axis(Face face) {
        return face == Face.UP || face == Face.DOWN ? 1 : face == Face.EAST || face == Face.WEST ? 0 : 2;
    }

    private static int[] vector(Face face) {
        Pos step = face.step(new Pos(0, 0, 0));
        return new int[] {step.x(), step.y(), step.z()};
    }

    private static int[] cross(int[] a, int[] b) {
        return new int[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static void addScaled(int[] sum, int[] vector, int scale) {
        for (int i = 0; i < 3; i++) {
            sum[i] += vector[i] * scale;
        }
    }

    private static Face faceOf(int[] vector) {
        for (Face face : Face.values()) {
            if (Arrays.equals(vector(face), vector)) {
                return face;
            }
        }
        throw new IllegalArgumentException("not a unit axis: " + Arrays.toString(vector));
    }
}
