package io.corefactory.resourcepack.art;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

/**
 * The artillery shells and the propellant charges, as item sprites. A shell is drawn nose up: an ogive nose, a body and a
 * flared tail with fins. Each kind has its own colours and markings, and the markings are the ones the kind is known by: bands,
 * the phosphorus band, the napalm drip, the split seams of a cluster shell, the radiation mark, five bombs, and the glowing ring
 * of the antimatter shell. The charges are cylinders in three tiers, from bronze to silver-blue to purple-black with a glowing rim.
 *
 * <p>Two sprites are animated, as vertical strips of square frames with a {@code .mcmeta} beside them: the phosphorus band
 * flickers over {@code 4} frames, and the napalm drip flares over {@code 6}. The other sprites are static.
 */
public final class ShellArt {

    /**
     * An animated sprite.
     *
     * @param id        the component id, such as {@code napalm_shell}
     * @param frames    the number of square frames in the strip, from 4 to 8
     * @param frametime the ticks each frame is shown, from 1 to 4
     */
    public record Clip(String id, int frames, int frametime) {
    }

    /** The animated sprites: the phosphorus band, which flickers, and the napalm drip, which flares. */
    public static final List<Clip> CLIPS = List.of(
            new Clip("white_phosphorus_shell", 4, 3),
            new Clip("napalm_shell", 6, 2));

    /** The white-hot core of the napalm flare. */
    private static final int FLAME_CORE = Color.hex("#FFE070");

    /**
     * The phase of the napalm flare's cycle, in radians: 15 degrees. A sine is the same on both sides of its peak, so with no phase
     * two neighbouring frames match on a frame count of 6. The phase keeps every frame apart from the next.
     */
    private static final double FLARE_PHASE = Math.PI / 12;

    /** Paints the markings of one shell on its body, which is {@code 2 * half} units wide. */
    @FunctionalInterface
    private interface Marking {
        void paint(Canvas c, int base, int accent, double half);
    }

    /** The shells and the charges: nine shells, then the three tiers of charge. */
    public static final List<ComponentArt.Spec> SPRITES = List.of(
            new ComponentArt.Spec("explosive_shell", "#6B7A3E", "#E0B040", shell(5, ShellArt::explosive)),
            new ComponentArt.Spec("high_explosive_shell", "#8A3A2A", "#FFD040", shell(5, ShellArt::highExplosive)),
            new ComponentArt.Spec("white_phosphorus_shell", "#EDEFF2", "#FFD23A", shell(5, ShellArt::phosphorus)),
            new ComponentArt.Spec("napalm_shell", "#8C8C90", "#FF8A20", shell(5, ShellArt::napalm)),
            new ComponentArt.Spec("cluster_shell", "#6A7480", "#FFB347", shell(5, ShellArt::cluster)),
            new ComponentArt.Spec("enriched_uranium_shell", "#1B1D20", "#7CFF5A", shell(5, ShellArt::enrichedUranium)),
            new ComponentArt.Spec("hydrogen_thermonuclear_shell", "#F4F7FB", "#3A7AFF", shell(7, ShellArt::hydrogen)),
            new ComponentArt.Spec("cluster_enriched_uranium_shell", "#2E3A2A", "#C8FF5A", shell(5, ShellArt::clusterBombs)),
            new ComponentArt.Spec("condensed_antimatter_shell", "#2A1040", "#E0B0FF", shell(5, ShellArt::antimatterRing)),
            new ComponentArt.Spec("tier1_explosive_charge", "#B87333", "#F0B070", charge(1)),
            new ComponentArt.Spec("tier2_explosive_charge", "#9FB4C8", "#D8F0FF", charge(2)),
            new ComponentArt.Spec("tier3_explosive_charge", "#1E1428", "#B56BFF", charge(3)));

    private ShellArt() {
    }

    /** The animation of a sprite, if it has one. */
    public static Optional<Clip> clip(String id) {
        return CLIPS.stream().filter(clip -> clip.id().equals(id)).findFirst();
    }

