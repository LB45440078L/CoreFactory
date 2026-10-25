package io.corefactory.core.water;

import io.corefactory.core.energy.Pos;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * A block of a water network: its position, its kind, a splitter's input and output faces, and the faces a neighbour may join it on.
 * Build one with the factory methods, which fill the faces from the kind.
 *
 * @param pos        the block's position
 * @param kind       the block's kind
 * @param inputFace  a splitter's input face; null for other kinds
 * @param outputFace a splitter's output face, on another line than the input; null for other kinds, and for a splitter given no output
 * @param faces      the faces a neighbour may join on
 * @throws IllegalArgumentException when the faces do not match the kind, or the input and output faces do not fit it
 */
public record WaterNode(Pos pos, WaterKind kind, Face inputFace, Face outputFace, Set<Face> faces) {

    public WaterNode {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(faces, "faces");
        EnumSet<Face> copy = EnumSet.noneOf(Face.class);
        copy.addAll(faces);
        if (kind == WaterKind.CONSUMER) {
            if (inputFace != null || outputFace != null) {
                throw new IllegalArgumentException("a consumer has no input or output face");
            }
        } else if (!kind.isSplitter()) {
            if (inputFace != null || outputFace != null) {
                throw new IllegalArgumentException(kind + " has no input or output face");
            }
            if (!copy.equals(WaterRules.allowedFaces(kind, null, null))) {
                throw new IllegalArgumentException(kind + " at " + pos + " has faces that do not match its kind");
            }
        } else {
            if (!copy.equals(WaterRules.allowedFaces(kind, inputFace, outputFace))) {
                throw new IllegalArgumentException(kind + " at " + pos + " has faces that do not match its kind");
            }
        }
        faces = Collections.unmodifiableSet(copy);
    }

    /**
     * A node with a splitter's output chosen by default, which needs a horizontal input.
     *
     * @param pos       the block's position
     * @param kind      the block's kind
     * @param inputFace the input face, horizontal
     * @param faces     the faces a neighbour may join on
     */
    public WaterNode(Pos pos, WaterKind kind, Face inputFace, Set<Face> faces) {
        this(pos, kind, inputFace, inputFace == null || !inputFace.horizontal() ? null : WaterOrientation.defaultOutput(inputFace), faces);
    }

    /** A pipe, which joins on all six faces. */
    public static WaterNode pipe(Pos pos) {
        return new WaterNode(pos, WaterKind.PIPE, null, null, EnumSet.allOf(Face.class));
    }

    /** A pump, which joins on all six faces. */
    public static WaterNode pump(Pos pos) {
        return new WaterNode(pos, WaterKind.PUMP, null, null, EnumSet.allOf(Face.class));
    }

    /**
     * A splitter whose output follows from its input: a horizontal input gets the default output.
     *
     * @param kind      SPLITTER_T or SPLITTER_X
     * @param inputFace the face water enters from, horizontal
     */
    public static WaterNode splitter(Pos pos, WaterKind kind, Face inputFace) {
        if (!Objects.requireNonNull(kind, "kind").isSplitter()) {
            throw new IllegalArgumentException(kind + " is not a splitter");
        }
        Face output = inputFace == null || !inputFace.horizontal() ? null : WaterOrientation.defaultOutput(inputFace);
        return splitter(pos, kind, inputFace, output);
    }

    /**
     * A splitter with its input and output faces given. Its faces follow from both.
     *
     * @param kind       SPLITTER_T or SPLITTER_X
     * @param inputFace  the face water enters from
     * @param outputFace the face across the input that the splitter's second line runs along
     */
    public static WaterNode splitter(Pos pos, WaterKind kind, Face inputFace, Face outputFace) {
        if (!Objects.requireNonNull(kind, "kind").isSplitter()) {
            throw new IllegalArgumentException(kind + " is not a splitter");
        }
        return new WaterNode(pos, kind, inputFace, outputFace, WaterRules.allowedFaces(kind, inputFace, outputFace));
    }

    /**
     * A consumer, a machine or generator that takes water.
     *
     * @param faces the faces where a pipe may attach
     */
    public static WaterNode consumer(Pos pos, Set<Face> faces) {
        return new WaterNode(pos, WaterKind.CONSUMER, null, null, faces);
    }
}
