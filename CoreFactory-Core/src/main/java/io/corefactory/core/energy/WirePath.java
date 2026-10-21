package io.corefactory.core.energy;

import java.util.ArrayList;
import java.util.List;

/**
 * The geometry and the budget of a wire as it is drawn: the path of a wire between its two ends, a gentle sag below the straight
 * line, and the travelling pulses that show the power the wire carries. Points are {x, y, z}. Pure, so the shape, the pulse timing
 * and the particle budget are tested without a server.
 *
 * <p>A wire sags along a catenary: the drop below the straight line is zero at both ends, maximal at the middle, and grows with the
 * length of the wire. A pulse moves from the generator end toward the pylon end, and a wire that carries no power has none.
 */
public final class WirePath {

    /** The sag of a wire per block of its length: 0.03, so a wire of 24 blocks sags 0.72 blocks. */
    public static final double SAG_PER_BLOCK = 0.03;

    /** The most a wire sags, in blocks. */
    public static final double MAX_SAG = 1.0;

    /** The steepness of the catenary, in units of its half length. Small values are close to a parabola. */
    private static final double CATENARY_K = 2.0;

    /** The fewest points of a wire: its two ends. */
    public static final int MIN_POINTS = 2;

    /** The most points of a wire. */
    public static final int MAX_POINTS = 64;

    /** The most pulses on a wire, at full flow. */
    public static final int MAX_PULSES = 3;

    /** The flow below which a wire carries no pulse. */
    public static final double MIN_FLOW = 0.02;

    /** The speed of a pulse at the least flow, in blocks per second. */
    public static final double SLOWEST_BLOCKS_PER_SECOND = 2.0;

    /** The speed of a pulse at full flow, in blocks per second. */
    public static final double FASTEST_BLOCKS_PER_SECOND = 4.0;

    private WirePath() {
    }

