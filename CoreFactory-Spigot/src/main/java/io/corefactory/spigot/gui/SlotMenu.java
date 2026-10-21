package io.corefactory.spigot.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * A menu that decides its own clicks and drags: a machine's input and output slots, a research console's pack slots, or a
 * splitter's filter panel. {@link GuiListener} does not cancel clicks or drags in it. Instead the menu decides each one,
 * cancelling what it does not allow and handling the rest itself. A drag is cancelled unless the menu overrides
 * {@link #slotDrag}.
 */
public interface SlotMenu extends MenuHolder {

    /**
     * Decides one click. The click may be on the menu's own slots (the top inventory) or the player's inventory
     * (the bottom one). Cancel the event to refuse it, or leave it alone to let the server move the items.
     *
     * @param event  the click; {@link InventoryClickEvent#getRawSlot()} is below the menu size for the menu's own slots
     * @param player who clicked
     */
    void slotClick(InventoryClickEvent event, Player player);

    /**
     * Decides one drag. The drag may cover the menu's own slots (the top inventory) and the player's inventory (the bottom one).
     * The default cancels it, so a menu that takes no drags needs no override. Leave the event alone only to let the server
     * apply the drag; a menu that places items itself cancels the event and places them.
     *
     * @param event  the drag; {@link InventoryDragEvent#getRawSlots()} holds the covered slots
     * @param player who dragged
     */
    default void slotDrag(InventoryDragEvent event, Player player) {
        event.setCancelled(true);
    }

    @Override
    default void click(InventoryClickEvent event, Player player) {
        slotClick(event, player);
    }
}
