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
     * The faces a block of this kind may connect on, with a splitter's output chosen by default: for a horizontal input the
     * output is the face {@link WaterOrientation#defaultOutput} gives. A pipe, a pump and a consumer accept all six faces; a
     * consumer's node narrows that to its own attach faces. A T accepts its input and the two faces across it. A cross accepts
     * four faces, two on its input's line and two on the line across.
     *
     * @param kind      the block's kind
     * @param inputFace the face a splitter takes water from; ignored for other kinds
     * @return the allowed faces, unmodifiable
     * @throws IllegalArgumentException when a splitter's input face is missing or vertical
     */
    public static Set<Face> allowedFaces(WaterKind kind, Face inputFace) {
        return switch (kind) {
            case SPLITTER_T, SPLITTER_X -> {
                Face input = requireInput(kind, inputFace);
                yield allowedFaces(kind, input, WaterOrientation.defaultOutput(input));
            }
            default -> allowedFaces(kind, inputFace, null);
        };
    }

    /**
     * The faces a block of this kind may connect on, with a splitter's output axis given. A T takes water on its input and sends it on
     * to the two faces across the input, which are its output and the output's opposite. A cross has two arms on the line of its input
     * and two on the line of its output. Pipes, pumps and consumers ignore the faces given.
     *
     * @param kind       the block's kind
     * @param inputFace  a splitter's input face; for other kinds ignored
     * @param outputFace a splitter's output face, on another line than the input; for other kinds ignored
     * @return the allowed faces, unmodifiable
     * @throws IllegalArgumentException when a splitter's input or output is missing or on the input's line
     */
    public static Set<Face> allowedFaces(WaterKind kind, Face inputFace, Face outputFace) {
        Objects.requireNonNull(kind, "kind");
        return switch (kind) {
            case PIPE, PUMP, CONSUMER -> Collections.unmodifiableSet(EnumSet.allOf(Face.class));
            case SPLITTER_T -> {
                Face input = requireInput(kind, inputFace);
                Face output = requireOutput(kind, input, outputFace);
                yield Collections.unmodifiableSet(EnumSet.of(input, output, output.opposite()));
            }
            case SPLITTER_X -> {
                Face input = requireInput(kind, inputFace);
                Face output = requireOutput(kind, input, outputFace);
                yield Collections.unmodifiableSet(EnumSet.of(input, input.opposite(), output, output.opposite()));
            }
        };
    }

    /**
     * True when two kinds may join at all. Transport kinds (pipe, pump, splitters) join each other. A consumer joins a
     * pipe or a splitter; a splitter joins its consumer on an output face only (see {@link #connects}).
     */
    public static boolean kindsConnect(WaterKind a, WaterKind b) {
        if (a == WaterKind.CONSUMER || b == WaterKind.CONSUMER) {
            WaterKind other = a == WaterKind.CONSUMER ? b : a;
            return other == WaterKind.PIPE || other == WaterKind.SPLITTER_T || other == WaterKind.SPLITTER_X;
        }
        return true;
    }

    /**
     * True when {@code node} joins {@code neighbour} across {@code face}. The neighbour must sit on that face, the kinds
     * must join, and both blocks must allow the shared face: the node on {@code face}, the neighbour on the opposite face.
     * A consumer never joins a splitter on the splitter's input face, because the water would then flow into the
     * consumer and not out of the splitter.
     */
    public static boolean connects(WaterNode node, Face face, WaterNode neighbour) {
        return neighbour.pos().equals(face.step(node.pos()))
                && kindsConnect(node.kind(), neighbour.kind())
                && node.faces().contains(face)
                && neighbour.faces().contains(face.opposite())
                && !consumerOnSplitterInput(node, face, neighbour);
    }

    /** True when a consumer and a splitter meet on the splitter's input face. */
    private static boolean consumerOnSplitterInput(WaterNode node, Face face, WaterNode neighbour) {
        if (node.kind() == WaterKind.CONSUMER && isSplitter(neighbour.kind())) {
            return neighbour.inputFace() == face.opposite();
        }
        if (neighbour.kind() == WaterKind.CONSUMER && isSplitter(node.kind())) {
            return node.inputFace() == face;
        }
        return false;
    }

    private static boolean isSplitter(WaterKind kind) {
        return kind == WaterKind.SPLITTER_T || kind == WaterKind.SPLITTER_X;
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

    private static Face requireInput(WaterKind kind, Face inputFace) {
        if (inputFace == null) {
            throw new IllegalArgumentException(kind + " needs an input face");
        }
        return inputFace;
    }

    private static Face requireOutput(WaterKind kind, Face input, Face outputFace) {
        if (outputFace == null || outputFace.opposite() == input || outputFace == input) {
            throw new IllegalArgumentException(kind + " needs an output face across its input " + input + ", got " + outputFace);
        }
        return outputFace;
    }
}
