package io.corefactory.resourcepack.art;

import java.util.List;

/**
 * The vertical transport belt, levels I to III: a tower block of one level's colour, with a steel shaft, a rail on each side
 * and a cap. Its north face is the animated arrow strip. The arrows are chevrons whose tips point up, and each frame moves them
 * up two pixels, so the eight frames climb one 16 pixel period and the strip loops.
 *
 * <p>The frame time is the belt's pace: level I changes frame every two ticks, and levels II and III every tick.
 */
public final class VerticalBeltArt {

    /** The distance between two arrows on the face, in canvas pixels. */
    private static final int PERIOD = 16;

    /** How far the arrows climb each frame, in canvas pixels: one period over the eight frames, two pixels a frame. */
    private static final int CLIMB = PERIOD / MachineAnimation.FRAMES;

    private static final ComponentArt.Painter NONE = (c, base, accent, seed) -> {
    };

    /** The tower of each level: steel with white arrows, copper with orange arrows, and gold with yellow arrows. */
    public static final List<MachineArt.Spec> ALL = List.of(
            new MachineArt.Spec("vertical_belt_1", "#5A6470", "#E8ECF2", towerBoxes(), NONE),
            new MachineArt.Spec("vertical_belt_2", "#B87333", "#FFB070", towerBoxes(), NONE),
            new MachineArt.Spec("vertical_belt_3", "#C9A03A", "#FFE070", towerBoxes(), NONE));

    /** The animated north face of each level: the frame time sets the belt's pace. */
    public static final List<MachineAnimation.Clip> CLIPS = List.of(
            new MachineAnimation.Clip("vertical_belt_1", MachineArt.Face.NORTH, 2),
            new MachineAnimation.Clip("vertical_belt_2", MachineArt.Face.NORTH, 1),
            new MachineAnimation.Clip("vertical_belt_3", MachineArt.Face.NORTH, 1));

    /** The inventory sprite of each level: a small tower block in the level's colour, with an up arrow on it. */
    public static final List<ComponentArt.Spec> SPRITES = List.of(
            new ComponentArt.Spec("vertical_belt_1", "#5A6470", "#E8ECF2", VerticalBeltArt::towerSprite),
            new ComponentArt.Spec("vertical_belt_2", "#B87333", "#FFB070", VerticalBeltArt::towerSprite),
            new ComponentArt.Spec("vertical_belt_3", "#C9A03A", "#FFE070", VerticalBeltArt::towerSprite));

    private VerticalBeltArt() {
    }

    /** Whether an animated face of this class belongs to the machine id. */
    static boolean owns(String id) {
        return CLIPS.stream().anyMatch(clip -> clip.id().equals(id));
    }

    /**
     * The tower's cuboids: a foot, the shaft that runs the full height, a rail on the west and on the east, and a cap. The rails
     * are thin across the block's depth and stand beside the shaft at the middle of the depth.
     */
    private static List<MachineArt.Box> towerBoxes() {
        return List.of(
                new MachineArt.Box(3, 0, 3, 13, 1, 13),
                new MachineArt.Box(4, 1, 4, 12, 15, 12),
                new MachineArt.Box(1, 1, 7, 4, 15, 9),
                new MachineArt.Box(12, 1, 7, 15, 15, 9),
                new MachineArt.Box(3, 15, 3, 13, 16, 13));
    }

    /**
     * Paints one frame of the arrow strip over the shaft's recess: chevrons with their tips up, climbing {@link #CLIMB} pixels a
     * frame. The chevrons are drawn on each period above and below the face, so the face has no gap at its edges.
     */
    static void paintFrame(Canvas c, MachineArt.Spec spec, int frame) {
        int base = Color.hex(spec.base());
        int arrow = Color.hex(spec.accent());
        int climb = CLIMB * frame;
        c.paint(Area.rect(9, 0, 23, Canvas.SIZE), Color.scale(base, 0.45));
        for (int period = -1; period <= 1; period++) {
            double tip = 24 + PERIOD * period - climb;
            c.paint(Area.line(16, tip, 9, tip + 7, 2), arrow);
            c.paint(Area.line(16, tip, 23, tip + 7, 2), arrow);
        }
    }

    /** The inventory sprite: a tower block with a cap, two rails and an up arrow, in the level's colour. */
    private static void towerSprite(Canvas c, int base, int accent, long seed) {
        Area shaft = Area.roundRect(10, 5, 22, 28, 2);
        Area rails = Area.rect(7, 8, 10, 26).or(Area.rect(22, 8, 25, 26));
        Sprite.body(c, shaft.or(rails), base, seed);
        c.paint(Area.rect(7, 8, 10, 26), Color.scale(base, 0.7));
        c.paint(Area.rect(22, 8, 25, 26), Color.scale(base, 0.7));
        c.paint(Area.roundRect(8, 3, 24, 7, 1.5), accent);
        c.paint(Area.line(16, 12, 11, 17, 1.8), accent);
        c.paint(Area.line(16, 12, 21, 17, 1.8), accent);
        c.paint(Area.line(16, 12, 16, 24, 1.8), accent);
    }
}
