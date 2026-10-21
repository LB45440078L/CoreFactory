package io.corefactory.core.item;

import io.corefactory.api.TntDefinition;
import io.corefactory.core.config.Messages;
import io.corefactory.core.geology.OreDefinition;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;

/**
 * Builds TNT, component and ore block items and reads their identity back. Identity lives in the item's persistent
 * data ({@code corefactory:tnt_id}, {@code corefactory:component_id} or {@code corefactory:ore_id}), and the look comes
 * from the item model {@code corefactory:tnt/<id>}, {@code corefactory:component/<id>} or {@code corefactory:ore/<id>},
 * which the resource pack defines. Every model key is built by {@link #modelKey}, so the kind prefix is written once.
 */
public final class ItemFactory {

    public static final String NAMESPACE = "corefactory";

    /** The model kinds the resource pack defines, each a folder under {@code items/} in the pack. */
    public static final String TNT = "tnt";
    public static final String COMPONENT = "component";
    public static final String ORE = "ore";
    public static final String MACHINE = "machine";
    public static final String PARTICLE = "particle";
    public static final String BELT = "belt";

    /** The lore line of an ore block item. */
    public static final String ORE_LORE = "&7Placeable. Mine it for raw ore; silk touch keeps the block.";

    private final NamespacedKey tntKey = new NamespacedKey(NAMESPACE, "tnt_id");
    private final NamespacedKey componentKey = new NamespacedKey(NAMESPACE, "component_id");
    private final NamespacedKey oreKey = new NamespacedKey(NAMESPACE, "ore_id");

    /**
     * The item model key of an entry: its kind and its id joined by a slash, such as {@code tnt/nuke_little_boy}. The
     * resource pack writes the definition at {@code assets/corefactory/items/<key>.json} and the model at
     * {@code assets/corefactory/models/item/<key>.json}, so the key is the same on both sides.
     *
     * @param kind one of the kinds of this class, such as {@link #TNT}
     * @param id   the entry's id
     * @return the key, without the namespace
     */
    public static String modelKey(String kind, String id) {
        return kind + "/" + id;
    }

    /**
     * The display model of a placed device or machine, keyed by its component id. Its 3D model is the machine kind's, so
     * the key is {@code machine/<componentId>}, never the flat component sprite.
     *
     * @param componentId the component id, such as {@code pylon_2} or {@code picker_1}
     * @return the key, without the namespace
     */
    public static String displayKey(String componentId) {
        return modelKey(MACHINE, componentId);
    }

    /**
     * The display model of a belt unit. The straight shape uses {@code belt/<componentId>}; the corner shape uses the corner
     * model of the same belt, {@code belt/<componentId>/corner}.
     *
     * @param componentId the belt's component id, such as {@code belt_1}
     * @param corner      true for a corner belt
     * @return the key, without the namespace
     */
    public static String beltKey(String componentId, boolean corner) {
        return modelKey(BELT, corner ? componentId + "/corner" : componentId);
    }

    public ItemStack tnt(TntDefinition definition, int amount) {
        ItemStack item = new ItemStack(definition.material(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(Messages.color(definition.displayName()));
        meta.setLore(color(definition.lore()));
        meta.setItemModel(new NamespacedKey(NAMESPACE, modelKey(TNT, definition.id())));
        meta.getPersistentDataContainer().set(tntKey, PersistentDataType.STRING, definition.id());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack component(ComponentDefinition definition, int amount) {
        ItemStack item = new ItemStack(definition.material(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(Messages.color(definition.displayName()));
        meta.setLore(color(definition.lore()));
        meta.setItemModel(new NamespacedKey(NAMESPACE, modelKey(COMPONENT, definition.id())));
        meta.getPersistentDataContainer().set(componentKey, PersistentDataType.STRING, definition.id());
        item.setItemMeta(meta);
        return item;
    }

    /**
     * The ore block item: the ore's host block, named and modelled as the ore, carrying the ore's id. Empty when the
     * host is not a block material. Placing it registers the block as the ore (see the Spigot geology listener).
     */
    public Optional<ItemStack> ore(OreDefinition ore, int amount) {
        Material host = Material.matchMaterial(ore.host());
        if (host == null || !host.isBlock()) {
            return Optional.empty();
        }
        ItemStack item = new ItemStack(host, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        meta.setDisplayName(Messages.color(ore.name()));
        meta.setLore(List.of(Messages.color(ORE_LORE)));
        meta.setItemModel(new NamespacedKey(NAMESPACE, modelKey(ORE, ore.id())));
        meta.getPersistentDataContainer().set(oreKey, PersistentDataType.STRING, ore.id());
        item.setItemMeta(meta);
        return Optional.of(item);
    }

    /**
     * The model of a placed machine, drawn over its host block. It carries no identity: the placeable item is
     * {@link #component}, which keeps the component id.
     */
    public ItemStack machine(ComponentDefinition definition) {
        ItemStack item = new ItemStack(definition.material(), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(Messages.color(definition.displayName()));
        meta.setItemModel(new NamespacedKey(NAMESPACE, displayKey(definition.id())));
        item.setItemMeta(meta);
        return item;
    }

    public Optional<String> tntId(ItemStack item) {
        return readId(item, tntKey);
    }

    public Optional<String> componentId(ItemStack item) {
        return readId(item, componentKey);
    }

    /** The ore id of an ore block item. A plain block of the same material has none. */
    public Optional<String> oreId(ItemStack item) {
        return readId(item, oreKey);
    }

    private static Optional<String> readId(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(meta.getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }

    private static List<String> color(List<String> lines) {
        return lines.stream().map(Messages::color).toList();
    }
}
