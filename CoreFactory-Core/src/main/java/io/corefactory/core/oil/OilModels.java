package io.corefactory.core.oil;

import io.corefactory.core.water.Face;
import io.corefactory.core.water.WaterModels;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The model ids of the oil tubes. An oil tube has the shapes and the orientation rules of a water pipe: a straight run, a bend, a hub
 * with arms, a T and a cross. Its models are the water models with the prefix changed from {@code water_} to {@code oil_}, and they are
 * re-skinned dark with orange accents by the resource pack. There are no pumps: an oil tube takes oil from the drills and machines it
 * joins. Pure.
 */
public final class OilModels {

    private static final String WATER = "water_";
    private static final String OIL = "oil_";

    private OilModels() {
    }

    /** The oil model of a water model id: the same shape with the oil prefix. */
    public static String of(String waterId) {
        if (!waterId.startsWith(WATER)) {
            throw new IllegalArgumentException("not a water pipe model: " + waterId);
        }
        return OIL + waterId.substring(WATER.length());
    }

    /**
     * The oil model of a pipe from the faces it connects on: a straight run, a bend or the junction hub.
     *
     * @param connections the faces where the pipe joins a neighbour
     * @see WaterModels#pipe
     */
    public static String pipe(Set<Face> connections) {
        return of(WaterModels.pipe(connections));
    }

    /**
     * The oil model of one arm of a junction.
     *
     * @param face the face the arm reaches
     */
    public static String arm(Face face) {
        return of(WaterModels.arm(face));
    }

    /**
     * The oil model of a T splitter.
     *
     * @param input  the face oil enters from
     * @param output one of the two outputs, on a line across the input
     * @see WaterModels#tee
     */
    public static String tee(Face input, Face output) {
        return of(WaterModels.tee(input, output));
    }

    /**
     * The oil model of a cross splitter.
     *
     * @param arm    one of the arms
     * @param across the arm at a right angle to it
     * @see WaterModels#cross
     */
    public static String cross(Face arm, Face across) {
        return of(WaterModels.cross(arm, across));
    }

    /** Every oil tube model: the water pipe models without the pumps, with the oil prefix, in the water order. */
    public static List<String> all() {
        List<String> ids = new ArrayList<>();
        for (String id : WaterModels.all()) {
            if (!id.startsWith("water_pump")) {
                ids.add(of(id));
            }
        }
        return List.copyOf(ids);
    }
}
