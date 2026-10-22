package io.corefactory.core.water;

import io.corefactory.core.energy.Pos;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * A block of a water network: its position, its kind, the face a splitter takes water from, and the faces a neighbour
 * may join it on. Build one with the factory methods, which fill the faces from the kind.
 *
 * @param pos       the block's position
 * @param kind      the block's kind
 * @param inputFace a splitter's input face, horizontal; null for other kinds
 * @param faces     the faces a neighbour may join on
 * @throws IllegalArgumentException when the faces do not match the kind, or the input face does not fit it
 */
public record WaterNode(Pos pos, WaterKind kind, Face inputFace, Set<Face> faces) {

    public WaterNode {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(faces, "faces");
        EnumSet<Face> copy = EnumSet.noneOf(Face.class);
        copy.addAll(faces);
        if (kind == WaterKind.CONSUMER) {
            if (inputFace != null) {
                throw new IllegalArgumentException("a consumer has no input face");
            }
        } else {
            if (!kind.isSplitter() && inputFace != null) {
                throw new IllegalArgumentException(kind + " has no input face");
            }
            if (!copy.equals(WaterRules.allowedFaces(kind, inputFace))) {
                throw new IllegalArgumentException(kind + " at " + pos + " has faces that do not match its kind");
            }
        }
        faces = Collections.unmodifiableSet(copy);
    }

    /** A pipe, which joins on all six faces. */
    public static WaterNode pipe(Pos pos) {
        return new WaterNode(pos, WaterKind.PIPE, null, EnumSet.allOf(Face.class));
    }

    /** A pump, which joins on all six faces. */
    public static WaterNode pump(Pos pos) {
        return new WaterNode(pos, WaterKind.PUMP, null, EnumSet.allOf(Face.class));
    }

    /**
     * A splitter. Its faces follow from the kind and the input face.
     *
     * @param kind      SPLITTER_T or SPLITTER_X
     * @param inputFace the horizontal face water enters from
     */
    public static WaterNode splitter(Pos pos, WaterKind kind, Face inputFace) {
        if (!Objects.requireNonNull(kind, "kind").isSplitter()) {
            throw new IllegalArgumentException(kind + " is not a splitter");
        }
        return new WaterNode(pos, kind, inputFace, WaterRules.allowedFaces(kind, inputFace));
    }

    /**
     * A consumer, a machine or generator that takes water.
     *
     * @param faces the faces where a pipe may attach
     */
    public static WaterNode consumer(Pos pos, Set<Face> faces) {
        return new WaterNode(pos, WaterKind.CONSUMER, null, faces);
    }
}