    /**
     * An animated sprite: {@code frames} square frames of 32 pixels, stacked top to bottom. Each frame is the static sprite with the
     * frame's animated detail painted over it.
     */
    public static BufferedImage strip(ComponentArt.Spec spec) {
        Clip clip = clip(spec.id()).orElseThrow(() -> new IllegalArgumentException("No animation for " + spec.id()));
        BufferedImage strip = new BufferedImage(Canvas.SIZE, Canvas.SIZE * clip.frames(), BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < clip.frames(); frame++) {
            Canvas canvas = new Canvas();
            ComponentArt.paint(canvas, spec);
            paintFrame(canvas, spec, frame, clip.frames());
            int[] pixels = canvas.toImage().getRGB(0, 0, Canvas.SIZE, Canvas.SIZE, null, 0, Canvas.SIZE);
            strip.setRGB(0, frame * Canvas.SIZE, Canvas.SIZE, Canvas.SIZE, pixels, 0, Canvas.SIZE);
        }
        return strip;
    }

    /** Paints the animated detail of one frame of a sprite over the static sprite on the canvas. */
    static void paintFrame(Canvas c, ComponentArt.Spec spec, int frame, int frames) {
        double cycle = 2 * Math.PI * frame / frames;
        switch (spec.id()) {
            case "white_phosphorus_shell" -> {
                double f = 0.5 + 0.3 * Math.sin(cycle) + 0.2 * Math.sin(2 * cycle + 1.3);
                c.paint(Area.rect(11, 14, 21, 19), Color.withAlpha(Color.WHITE, 0.1 + 0.5 * f));
            }
            case "napalm_shell" -> {
                double f = 0.5 - 0.5 * Math.cos(cycle - FLARE_PHASE);
                double flare = 2.6 + 2.4 * f + 0.4 * Math.sin(2 * cycle + 0.7);
                c.paint(Area.polygon(16.5, 23.5, 19.5, 23.5, 18, 23.5 + flare),
                        Color.mix(Color.hex(spec.accent()), FLAME_CORE, f));
                c.paint(Area.rect(17.5, 23.5, 18.5, 23.5 + flare * 0.5), FLAME_CORE);
            }
            default -> throw new IllegalArgumentException("No animation for " + spec.id());
        }
    }

    /** The silhouette of a shell {@code 2 * half} units wide: an ogive nose, a body, and a flared tail with fins. */
    private static Area silhouette(double half) {
        return Area.ellipse(16, 11, half, 8)
                .or(Area.rect(16 - half, 11, 16 + half, 25))
                .or(Area.polygon(16 - half, 22, 16 + half, 22, 16 + half + 2.5, 29, 16 - half - 2.5, 29));
    }

    /** A shell's sprite: its silhouette lit with {@link Sprite}, and its markings on the body. */
    private static ComponentArt.Painter shell(double half, Marking markings) {
        return (c, base, accent, seed) -> {
            Sprite.body(c, silhouette(half), base, seed);
            markings.paint(c, base, accent, half);
        };
    }

    /** The fuse cap on the tip of a nose. */
    private static void nose(Canvas c, int colour) {
        c.paint(Area.disc(16, 5.5, 1.6), colour);
    }

    private static void explosive(Canvas c, int base, int accent, double half) {
        c.paint(Area.rect(16 - half, 15, 16 + half, 17), accent);
        nose(c, accent);
    }

    private static void highExplosive(Canvas c, int base, int accent, double half) {
        c.paint(Area.rect(16 - half, 13, 16 + half, 14.5), accent);
        c.paint(Area.rect(16 - half, 19, 16 + half, 20.5), accent);
        nose(c, accent);
    }

    /** A white shell with a broad yellow band; the band's glow flickers over the frames. */
    private static void phosphorus(Canvas c, int base, int accent, double half) {
        c.paint(Area.rect(16 - half, 14, 16 + half, 19), accent);
        nose(c, Color.scale(base, 0.5));
    }

