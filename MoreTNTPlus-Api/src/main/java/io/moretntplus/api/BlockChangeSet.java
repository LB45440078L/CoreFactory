package io.moretntplus.api;

import org.bukkit.block.data.BlockData;

import java.util.Arrays;

/**
 * A reusable, primitive-backed list of block writes. Mechanics fill one of these instead of calling
 * {@code Block#setType} directly, so the core can apply it in batches through WorldEdit or FAWE and,
 * for large blasts, spread the work over several ticks.
 *
 * <p>Later writes to the same position win when the set is applied.
 */
public final class BlockChangeSet {

    private int[] xs;
    private int[] ys;
    private int[] zs;
    private BlockData[] data;
    private int size;

    public BlockChangeSet() {
        this(256);
    }

    public BlockChangeSet(int initialCapacity) {
        int capacity = Math.max(16, initialCapacity);
        xs = new int[capacity];
        ys = new int[capacity];
        zs = new int[capacity];
        data = new BlockData[capacity];
    }

    /** Queues one block write. */
    public void set(int x, int y, int z, BlockData blockData) {
        if (size == xs.length) {
            grow();
        }
        xs[size] = x;
        ys[size] = y;
        zs[size] = z;
        data[size] = blockData;
        size++;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int x(int index) {
        return xs[index];
    }

    public int y(int index) {
        return ys[index];
    }

    public int z(int index) {
        return zs[index];
    }

    public BlockData data(int index) {
        return data[index];
    }

    /** Removes every queued write, keeping the allocated arrays for reuse. */
    public void clear() {
        size = 0;
        Arrays.fill(data, 0, data.length, null);
    }

    private void grow() {
        int capacity = xs.length * 2;
        xs = Arrays.copyOf(xs, capacity);
        ys = Arrays.copyOf(ys, capacity);
        zs = Arrays.copyOf(zs, capacity);
        data = Arrays.copyOf(data, capacity);
    }
}
