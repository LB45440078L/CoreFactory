package io.corefactory.paper.machine;

import io.corefactory.core.process.ItemRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Which vanilla blocks are storage a belt or a picker or a splitter may deliver into: a chest, a trapped chest, a barrel, a shulker
 * box of any colour, and a copper chest of every oxidation stage, waxed or not. A furnace, a brewing stand, a dispenser, a hopper and
 * an ender chest are not storage here: they are not chest-like inventories the network should fill. The names are kept as text so the
 * rule compiles against every API the build targets; a test checks that each name is a material of the API.
 *
 * <p>A shulker box may not hold another shulker box, so a shulker box item is refused by a shulker box container. Pure over names, so
 * it is tested without a server.
 */
public final class StorageKinds {

    private static final List<String> COLOURS = List.of("WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK",
            "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK");

    private static final Set<String> SHULKER_BOXES = shulkerBoxes();

    private static final String CRAFTER = "CRAFTER";

    private static final Set<String> STORAGE = storage();

    private StorageKinds() {
    }

    /** True for a block a belt, picker or splitter may deliver into: a chest, a trapped chest, a barrel, a shulker box or a copper chest. */
    public static boolean isStorage(String material) {
        return STORAGE.contains(material);
    }

    /** True for a crafter: a storage a picker takes from and a belt or picker puts into, through its nine grid slots. */
    public static boolean isCrafter(String material) {
        return CRAFTER.equals(material);
    }

    /** True for a shulker box of any colour, or the plain shulker box. */
    public static boolean isShulkerBox(String material) {
        return SHULKER_BOXES.contains(material);
    }

    /**
     * True when an item may not go into a container: a shulker box item into a shulker box. A component is never a shulker box.
     *
     * @param container the container's material name
     * @param item      the item's reference
     */
    public static boolean refuses(String container, ItemRef item) {
        return item.kind() == ItemRef.Kind.MATERIAL && isShulkerBox(container) && isShulkerBox(item.name());
    }

    private static Set<String> shulkerBoxes() {
        List<String> names = new ArrayList<>();
        names.add("SHULKER_BOX");
        for (String colour : COLOURS) {
            names.add(colour + "_SHULKER_BOX");
        }
        return Set.copyOf(names);
    }

    private static Set<String> storage() {
        List<String> names = new ArrayList<>(List.of("CHEST", "TRAPPED_CHEST", "BARREL",
                "COPPER_CHEST", "EXPOSED_COPPER_CHEST", "WEATHERED_COPPER_CHEST", "OXIDIZED_COPPER_CHEST",
                "WAXED_COPPER_CHEST", "WAXED_EXPOSED_COPPER_CHEST", "WAXED_WEATHERED_COPPER_CHEST", "WAXED_OXIDIZED_COPPER_CHEST"));
        names.addAll(SHULKER_BOXES);
        return Set.copyOf(names);
    }
}
