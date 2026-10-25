package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Which item displays make up an artillery piece, and where each one stands, in blocks above the piece's block. A piece has a body, which turns
 * to the aim, and a barrel, which turns with the body and tilts about its trunnion. Level VII also has an upper model, which stands three blocks
 * above the base and turns with the body but does not tilt. Every artillery model is written from the floor of its frame, so each display is lifted
 * one block. The displays keep their models' own heights: no display is scaled by the height of its level. Pure, so the layout is tested without a
 * server.
 *
 * <p>The numbers are those of the resource pack's {@code ArtilleryArt}, which the plugin does not compile against. A test checks that they agree
 * with it.
 */
public final class ArtilleryDisplays {

    /** The model of level VII's upper part. It has no item sprite of its own. */
    public static final String TOP_MODEL = "artillery_7_top";

    /** The barrel model of level VII, which lies in the upper part, three blocks above the base. */
    public static final String LEVEL_SEVEN_BARREL = "artillery_7_barrel";

    /** The lift of every artillery model, in blocks: each model's frame starts one block below its block's centre. */
    static final double LIFT_BLOCKS = 1.0;

    /** How far above the base the upper parts of level VII stand, in blocks: the base covers three blocks. */
    static final double TOP_OFFSET_BLOCKS = 3.0;

    /**
     * The height of each level's trunnion above the floor of its block, in blocks, from level I to VII. It is the middle of the barrel's
     * cross-section: the top of the level less 4 units, less half the barrel's width.
     */
    private static final double[] TRUNNION_BLOCKS = {1.125, 1.09375, 2.09375, 2.0625, 2.0625, 2.53125, 3.25};

    /** The part of a piece that a display shows. */
    public enum Part {
        /** The carriage, the turret and the base: turns to the aim. */
        BODY,
        /** The barrel and its breech: turns with the body and tilts to the elevation about its trunnion. */
        BARREL,
        /** The upper model of level VII: turns with the body and does not tilt. */
        TOP
    }

    private ArtilleryDisplays() {
    }

    /**
     * One display of a piece.
     *
     * @param model             the model's id, such as {@code artillery_3_body}; its item model key is {@code machine/} followed by this id
     * @param part              the part of the piece the display shows
     * @param translationBlocks how far the display's origin is moved up from the centre of the piece's block, in blocks, at rest
     * @param trunnionBlocks    for a barrel, the height of its trunnion above the floor of the piece's block, in blocks; 0 for the other parts
     */
    public record Layer(String model, Part part, double translationBlocks, double trunnionBlocks) {
    }

    /**
     * The displays of a piece at a level, the body first: the body and the barrel for every level, and the upper model for level VII.
     *
     * @param level the piece's level
     * @return the displays, the body first
     */
    public static List<Layer> layers(ArtilleryLevel level) {
        String piece = ArtilleryIds.itemOf(level);
        String barrel = piece + "_barrel";
        List<Layer> layers = new ArrayList<>();
        layers.add(new Layer(piece + "_body", Part.BODY, translationOf(piece + "_body"), 0.0));
        layers.add(new Layer(barrel, Part.BARREL, translationOf(barrel), TRUNNION_BLOCKS[level.number() - 1]));
        if (level == ArtilleryLevel.VII) {
            layers.add(new Layer(TOP_MODEL, Part.TOP, translationOf(TOP_MODEL), 0.0));
        }
        return layers;
    }

    /**
     * How far a model's display is lifted, in blocks.
     *
     * @param model the model's id
     * @return one block for every artillery model
     * @throws IllegalArgumentException when the model is not an artillery model
     */
    public static double lift(String model) {
        if (!model.startsWith(ArtilleryIds.ITEM_PREFIX)) {
            throw new IllegalArgumentException("Not an artillery model: " + model);
        }
        return LIFT_BLOCKS;
    }

    /**
     * How far above the base a model stands, in blocks: 3 for the upper model and the barrel of level VII, and 0 for every other model.
     *
     * @param model the model's id
     * @return the offset in blocks
     */
    public static double offset(String model) {
        return TOP_MODEL.equals(model) || LEVEL_SEVEN_BARREL.equals(model) ? TOP_OFFSET_BLOCKS : 0.0;
    }

    /** The translation of a model's display at rest: its lift plus its offset, in blocks above the centre of its block. */
    static double translationOf(String model) {
        return lift(model) + offset(model);
    }
}
