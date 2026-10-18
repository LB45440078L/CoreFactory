package io.moretntplus.spigot.gui;

import io.moretntplus.api.Mechanic;
import io.moretntplus.api.TntDefinition;
import io.moretntplus.core.MoreTntPlusCore;
import io.moretntplus.core.config.Messages;
import io.moretntplus.core.item.ComponentDefinition;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * A read-only catalogue of every TNT and component. Clicking one gives a copy, if the player may give items.
 */
public final class CatalogGui implements InventoryHolder {

    public static final String TITLE = Messages.color("&8MoreTNT+ Catalogue");

    private final Inventory inventory;

    private CatalogGui(MoreTntPlusCore core) {
        List<ItemStack> entries = new ArrayList<>();
        for (TntDefinition definition : core.definitions()) {
            ItemStack item = core.items().tnt(definition, 1);
            withLore(item, core.mechanicOf(definition).map(Mechanic::summary).orElse(""));
            entries.add(item);
        }
        for (ComponentDefinition component : core.componentDefinitions().values()) {
            entries.add(core.items().component(component, 1));
        }
        int size = Math.min(54, Math.max(9, ((entries.size() + 8) / 9) * 9));
        this.inventory = Bukkit.createInventory(this, size, TITLE);
        for (int i = 0; i < Math.min(entries.size(), size); i++) {
            inventory.setItem(i, entries.get(i));
        }
    }

    public static CatalogGui create(MoreTntPlusCore core) {
        return new CatalogGui(core);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static void withLore(ItemStack item, String summary) {
        if (summary.isEmpty()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        List<String> lore = new ArrayList<>(meta.getLore() == null ? List.of() : meta.getLore());
        lore.add(Messages.color("&8Mechanic: &7" + summary));
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}
