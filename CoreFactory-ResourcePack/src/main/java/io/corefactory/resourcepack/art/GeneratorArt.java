package io.corefactory.resourcepack.art;

import java.util.List;
import java.util.Map;

/**
 * The power devices: the five generators, each two blocks tall, and the solar panel, one block. The lower part of a
 * generator has the device id and the upper part has that id with {@code _top}. Each part spans one block, so the upper
 * part sits directly on the lower one. The animated upper parts are the wind rotor, the charcoal stack, the petroleum
 * pump rocker, the diesel firebox, the nuclear Cherenkov window, and the solar glint.
 */
public final class GeneratorArt {

    private static final int FIRE_RED = Color.hex("#E8461A");
    private static final int FIRE_TIP = Color.hex("#FFB040");
    private static final int DEEP = Color.hex("#140804");
    private static final int LIGHT_BLUE = Color.hex("#9FE0FF");
    private static final int GOLD = Color.hex("#FFD040");
    private static final int STEEL_WHITE = Color.hex("#F4F5F7");
    private static final ComponentArt.Painter NONE = (c, base, accent, seed) -> {
    };

    /** The generators and the solar panel. */
    public static final List<MachineArt.Spec> ALL = List.of(
            new MachineArt.Spec("wind_generator", "#E4E8EE", "#3A8FD8", List.of(
                    new MachineArt.Box(3, 0, 3, 13, 2, 13),
                    new MachineArt.Box(5, 2, 5, 11, 16, 11)), GeneratorArt::windFront),
            new MachineArt.Spec("wind_generator_top", "#E4E8EE", "#3A8FD8", List.of(
                    new MachineArt.Box(3, 0, 4, 13, 13, 14),
                    new MachineArt.Box(7, 10, 13, 9, 16, 15)), NONE),
            new MachineArt.Spec("charcoal_generator", "#4A3A30", "#FF7A20", List.of(
                    new MachineArt.Box(1, 0, 1, 15, 16, 15)), GeneratorArt::charcoalFront),
            new MachineArt.Spec("charcoal_generator_top", "#4A3A30", "#FF7A20", List.of(
                    new MachineArt.Box(3, 0, 3, 13, 2, 13),
                    new MachineArt.Box(4, 0, 4, 12, 16, 12)), NONE),
            new MachineArt.Spec("petroleum_generator", "#2A2F38", "#E0A030", List.of(
                    new MachineArt.Box(1, 0, 2, 15, 16, 14)), GeneratorArt::tankFront),
            new MachineArt.Spec("petroleum_generator_top", "#2A2F38", "#E0A030", List.of(
                    new MachineArt.Box(0, 0, 0, 16, 1, 16),
                    new MachineArt.Box(2, 1, 2, 14, 12, 14),
                    new MachineArt.Box(6, 12, 6, 10, 16, 10)), NONE),
            new MachineArt.Spec("diesel_generator", "#3A4048", "#E89020", List.of(
                    new MachineArt.Box(0, 0, 2, 16, 16, 14)), GeneratorArt::grilleFront),
            new MachineArt.Spec("diesel_generator_top", "#3A4048", "#E89020", List.of(
                    new MachineArt.Box(1, 0, 2, 15, 15, 14),
                    new MachineArt.Box(3, 15, 4, 13, 16, 12)), NONE),
            new MachineArt.Spec("nuclear_generator", "#2A3A4A", "#3AA8FF", List.of(
                    new MachineArt.Box(1, 0, 1, 15, 1, 15),
                    new MachineArt.Box(2, 1, 2, 14, 16, 14)), GeneratorArt::containmentFront),
            new MachineArt.Spec("nuclear_generator_top", "#2A3A4A", "#3AA8FF", List.of(
                    new MachineArt.Box(2, 0, 2, 14, 12, 14),
                    new MachineArt.Box(4, 12, 4, 12, 16, 12)), NONE),
            new MachineArt.Spec("solar_panel", "#C4CBD4", "#2A6FD8", List.of(
                    new MachineArt.Box(3, 0, 3, 13, 2, 13),
                    new MachineArt.Box(7, 2, 7, 9, 8, 9),
                    new MachineArt.Box(0, 8, 3, 16, 10, 13),
                    new MachineArt.Box(0, 10, 1, 16, 12, 15)), GeneratorArt::solarFront));

