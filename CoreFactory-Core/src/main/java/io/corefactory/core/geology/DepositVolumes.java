package io.corefactory.core.geology;

import java.util.Arrays;

/**
 * The petroleum left in each crude deposit of a chunk, packed as pairs of ints: the block's key (as
 * {@link GeologyRegistry} packs it, with ore index 0) and its remaining volume. Stored in the chunk's persistent data
 * next to the registry, so it survives restarts. A block with no pair holds the full volume, so a deposit that was never
 * pumped needs no entry. Pure: the arrays are copied on change.
 */
public final class DepositVolumes {

    private DepositVolumes() {
    }

    /**
     * The volume left in the deposit at a block.
     *
     * @param pairs the packed pairs of the chunk, or null for none
     * @param full  the volume of a deposit with no stored pair
     * @return the stored volume, or {@code full} when the block has no pair or lies outside the registry layout
     */
    public static int get(int[] pairs, int x, int y, int z, int full) {
        if (!inLayout(y)) {
            return full;
        }
        int at = indexOf(pairs, GeologyRegistry.pack(x, y, z, 0));
        return at < 0 ? full : pairs[at + 1];
    }

    /**
     * The pairs with the block's volume set. Returns a new array.
     *
     * @throws IllegalArgumentException when the volume is negative or the block lies outside the registry layout
     */
    public static int[] put(int[] pairs, int x, int y, int z, int volume) {
        if (volume < 0) {
            throw new IllegalArgumentException("a deposit volume cannot be negative: " + volume);
        }
        int key = GeologyRegistry.pack(x, y, z, 0);
        int[] base = pairs == null ? new int[0] : pairs;
        int at = indexOf(base, key);
        if (at >= 0) {
            int[] out = base.clone();
            out[at + 1] = volume;
            return out;
        }
        int[] out = Arrays.copyOf(base, base.length + 2);
        out[base.length] = key;
        out[base.length + 1] = volume;
        return out;
    }

    /**
     * The pairs without the block's volume. Returns the same array when the block has no pair.
     */
    public static int[] remove(int[] pairs, int x, int y, int z) {
        if (pairs == null || !inLayout(y)) {
            return pairs;
        }
        int at = indexOf(pairs, GeologyRegistry.pack(x, y, z, 0));
        if (at < 0) {
            return pairs;
        }
        int[] out = new int[pairs.length - 2];
        System.arraycopy(pairs, 0, out, 0, at);
        System.arraycopy(pairs, at + 2, out, at, pairs.length - at - 2);
        return out;
    }

    private static boolean inLayout(int y) {
        return y >= OreDefinition.MIN_Y && y <= OreDefinition.MAX_Y;
    }

    /** Index of the key's pair (its key slot), or -1. */
    private static int indexOf(int[] pairs, int key) {
        if (pairs == null) {
            return -1;
        }
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (pairs[i] == key) {
                return i;
            }
        }
        return -1;
    }
}
