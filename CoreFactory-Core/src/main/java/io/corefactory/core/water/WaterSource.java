package io.corefactory.core.water;

import io.corefactory.core.energy.Pos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The source check of a pump's network: the water blocks connected to a block of the network, counted by a 6-connected flood fill that
 * stops at a cap. A block of the network may stand in water, or on land beside water: a source tube's end goes into the water and
 * joins the pump on its side.
 */
public final class WaterSource {

    private WaterSource() {
    }

    /** Tells whether the block at a position is water. */
    @FunctionalInterface
    public interface BlockProbe {
        boolean isWater(int x, int y, int z);
    }

    /**
     * Counts the water blocks 6-connected to the start, walking through water only. The start counts when it is water.
     * The fill stops as soon as the count reaches the cap.
     *
     * @param cap the largest count wanted, at least 1
     * @return the count, at most {@code cap}; 0 when the start is not water
     */
    public static int sourceBlocks(int startX, int startY, int startZ, BlockProbe isWater, int cap) {
        Objects.requireNonNull(isWater, "probe");
        if (cap < 1) {
            throw new IllegalArgumentException("cap must be at least 1, got " + cap);
        }
        if (!isWater.isWater(startX, startY, startZ)) {
            return 0;
        }
        return flood(List.of(new Pos(startX, startY, startZ)), isWater, cap);
    }

    /**
     * Counts the water blocks connected to a block that may stand in water or beside it: the flood fill starts from the block itself
     * when it is water, and from each water block on its six faces. The fill stops at the cap.
     *
     * @param cap the largest count wanted, at least 1
     * @return the count, at most {@code cap}; 0 when no water touches the block
     */
    public static int sourceAround(int x, int y, int z, BlockProbe isWater, int cap) {
        Objects.requireNonNull(isWater, "probe");
        if (cap < 1) {
            throw new IllegalArgumentException("cap must be at least 1, got " + cap);
        }
        Pos at = new Pos(x, y, z);
        List<Pos> seeds = new ArrayList<>();
        if (isWater.isWater(x, y, z)) {
            seeds.add(at);
        }
        for (Face face : Face.values()) {
            Pos next = face.step(at);
            if (isWater.isWater(next.x(), next.y(), next.z())) {
                seeds.add(next);
            }
        }
        return seeds.isEmpty() ? 0 : flood(seeds, isWater, cap);
    }

    /**
     * Whether the start has at least {@code minimum} connected water blocks.
     *
     * @param minimum the number of water blocks a pump needs, at least 1
     */
    public static boolean hasSource(int x, int y, int z, BlockProbe isWater, int minimum) {
        return sourceBlocks(x, y, z, isWater, minimum) >= minimum;
    }

    private static int flood(List<Pos> starts, BlockProbe isWater, int cap) {
        Set<Pos> seen = new HashSet<>();
        Deque<Pos> queue = new ArrayDeque<>();
        int count = 0;
        for (Pos start : starts) {
            if (seen.add(start)) {
                queue.add(start);
                count++;
                if (count >= cap) {
                    return count;
                }
            }
        }
        while (!queue.isEmpty()) {
            Pos current = queue.poll();
            for (Face face : Face.values()) {
                Pos next = face.step(current);
                if (!seen.add(next) || !isWater.isWater(next.x(), next.y(), next.z())) {
                    continue;
                }
                count++;
                if (count >= cap) {
                    return count;
                }
                queue.add(next);
            }
        }
        return count;
    }
}