    /** The animated upper parts of the generators, and the solar panel's glint. */
    public static final List<MachineAnimation.Clip> CLIPS = List.of(
            new MachineAnimation.Clip("wind_generator_top", MachineArt.Face.NORTH, 2),
            new MachineAnimation.Clip("charcoal_generator_top", MachineArt.Face.NORTH, 3),
            new MachineAnimation.Clip("petroleum_generator_top", MachineArt.Face.NORTH, 3),
            new MachineAnimation.Clip("diesel_generator_top", MachineArt.Face.NORTH, 2),
            new MachineAnimation.Clip("nuclear_generator_top", MachineArt.Face.NORTH, 4),
            new MachineAnimation.Clip("solar_panel", MachineArt.Face.UP, 4));

    private GeneratorArt() {
    }

    /** Whether an animated face of this class belongs to the machine id. */
    static boolean owns(String id) {
        return CLIPS.stream().anyMatch(clip -> clip.id().equals(id));
    }

    /** Paints the animated detail of one frame of a generator's upper part or of the solar panel. */
    static void paintFrame(Canvas c, MachineArt.Spec spec, int frame) {
        int base = Color.hex(spec.base());
        int accent = Color.hex(spec.accent());
        switch (spec.id()) {
            case "wind_generator_top" -> rotor(c, base, accent, frame);
            case "charcoal_generator_top" -> stack(c, base, frame);
            case "petroleum_generator_top" -> rocker(c, base, accent, frame);
            case "diesel_generator_top" -> firebox(c, base, accent, frame);
            case "nuclear_generator_top" -> cherenkov(c, accent, frame);
            case "solar_panel" -> glint(c, accent, frame);
            default -> throw new IllegalArgumentException("No animation for " + spec.id());
        }
    }

