package io.corefactory.resourcepack.art;

/**
 * Pictograms drawn from simple shapes. Each one is drawn centred on ({@code cx}, {@code cy}) within a radius of
 * {@code r} pixels, in an {@code ink} colour with an {@code accent} colour for detail.
 */
public enum Icon {
    RADIATION, MUSHROOM, BOMB, BURST, ATOM, ORB, SUN, WARNING, BIOHAZARD, FLAME, CHAIN, CONE, SPIRAL, ARROWS_IN,
    LINK, HOURGLASS, TESSERACT, CRYSTAL, DROP, SPROUT, MOUNTAIN, DIE, CHISEL, UP, PORTAL, FLIP, QUESTION, TARGET,
    WAVE, SPLIT, RINGS, SHARDS, ECHO, MUTE, BARS, CROSSHAIR, ANCHOR, SKULL, FLARE;

    public void draw(Canvas c, double cx, double cy, double r, int ink, int accent) {
        switch (this) {
            case RADIATION -> {
                c.paint(Area.disc(cx, cy, 0.22 * r), ink);
                for (int k = 0; k < 3; k++) {
                    double a = -Math.PI / 2 + k * 2 * Math.PI / 3;
                    c.paint(Area.arc(cx, cy, 0.4 * r, r, a - Math.PI / 3.2, a + Math.PI / 3.2), ink);
                }
            }
            case MUSHROOM -> {
                c.paint(Area.polygon(scaled(cx, cy, r, -0.2, 0, 0.2, 0, 0.36, 1, -0.36, 1)), ink);
                c.paint(Area.ellipse(cx, cy - 0.2 * r, r, 0.6 * r), ink);
                c.paint(Area.ellipse(cx - 0.35 * r, cy - 0.4 * r, 0.35 * r, 0.18 * r), accent);
            }
            case BOMB -> {
                c.paint(Area.ellipse(cx - 0.1 * r, cy + 0.25 * r, 0.7 * r, 0.85 * r), ink);
                c.paint(Area.line(cx + 0.35 * r, cy - 0.5 * r, cx + 0.8 * r, cy - 0.95 * r, 0.22 * r), ink);
                c.paint(Area.disc(cx + 0.85 * r, cy - 1.0 * r, 0.2 * r), accent);
                c.paint(Area.ellipse(cx - 0.4 * r, cy + 0.05 * r, 0.18 * r, 0.3 * r), accent);
            }
            case BURST -> {
                double[] star = new double[32];
                for (int i = 0; i < 16; i++) {
                    double a = -Math.PI / 2 + i * Math.PI / 8;
                    double reach = i % 2 == 0 ? r : 0.5 * r;
                    star[2 * i] = cx + Math.cos(a) * reach;
                    star[2 * i + 1] = cy + Math.sin(a) * reach;
                }
                c.paint(Area.polygon(star), ink);
                c.paint(Area.disc(cx, cy, 0.25 * r), accent);
            }
            case ATOM -> {
                c.paint(Area.disc(cx, cy, 0.22 * r), accent);
                for (int k = 0; k < 3; k++) {
                    c.paint(Area.orbit(cx, cy, r, 0.36 * r, k * Math.PI / 3, 0.2), ink);
                }
            }
            case ORB -> {
                c.paint(Area.disc(cx, cy, r), ink);
                c.paint(Area.orbit(cx, cy, r, 0.4 * r, 0.35, 0.12), accent);
                c.paint(Area.disc(cx - 0.35 * r, cy - 0.35 * r, 0.22 * r), accent);
            }
            case SUN -> {
                c.paint(Area.disc(cx, cy, 0.45 * r), ink);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    c.paint(Area.line(cx + Math.cos(a) * 0.6 * r, cy + Math.sin(a) * 0.6 * r,
                            cx + Math.cos(a) * r, cy + Math.sin(a) * r, 0.28 * r), accent);
                }
            }
            case WARNING -> {
                c.paint(Area.polygon(scaled(cx, cy, r, 0, -1, 1, 0.85, -1, 0.85)), ink);
                c.paint(Area.rect(cx - 0.12 * r, cy - 0.5 * r, cx + 0.12 * r, cy + 0.25 * r), accent);
                c.paint(Area.disc(cx, cy + 0.55 * r, 0.14 * r), accent);
            }
            case BIOHAZARD -> {
                for (int k = 0; k < 3; k++) {
                    double a = -Math.PI / 2 + k * 2 * Math.PI / 3;
                    c.paint(Area.disc(cx + Math.cos(a) * 0.55 * r, cy + Math.sin(a) * 0.55 * r, 0.45 * r), ink);
                }
                c.paint(Area.disc(cx, cy, 0.3 * r), accent);
                c.paint(Area.disc(cx, cy, 0.14 * r), ink);
            }
            case FLAME -> {
                double[] outer = {-0.6, 0.9, -0.95, 0.2, -0.5, -0.2, -0.3, -0.9, 0, -0.4, 0.2, -1, 0.6, -0.1, 0.95, 0.3,
                        0.6, 0.9};
                c.paint(Area.polygon(scaled(cx, cy, r, outer)), ink);
                c.paint(Area.polygon(scaled(cx, cy, r, shrink(outer, 0.5, 0.35))), accent);
            }
            case CHAIN -> {
                c.paint(Area.orbit(cx - 0.45 * r, cy, 0.5 * r, 0.95 * r, 0, 0.16), ink);
                c.paint(Area.orbit(cx + 0.45 * r, cy, 0.5 * r, 0.95 * r, 0, 0.16), accent);
            }
            case CONE -> {
                c.paint(Area.polygon(scaled(cx, cy, r, -1, -0.9, 1, -0.9, 0, 1)), ink);
                c.paint(Area.ellipse(cx, cy - 0.9 * r, r, 0.25 * r), accent);
            }
            case SPIRAL -> {
                c.paint(Area.arc(cx, cy, 0.55 * r, r, -Math.PI / 2, Math.PI / 2), ink);
                c.paint(Area.arc(cx, cy, 0.15 * r, 0.55 * r, Math.PI / 2, 3 * Math.PI / 2), accent);
                c.paint(Area.disc(cx, cy, 0.14 * r), accent);
            }
            case ARROWS_IN -> {
                double[] arrow = {0, -0.15, -0.45, -0.95, 0.45, -0.95};
                for (int k = 0; k < 4; k++) {
                    c.paint(Area.polygon(scaled(cx, cy, r, rotated(k * Math.PI / 2, arrow))), ink);
                }
                c.paint(Area.disc(cx, cy, 0.12 * r), accent);
            }
            case LINK -> {
                c.paint(Area.line(cx - 0.6 * r, cy, cx + 0.6 * r, cy, 0.22 * r), accent);
                c.paint(Area.disc(cx - 0.6 * r, cy, 0.4 * r), ink);
                c.paint(Area.disc(cx + 0.6 * r, cy, 0.4 * r), ink);
            }
            case HOURGLASS -> {
                c.paint(Area.polygon(scaled(cx, cy, r, -0.75, -1, 0.75, -1, 0, 0)), ink);
                c.paint(Area.polygon(scaled(cx, cy, r, -0.75, 1, 0.75, 1, 0, 0)), ink);
                c.paint(Area.rect(cx - 0.85 * r, cy - 1.15 * r, cx + 0.85 * r, cy - 0.95 * r), ink);
                c.paint(Area.rect(cx - 0.85 * r, cy + 0.95 * r, cx + 0.85 * r, cy + 1.15 * r), ink);
                c.paint(Area.disc(cx, cy + 0.5 * r, 0.2 * r), accent);
            }
            case TESSERACT -> {
                c.paint(Area.rect(cx - r, cy - r, cx + r, cy + r)
                        .minus(Area.rect(cx - 0.7 * r, cy - 0.7 * r, cx + 0.7 * r, cy + 0.7 * r)), ink);
                c.paint(Area.rect(cx - 0.5 * r, cy - 0.5 * r, cx + 0.5 * r, cy + 0.5 * r)
                        .minus(Area.rect(cx - 0.25 * r, cy - 0.25 * r, cx + 0.25 * r, cy + 0.25 * r)), accent);
                for (int sx = -1; sx <= 1; sx += 2) {
                    for (int sy = -1; sy <= 1; sy += 2) {
                        c.paint(Area.line(cx + sx * r, cy + sy * r, cx + sx * 0.5 * r, cy + sy * 0.5 * r,
                                0.14 * r), ink);
                    }
                }
            }
            case CRYSTAL -> {
                double[] shard = {0, -1, 0.6, -0.35, 0.5, 0.8, 0, 1, -0.5, 0.8, -0.6, -0.35};
                c.paint(Area.polygon(scaled(cx, cy, r, shard)), ink);
                c.paint(Area.polygon(scaled(cx, cy, r, 0, -1, 0.6, -0.35, 0.5, 0.8, 0, 1)),
                        Color.mix(ink, accent, 0.5));
            }
            case DROP -> {
                c.paint(Area.polygon(scaled(cx, cy, r, -0.7, 0.3, 0.7, 0.3, 0, -1)), ink);
                c.paint(Area.disc(cx, cy + 0.3 * r, 0.7 * r), ink);
                c.paint(Area.ellipse(cx - 0.3 * r, cy, 0.18 * r, 0.35 * r), accent);
            }
            case SPROUT -> {
                c.paint(Area.line(cx, cy + r, cx, cy - 0.1 * r, 0.22 * r), ink);
                c.paint(Area.polygon(scaled(cx, cy, r, 0, -0.1, -0.95, -0.55, -0.6, -1, 0, -0.4)), ink);
                c.paint(Area.polygon(scaled(cx, cy, r, 0, -0.1, 0.95, -0.55, 0.6, -1, 0, -0.4)), accent);
            }
            case MOUNTAIN -> {
                c.paint(Area.polygon(scaled(cx, cy, r, -1, 1, 0, -1, 1, 1)), ink);
                c.paint(Area.polygon(scaled(cx, cy, r, -0.4, -0.4, 0, -1, 0.4, -0.4)), accent);
            }
            case DIE -> {
                c.paint(Area.roundRect(cx - r, cy - r, cx + r, cy + r, 0.3 * r), ink);
                double[][] pips = {{-0.5, -0.5}, {0.5, -0.5}, {0, 0}, {-0.5, 0.5}, {0.5, 0.5}};
                for (double[] p : pips) {
                    c.paint(Area.disc(cx + p[0] * r, cy + p[1] * r, 0.17 * r), accent);
                }
            }
            case CHISEL -> {
                double[] blade = rotated(Math.PI / 4, -0.22, -1, 0.22, -1, 0.22, 0.3, 0, 0.9, -0.22, 0.3);
                c.paint(Area.polygon(scaled(cx, cy, r, blade)), ink);
                c.paint(Area.polygon(scaled(cx, cy, r, rotated(Math.PI / 4, -0.22, -1, 0, -1, 0, 0.9, -0.22, 0.3))),
                        accent);
            }
            case UP -> c.paint(Area.polygon(scaled(cx, cy, r, 0, -1, 0.6, -0.1, 0.25, -0.1, 0.25, 1, -0.25, 1,
                    -0.25, -0.1, -0.6, -0.1)), ink);
            case PORTAL -> {
                c.paint(Area.ring(cx, cy, 0.8 * r, 0.32 * r), ink);
                c.paint(Area.disc(cx, cy, 0.35 * r), accent);
            }
            case FLIP -> {
                c.paint(Area.polygon(scaled(cx, cy, r, -0.6, -0.1, 0, -1, 0.6, -0.1)), ink);
                c.paint(Area.polygon(scaled(cx, cy, r, -0.6, 0.1, 0, 1, 0.6, 0.1)), accent);
            }
            case QUESTION -> {
                c.paint(Area.arc(cx, cy - 0.35 * r, 0.38 * r, 0.7 * r, Math.PI, 2 * Math.PI), ink);
                c.paint(Area.line(cx + 0.54 * r, cy - 0.35 * r, cx, cy + 0.2 * r, 0.3 * r), ink);
                c.paint(Area.disc(cx, cy + 0.75 * r, 0.2 * r), accent);
            }
            case TARGET -> {
                c.paint(Area.ring(cx, cy, 0.85 * r, 0.24 * r), ink);
                c.paint(Area.ring(cx, cy, 0.45 * r, 0.24 * r), ink);
                c.paint(Area.disc(cx, cy, 0.2 * r), accent);
            }
            case WAVE -> {
                Area wave = (x, y) -> false;
                for (int i = 0; i < 8; i++) {
                    double t0 = -1 + i * 0.25;
                    double t1 = t0 + 0.25;
                    wave = wave.or(Area.line(cx + t0 * r, cy + wavy(t0) * r, cx + t1 * r, cy + wavy(t1) * r,
                            0.22 * r));
                }
                c.paint(wave, ink);
            }
            case SPLIT -> {
                c.paint(Area.polygon(scaled(cx, cy, r, -1, -1, 1, -1, -1, 1)), ink);
                c.paint(Area.polygon(scaled(cx, cy, r, 1, -1, 1, 1, -1, 1)), accent);
            }
            case RINGS -> {
                c.paint(Area.disc(cx, cy, 0.14 * r), accent);
                c.paint(Area.ring(cx, cy, 0.42 * r, 0.2 * r), ink);
                c.paint(Area.ring(cx, cy, 0.74 * r, 0.2 * r), accent);
                c.paint(Area.ring(cx, cy, 1.0 * r, 0.2 * r), ink);
            }
            case SHARDS -> {
                double[][] shards = {{-0.7, -0.6, -0.2, -0.9, -0.4, -0.2}, {0.2, -0.1, 0.9, -0.4, 0.6, 0.3},
                        {-0.5, 0.4, 0.1, 0.5, -0.2, 0.95}, {0.5, 0.6, 0.9, 0.8, 0.2, 0.95}};
                for (double[] shard : shards) {
                    c.paint(Area.polygon(scaled(cx, cy, r, shard)), ink);
                }
                c.paint(Area.polygon(scaled(cx, cy, r, -0.1, -0.7, 0.4, -0.9, 0.3, -0.4)), accent);
            }
            case ECHO -> {
                double ox = cx - 0.6 * r;
                c.paint(Area.disc(ox, cy, 0.22 * r), ink);
                c.paint(Area.arc(ox, cy, 0.4 * r, 0.6 * r, -Math.PI / 3, Math.PI / 3), ink);
                c.paint(Area.arc(ox, cy, 0.75 * r, 0.95 * r, -Math.PI / 2.4, Math.PI / 2.4), accent);
                c.paint(Area.arc(ox, cy, 1.1 * r, 1.3 * r, -Math.PI / 2.2, Math.PI / 2.2), accent);
            }
            case MUTE -> {
                c.paint(Area.polygon(scaled(cx, cy, r, -1, -0.35, -0.4, -0.35, 0.1, -0.9, 0.1, 0.9, -0.4, 0.35,
                        -1, 0.35)), ink);
                c.paint(Area.line(cx - 0.9 * r, cy - 0.9 * r, cx + 0.9 * r, cy + 0.9 * r, 0.2 * r), accent);
            }
            case BARS -> {
                double[] heights = {0.55, 1.0, 0.75};
                for (int i = 0; i < 3; i++) {
                    double x = cx + (i - 1) * 0.75 * r;
                    c.paint(Area.rect(x - 0.25 * r, cy + r - 2 * r * heights[i], x + 0.25 * r, cy + r),
                            i == 1 ? accent : ink);
                }
            }
            case CROSSHAIR -> {
                c.paint(Area.ring(cx, cy, 0.75 * r, 0.2 * r), ink);
                c.paint(Area.line(cx - r, cy, cx - 0.4 * r, cy, 0.2 * r), ink);
                c.paint(Area.line(cx + r, cy, cx + 0.4 * r, cy, 0.2 * r), ink);
                c.paint(Area.line(cx, cy - r, cx, cy - 0.4 * r, 0.2 * r), ink);
                c.paint(Area.line(cx, cy + r, cx, cy + 0.4 * r, 0.2 * r), ink);
                c.paint(Area.disc(cx, cy, 0.14 * r), accent);
            }
            case ANCHOR -> {
                c.paint(Area.ring(cx, cy - 0.7 * r, 0.25 * r, 0.22 * r), ink);
                c.paint(Area.line(cx, cy - 0.45 * r, cx, cy + 0.9 * r, 0.24 * r), ink);
                c.paint(Area.line(cx - 0.6 * r, cy - 0.25 * r, cx + 0.6 * r, cy - 0.25 * r, 0.22 * r), ink);
                c.paint(Area.arc(cx, cy + 0.1 * r, 0.6 * r, 0.9 * r, 0, Math.PI), accent);
            }
            case SKULL -> {
                c.paint(Area.disc(cx, cy - 0.15 * r, 0.85 * r).or(Area.rect(cx - 0.5 * r, cy + 0.3 * r, cx + 0.5 * r, cy + 0.9 * r)), ink);
                c.paint(Area.disc(cx - 0.38 * r, cy - 0.1 * r, 0.26 * r), accent);
                c.paint(Area.disc(cx + 0.38 * r, cy - 0.1 * r, 0.26 * r), accent);
                c.paint(Area.polygon(scaled(cx, cy, r, 0, 0.05, -0.13, 0.35, 0.13, 0.35)), accent);
                c.paint(Area.rect(cx - 0.2 * r, cy + 0.55 * r, cx - 0.06 * r, cy + 0.9 * r), accent);
                c.paint(Area.rect(cx + 0.06 * r, cy + 0.55 * r, cx + 0.2 * r, cy + 0.9 * r), accent);
            }
            case FLARE -> {
                double[] star = {0, -1, 0.18, -0.18, 1, 0, 0.18, 0.18, 0, 1, -0.18, 0.18, -1, 0, -0.18, -0.18};
                c.paint(Area.polygon(scaled(cx, cy, r, star)), ink);
                c.paint(Area.disc(cx, cy, 0.3 * r), accent);
            }
        }
    }

    private static double wavy(double t) {
        return Math.sin(t * Math.PI * 2) * 0.45;
    }

    /** Converts points given in units of {@code r} around ({@code cx}, {@code cy}) into canvas coordinates. */
    private static double[] scaled(double cx, double cy, double r, double... rel) {
        double[] out = new double[rel.length];
        for (int i = 0; i < rel.length; i += 2) {
            out[i] = cx + rel[i] * r;
            out[i + 1] = cy + rel[i + 1] * r;
        }
        return out;
    }

    private static double[] rotated(double angle, double... rel) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double[] out = new double[rel.length];
        for (int i = 0; i < rel.length; i += 2) {
            out[i] = rel[i] * cos - rel[i + 1] * sin;
            out[i + 1] = rel[i] * sin + rel[i + 1] * cos;
        }
        return out;
    }

    private static double[] shrink(double[] rel, double factor, double dy) {
        double[] out = new double[rel.length];
        for (int i = 0; i < rel.length; i += 2) {
            out[i] = rel[i] * factor;
            out[i + 1] = rel[i + 1] * factor + dy;
        }
        return out;
    }
}
