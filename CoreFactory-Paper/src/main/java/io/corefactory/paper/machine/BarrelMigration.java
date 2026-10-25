package io.corefactory.paper.machine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.ToIntFunction;

/**
 * The one-time move from the storage of older versions. Lab tools used to stand on a dispenser, a furnace and the like, and
 * ore drills on a dropper; each kept its items in machines.yml. Now they stand on a barrel and keep their items in it. This class says which block an
 * older version used for each machine, and which stored stacks can move into the barrel without overwriting anything.
 */
public final class BarrelMigration {

    private static final Map<String, String> LEGACY_HOSTS = Map.ofEntries(
            Map.entry("crusher", "DISPENSER"),
            Map.entry("roaster", "FURNACE"),
            Map.entry("separator", "CAULDRON"),
            Map.entry("purifier", "BREWING_STAND"),
            Map.entry("converter", "BLAST_FURNACE"),
            Map.entry("enrichment_cascade", "IRON_BLOCK"),
            Map.entry("transmuter", "LODESTONE"),
            Map.entry("spectrometer", "LECTERN"),
            Map.entry("particle_accelerator", "DEEPSLATE"),
            Map.entry("antimatter_condenser", "RESPAWN_ANCHOR"),
            Map.entry("research_console", "BOOKSHELF"),
            Map.entry("crude_oil_refinery", "BARREL"),
            Map.entry("ore_drill_1", "DROPPER"),
            Map.entry("ore_drill_2", "DROPPER"),
            Map.entry("ore_drill_3", "DROPPER"),
            Map.entry("ore_drill_4", "DROPPER"),
            Map.entry("ore_drill_5", "DROPPER"));

    /**
     * The machines of the ammunition, powder, turret and metal chains. No older version had them, so they have no legacy host.
     */
    private static final Set<String> ADDED_LATER = Set.of("induction_furnace", "powder_mill", "cartridge_press",
            "bullet_swaging_machine", "ammunition_assembly", "metal_lathe", "component_assembly", "heavy_weapons_assembly");

    private BarrelMigration() {
    }

    /** The block an older version stood a machine on, by machine id, or empty for a machine it never had. */
    public static Optional<String> legacyHost(String machineId) {
        return Optional.ofNullable(LEGACY_HOSTS.get(machineId));
    }

    /** Whether the machine was added after the barrel change, so it has no legacy host by design. */
    public static boolean addedLater(String machineId) {
        return ADDED_LATER.contains(machineId);
    }

    /**
     * Where the stored stacks went.
     *
     * @param moved the stacks that move, by barrel slot
     * @param kept  the stacks that stay where they are stored, by their key
     * @param <K>   the key of a stored stack
     * @param <V>   the stored stack
     */
    public record Plan<K, V>(Map<Integer, V> moved, Map<K, V> kept) {
    }

    /**
     * Plans the move of stored stacks into an empty barrel. A stack moves when its barrel slot exists and is empty. Any other
     * stack, one whose slot holds something or has no barrel slot, is kept, so nothing is overwritten or lost.
     *
     * @param stored      the stacks in the file, by key
     * @param barrelIndex the barrel slot of each key, or a negative number when it has none
     * @param empty       true for a barrel slot that holds nothing
     */
    public static <K, V> Plan<K, V> plan(Map<K, V> stored, ToIntFunction<K> barrelIndex, IntPredicate empty) {
        Map<Integer, V> moved = new LinkedHashMap<>();
        Map<K, V> kept = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : stored.entrySet()) {
            int index = barrelIndex.applyAsInt(entry.getKey());
            if (index >= 0 && !moved.containsKey(index) && empty.test(index)) {
                moved.put(index, entry.getValue());
            } else {
                kept.put(entry.getKey(), entry.getValue());
            }
        }
        return new Plan<>(moved, kept);
    }
}
