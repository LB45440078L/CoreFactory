package io.moretntplus.core.item;

import io.moretntplus.api.TntDefinition;
import io.moretntplus.core.config.Messages;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Optional;

/**
 * Builds TNT and component items and reads their identity back. Identity lives in the item's persistent data
 * ({@code moretntplus:tnt_id} or {@code moretntplus:component_id}), and the look comes from the item model
 * {@code moretntplus:tnt/<id>} or {@code moretntplus:component/<id>}, which the resource pack defines.
 */
public final class ItemFactory {

    public static final String NAMESPACE = "moretntplus";

    private final NamespacedKey tntKey = new NamespacedKey(NAMESPACE, "tnt_id");
    private final NamespacedKey componentKey = new NamespacedKey(NAMESPACE, "component_id");

    public ItemStack tnt(TntDefinition definition, int amount) {
        ItemStack item = new ItemStack(definition.material(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(Messages.color(definition.displayName()));
        meta.setLore(color(definition.lore()));
        meta.setItemModel(new NamespacedKey(NAMESPACE, "tnt/" + definition.id()));
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
        meta.setItemModel(new NamespacedKey(NAMESPACE, "component/" + definition.id()));
        meta.getPersistentDataContainer().set(componentKey, PersistentDataType.STRING, definition.id());
        item.setItemMeta(meta);
        return item;
    }

    public Optional<String> tntId(ItemStack item) {
        return readId(item, tntKey);
    }

    public Optional<String> componentId(ItemStack item) {
        return readId(item, componentKey);
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