    /** The lower block of a wind generator: a door in the tower, with a band of the accent colour across the top. */
    private static void windFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(0, 4, Canvas.SIZE, 7), accent);
        c.paint(Area.roundRect(11, 12, 21, 30, 2), Color.hex("#2A323C"));
        c.paint(Area.rect(13, 15, 19, 22), Color.withAlpha(accent, 0.5));
        c.paint(Area.rect(13, 25, 19, 26), Color.scale(base, 0.5));
    }

    /** The lower block of a charcoal generator: a firebox door with grate bars. */
    private static void charcoalFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(0, 2, Canvas.SIZE, 4), accent);
        c.paint(Area.roundRect(8, 9, 24, 27, 2), DEEP);
        for (double y = 12; y <= 22; y += 3) {
            c.paint(Area.rect(10, y, 22, y + 0.8), Color.scale(base, 0.45));
        }
        c.paint(Area.rect(0, 28, Canvas.SIZE, 30), accent);
    }

    /** The lower block of a petroleum generator: a tank with two pipe bands and a pressure gauge. */
    private static void tankFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(0, 6, Canvas.SIZE, 8), accent);
        c.paint(Area.rect(0, 22, Canvas.SIZE, 24), accent);
        c.paint(Area.disc(16, 15, 4.5), Color.hex("#14120C"));
        c.paint(Area.ring(16, 15, 4.5, 1.2), accent);
        c.paint(Area.line(16, 15, 19, 12, 0.9), STEEL_WHITE);
    }

    /** The lower block of a diesel generator: a grille of louvres and an accent stripe at each end. */
    private static void grilleFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(0, 2, Canvas.SIZE, 3), accent);
        for (double x = 6; x <= 26; x += 4) {
            c.paint(Area.rect(x, 5, x + 1.5, 27), Color.scale(base, 0.5));
        }
        c.paint(Area.rect(0, 29, Canvas.SIZE, 31), accent);
    }

    /** The lower block of a nuclear generator: a containment porthole and a hazard band. */
    private static void containmentFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.disc(16, 14, 8), STEEL_WHITE);
        c.paint(Area.ring(16, 14, 8, 1.6), accent);
        c.paint(Area.disc(16, 14, 4), Color.withAlpha(accent, 0.4));
        c.paint(Area.rect(0, 24, Canvas.SIZE, 26), accent);
        c.paint(Area.rect(0, 29, Canvas.SIZE, Canvas.SIZE), Color.scale(base, 0.5));
    }

    /** The static front of the solar panel: a grid of cells and the sun pictogram. */
    private static void solarFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(3, 6, 29, 26), Color.scale(accent, 0.7));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                double x = 5 + 6 * col;
                double y = 8 + 6 * row;
                c.paint(Area.rect(x, y, x + 5, y + 5), accent);
            }
        }
        Icon.SUN.draw(c, 26, 3, 3, GOLD, Color.hex("#FFF2B0"));
    }

    /**
     * The rotor of a wind generator, on the nacelle's front. The blades turn 15 degrees a frame, so the eight frames make
     * one third of a turn, which is the period of the three blades: the strip loops.
     */
    private static void rotor(Canvas c, int base, int accent, int frame) {
        c.paint(Area.rect(0, 27, Canvas.SIZE, 30), Color.scale(base, 0.5));
        double turn = frame * Math.PI / 12;
        for (int k = 0; k < 3; k++) {
            double a = -Math.PI / 2 + turn + k * 2 * Math.PI / 3;
            c.paint(Area.line(16, 14, 16 + Math.cos(a) * 12, 14 + Math.sin(a) * 12, 2.6), accent);
        }
        c.paint(Area.disc(16, 14, 3), STEEL_WHITE);
    }

    /**
     * The face of a charcoal generator's stack: flames at the foot, with grey smoke wisps rising through the stack. Each
     * wisp climbs 3 pixels a frame across 24 pixels, so the eight frames loop.
     */
    private static void stack(Canvas c, int base, int frame) {
        for (double y = 8; y <= 24; y += 8) {
            c.paint(Area.rect(4, y, 28, y + 1), Color.scale(base, 0.5));
        }
        MachineAnimation.flames(c, Area.rect(0, 18, Canvas.SIZE, Canvas.SIZE), 6, 26, 32, 12, frame, FIRE_RED, FIRE_TIP);
        for (int k = 0; k < 3; k++) {
            double x = 9 + 7 * k;
            double y = 22 - ((frame * 3 + k * 8) % 24);
            c.paint(Area.disc(x, y, 3), Color.withAlpha(Color.WHITE, 0.45));
            c.paint(Area.disc(x + 1, y - 4, 2.2), Color.withAlpha(Color.WHITE, 0.3));
        }
    }

    /**
     * The pump rocker of a petroleum generator: a beam on a pivot that rocks through 0.35 radians, one end up while the
     * other goes down. The sine makes the eight frames loop.
     */
    private static void rocker(Canvas c, int base, int accent, int frame) {
        c.paint(Area.rect(14.5, 14, 17.5, 28), Color.scale(base, 0.6));
        double angle = 0.35 * Math.sin(2 * Math.PI * frame / MachineAnimation.FRAMES);
        double dx = Math.cos(angle) * 12;
        double dy = Math.sin(angle) * 12;
        c.paint(Area.line(16 - dx, 14 + dy, 16 + dx, 14 - dy, 2.6), accent);
        c.paint(Area.disc(16 - dx, 14 + dy, 3), Color.scale(base, 0.5));
        c.paint(Area.disc(16 + dx, 14 - dy, 2), accent);
        c.paint(Area.disc(16, 14, 2.2), STEEL_WHITE);
        c.paint(Area.rect(0, 28, Canvas.SIZE, 31), Color.scale(base, 0.5));
    }

    /** The firebox of a diesel generator: red-orange flames behind the grille. */
    private static void firebox(Canvas c, int base, int accent, int frame) {
        c.paint(Area.rect(0, 2, Canvas.SIZE, 4), accent);
        for (double y = 7; y <= 25; y += 4) {
            c.paint(Area.rect(5, y, 27, y + 1.5), Color.scale(base, 0.45));
        }
        Area window = Area.roundRect(9, 8, 23, 25, 3);
        c.paint(window, Color.hex("#1A0804"));
        MachineAnimation.flames(c, window, 9, 23, 24, 13, frame, FIRE_RED, FIRE_TIP);
    }

    /** A Cherenkov window: a light blue panel that brightens and dims slowly, with a pale core. */
    private static void cherenkov(Canvas c, int accent, int frame) {
        double p = MachineAnimation.pulse(frame);
        c.paint(Area.roundRect(6, 6, 26, 26, 4), Color.mix(accent, LIGHT_BLUE, 0.7 * p));
        c.paint(Area.roundRect(10, 10, 22, 22, 2), Color.withAlpha(Color.WHITE, 0.2 + 0.2 * p));
    }

    /** The solar glint: each cell brightens as the glint sweeps across the panel from left to right over the frames. */
    private static void glint(Canvas c, int accent, int frame) {
        c.paint(Area.rect(3, 6, 29, 26), Color.scale(accent, 0.7));
        double sweep = 2 + 4 * frame;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                double x = 5 + 6 * col;
                double y = 8 + 6 * row;
                double near = Math.clamp(1 - Math.abs(x + 2.5 - sweep) / 7, 0, 1);
                c.paint(Area.rect(x, y, x + 5, y + 5), Color.mix(accent, Color.WHITE, 0.6 * near));
            }
        }
    }
}
