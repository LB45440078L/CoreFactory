package io.moretntplus.spigot.gui;

import io.moretntplus.core.MoreTntPlusCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Handles clicks in the catalogue: a click gives a copy of the clicked item, and never moves the item itself.
 */
public final class CatalogListener implements Listener {

    private final MoreTntPlusCore core;

    public CatalogListener(MoreTntPlusCore core) {
        this.core = core;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof CatalogGui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }
        if (!player.hasPermission("moretntplus.give")) {
            player.sendMessage(core.messages().get("no-permission"));
            return;
        }
        Optional<ItemStack> copy = core.tntIdOf(clicked)
                .flatMap(id -> core.createTntItem(id, 1))
                .or(() -> core.items().componentId(clicked)
                        .flatMap(id -> core.createComponentItem(id, 1)));
        copy.ifPresent(item -> player.getInventory().addItem(item));
    }
}
