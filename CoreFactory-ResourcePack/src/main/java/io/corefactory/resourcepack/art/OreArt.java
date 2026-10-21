package io.corefactory.resourcepack.art;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * The block texture of every ore. An ore is a vanilla block the resource pack re-textures, so each one is painted as its
 * mineral: a rock colour with the mineral's inclusions. Every ore is painted on a {@link Canvas} at its full size and
 * reduced to a 16 x 16 block texture by {@link #block}.
 */
public final class OreArt {

    /** The size of a vanilla block texture. */
    public static final int BLOCK_SIZE = 16;

    /**
     * One ore's look.
     *
     * @param id      the ore id from ores.yml
     * @param host    the vanilla block material the ore is placed as
     * @param base    the rock colour, {@code #RRGGBB}
     * @param accent  the mineral colour, {@code #RRGGBB}
     * @param painter draws the rock and the mineral
     */
    public record Spec(String id, String host, String base, String accent, ComponentArt.Painter painter) {
    }

    /** Every ore, in the order of ores.yml. */
    public static final List<Spec> ALL = List.of(
            new Spec("uraninite_ore", "BLACK_GLAZED_TERRACOTTA", "#1B1D20", "#7CFF5A", OreArt::uraninite),
            new Spec("pitchblende_ore", "BROWN_GLAZED_TERRACOTTA", "#2A1D12", "#9A7A46", OreArt::pitchblende),
            new Spec("monazite_ore", "YELLOW_GLAZED_TERRACOTTA", "#E0B040", "#B8651A", OreArt::monazite),
            new Spec("thorite_ore", "GRAY_GLAZED_TERRACOTTA", "#2C2C30", "#FF8A20", OreArt::thorite),
            new Spec("galena_ore", "LIGHT_GRAY_GLAZED_TERRACOTTA", "#9AA2AC", "#F4F7FA", OreArt::galena),
            new Spec("cinnabar_ore", "RED_GLAZED_TERRACOTTA", "#A8201E", "#141010", OreArt::cinnabar),
            new Spec("fluorite_ore", "PURPLE_GLAZED_TERRACOTTA", "#8A4DC4", "#E8DCFF", OreArt::fluorite),
            new Spec("crude_petroleum_deposit", "WHITE_GLAZED_TERRACOTTA", "#2B1F17", "#3FC8A8",
                    OreArt::crudePetroleum),
            new Spec("saltpeter_deposit", "LIME_GLAZED_TERRACOTTA", "#C4C0B4", "#FFFFFF", OreArt::saltpeter),
            new Spec("sulfur_deposit", "ORANGE_GLAZED_TERRACOTTA", "#4A4638", "#F2D21A", OreArt::sulfur),
            new Spec("zinc_deposit", "CYAN_GLAZED_TERRACOTTA", "#4A3524", "#4FD0E0", OreArt::sphalerite),
            new Spec("coal_ore", "COAL_ORE", "#5C5C60", "#141416", OreArt::coal));

    private OreArt() {
    }

    /** Paints one ore onto a fresh canvas. The result is opaque. */
    public static void paint(Canvas c, Spec spec) {
        spec.painter().paint(c, Color.hex(spec.base()), Color.hex(spec.accent()), spec.id().hashCode());
    }

    /**
     * The 16 x 16 block texture of a painted ore. Each block pixel is the mean of a 2 x 2 block of the painting, a box
     * filter, so the fine detail is averaged rather than dropped. The ores are opaque, so the mean is taken per channel.
     */
    public static BufferedImage block(Canvas painting) {
        int scale = Canvas.SIZE / BLOCK_SIZE;
        BufferedImage block = new BufferedImage(BLOCK_SIZE, BLOCK_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < BLOCK_SIZE; y++) {
            for (int x = 0; x < BLOCK_SIZE; x++) {
                int a = 0;
                int r = 0;
                int g = 0;
                int b = 0;
                for (int dy = 0; dy < scale; dy++) {
                    for (int dx = 0; dx < scale; dx++) {
                        int p = painting.get(x * scale + dx, y * scale + dy);
                        a += Color.alpha(p);
                        r += Color.red(p);
                        g += Color.green(p);
                        b += Color.blue(p);
                    }
                }
                int n = scale * scale;
                block.setRGB(x, y, Color.argb(a / n, r / n, g / n, b / n));
            }
        }
        return block;
    }

    private static void uraninite(Canvas c, int base, int accent, long seed) {
        rock(c, base, seed);
        c.paint(Area.line(4, 20, 12, 27, 0.8), Color.scale(base, 0.35));
        c.paint(Area.line(20, 3, 29, 9, 0.8), Color.scale(base, 0.35));
        glints(c, 22, accent, seed);
    }

    private static void pitchblende(Canvas c, int base, int accent, long seed) {
        rock(c, base, seed);
        for (int i = 0; i < 9; i++) {
            double x = position(i, 0, seed);
            double y = position(i, 1, seed);
            c.paint(Area.disc(x, y, 2.4), Color.scale(base, 0.45));
            c.paint(Area.disc(x - 0.8, y - 0.8, 0.9), Color.withAlpha(accent, 0.85));
        }
        c.paint(Area.line(6, 10, 14, 6, 0.9), Color.withAlpha(accent, 0.6));
        c.paint(Area.line(18, 24, 27, 21, 0.9), Color.withAlpha(accent, 0.6));
    }

    private static void monazite(Canvas c, int base, int accent, long seed) {
        rock(c, base, seed);
        dots(c, 44, 1.1, accent, seed);
        dots(c, 14, 0.6, Color.mix(base, Color.WHITE, 0.6), seed + 3);
    }

    private static void thorite(Canvas c, int base, int accent, long seed) {
        rock(c, base, seed);
        dots(c, 8, 1.0, Color.scale(base, 0.5), seed);
        vein(c, accent, 2, 8, 10, 12, 16, 10, 24, 16, 31, 14);
        vein(c, accent, 6, 31, 12, 23, 18, 25, 22, 18, 27, 20);
        vein(c, accent, 20, 0, 24, 7, 31, 5);
    }

    private static void galena(Canvas c, int base, int accent, long seed) {
        rock(c, base, seed);
        for (int i = 0; i < 12; i++) {
            double x = position(i, 0, seed);
            double y = position(i, 1, seed);
            c.paint(Area.rect(x - 2, y - 2, x + 2, y + 2), accent);
            c.paint(Area.rect(x, y, x + 2, y + 2), Color.scale(accent, 0.6));
            c.paint(Area.rect(x - 2, y - 2, x, y - 1), Color.WHITE);
        }
    }

    private static void cinnabar(Canvas c, int base, int accent, long seed) {
        rock(c, base, seed);
        dots(c, 30, 1.0, Color.withAlpha(Color.hex("#FF6A55"), 0.6), seed + 5);
        dots(c, 48, 1.0, accent, seed);
    }

    private static void fluorite(Canvas c, int base, int accent, long seed) {
        rock(c, base, seed);
        dots(c, 10, 0.9, Color.scale(base, 0.5), seed);
        facet(c, 8, 9, 4.5, accent);
        facet(c, 22, 6, 3.5, accent);
        facet(c, 16, 21, 5, accent);
        facet(c, 26, 23, 3, accent);
        facet(c, 6, 24, 3, accent);
    }

    private static void crudePetroleum(Canvas c, int base, int accent, long seed) {
        rock(c, base, seed);
        dots(c, 14, 1.0, Color.scale(base, 0.45), seed + 9);
        crack(c, accent, 3, 9, 13, 14, 22, 12);
        crack(c, accent, 17, 22, 28, 26);
        crack(c, accent, 8, 27, 12, 21);
    }

    /** White potassium nitrate crystals standing out of the rock. */
    private static void saltpeter(Canvas c, int base, int accent, long seed) {
        rock(c, base, seed);
        facet(c, 7, 9, 4, accent);
        facet(c, 21, 7, 3.5, accent);
        facet(c, 14, 18, 5, accent);
        facet(c, 25, 20, 3.5, accent);
        facet(c, 7, 24, 3, accent);
        facet(c, 20, 27, 3, accent);
    }

    /** Native sulfur: yellow lumps in the rock, each with a pale sheen. */
    private static void sulfur(Canvas c, int base, int accent, long seed) {
        rock(c, base, seed);
        c.paint(Area.ellipse(9, 10, 4, 2.6), accent);
        c.paint(Area.ellipse(22, 20, 4.5, 3), accent);
        c.paint(Area.ellipse(14, 26, 3.5, 2.2), accent);
        c.paint(Area.ellipse(8, 9, 1.4, 0.8), Color.withAlpha(Color.WHITE, 0.5));
        c.paint(Area.ellipse(21, 19, 1.6, 0.9), Color.withAlpha(Color.WHITE, 0.5));
    }

    /** Sphalerite: a dark brown rock speckled with cyan. */
    private static void sphalerite(Canvas c, int base, int accent, long seed) {
        rock(c, base, seed);
        dots(c, 34, 1.0, accent, seed + 2);
        dots(c, 12, 0.7, Color.scale(base, 0.45), seed + 4);
    }

    /** Coal: a grey stone with black flecks. */
    private static void coal(Canvas c, int base, int accent, long seed) {
        rock(c, base, seed);
        dots(c, 36, 1.2, accent, seed + 6);
    }

    /** A faint sheen in a crack: a wide pale halo under a thin line. */
    private static void crack(Canvas c, int colour, double... xy) {
        for (int i = 0; i + 3 < xy.length; i += 2) {
            c.paint(Area.line(xy[i], xy[i + 1], xy[i + 2], xy[i + 3], 2.2), Color.withAlpha(colour, 0.18));
        }
        for (int i = 0; i + 3 < xy.length; i += 2) {
            c.paint(Area.line(xy[i], xy[i + 1], xy[i + 2], xy[i + 3], 0.7), Color.withAlpha(colour, 0.6));
        }
    }

    /** A mottled rock colour with fine grain, over the whole canvas. */
    private static void rock(Canvas c, int base, long seed) {
        c.shade(Area.rect(0, 0, Canvas.SIZE, Canvas.SIZE), (x, y, current) -> {
            double blotch = Noise.smooth(x / 6.0, y / 6.0, seed) - 0.5;
            double grain = Noise.hash(x, y, seed + 7) - 0.5;
            return Color.scale(base, 1 + 0.5 * blotch + 0.16 * grain);
        });
    }

    /** Small bright glints, each a point with a faint halo. */
    private static void glints(Canvas c, int count, int colour, long seed) {
        for (int i = 0; i < count; i++) {
            double x = position(i, 0, seed);
            double y = position(i, 1, seed);
            c.paint(Area.disc(x, y, 1.7), Color.withAlpha(colour, 0.3));
            c.paint(Area.disc(x, y, 0.8), colour);
        }
    }

    /** Dots of one colour at seeded positions. */
    private static void dots(Canvas c, int count, double radius, int colour, long seed) {
        for (int i = 0; i < count; i++) {
            c.paint(Area.disc(position(i, 0, seed), position(i, 1, seed), radius), colour);
        }
    }

    /** A vein: a polyline through the given points, with a faint halo. */
    private static void vein(Canvas c, int colour, double... xy) {
        for (int i = 0; i + 3 < xy.length; i += 2) {
            c.paint(Area.line(xy[i], xy[i + 1], xy[i + 2], xy[i + 3], 2.6), Color.withAlpha(colour, 0.3));
        }
        for (int i = 0; i + 3 < xy.length; i += 2) {
            c.paint(Area.line(xy[i], xy[i + 1], xy[i + 2], xy[i + 3], 1.2), colour);
        }
    }

    /** A crystal facet: a pale hexagon with a lighter centre. */
    private static void facet(Canvas c, double cx, double cy, double r, int colour) {
        double[] hexagon = new double[12];
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            hexagon[2 * k] = cx + Math.cos(a) * r;
            hexagon[2 * k + 1] = cy + Math.sin(a) * r;
        }
        c.paint(Area.polygon(hexagon), Color.withAlpha(colour, 0.9));
        c.paint(Area.disc(cx - 0.3 * r, cy - 0.3 * r, 0.45 * r), Color.withAlpha(Color.WHITE, 0.55));
    }

    /** A seeded value in [0, 1) for the i-th scattered item along one axis. */
    private static double position(int i, int axis, long seed) {
        return Noise.hash(i, axis, seed + 101) * (Canvas.SIZE - 2) + 1;
    }
}
