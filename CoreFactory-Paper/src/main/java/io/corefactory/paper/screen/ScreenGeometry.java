package io.corefactory.paper.screen;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.water.Face;

/**
 * The placement of a device's screen: a dark frame, a black panel in front of it and a text in front of the panel, all on one horizontal
 * face of the device. The face is the one toward the player who placed the device. The frame's back lies on the face of the anchor block,
 * so the screen sits flush on the device and does not float. The panel is 0.6 blocks wide, 0.75 tall and 0.02 thick. Each of the frame and
 * the panel has its transformation turn its +z axis to the front, and moves its own centre onto the point its entity stands at. Pure: the
 * numbers are tested without a server, and {@link DeviceScreen} turns them into entities.
 */
public final class ScreenGeometry {

    /** The panel's width along its own x axis, in blocks. */
    public static final double WIDTH = 0.6;

    /** The panel's height along its own y axis, in blocks. */
    public static final double HEIGHT = 0.75;

    /** The panel's thickness along its own z axis, in blocks. */
    public static final double THICKNESS = 0.02;

    /** How far the frame extends beyond the panel on each side, in blocks. */
    public static final double FRAME_BORDER = 0.04;

    /** The frame's width along its own x axis, in blocks: the panel's width and a border on each side. */
    public static final double FRAME_WIDTH = WIDTH + 2 * FRAME_BORDER;

    /** The frame's height along its own y axis, in blocks: the panel's height and a border on each side. */
    public static final double FRAME_HEIGHT = HEIGHT + 2 * FRAME_BORDER;

    /** The frame's thickness along its own z axis, in blocks. */
    public static final double FRAME_THICKNESS = 0.02;

    /**
     * How far the frame's centre lies from the centre of the anchor block, along the front, in blocks. The face of the block is 0.5 from
     * its centre, and the frame's back lies half its thickness behind its centre, so the back is 0.005 inside the block.
     */
    public static final double BEZEL_OFFSET = 0.505;

    /**
     * How far the panel's centre lies from the centre of the anchor block, along the front, in blocks. The panel's back lies on the frame's
     * front face, so the panel sits in the frame.
     */
    public static final double PANEL_OFFSET = 0.525;

    /**
     * How far the text lies from the centre of the anchor block, along the front, in blocks. The panel's front face is 0.535 from the
     * centre, so the text stands 0.005 in front of it.
     */
    public static final double TEXT_OFFSET = 0.54;

    /** The scale of the text, on all three axes. */
    public static final double TEXT_SCALE = 0.25;

    /**
     * The line width of the text, in the text's own pixels. At the text scale a pixel is 0.00625 blocks, so this width spans the panel's
     * 0.6 blocks.
     */
    public static final int LINE_WIDTH = 96;

    /** The panel's size in its own frame, before its scale. */
    private static final Vec PANEL_SIZE = new Vec(WIDTH, HEIGHT, THICKNESS);

    /** The frame's size in its own frame, before its scale. */
    private static final Vec FRAME_SIZE = new Vec(FRAME_WIDTH, FRAME_HEIGHT, FRAME_THICKNESS);

    private ScreenGeometry() {
    }

    /**
     * A point or a size in blocks.
     *
     * @param x the east component
     * @param y the up component
     * @param z the south component
     */
    public record Vec(double x, double y, double z) {
    }

    /**
     * Where the screen goes.
     *
     * @param panel            the world point the panel's centre lies on, and the point its entity stands at
     * @param text             the world point the text stands at
     * @param bezel            the world point the frame's centre lies on, and the point its entity stands at
     * @param yaw              the rotation about the vertical axis, in radians, that turns the panel's +z axis to the front
     * @param translation      the translation of the panel's transformation, which moves its centre onto the entity's point
     * @param bezelTranslation the translation of the frame's transformation, which moves its centre onto the entity's point
     */
    public record Layout(Vec panel, Vec text, Vec bezel, double yaw, Vec translation, Vec bezelTranslation) {
    }

    /**
     * The layout of a screen on a device.
     *
     * @param anchor the device's anchor block, the lower block
     * @param front  the horizontal face the screen is on
     * @param height the number of blocks the device stands, 1 or 2; a tall device's panel spans both
     * @throws IllegalArgumentException when the front is vertical
     */
    public static Layout of(Pos anchor, Face front, int height) {
        if (!front.horizontal()) {
            throw new IllegalArgumentException("a screen is on a horizontal face, got " + front);
        }
        Pos step = front.step(new Pos(0, 0, 0));
        double dx = step.x();
        double dz = step.z();
        double yaw = Math.atan2(dx, dz);
        Vec panel = frontPoint(anchor, height, dx, dz, PANEL_OFFSET);
        Vec text = frontPoint(anchor, height, dx, dz, TEXT_OFFSET);
        Vec bezel = frontPoint(anchor, height, dx, dz, BEZEL_OFFSET);
        return new Layout(panel, text, bezel, yaw, translationOf(PANEL_SIZE, yaw), translationOf(FRAME_SIZE, yaw));
    }

    /** The point on the device's centre line, the given distance from the centre of its anchor block along the front. */
    private static Vec frontPoint(Pos anchor, int height, double dx, double dz, double offset) {
        return new Vec(anchor.x() + 0.5 + offset * dx, anchor.y() + height / 2.0, anchor.z() + 0.5 + offset * dz);
    }

    /**
     * The translation of a box's transformation, which moves the box's centre onto its entity's point. The box is scaled and turned about
     * its corner, so its centre is turned and the translation is its negation.
     */
    private static Vec translationOf(Vec size, double yaw) {
        Vec turned = rotateY(yaw, new Vec(size.x() / 2, size.y() / 2, size.z() / 2));
        return new Vec(-turned.x(), -turned.y(), -turned.z());
    }

    /**
     * A vector turned about the vertical axis by a yaw, in the same sense as a rotation about +y: the rotation matrix maps
     * {@code (0, 0, 1)} to {@code (sin yaw, 0, cos yaw)}.
     *
     * @param yaw    the yaw in radians
     * @param vector the vector to turn
     */
    public static Vec rotateY(double yaw, Vec vector) {
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        return new Vec(vector.x() * cos + vector.z() * sin, vector.y(), -vector.x() * sin + vector.z() * cos);
    }
}
