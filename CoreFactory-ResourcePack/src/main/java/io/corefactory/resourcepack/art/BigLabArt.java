package io.corefactory.resourcepack.art;

import java.util.List;

/**
 * The large lab tools, each one block tall: the crude oil refinery, the particle accelerator, the antimatter condenser and
 * the enrichment cascade. The refinery's top carries its bubble column, the accelerator's front a pulsing ring, and the
 * condenser's front a glowing core.
 */
public final class BigLabArt {

    private static final int STEEL_WHITE = Color.hex("#F4F5F7");

    /** The four lab tools. */
    public static final List<MachineArt.Spec> ALL = List.of(
            new MachineArt.Spec("crude_oil_refinery", "#4A4A52", "#FF8C2A", List.of(
                    new MachineArt.Box(1, 0, 1, 15, 2, 15),
                    new MachineArt.Box(5, 2, 5, 11, 16, 11),
                    new MachineArt.Box(0, 0, 5, 4, 8, 11),
                    new MachineArt.Box(12, 0, 5, 16, 8, 11),
                    new MachineArt.Box(4, 6, 7, 5, 7, 9),
                    new MachineArt.Box(11, 6, 7, 12, 7, 9),
                    new MachineArt.Box(1, 0, 13, 2, 10, 14),
                    new MachineArt.Box(14, 0, 13, 15, 10, 14)), BigLabArt::refineryFront),
            new MachineArt.Spec("particle_accelerator", "#4A4E60", "#7AB8FF", List.of(
                    new MachineArt.Box(1, 0, 1, 15, 3, 15),
                    new MachineArt.Box(2, 3, 2, 14, 13, 14),
                    new MachineArt.Box(5, 13, 5, 11, 16, 11),
                    new MachineArt.Box(0, 6, 5, 2, 10, 11),
                    new MachineArt.Box(14, 6, 5, 16, 10, 11)), (c, b, a, seed) -> {
            }),
            new MachineArt.Spec("antimatter_condenser", "#1A1E3A", "#C080FF", List.of(
                    new MachineArt.Box(2, 0, 2, 14, 2, 14),
                    new MachineArt.Box(4, 2, 4, 12, 14, 12),
                    new MachineArt.Box(5, 14, 5, 11, 16, 11),
                    new MachineArt.Box(1, 4, 3, 3, 12, 13),
                    new MachineArt.Box(13, 4, 3, 15, 12, 13)), (c, b, a, seed) -> {
            }),
            new MachineArt.Spec("enrichment_cascade", "#3A4A60", "#C0F060", List.of(
                    new MachineArt.Box(1, 0, 3, 5, 16, 7),
                    new MachineArt.Box(6, 0, 3, 10, 16, 7),
                    new MachineArt.Box(11, 0, 3, 15, 16, 7),
                    new MachineArt.Box(0, 13, 6, 16, 15, 10),
                    new MachineArt.Box(0, 0, 8, 16, 2, 12)), BigLabArt::cascadeFront));

    /** The animated faces: the refinery's column on top, the accelerator's ring and the condenser's core on the front. */
    public static final List<MachineAnimation.Clip> CLIPS = List.of(
            new MachineAnimation.Clip("crude_oil_refinery", MachineArt.Face.UP, 3),
            new MachineAnimation.Clip("particle_accelerator", MachineArt.Face.NORTH, 3),
            new MachineAnimation.Clip("antimatter_condenser", MachineArt.Face.NORTH, 3));

    private BigLabArt() {
    }

    /** Whether an animated face of this class belongs to the machine id. */
    static boolean owns(String id) {
        return CLIPS.stream().anyMatch(clip -> clip.id().equals(id));
    }

    /** Paints the animated detail of one frame of a lab tool. */
    static void paintFrame(Canvas c, MachineArt.Spec spec, int frame) {
        int accent = Color.hex(spec.accent());
        switch (spec.id()) {
            case "crude_oil_refinery" -> MachineAnimation.column(c, accent, frame);
            case "particle_accelerator" -> ring(c, accent, frame);
            case "antimatter_condenser" -> core(c, accent, frame);
            default -> throw new IllegalArgumentException("No animation for " + spec.id());
        }
    }

    /** The front of the refinery: two pipe bands and a sight glass of the column. */
    private static void refineryFront(Canvas c, int base, int accent, long seed) {
        c.paint(Area.rect(0, 10, Canvas.SIZE, 12), accent);
        c.paint(Area.rect(0, 20, Canvas.SIZE, 22), accent);
        c.paint(Area.rect(13, 6, 19, 26), Color.withAlpha(Color.WHITE, 0.35));
        c.paint(Area.rect(0, 29, Canvas.SIZE, Canvas.SIZE), Color.scale(base, 0.5));
    }

    /** The front of the enrichment cascade: three round gauges above a bright conveyor band. */
    private static void cascadeFront(Canvas c, int base, int accent, long seed) {
        for (double x : new double[]{8, 16, 24}) {
            c.paint(Area.disc(x, 11, 3.5), STEEL_WHITE);
            c.paint(Area.ring(x, 11, 3.5, 1), accent);
        }
        c.paint(Area.rect(0, 18, Canvas.SIZE, 21), Color.scale(accent, 0.6));
    }

    /**
     * The particle accelerator's ring: a steel ring around the beam, which brightens and dims over the cycle. The beam
     * runs through the middle as a white line.
     */
    private static void ring(Canvas c, int accent, int frame) {
        double p = MachineAnimation.pulse(frame);
        c.paint(Area.rect(0, 15, Canvas.SIZE, 17), Color.withAlpha(Color.WHITE, 0.9));
        c.paint(Area.ring(16, 16, 9, 2 + 1.5 * p), Color.mix(accent, Color.WHITE, 0.2 + 0.6 * p));
        c.paint(Area.disc(16, 16, 2.5), Color.withAlpha(Color.WHITE, 0.9));
    }

    /**
     * The antimatter condenser's core: a dark window with a glowing column, a halo that swells and dims, and a bright
     * centre, all pulsing over the cycle.
     */
    private static void core(Canvas c, int accent, int frame) {
        double p = MachineAnimation.pulse(frame);
        c.paint(Area.roundRect(9, 4, 23, 28, 5), Color.hex("#0C0E1A"));
        c.paint(Area.disc(16, 16, 5 + 2 * p), Color.withAlpha(accent, 0.25 + 0.4 * p));
        c.paint(Area.rect(15, 6, 17, 26), Color.withAlpha(accent, 0.4 + 0.5 * p));
        c.paint(Area.disc(16, 16, 2.5), Color.mix(accent, Color.WHITE, 0.4 + 0.5 * p));
    }
}