    /**
     * The distance between two points.
     *
     * @param a one point, {x, y, z}
     * @param b the other point, {x, y, z}
     */
    public static double length(double[] a, double[] b) {
        double dx = b[0] - a[0];
        double dy = b[1] - a[1];
        double dz = b[2] - a[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * The sag of a wire of a given length: {@link #SAG_PER_BLOCK} per block, at most {@link #MAX_SAG}. Zero for a length of 0 or less.
     *
     * @param lengthBlocks the length of the wire, in blocks
     */
    public static double sag(double lengthBlocks) {
        if (!(lengthBlocks > 0)) {
            return 0.0;
        }
        return Math.min(MAX_SAG, SAG_PER_BLOCK * lengthBlocks);
    }

    /**
     * The drop of a wire below its straight line at a point along it: zero at both ends, the sag at the middle, and a catenary
     * curve in between.
     *
     * @param t        the place along the wire, from 0 at one end to 1 at the other; clamped to that range
     * @param sagBlocks the sag of the wire, in blocks
     */
    public static double drop(double t, double sagBlocks) {
        double x = 2 * clamp01(t) - 1;
        double k = CATENARY_K;
        return sagBlocks * (Math.cosh(k) - Math.cosh(k * x)) / (Math.cosh(k) - 1);
    }

    /**
     * The point of a wire at a place along it, sagging below the straight line from a to b.
     *
     * @param a the one end, {x, y, z}
     * @param b the other end, {x, y, z}
     * @param t the place along the wire, from 0 at a to 1 at b; clamped to that range
     */
    public static double[] point(double[] a, double[] b, double t) {
        double f = clamp01(t);
        double sagBlocks = sag(length(a, b));
        return new double[] {
            a[0] + (b[0] - a[0]) * f,
            a[1] + (b[1] - a[1]) * f - drop(f, sagBlocks),
            a[2] + (b[2] - a[2]) * f};
    }

    /**
     * The number of points of a wire of a given length, one every spacing blocks along it, with both ends included. At least
     * {@link #MIN_POINTS} and at most {@link #MAX_POINTS}.
     *
     * @param lengthBlocks the length of the wire, in blocks
     * @param spacing      the distance between points, in blocks, positive
     */
    public static int pointCount(double lengthBlocks, double spacing) {
        if (!(spacing > 0)) {
            throw new IllegalArgumentException("spacing must be positive: " + spacing);
        }
        if (!(lengthBlocks > 0)) {
            return MIN_POINTS;
        }
        double steps = Math.ceil(lengthBlocks / spacing - 1e-9);
        return (int) Math.min(MAX_POINTS, Math.max(MIN_POINTS, steps + 1));
    }

    /**
     * The points of a wire from a to b, sagging, evenly spaced along it with both ends included.
     *
     * @param a       the one end, {x, y, z}
     * @param b       the other end, {x, y, z}
     * @param spacing the distance between points, in blocks, positive
     */
    public static List<double[]> path(double[] a, double[] b, double spacing) {
        int count = pointCount(length(a, b), spacing);
        List<double[]> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            points.add(point(a, b, (double) i / (count - 1)));
        }
        return points;
    }

    /**
     * The flow of a wire: the share of its power that reaches the load, from 0 to 1. A wire carries nothing when its grid has no load
     * or delivers nothing; otherwise it carries the grid's satisfaction.
     *
     * @param satisfaction the grid's satisfaction, from 0 to 1
     * @param loadW        the load of the grid, in W
     * @param carriedW     the power the grid delivered or drew from storage this tick, in W
     */
    public static double flow(double satisfaction, double loadW, double carriedW) {
        if (!(loadW > 0) || !(carriedW > 0)) {
            return 0.0;
        }
        return clamp01(satisfaction);
    }

    /**
     * The speed of a pulse for a flow, in blocks per second: from {@link #SLOWEST_BLOCKS_PER_SECOND} at a flow of 0 to
     * {@link #FASTEST_BLOCKS_PER_SECOND} at a flow of 1.
     *
     * @param flow the flow of the wire, from 0 to 1
     */
    public static double pulseSpeed(double flow) {
        return SLOWEST_BLOCKS_PER_SECOND + (FASTEST_BLOCKS_PER_SECOND - SLOWEST_BLOCKS_PER_SECOND) * clamp01(flow);
    }

    /**
     * The number of pulses on a wire for a flow: none below {@link #MIN_FLOW}, otherwise one for each third of the full flow, at
     * least one and at most {@link #MAX_PULSES}.
     *
     * @param flow the flow of the wire, from 0 to 1
     */
    public static int pulseCount(double flow) {
        double f = clamp01(flow);
        if (f < MIN_FLOW) {
            return 0;
        }
        return (int) Math.max(1L, Math.min(MAX_PULSES, Math.round(f * MAX_PULSES)));
    }

    /**
     * The place of a pulse along a wire at a time, from 0 at the generator end to 1 at the pylon end, wrapping back to 0 after the
     * end. The pulses of a wire are spaced evenly, so pulse {@code index} of {@code count} is offset by index / count of the wire.
     *
     * @param seconds         the time, in seconds
     * @param blocksPerSecond the speed of the pulse, in blocks per second
     * @param lengthBlocks    the length of the wire, in blocks
     * @param index           which pulse, from 0 to count - 1
     * @param count           how many pulses the wire has, at least 1
     */
    public static double pulseFraction(double seconds, double blocksPerSecond, double lengthBlocks, int index, int count) {
        if (!(lengthBlocks > 0) || count < 1) {
            return 0.0;
        }
        double travelled = seconds * blocksPerSecond / lengthBlocks + (double) index / count;
        double place = travelled - Math.floor(travelled);
        return place >= 1.0 ? 0.0 : place;
    }

    /**
     * The budget rule: which wires are drawn to a player within a particle cap. The wires are given nearest first, each with its cost
     * in particles (its points and its pulses). A wire is drawn whole when its cost fits in what is left; a wire that does not fit is
     * skipped, and the later ones are still tried. The costs of the drawn wires never sum to more than the cap.
     *
     * @param costs the cost of each wire, nearest first, not negative
     * @param cap   the most particles this player may be sent, not negative
     * @return for each wire, whether it is drawn
     */
    public static boolean[] fit(int[] costs, int cap) {
        boolean[] drawn = new boolean[costs.length];
        int left = Math.max(0, cap);
        for (int i = 0; i < costs.length; i++) {
            if (costs[i] >= 0 && costs[i] <= left) {
                drawn[i] = true;
                left -= costs[i];
            }
        }
        return drawn;
    }

    /**
     * A colour moved toward white: the amount 0 gives the colour, 1 gives white.
     *
     * @param rgb    the colour, as 0xRRGGBB
     * @param amount the share of white, from 0 to 1; clamped to that range
     */
    public static int brighten(int rgb, double amount) {
        double f = clamp01(amount);
        int r = mix((rgb >> 16) & 0xFF, f);
        int g = mix((rgb >> 8) & 0xFF, f);
        int b = mix(rgb & 0xFF, f);
        return (r << 16) | (g << 8) | b;
    }

    private static int mix(int channel, double f) {
        return (int) Math.round(channel + (255 - channel) * f);
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }
}
