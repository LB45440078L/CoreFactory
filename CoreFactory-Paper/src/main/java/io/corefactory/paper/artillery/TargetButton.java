package io.corefactory.paper.artillery;

import java.util.Optional;

/**
 * The buttons drawn on the Command Center's map, with the rectangle each one takes in pixels. The rectangles are the single table the
 * click test reads, and the map draws its buttons from the same numbers. A rectangle includes its left and top pixels and excludes its right
 * and bottom ones. Pure, so the hit test is tested without a server.
 */
public enum TargetButton {

    /** Zooms in: one zoom level closer. */
    ZOOM_IN(112, 4, 124, 16, "+"),

    /** Zooms out: one zoom level wider. */
    ZOOM_OUT(112, 19, 124, 31, "-"),

    /** Moves the centre north, a quarter of the view. */
    NORTH(26, 84, 38, 96, "N"),

    /** Moves the centre west, a quarter of the view. */
    WEST(12, 94, 24, 106, "W"),

    /** Moves the centre east, a quarter of the view. */
    EAST(40, 94, 52, 106, "E"),

    /** Moves the centre south, a quarter of the view. */
    SOUTH(26, 104, 38, 116, "S"),

    /** Opens the coordinates dialog, which sets the centre. */
    COORDINATES(4, 4, 60, 16, "COORDS"),

    /** Confirms the target: the centre becomes the aim, and the map stays on the screen. Drawn as a green tick. */
    CONFIRM(96, 116, 124, 127, ""),

    /** Cancels: the target is not stored. */
    CANCEL(4, 116, 30, 127, "X");

    private final int x0;
    private final int y0;
    private final int x1;
    private final int y1;
    private final String label;

    TargetButton(int x0, int y0, int x1, int y1, String label) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.label = label;
    }

    /** The first pixel column of the button. */
    public int x0() {
        return x0;
    }

    /** The first pixel row of the button. */
    public int y0() {
        return y0;
    }

    /** The pixel column after the button's last one. */
    public int x1() {
        return x1;
    }

    /** The pixel row after the button's last one. */
    public int y1() {
        return y1;
    }

    /** The text drawn on the button, or an empty text for a button drawn with a shape. */
    public String label() {
        return label;
    }

    /** The pixel column at the middle of the button, which lies inside it. */
    public int centreX() {
        return (x0 + x1) / 2;
    }

    /** The pixel row at the middle of the button, which lies inside it. */
    public int centreY() {
        return (y0 + y1) / 2;
    }

    /**
     * The button under a pixel, if any.
     *
     * @param px the pixel column
     * @param py the pixel row
     * @return the button, or empty when the pixel is on no button
     */
    public static Optional<TargetButton> at(int px, int py) {
        for (TargetButton button : values()) {
            if (px >= button.x0 && px < button.x1 && py >= button.y0 && py < button.y1) {
                return Optional.of(button);
            }
        }
        return Optional.empty();
    }
}
