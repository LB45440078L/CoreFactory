package io.corefactory.core.water;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * The connection and nuclear rules of the water network. Every method is pure: it reads what it is given and returns
 * an answer.
 */
public final class WaterRules {

    private WaterRules() {
    }

    /**
     * The faces a block of this kind may connect on. A pipe, a pump and a consumer accept all six faces; a consumer's
     * node narrows that to its own attach faces. A T splitter accepts its input and the two horizontal faces
     * perpendicular to the input, its left and right arms. A cross accepts the four horizontal faces, so the input's
     * opposite face is one of its outputs. A splitter's input must be horizontal, because its models lie in the
     * horizontal plane.
     *
     * @param kind      the block's kind
     * @param inputFace the face a splitter takes water from; ignored for other kinds
     * @return the allowed faces, unmodifiable
     * @throws IllegalArgumentException when a splitter's input face is missing or vertical
     */
    public static Set<Face> allowedFaces(WaterKind kind, Face inputFace) {
        Objects.requireNonNull(kind, "kind");
        return switch (kind) {
            case PIPE, PUMP, CONSUMER -> Collections.unmodifiableSet(EnumSet.allOf(Face.class));
            case SPLITTER_T -> {
                Face input = horizontalInput(kind, inputFace);
                EnumSet<Face> faces = input == Face.NORTH || input == Face.SOUTH
                        ? EnumSet.of(Face.EAST, Face.WEST)
                        : EnumSet.of(Face.NORTH, Face.SOUTH);
                faces.add(input);
                yield Collections.unmodifiableSet(faces);
            }
            case SPLITTER_X -> {
                horizontalInput(kind, inputFace);
                yield Collections.unmodifiableSet(EnumSet.of(Face.NORTH, Face.SOUTH, Face.EAST, Face.WEST));
            }
        };
    }

    /**
     * True when two kinds may join at all. Transport kinds (pipe, pump, splitters) join each other. A consumer joins
     * only a pipe.
     */
    public static boolean kindsConnect(WaterKind a, WaterKind b) {
        if (a == WaterKind.CONSUMER || b == WaterKind.CONSUMER) {
            return (a == WaterKind.PIPE && b == WaterKind.CONSUMER) || (a == WaterKind.CONSUMER && b == WaterKind.PIPE);
        }
        return true;
    }

    /**
     * True when {@code node} joins {@code neighbour} across {@code face}. The neighbour must sit on that face, the kinds
     * must join, and both blocks must allow the shared face: the node on {@code face}, the neighbour on the opposite face.
     */
    public static boolean connects(WaterNode node, Face face, WaterNode neighbour) {
        return neighbour.pos().equals(face.step(node.pos()))
                && kindsConnect(node.kind(), neighbour.kind())
                && node.faces().contains(face)
                && neighbour.faces().contains(face.opposite());
    }

    /**
     * Whether a nuclear generator may run. It runs on the underwater rule, which needs its own block to be water and
     * the caller's underwater flag. It also runs when a pipe is attached to it and the pipe's network delivers water.
     *
     * @param ownBlockIsWater     true when the generator's own block is water
     * @param underwaterRuleMet   the caller's flag for the underwater rule
     * @param pipeAttached        true when a pipe joins the generator on any face
     * @param networkDelivered    units per second the network delivers to its consumers
     * @return true when the generator may run
     */
    public static boolean nuclearMayRun(boolean ownBlockIsWater, boolean underwaterRuleMet,
                                        boolean pipeAttached, double networkDelivered) {
        return (ownBlockIsWater && underwaterRuleMet) || (pipeAttached && networkDelivered > 0);
    }

    private static Face horizontalInput(WaterKind kind, Face inputFace) {
        if (inputFace == null || !inputFace.horizontal()) {
            throw new IllegalArgumentException(kind + " needs a horizontal input face, got " + inputFace);
        }
        return inputFace;
    }
}
