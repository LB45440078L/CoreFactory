package io.corefactory.resourcepack.art;

/** Colour arithmetic on packed ARGB ints. */
public final class Color {

    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;

    private Color() {
    }

    /** Parses {@code #RRGGBB} (the {@code #} is optional) into an opaque colour. Three-digit forms are rejected. */
    public static int hex(String text) {
        String digits = text.startsWith("#") ? text.substring(1) : text;
        if (digits.length() != 6) {
            throw new IllegalArgumentException("A colour is six hex digits, got: " + text);
        }
        return 0xFF000000 | Integer.parseInt(digits, 16);
    }

    public static int argb(int alpha, int red, int green, int blue) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
    }

    public static int alpha(int c) {
        return c >>> 24;
    }

    public static int red(int c) {
        return (c >>> 16) & 0xFF;
    }

    public static int green(int c) {
        return (c >>> 8) & 0xFF;
    }

    public static int blue(int c) {
        return c & 0xFF;
    }

    /** Linear blend from {@code a} at {@code t = 0} to {@code b} at {@code t = 1}, channel by channel. */
    public static int mix(int a, int b, double t) {
        double u = Math.clamp(t, 0, 1);
        return argb(lerp(alpha(a), alpha(b), u), lerp(red(a), red(b), u),
                lerp(green(a), green(b), u), lerp(blue(a), blue(b), u));
    }

    /** Multiplies the colour channels by {@code k}. Alpha is kept. */
    public static int scale(int c, double k) {
        return argb(alpha(c), channel(red(c) * k), channel(green(c) * k), channel(blue(c) * k));
    }

    /** The same colour at the given opacity, from 0 to 1. */
    public static int withAlpha(int c, double opacity) {
        return argb(channel(Math.clamp(opacity, 0, 1) * 255), red(c), green(c), blue(c));
    }

    /** Source-over compositing of {@code src} onto {@code dst}. */
    public static int over(int dst, int src) {
        double a = alpha(src) / 255.0;
        if (a >= 1) {
            return src;
        }
        if (a <= 0) {
            return dst;
        }
        double da = alpha(dst) / 255.0;
        double outAlpha = a + da * (1 - a);
        return argb(channel(outAlpha * 255),
                channel((red(src) * a + red(dst) * da * (1 - a)) / outAlpha),
                channel((green(src) * a + green(dst) * da * (1 - a)) / outAlpha),
                channel((blue(src) * a + blue(dst) * da * (1 - a)) / outAlpha));
    }

    private static int lerp(int a, int b, double t) {
        return (int) Math.round(a + (b - a) * t);
    }

    private static int channel(double value) {
        return (int) Math.round(Math.clamp(value, 0, 255));
    }
}
