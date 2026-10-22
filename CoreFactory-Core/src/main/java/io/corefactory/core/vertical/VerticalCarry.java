package io.corefactory.core.vertical;

import java.util.ArrayList;
import java.util.List;

/**
 * The items on one vertical tower. The tower is a line of {@link #blocks()} blocks: coordinate 0 is the input end, and
 * coordinate {@code blocks} is the output end, the edge beyond the output belt. The caller maps a coordinate to a height:
 * {@code bottom.y + p} for an UP tower, {@code top.y + 1 - p} for a DOWN tower.
 *
 * <p>An item moves at the tower's speed toward the output end. The items keep at least {@code 1 / capacityPerBlock} between
 * them, so a block holds at most its capacity. At the output end up to {@code capacityPerBlock} items gather and wait until
 * the output cell takes them with {@link #output(int)}; the items behind them back up. Items are kept from the output end
 * backward, so the first item of the list is the one nearest the output.
 */
public final class VerticalCarry {

    private static final double EPS = 1e-9;

    private final int blocks;
    private final List<Double> positions = new ArrayList<>();

    /**
     * @param blocks the tower's length in blocks, at least 1
     * @throws IllegalArgumentException when the length is below 1
     */
    public VerticalCarry(int blocks) {
        if (blocks < 1) {
            throw new IllegalArgumentException("a tower has at least one block: " + blocks);
        }
        this.blocks = blocks;
    }

    /** The tower's length in blocks; the output end is at this coordinate. */
    public int blocks() {
        return blocks;
    }

    /** The number of items on the tower, including the ones waiting at the output end. */
    public int size() {
        return positions.size();
    }

    /** The number of items that have reached the output end and wait for the output cell. */
    public int waiting() {
        int count = 0;
        for (double position : positions) {
            if (position >= blocks - EPS) {
                count++;
            }
        }
        return count;
    }

    /** The coordinate of each item, nearest the output end first. The list cannot be changed. */
    public List<Double> positions() {
        return List.copyOf(positions);
    }

    /**
     * Puts an item on the tower at the entry edge of a block: the input end for block 0, or the side of a belt in the middle
     * of the tower.
     *
     * @param block the block index from the input end, 0 to {@code blocks - 1}
     * @param capacityPerBlock the items per block at this tower's level
     * @return false, and nothing changes, when the item would be closer than {@code 1 / capacityPerBlock} to a neighbour
     * @throws IllegalArgumentException when the block or the capacity is out of range
     */
    public boolean offer(int block, int capacityPerBlock) {
        if (block < 0 || block >= blocks) {
            throw new IllegalArgumentException("block must be 0 to " + (blocks - 1) + ": " + block);
        }
        checkCapacity(capacityPerBlock);
        double gap = 1.0 / capacityPerBlock;
        double entry = block;
        int index = 0;
        while (index < positions.size() && positions.get(index) > entry) {
            index++;
        }
        if (index > 0 && positions.get(index - 1) - entry < gap - EPS) {
            return false;
        }
        if (index < positions.size() && entry - positions.get(index) < gap - EPS) {
            return false;
        }
        positions.add(index, entry);
        return true;
    }

    /**
     * Moves the items on by one time step.
     *
     * <p>Each item moves {@code speed * dt} blocks toward the output end, and never past the item ahead of it: next to an
     * item that is in the output end, it stops at {@code 1 / capacityPerBlock} behind, unless it may join the output end
     * (fewer than {@code capacityPerBlock} items ahead of it).
     *
     * @param dt the time step in seconds, zero or more
     * @param speedBlocksPerSecond the tower's carry speed
     * @param capacityPerBlock the items per block at this tower's level
     * @return the items waiting at the output end after the step; the caller passes the number the output cell accepts to
     *         {@link #output(int)}, and only then do they leave the tower
     * @throws IllegalArgumentException when a value is out of range
     */
    public int step(double dt, double speedBlocksPerSecond, int capacityPerBlock) {
        if (!Double.isFinite(dt) || dt < 0.0) {
            throw new IllegalArgumentException("time step must be finite and not negative: " + dt);
        }
        if (!Double.isFinite(speedBlocksPerSecond) || speedBlocksPerSecond <= 0.0) {
            throw new IllegalArgumentException("speed must be finite and above zero: " + speedBlocksPerSecond);
        }
        checkCapacity(capacityPerBlock);
        double distance = speedBlocksPerSecond * dt;
        double gap = 1.0 / capacityPerBlock;
        double exit = blocks;
        for (int i = 0; i < positions.size(); i++) {
            double current = positions.get(i);
            double target = current + distance;
            double limit;
            if (i == 0) {
                limit = exit;
            } else {
                double ahead = positions.get(i - 1);
                if (ahead >= exit - EPS) {
                    limit = (i < capacityPerBlock && target >= exit) ? exit : exit - gap;
                } else {
                    limit = ahead - gap;
                }
            }
            double next = Math.max(current, Math.min(target, limit));
            positions.set(i, next >= exit - EPS ? exit : next);
        }
        return waiting();
    }

    /**
     * Removes the items the output cell accepted. They are the ones waiting at the output end, nearest first.
     *
     * @param accepted how many of the waiting items the output cell took, 0 to {@link #waiting()}
     * @throws IllegalArgumentException when the count is outside 0 to the number waiting
     */
    public void output(int accepted) {
        if (accepted < 0 || accepted > waiting()) {
            throw new IllegalArgumentException("accepted must be 0 to " + waiting() + ": " + accepted);
        }
        positions.subList(0, accepted).clear();
    }

    private static void checkCapacity(int capacityPerBlock) {
        if (capacityPerBlock < 1) {
            throw new IllegalArgumentException("capacity per block must be at least 1: " + capacityPerBlock);
        }
    }
}
