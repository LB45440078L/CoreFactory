package io.corefactory.paper.machine;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.process.ItemRef;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

/**
 * Converts between inventory items and the item references processes use. A component item is {@code item:<id>}, any
 * other item is {@code material:<NAME>}. Slot hints are marked so they are never read as items.
 */
public final class ItemRefs {

    private static final NamespacedKey HINT = new NamespacedKey(ItemFactory.NAMESPACE, "slot_hint");

    private final CoreFactoryCore core;

    public ItemRefs(CoreFactoryCore core) {
        this.core = core;
    }

    /** The reference an item stands for. Empty for air, nothing, and a slot hint. */
    public Optional<ItemRef> of(ItemStack item) {
        if (item == null || item.getType().isAir() || isHint(item)) {
            return Optional.empty();
        }
        Optional<String> component = core.items().componentId(item);
        if (component.isPresent()) {
            return Optional.of(ItemRef.item(component.get()));
        }
        return Optional.of(ItemRef.material(item.getType().name()));
    }

    /** A stack of {@code amount} items for a reference, or null when the reference names no item. */
    public ItemStack create(ItemRef ref, int amount) {
        if (ref.kind() == ItemRef.Kind.ITEM) {
            return core.createComponentItem(ref.name(), amount).orElse(null);
        }
        Material material = Material.matchMaterial(ref.name());
        return material == null ? null : new ItemStack(material, amount);
    }

    /** The largest stack of a reference. One when the reference names no item. */
    public int limit(ItemRef ref) {
        if (ref.kind() == ItemRef.Kind.ITEM) {
            return core.createComponentItem(ref.name(), 1).map(ItemStack::getMaxStackSize).orElse(1);
        }
        Material material = Material.matchMaterial(ref.name());
        return material == null ? 1 : material.getMaxStackSize();
    }

    /** True for a slot hint, which stands for an empty slot. */
    public static boolean isHint(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(HINT, PersistentDataType.BYTE);
    }

    /** True for an empty slot: nothing in it, or only a hint. */
    public static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir() || isHint(item);
    }

    static ItemStack markHint(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(HINT, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }
}
