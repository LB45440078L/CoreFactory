package io.corefactory.spigot.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * A plugin menu. Its items never move: {@link GuiListener} cancels every click in it and passes the clicks on here.
 */
public interface MenuHolder extends InventoryHolder {

    /**
     * Handles a click on one of this menu's own slots. The click is already cancelled.
     *
     * @param event  the click, with {@link InventoryClickEvent#getSlot()} the slot in this menu
     * @param player who clicked
     */
    void click(InventoryClickEvent event, Player player);
}