    /** An orange shoulder band, and a drip running down the side from it; the drip's tip flares over the frames. */
    private static void napalm(Canvas c, int base, int accent, double half) {
        c.paint(Area.rect(16 - half, 11, 16 + half, 13), accent);
        c.paint(Area.line(18, 17, 18, 22, 1.6), accent);
        c.paint(Area.disc(18, 22.5, 1.5), accent);
    }

    /** The split marks of a cluster shell: two seams from the nose to the sides, a seam across the body, and rivets. */
    private static void cluster(Canvas c, int base, int accent, double half) {
        int seam = Color.scale(base, 0.4);
        c.paint(Area.line(16, 12, 16 - half, 18, 0.9), seam);
        c.paint(Area.line(16, 12, 16 + half, 18, 0.9), seam);
        c.paint(Area.rect(16 - half, 21, 16 + half, 21.8), seam);
        double[][] rivets = {{16 - half / 2, 17}, {16 + half / 2, 17}, {16, 24}};
        for (double[] r : rivets) {
            c.paint(Area.disc(r[0], r[1], 0.9), accent);
        }
    }

    /** A radiation mark in green on the black body, with a green cap. */
    private static void enrichedUranium(Canvas c, int base, int accent, double half) {
        Icon.RADIATION.draw(c, 16, 17, 4.5, accent, accent);
        nose(c, accent);
    }

    /** A large white shell with two blue bands and a blue cap. */
    private static void hydrogen(Canvas c, int base, int accent, double half) {
        c.paint(Area.rect(16 - half, 15, 16 + half, 17.5), accent);
        c.paint(Area.rect(16 - half, 21, 16 + half, 23), accent);
        c.paint(Area.disc(16, 5.5, 2), accent);
    }

    /** Five small bombs on the body, each a green disc with a dark centre. */
    private static void clusterBombs(Canvas c, int base, int accent, double half) {
        double[][] bombs = {{13.5, 14}, {18.5, 14}, {16, 18.5}, {13.5, 23}, {18.5, 23}};
        for (double[] b : bombs) {
            c.paint(Area.disc(b[0], b[1], 1.7), accent);
            c.paint(Area.disc(b[0], b[1], 0.6), Color.scale(base, 0.5));
        }
    }

    /** A glowing ring around the body, with a soft halo outside it. */
    private static void antimatterRing(Canvas c, int base, int accent, double half) {
        c.paint(Area.ellipse(16, 17, half + 3, 4), Color.withAlpha(accent, 0.3));
        c.paint(Area.ellipse(16, 17, half + 1.5, 2.4).minus(Area.ellipse(16, 17, half - 0.5, 1.4)), accent);
        nose(c, accent);
    }

    /** The silhouette of a charge: a cylinder, with an elliptical cap at each end. */
    private static Area cylinder() {
        return Area.roundRect(9, 7, 23, 27, 3).or(Area.ellipse(16, 7, 7, 3)).or(Area.ellipse(16, 27, 7, 3));
    }

    /**
     * A propellant charge of one tier: tier 1 has two bands, tier 2 has three, and tier 3 has none but a glowing rim, which it
     * shows as a halo behind the cylinder and a bright ring on its cap.
     */
    private static ComponentArt.Painter charge(int tier) {
        return (c, base, accent, seed) -> {
            if (tier == 3) {
                c.paint(Area.ellipse(16, 7, 9, 4.5), Color.withAlpha(accent, 0.3));
            }
            Sprite.body(c, cylinder(), base, seed);
            double[] bands = switch (tier) {
                case 1 -> new double[]{12, 20};
                case 2 -> new double[]{11, 16, 21};
                default -> new double[0];
            };
            for (double y : bands) {
                c.paint(Area.rect(9, y, 23, y + 1.5), accent);
            }
            if (tier == 3) {
                c.paint(Area.ellipse(16, 7, 7, 3).minus(Area.ellipse(16, 7, 5.2, 1.8)), accent);
            }
        };
    }
}
