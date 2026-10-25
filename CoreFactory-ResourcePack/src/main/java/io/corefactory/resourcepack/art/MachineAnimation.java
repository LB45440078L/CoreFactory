package io.corefactory.resourcepack.art;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The animated faces of the machines. A machine listed here has one face that is a vertical strip of square frames,
 * {@link #FRAME} pixels wide, and the resource pack builder writes an animation of the clip's frame time beside it, as it
 * does for the belt tops. A clip has {@link #FRAMES} frames unless it gives its own count. The other faces of the machine
 * stay static.
 *
 * <p>Each frame is painted on a canvas over the same casing as the static face, and then halved, so an animated face has
 * half the resolution of the static ones. Frame {@code k} is the animation's state at {@code k / FRAMES} of its cycle, so
 * a pulse goes up over the first half of the strip and back down over the second, and the strip loops.
 */
public final class MachineAnimation {

    /** The number of frames in an animated face. */
    public static final int FRAMES = 8;

    /** The width and height of one frame, in pixels: half a canvas. */
    public static final int FRAME = Canvas.SIZE / 2;

    private static final int GOLD = Color.hex("#FFD040");

    /**
     * One animated face.
     *
     * @param id        the machine id, such as {@code pylon_1}
     * @param face      the side that is animated
     * @param frametime the ticks each frame is shown, from 1 to 4
     * @param frames    the number of frames in the strip, from 4 to 8
     */
    public record Clip(String id, MachineArt.Face face, int frametime, int frames) {

        /** A clip of the standard {@link MachineAnimation#FRAMES} frames. */
        public Clip(String id, MachineArt.Face face, int frametime) {
            this(id, face, frametime, FRAMES);
        }
    }

    /**
     * Every animated face: the beacons and the charge meters, which are painted here, and the generators, drills, lab
     * tools, turrets, pumps, vertical belts, artillery pieces and the command center, which their own classes list and paint.
     */
    public static final List<Clip> ALL = Stream.of(
                    List.of(
                            new Clip("pylon_1", MachineArt.Face.UP, 3),
                            new Clip("pylon_2", MachineArt.Face.UP, 3),
                            new Clip("pylon_3", MachineArt.Face.UP, 3),
                            new Clip("pylon_4", MachineArt.Face.UP, 3),
                            new Clip("pylon_5", MachineArt.Face.UP, 3),
                            new Clip("accumulator_1", MachineArt.Face.NORTH, 3),
                            new Clip("accumulator_2", MachineArt.Face.NORTH, 3),
                            new Clip("accumulator_3", MachineArt.Face.NORTH, 3),
                            new Clip("accumulator_4", MachineArt.Face.NORTH, 3),
                            new Clip("accumulator_5", MachineArt.Face.NORTH, 3)),
                    GeneratorArt.CLIPS, DrillArt.CLIPS, BigLabArt.CLIPS, TurretArt.CLIPS, TurretLevelArt.CLIPS,
                    WaterVariants.CLIPS, VerticalBeltArt.CLIPS, DuplexArt.CLIPS, ArtilleryArt.CLIPS, RailgunArt.CLIPS)
            .flatMap(List::stream).toList();

    private MachineAnimation() {
    }

    /** The animation of a machine, if it has one. */
    public static Optional<Clip> clip(String id) {
        return ALL.stream().filter(clip -> clip.id().equals(id)).findFirst();
    }

    /** Whether a face of a machine is one of its animated faces. */
    public static boolean animates(MachineArt.Spec spec, MachineArt.Face face) {
        return clip(spec.id()).filter(clip -> clip.face() == face).isPresent();
    }

    /**
     * An animated face: the clip's frames of {@link #FRAME} pixels, stacked top to bottom, so the image is {@code FRAME} wide
     * and {@code FRAME * frames} tall.
     */
    public static BufferedImage strip(MachineArt.Spec spec, MachineArt.Face face) {
        int frames = clip(spec.id()).map(Clip::frames).orElse(FRAMES);
        BufferedImage strip = new BufferedImage(FRAME, FRAME * frames, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < frames; frame++) {
            Canvas canvas = new Canvas();
            MachineArt.paintCasing(canvas, spec, face);
            paintFrame(canvas, spec, frame);
            strip.setRGB(0, frame * FRAME, FRAME, FRAME, halve(canvas), 0, FRAME);
        }
        return strip;
    }

    /** Paints the animated detail of a face for one frame, over the casing already on the canvas. */
    static void paintFrame(Canvas c, MachineArt.Spec spec, int frame) {
        int base = Color.hex(spec.base());
        int accent = Color.hex(spec.accent());
        switch (spec.id()) {
            case "pylon_1", "pylon_2", "pylon_3", "pylon_4", "pylon_5" -> beacon(c, accent, frame, level(spec.id()));
            case "accumulator_1", "accumulator_2", "accumulator_3", "accumulator_4", "accumulator_5" ->
                    meter(c, base, accent, level(spec.id()), frame);
            default -> {
                if (GeneratorArt.owns(spec.id())) {
                    GeneratorArt.paintFrame(c, spec, frame);
                } else if (DrillArt.owns(spec.id())) {
                    DrillArt.paintFrame(c, spec, frame);
                } else if (TurretArt.owns(spec.id())) {
                    TurretArt.paintFrame(c, spec, frame);
                } else if (TurretLevelArt.owns(spec.id())) {
                    TurretLevelArt.paintFrame(c, spec, frame);
                } else if (WaterVariants.owns(spec.id())) {
                    WaterArt.paintFrame(c, spec, frame);
                } else if (VerticalBeltArt.owns(spec.id())) {
                    VerticalBeltArt.paintFrame(c, spec, frame);
                } else if (DuplexArt.owns(spec.id())) {
                    DuplexArt.paintFrame(c, spec, frame);
                } else if (ArtilleryArt.owns(spec.id())) {
                    ArtilleryArt.paintFrame(c, spec, frame);
                } else if (RailgunArt.owns(spec.id())) {
                    RailgunArt.paintFrame(c, spec, frame);
                } else {
                    BigLabArt.paintFrame(c, spec, frame);
                }
            }
        }
    }

    /** The frame's 32 x 32 canvas halved to {@link #FRAME} pixels square, each pixel the mean of a 2 x 2 block. */
    private static int[] halve(Canvas canvas) {
        int[] pixels = new int[FRAME * FRAME];
        for (int y = 0; y < FRAME; y++) {
            for (int x = 0; x < FRAME; x++) {
                int alpha = 0;
                int red = 0;
                int green = 0;
                int blue = 0;
                for (int dy = 0; dy < 2; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        int p = canvas.get(2 * x + dx, 2 * y + dy);
                        alpha += Color.alpha(p);
                        red += Color.red(p);
                        green += Color.green(p);
                        blue += Color.blue(p);
                    }
                }
                pixels[y * FRAME + x] = Color.argb(alpha / 4, red / 4, green / 4, blue / 4);
            }
        }
        return pixels;
    }

    /** The level of a pylon or accumulator, from the last digit of its id. */
    private static int level(String id) {
        return id.charAt(id.length() - 1) - '0';
    }

    /** The pulse of a cycle: 0 at frame 0, 1 at the middle frame, and back to 0, so the strip loops. */
    static double pulse(int frame) {
        return 0.5 - 0.5 * Math.cos(2 * Math.PI * frame / FRAMES);
    }

    /**
     * A lamp in a ring, with a halo that brightens and dims: the ring is brighter at the middle of the cycle. The fourth
     * level adds a ring outside the halo, and the fifth adds a thin ring at the edge of the top, which brightens with the
     * pulse.
     */
    private static void beacon(Canvas c, int accent, int frame, int level) {
        double p = pulse(frame);
        c.paint(Area.ring(16, 16, 10, 3), Color.withAlpha(accent, 0.5 * p));
        c.paint(Area.ring(16, 16, 7, 2.4), Color.mix(Color.scale(accent, 0.6), Color.mix(accent, Color.WHITE, 0.6), p));
        c.paint(Area.disc(16, 16, 3.5), Color.mix(accent, Color.WHITE, 0.4 + 0.4 * p));
        if (level >= 4) {
            c.paint(Area.ring(16, 16, 13, 2), Color.withAlpha(accent, 0.5 * p));
        }
        if (level == 5) {
            c.paint(Area.ring(16, 16, 15.5, 1), Color.mix(accent, Color.WHITE, 0.5 * p));
        }
    }

    /**
     * The charge meter of an accumulator: a fill that rises from the bottom of the meter and falls back over the cycle,
     * and the level's bars, the first {@code level} of them lit. The fourth and fifth levels have five bars, and the
     * fifth's fill is brighter, lifted towards white by the pulse.
     */
    private static void meter(Canvas c, int base, int accent, int level, int frame) {
        c.paint(Area.rect(0, 14, Canvas.SIZE, 17), accent);
        c.paint(Area.rect(9, 6, 23, 26), Color.hex("#0E1A14"));
        double top = 26 - 20 * pulse(frame);
        int fill = level == 5 ? Color.mix(accent, Color.WHITE, 0.3 + 0.4 * pulse(frame)) : Color.withAlpha(accent, 0.6);
        c.paint(Area.rect(11, top, 21, 26), fill);
        int bars = level >= 4 ? 5 : 3;
        for (int k = 0; k < bars; k++) {
            double y = level >= 4 ? 22 - 4 * k : 21 - 5 * k;
            c.paint(Area.rect(11, y, 21, y + (level >= 4 ? 1 : 3)), k < level ? GOLD : Color.scale(base, 0.4));
        }
    }

    /**
     * The glass column of a refinery top: a tube with bubbles that rise through it. Each bubble moves up 2.75 pixels a
     * frame and wraps inside the tube, so the eight frames loop.
     */
    static void column(Canvas c, int accent, int frame) {
        Area tube = Area.roundRect(11, 4, 21, 28, 4);
        c.paint(tube, Color.withAlpha(Color.WHITE, 0.2));
        c.paint(tube.minus(Area.roundRect(12, 5, 20, 27, 3)), accent);
        double[][] bubbles = {{13.5, 20, 1.4}, {16, 9, 1.8}, {18.5, 25, 1.2}};
        int bubble = Color.mix(accent, Color.WHITE, 0.6);
        for (double[] b : bubbles) {
            double rise = (b[1] - 5 - 22.0 * frame / FRAMES) % 22;
            if (rise < 0) {
                rise += 22;
            }
            c.paint(Area.disc(b[0], 5 + rise, b[2]), bubble);
        }
    }

    /**
     * Flames rising from the bottom of an area, clipped to it. Each column is as tall as the height at the middle of the
     * width, flickers over the frames, and has a tip in its top two rows.
     */
    static void flames(Canvas c, Area clip, double left, double right, double bottom, double height, int frame,
                       int body, int tip) {
        double centre = (left + right) / 2;
        double half = (right - left) / 2;
        double cycle = 2 * Math.PI * frame / FRAMES;
        for (int x = (int) left; x < right; x++) {
            double swell = 1 - 0.5 * square((x + 0.5 - centre) / half);
            double flicker = 0.6 + 0.25 * Math.sin(cycle + x * 1.7) + 0.15 * Math.sin(2 * cycle + x * 2.9);
            double h = height * swell * flicker;
            c.paint(Area.rect(x, bottom - h, x + 1, bottom).and(clip), body);
            c.paint(Area.rect(x, bottom - h, x + 1, bottom - h + 2).and(clip), tip);
        }
    }

    private static double square(double value) {
        return value * value;
    }
}
