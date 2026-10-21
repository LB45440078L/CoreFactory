package io.corefactory.spigot.geology;

import io.corefactory.core.item.ItemFactory;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Reads and writes the ore data a chunk keeps in its persistent data: the geology marker
 * ({@code corefactory:geology}, the version the chunk was laid with), the registry of placed ore blocks
 * ({@code corefactory:ore_blocks}, packed by {@link io.corefactory.core.geology.GeologyRegistry}) and the volume left in
 * each crude deposit ({@code corefactory:deposit_volumes}, packed by {@link io.corefactory.core.geology.DepositVolumes}).
 */
public final class OreChunks {

    static final NamespacedKey MARKER = new NamespacedKey(ItemFactory.NAMESPACE, "geology");
    static final NamespacedKey BLOCKS = new NamespacedKey(ItemFactory.NAMESPACE, "ore_blocks");
    static final NamespacedKey VOLUMES = new NamespacedKey(ItemFactory.NAMESPACE, "deposit_volumes");

    private OreChunks() {
    }

    /** The version the chunk was laid with, or 0 when it never was. */
    public static int marker(Chunk chunk) {
        Integer version = chunk.getPersistentDataContainer().get(MARKER, PersistentDataType.INTEGER);
        return version == null ? 0 : version;
    }

    public static void setMarker(Chunk chunk, int version) {
        chunk.getPersistentDataContainer().set(MARKER, PersistentDataType.INTEGER, version);
    }

    /** The packed registry of the chunk, or null when it has no ore blocks. */
    public static int[] registry(Chunk chunk) {
        return chunk.getPersistentDataContainer().get(BLOCKS, PersistentDataType.INTEGER_ARRAY);
    }

    /** Stores the packed registry; an empty or null one removes the key. */
    public static void storeRegistry(Chunk chunk, int[] packed) {
        PersistentDataContainer data = chunk.getPersistentDataContainer();
        if (packed == null || packed.length == 0) {
            data.remove(BLOCKS);
        } else {
            data.set(BLOCKS, PersistentDataType.INTEGER_ARRAY, packed);
        }
    }

    /** The packed deposit volumes of the chunk, or null when it has none. */
    public static int[] volumes(Chunk chunk) {
        return chunk.getPersistentDataContainer().get(VOLUMES, PersistentDataType.INTEGER_ARRAY);
    }

    /** Stores the packed deposit volumes; an empty or null one removes the key. */
    public static void storeVolumes(Chunk chunk, int[] pairs) {
        PersistentDataContainer data = chunk.getPersistentDataContainer();
        if (pairs == null || pairs.length == 0) {
            data.remove(VOLUMES);
        } else {
            data.set(VOLUMES, PersistentDataType.INTEGER_ARRAY, pairs);
        }
    }
}
