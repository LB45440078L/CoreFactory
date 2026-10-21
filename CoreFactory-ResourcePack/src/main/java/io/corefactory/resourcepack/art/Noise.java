package io.corefactory.resourcepack.art;

/** Seeded, repeatable noise. The same seed and position always give the same value. */
final class Noise {

    private Noise() {
    }

    /** A pseudo-random value in [0, 1) for one pixel. */
    static double hash(int x, int y, long seed) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Smooth value noise in [0, 1), interpolated between lattice points. */
    static double smooth(double x, double y, long seed) {
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double fx = smoothstep(x - x0);
        double fy = smoothstep(y - y0);
        double top = lerp(hash(x0, y0, seed), hash(x0 + 1, y0, seed), fx);
        double bottom = lerp(hash(x0, y0 + 1, seed), hash(x0 + 1, y0 + 1, seed), fx);
        return lerp(top, bottom, fy);
    }

    private static double smoothstep(double t) {
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
