package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * The timing and the spacing of what a shell in the air leaves behind it: the puffs of its trail, which are spread between its position of the
 * last tick and its position now, and the whistle, which sounds at a steady interval. A fast shell moves several blocks a tick, so its puffs are
 * spread over that gap rather than dropped at one point. Pure, so the spacing and the timing are tested without a server.
 */
final class ArtilleryTrail {

    /** The distance between two puffs of a trail, in blocks, when the trail is not capped. */
    static final double PUFF_SPACING_BLOCKS = 2.0;

    private ArtilleryTrail() {
    }

    /**
     * The points of a trail between two positions of a shell, one tick apart. They are spaced evenly from the earlier position, not
     * included, to the later one, included, at {@link #PUFF_SPACING_BLOCKS} apart, and no more than {@code maxPuffs} of them.
     *
     * @param from      the shell's position of the last tick
     * @param to        the shell's position now
     * @param maxPuffs  the most points, 0 or more
     * @return the points, in order from the earlier position: none when the shell did not move or no puff is allowed
     */
    static List<Position> puffs(Position from, Position to, int maxPuffs) {
        double dx = to.x() - from.x();
        double dy = to.y() - from.y();
        double dz = to.z() - from.z();
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int count = (int) Math.min(Math.max(0, maxPuffs), Math.ceil(length / PUFF_SPACING_BLOCKS));
        List<Position> points = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            double t = (double) i / count;
            points.add(new Position(from.x() + dx * t, from.y() + dy * t, from.z() + dz * t));
        }
        return points;
    }

    /**
     * True when a periodic effect of an interval is due between two times: when the interval has been crossed. A step that ends exactly on a
     * multiple of the interval counts, and a step that starts on one does not, so an effect sounds once per interval, however the steps fall.
     *
     * @param before   the time before the step, in ticks
     * @param after    the time after the step, in ticks; after the before
     * @param interval the interval, in ticks, above 0
     * @return true when the interval was crossed
     */
    static boolean crossed(long before, long after, int interval) {
        return after / interval > before / interval;
    }

    /**
     * True on the ticks of a shell's flight when its whistle sounds: every interval, and not on the launch.
     *
     * @param tick     the tick of the flight, from 0 at the launch
     * @param interval the interval between two whistles, in ticks, above 0
     * @return true when the whistle sounds
     */
    static boolean whistleDue(long tick, int interval) {
        return tick > 0 && tick % interval == 0;
    }
}
