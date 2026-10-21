package io.corefactory.core.geology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The ore positions of one chunk, packed as one int per position so they fit a chunk's persistent data. Positions
 * are given in block coordinates; only their offset inside the chunk is stored, so the chunk supplies the rest.
 *
 * <p>Bit layout of one entry, from the lowest bit up:
 * <pre>
 *   bits  0-3    x within the chunk (0-15)
 *   bits  4-7    z within the chunk (0-15)
 *   bits  8-17   y + 64 (10 bits: y runs from -64 to 959)
 *   bits 18-25   ore index (0-255)
 *   bits 26-31   zero
 * </pre>
 * An entry is 26 bits. The position part (bits 0-17) is the key: two entries with the same key are the same block.
 */
public final class GeologyRegistry {

    private static final int Y_SHIFT = 8;
    private static final int ORE_SHIFT = 18;
    private static final int KEY_MASK = (1 << ORE_SHIFT) - 1;

    private GeologyRegistry() {
    }

    /**
     * Packs one placement.
     *
     * @throws IllegalArgumentException when y or the ore index is outside the layout
     */
    public static int pack(int x, int y, int z, int oreIndex) {
        int key = key(x, y, z);
        if (key < 0) {
            throw new IllegalArgumentException("y " + y + " is outside " + OreDefinition.MIN_Y + ".."
                    + OreDefinition.MAX_Y);
        }
        if (oreIndex < 0 || oreIndex > 255) {
            throw new IllegalArgumentException("ore index " + oreIndex + " is outside 0..255");
        }
        return key | (oreIndex << ORE_SHIFT);
    }

    /** Packs placements that all lie in one chunk. */
    public static int[] encode(List<Placement> placements) {
        int[] out = new int[placements.size()];
        for (int i = 0; i < out.length; i++) {
            Placement p = placements.get(i);
            out[i] = pack(p.x(), p.y(), p.z(), p.oreIndex());
        }
        return out;
    }

    /**
     * Unpacks every entry into block coordinates.
     *
     * @param packed the entries of one chunk, or null for none
     * @param chunkX the chunk's x
     * @param chunkZ the chunk's z
     */
    public static List<Placement> decode(int[] packed, int chunkX, int chunkZ) {
        List<Placement> out = new ArrayList<>(packed == null ? 0 : packed.length);
        if (packed == null) {
            return out;
        }
        for (int value : packed) {
            out.add(new Placement(
                    (chunkX << 4) + (value & 15),
                    ((value >>> Y_SHIFT) & 1023) + OreDefinition.MIN_Y,
                    (chunkZ << 4) + ((value >>> 4) & 15),
                    (value >>> ORE_SHIFT) & 255));
        }
        return out;
    }

    /** The index of the entry at this block, or -1 when the block is not registered. */
    public static int indexOf(int[] packed, int x, int y, int z) {
        return packed == null ? -1 : find(packed, packed.length, x, y, z);
    }

    /** True when the block is registered. */
    public static boolean contains(int[] packed, int x, int y, int z) {
        return indexOf(packed, x, y, z) >= 0;
    }

    /** The ore index registered at this block, or -1 when the block is not registered. */
    public static int oreIndex(int[] packed, int x, int y, int z) {
        int at = indexOf(packed, x, y, z);
        return at < 0 ? -1 : (packed[at] >>> ORE_SHIFT) & 255;
    }

    /**
     * The registry without the block's entry. Returns the same array when the block is not registered, otherwise a
     * new, shorter array.
     */
    public static int[] remove(int[] packed, int x, int y, int z) {
        int at = indexOf(packed, x, y, z);
        if (at < 0) {
            return packed;
        }
        int[] out = new int[packed.length - 1];
        System.arraycopy(packed, 0, out, 0, at);
        System.arraycopy(packed, at + 1, out, at, packed.length - at - 1);
        return out;
    }

    /**
     * The registry with the placements added. A position already registered keeps its entry, so merging the same
     * plan twice changes nothing. Returns the same array when nothing is added.
     */
    public static int[] merge(int[] packed, List<Placement> additions) {
        int[] out = packed == null ? new int[0] : packed;
        int size = out.length;
        for (Placement p : additions) {
            if (find(out, size, p.x(), p.y(), p.z()) >= 0) {
                continue;
            }
            if (size == out.length) {
                out = Arrays.copyOf(out, Math.max(4, out.length * 2));
            }
            out[size++] = pack(p.x(), p.y(), p.z(), p.oreIndex());
        }
        return size == out.length ? out : Arrays.copyOf(out, size);
    }

    /** Index of the block's entry among the first {@code length} entries, or -1. */
    private static int find(int[] packed, int length, int x, int y, int z) {
        int key = key(x, y, z);
        if (key < 0) {
            return -1;
        }
        for (int i = 0; i < length; i++) {
            if ((packed[i] & KEY_MASK) == key) {
                return i;
            }
        }
        return -1;
    }

    /** The position part of the entry for this block, or -1 when the block lies outside the layout. */
    private static int key(int x, int y, int z) {
        if (y < OreDefinition.MIN_Y || y > OreDefinition.MAX_Y) {
            return -1;
        }
        return (x & 15) | ((z & 15) << 4) | ((y - OreDefinition.MIN_Y) << Y_SHIFT);
    }
}
