package io.corefactory.paper.turret;

import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The displays of a rail gun, as transformations. The body turns about the vertical axis through the middle of its block, the barrel turns
 * with it and tilts about its trunnion, the top of level III turns with the body and does not tilt, and the screen, a text on the body's
 * front, turns with the body too. The models are the resource pack's {@code RailgunArt}, which share the artillery's frame: each one is
 * written from the floor of its frame, so every display is lifted one block, and level III's top stands two blocks above the body. A model's
 * front faces north, toward -z, so a display turns its model by {@code 180 - yaw}, as the artillery does ({@code ArtilleryPose}).
 *
 * <p>The numbers are the resource pack's, which the plugin does not compile against: the trunnion heights, the lift, the top's offset and the
 * screen rectangle. Pure, so the layout is tested without a server.
 */
final class RailgunPose {

    /** The lift of every rail gun display, in blocks: each model is written from one block below its frame's floor. */
    static final double LIFT_BLOCKS = 1.0;

    /** How far above the body the top of level III stands, in blocks. */
    static final double TOP_OFFSET_BLOCKS = 2.0;

    /** The height of a display's origin above the floor of its block at rest, in blocks: the centre of the block, lifted. */
    static final double ORIGIN_BLOCKS = 0.5 + LIFT_BLOCKS;

    /** The height of each level's trunnion above the floor of its block, in blocks, from level I to III. */
    private static final double[] TRUNNION_BLOCKS = {0.75, 1.25, 1.625};

    /**
     * The screen in the body's model units, as {@code RailgunArt.SCREEN}: x0, y0, x1, y1 and the plane z it lies on. The y values are in the
     * body's own frame, which is the level unit less one block, so the screen's height in level units is y plus 16.
     */
    static final double[] SCREEN_MODEL_UNITS = {4.5, -13, 11.5, -7, 2};

    /** The model units in one block. */
    static final double UNITS_PER_BLOCK = 16;

    /** The unit at the middle of a model's frame, which is the origin of its display. */
    static final double MODEL_CENTRE_UNITS = 8;

    /** The width of one pixel of text at text scale 1, in blocks. The same as the command center's screen. */
    static final double PIXEL_BLOCKS = 0.025;

    /** The line width of the screen's text, in pixels: at the text scale it spans the screen's width. The command center's width. */
    static final int LINE_WIDTH = 200;

    /** How far the text stands in front of the screen's plane, toward the viewer, in blocks. */
    static final double TEXT_IN_FRONT_BLOCKS = 0.005;

    private RailgunPose() {
    }

    /**
     * The rotation about the vertical axis that a rail gun's display gives its model, in degrees: the model's front faces -z, so the turn is
     * {@code 180 - yaw}.
     *
     * @param yaw the body's yaw, in the game's convention, where 0 faces south
     */
    static double displayYaw(double yaw) {
        return 180.0 - yaw;
    }

    /**
     * The transformation of the body, or of the top of level III: a turn about the vertical axis through the origin, with no translation.
     *
     * @param yaw   the body's yaw
     * @param scale the scale of the display
     */
    static Transformation body(double yaw, float scale) {
        return new Transformation(new Vector3f(), new Quaternionf().rotationY((float) Math.toRadians(displayYaw(yaw))),
                new Vector3f(scale, scale, scale), new Quaternionf());
    }

    /**
     * The transformation of a barrel: it turns with the body and tilts to the elevation about its trunnion. The trunnion stays where it rests,
     * so the translation is the pivot's offset minus the rotated pivot.
     *
     * @param level the level of the rail gun, from 1 to 3
     * @param yaw   the body's yaw
     * @param pitch the barrel's pitch in the turret's convention, positive when it looks down; its elevation is the negative
     * @param scale the scale of the display
     */
    static Transformation barrel(int level, double yaw, double pitch, float scale) {
        Quaternionf rotation = new Quaternionf().rotationY((float) Math.toRadians(displayYaw(yaw)))
                .rotateX((float) Math.toRadians(-pitch));
        double pivot = trunnionBlocks(level) - ORIGIN_BLOCKS;
        Vector3f turned = rotation.transform(new Vector3f(0f, (float) pivot, 0f));
        Vector3f translation = new Vector3f(-turned.x, (float) pivot - turned.y, -turned.z);
        return new Transformation(translation, rotation, new Vector3f(scale, scale, scale), new Quaternionf());
    }

    /**
     * The transformation of the screen's text: the text stands on the body's front, turned with the body, and faces the front. The text's own
     * turn is a half turn, as the command center's is, so its front lies on the body's front; with the body's turn, the total is a turn of
     * {@code -yaw}.
     *
     * @param yaw   the body's yaw
     * @param scale the scale of the body's display, which moves the text out with the model
     */
    static Transformation screen(double yaw, float scale) {
        // A model's offset from its display's origin is its unit less the centre of the frame, which is eight units, over sixteen.
        double centre = ((SCREEN_MODEL_UNITS[1] + SCREEN_MODEL_UNITS[3]) / 2 - MODEL_CENTRE_UNITS) / UNITS_PER_BLOCK;
        double front = (SCREEN_MODEL_UNITS[4] - MODEL_CENTRE_UNITS) / UNITS_PER_BLOCK - TEXT_IN_FRONT_BLOCKS;
        Quaternionf body = new Quaternionf().rotationY((float) Math.toRadians(displayYaw(yaw)));
        Vector3f offset = body.transform(new Vector3f(0f, (float) centre, (float) front));
        Quaternionf text = new Quaternionf().rotationY((float) Math.toRadians(-yaw));
        return new Transformation(new Vector3f(scale * offset.x, scale * offset.y, scale * offset.z), text,
                new Vector3f((float) textScale(), (float) textScale(), (float) textScale()), new Quaternionf());
    }

    /**
     * The text scale that makes {@link #LINE_WIDTH} pixels span the screen's width. A text's pixel is {@link #PIXEL_BLOCKS} at scale 1.
     */
    static double textScale() {
        return screenWidth() / (LINE_WIDTH * PIXEL_BLOCKS);
    }

    /** The screen's width in blocks: its units across, over the units of a block. */
    static double screenWidth() {
        return (SCREEN_MODEL_UNITS[2] - SCREEN_MODEL_UNITS[0]) / UNITS_PER_BLOCK;
    }

    /**
     * The height of a level's trunnion above the floor of its block, in blocks.
     *
     * @param level the level, from 1 to 3
     */
    static double trunnionBlocks(int level) {
        return TRUNNION_BLOCKS[level - 1];
    }
}
