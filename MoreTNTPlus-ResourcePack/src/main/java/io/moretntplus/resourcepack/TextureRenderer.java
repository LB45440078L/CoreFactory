package io.moretntplus.resourcepack;

import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.Random;

/**
 * Procedurally draws the 16x16 block textures. Each pattern is a small, readable motif in the two colours of
 * the TNT's texture spec, with a little noise and a darker edge so the cube reads as a block.
 *
 * <p>The patterns are deterministic: the same spec and id always produce the same pixels.
 */
public final class TextureRenderer {

    public static final int SIZE = 16;

    private TextureRenderer() {
    }

    /**
     * @param base    the main colour, as {@code #RRGGBB}
     * @param accent  the motif colour, as {@code #RRGGBB}
     * @param pattern one of: stripes, dots, hazard, rings, checker, cross, nuclear, flame, bio, circuit, glyph
     * @param seed    a stable value (the TNT id) so noise is reproducible
     */
    public static BufferedImage render(String base, String accent, String pattern, String seed) {
        int[] main = parse(base, 0x808080);
        int[] motif = parse(accent, 0xFFFFFF);
        String kind = pattern == null ? "stripes" : pattern.toLowerCase(Locale.ROOT);
        Random random = new Random(seed.hashCode());
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                boolean on = motifAt(kind, x, y, random);
                int[] colour = on ? motif : main;
                double noise = (random.nextDouble() - 0.5) * 18;
                if (x == 0 || y == 0 || x == SIZE - 1 || y == SIZE - 1) {
                    noise -= 26;
                }
                int r = clamp(colour[0] + noise);
                int g = clamp(colour[1] + noise);
                int b = clamp(colour[2] + noise);
                image.setRGB(x, y, (0xFF << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return image;
    }

    /** Whether the motif is painted at this pixel. */
    private static boolean motifAt(String kind, int x, int y, Random random) {
        double centre = (SIZE - 1) / 2.0;
        double dx = x - centre;
        double dy = y - centre;
        return switch (kind) {
            case "dots" -> (x % 4 == 2) && (y % 4 == 2);
            case "hazard" -> ((x + y) / 3) % 2 == 0;
            case "rings" -> ((int) Math.round(Math.sqrt(dx * dx + dy * dy))) % 3 == 0;
            case "checker" -> ((x / 4) + (y / 4)) % 2 == 0;
            case "cross" -> x == 7 || x == 8 || y == 7 || y == 8;
            case "nuclear" -> {
                double angle = Math.atan2(dy, dx);
                double radius = Math.sqrt(dx * dx + dy * dy);
                boolean sector = ((int) Math.floor((angle + Math.PI) / (2 * Math.PI / 6))) % 2 == 0;
                yield (radius < 2.2) || (radius < 6.5 && sector);
            }
            case "flame" -> {
                double heat = (SIZE - y) / (double) SIZE + random.nextDouble() * 0.35;
                yield heat > 0.7 || (heat > 0.5 && (x + y) % 3 == 0);
            }
            case "bio" -> random.nextDouble() < 0.22;
            case "circuit" -> (x % 4 == 0) || (y % 4 == 0) && random.nextDouble() < 0.6;
            case "glyph" -> Math.abs(dx) + Math.abs(dy) <= 5 && Math.abs(Math.abs(dx) + Math.abs(dy) - 4) < 1.2;
            default -> ((x + y) / 2) % 3 == 0;
        };
    }

    /** Parses {@code #RRGGBB} (or {@code RRGGBB}) into {r, g, b}. Falls back to a neutral colour. */
    static int[] parse(String hex, int fallback) {
        String text = hex == null ? "" : hex.trim();
        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        int value = fallback;
        try {
            value = Integer.parseInt(text, 16);
        } catch (NumberFormatException ignored) {
            // keep the fallback
        }
        return new int[]{(value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF};
    }

    private static int clamp(double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value)));
    }
}
