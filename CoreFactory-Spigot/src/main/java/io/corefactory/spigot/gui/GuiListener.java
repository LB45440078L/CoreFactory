package io.corefactory.spigot.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Keeps every {@link MenuHolder} menu read-only, and forwards clicks on its own slots to the menu. A {@link SlotMenu}
 * is not cancelled here: it decides each of its clicks and drags itself.
 */
public final class GuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder menu)) {
            return;
        }
        if (menu instanceof SlotMenu slots) {
            if (event.getWhoClicked() instanceof Player player) {
                slots.slotClick(event, player);
            }
            return;
        }
        event.setCancelled(true);
        boolean ownSlot = event.getRawSlot() >= 0 && event.getRawSlot() < event.getView().getTopInventory().getSize();
        if (ownSlot && event.getWhoClicked() instanceof Player player) {
            menu.click(event, player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder menu)) {
            return;
        }
        if (menu instanceof SlotMenu slots && event.getWhoClicked() instanceof Player player) {
            slots.slotDrag(event, player);
            return;
        }
        event.setCancelled(true);
    }
}
