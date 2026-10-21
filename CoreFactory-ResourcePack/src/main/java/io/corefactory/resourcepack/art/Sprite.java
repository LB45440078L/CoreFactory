package io.corefactory.resourcepack.art;

/**
 * Shades a solid shape the way a lit item looks: a light falloff from the top left, a bright bevel on the lit edges,
 * a darker bevel on the shaded edges, fine grain, and a one-pixel dark outline.
 */
final class Sprite {

    private Sprite() {
    }

    static void body(Canvas c, Area shape, int base, long seed) {
        int light = Color.mix(base, Color.WHITE, 0.45);
        int dark = Color.scale(base, 0.5);
        for (int y = 0; y < Canvas.SIZE; y++) {
            for (int x = 0; x < Canvas.SIZE; x++) {
                if (!covered(shape, x, y)) {
                    continue;
                }
                double t = (x + y) / 62.0;
                int colour = Color.mix(Color.mix(light, dark, t), base, 0.3);
                colour = Color.scale(colour, 1 + (Noise.hash(x, y, seed) - 0.5) * 0.12);
                if (edge(shape, x, y) == 1) {
                    int bevel = bevel(shape, x, y);
                    if (bevel > 0) {
                        colour = Color.mix(colour, Color.WHITE, 0.5);
                    } else if (bevel < 0) {
                        colour = Color.scale(colour, 0.55);
                    }
                }
                c.set(x, y, colour);
            }
        }
        int outline = Color.scale(dark, 0.45);
        for (int y = 0; y < Canvas.SIZE; y++) {
            for (int x = 0; x < Canvas.SIZE; x++) {
                if (!covered(shape, x, y) && touches(shape, x, y)) {
                    c.set(x, y, outline);
                }
            }
        }
    }

    private static boolean covered(Area shape, int x, int y) {
        return Canvas.contains(x, y) && shape.covers(x + 0.5, y + 0.5);
    }

    /** Chebyshev distance from the pixel to the nearest pixel outside the shape. */
    private static int edge(Area shape, int x, int y) {
        for (int r = 1; r <= 3; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) == r && !covered(shape, x + dx, y + dy)) {
                        return r;
                    }
                }
            }
        }
        return 4;
    }

    /** +1 on the lit side (outside to the left or above), -1 on the shaded side, 0 otherwise. */
    private static int bevel(Area shape, int x, int y) {
        boolean lit = !covered(shape, x - 1, y) || !covered(shape, x, y - 1);
        boolean shaded = !covered(shape, x + 1, y) || !covered(shape, x, y + 1);
        if (lit == shaded) {
            return 0;
        }
        return lit ? 1 : -1;
    }

    private static boolean touches(Area shape, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ((dx != 0 || dy != 0) && covered(shape, x + dx, y + dy)) {
                    return true;
                }
            }
        }
        return false;
    }
}
