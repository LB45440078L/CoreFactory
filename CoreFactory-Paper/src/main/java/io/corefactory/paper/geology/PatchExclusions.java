package io.corefactory.paper.geology;

import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.item.ItemFactory;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

/**
 * The positions of a chunk that are never patch blocks, although the block there is the host of an ore: a block a player,
 * a piston or a dispenser put on a patch site, or a host block that was already there when the chunk was laid, such as
 * vanilla coal ore. Only the positions that are patch sites are recorded, so the record grows with the placements that
 * matter and not with every block that is built.
 *
 * <p>The record is a sorted int array of packed positions in the chunk's persistent data under
 * {@code corefactory:patch_exclusions}. One entry holds bits 0-3 for x within the chunk, bits 4-7 for z, and bits 8-17 for
 * y + 64, the layout of {@link io.corefactory.core.geology.GeologyRegistry}. A lookup is a binary search, and a write
 * copies the array once. Runs on the main thread.
 */
public final class PatchExclusions {

    static final NamespacedKey KEY = new NamespacedKey(ItemFactory.NAMESPACE, "patch_exclusions");

    private PatchExclusions() {
    }

    /**
     * @param x the block's x
     * @param y the block's y
     * @param z the block's z
     * @return the entry of the block within its chunk, or -1 when y lies outside the range an entry can store
     */
    public static int pack(int x, int y, int z) {
        if (y < OreDefinition.MIN_Y || y > OreDefinition.MAX_Y) {
            return -1;
        }
        return (x & 15) | ((z & 15) << 4) | ((y - OreDefinition.MIN_Y) << 8);
    }

    /** True when the block is excluded from being a patch block. */
    public static boolean contains(Chunk chunk, int x, int y, int z) {
        int key = pack(x, y, z);
        return key >= 0 && has(stored(chunk), key);
    }

    /** Excludes one block. A block outside the storable range is not recorded, since it can never be a patch site. */
    public static void add(Chunk chunk, int x, int y, int z) {
        int key = pack(x, y, z);
        if (key >= 0) {
            addAll(chunk, new int[]{key});
        }
    }

    /**
     * Excludes several blocks at once, given as entries from {@link #pack}. Entries already recorded are kept once.
     *
     * @param chunk the chunk the entries lie in
     * @param keys  the entries, in any order; may hold duplicates
     */
    public static void addAll(Chunk chunk, int[] keys) {
        if (keys.length == 0) {
            return;
        }
        int[] current = stored(chunk);
        int[] merged = union(current, keys);
        if (current != null && Arrays.equals(current, merged)) {
            return;
        }
        chunk.getPersistentDataContainer().set(KEY, PersistentDataType.INTEGER_ARRAY, merged);
    }

    private static int[] stored(Chunk chunk) {
        PersistentDataContainer data = chunk.getPersistentDataContainer();
        return data.get(KEY, PersistentDataType.INTEGER_ARRAY);
    }

    /** True when the entry is in the sorted record; a null record holds nothing. */
    static boolean has(int[] sorted, int key) {
        return sorted != null && Arrays.binarySearch(sorted, key) >= 0;
    }

    /** The sorted record with the entries added, each kept once. A null record counts as empty. */
    static int[] union(int[] sorted, int[] keys) {
        int[] base = sorted == null ? new int[0] : sorted;
        int[] all = Arrays.copyOf(base, base.length + keys.length);
        System.arraycopy(keys, 0, all, base.length, keys.length);
        Arrays.sort(all);
        int kept = 0;
        for (int value : all) {
            if (kept == 0 || value != all[kept - 1]) {
                all[kept++] = value;
            }
        }
        return Arrays.copyOf(all, kept);
    }
}
