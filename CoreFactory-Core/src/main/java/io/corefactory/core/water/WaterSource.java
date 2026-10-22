package io.corefactory.core.water;

import io.corefactory.core.energy.Pos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The source check of a pump: the water blocks connected to the pump's block, counted by a 6-connected flood fill that
 * stops at a cap.
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
        Pos start = new Pos(startX, startY, startZ);
        Set<Pos> seen = new HashSet<>();
        Deque<Pos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        int count = 1;
        if (count >= cap) {
            return count;
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

    /**
     * Whether the start has at least {@code minimum} connected water blocks.
     *
     * @param minimum the number of water blocks a pump needs, at least 1
     */
    public static boolean hasSource(int x, int y, int z, BlockProbe isWater, int minimum) {
        return sourceBlocks(x, y, z, isWater, minimum) >= minimum;
    }
}
