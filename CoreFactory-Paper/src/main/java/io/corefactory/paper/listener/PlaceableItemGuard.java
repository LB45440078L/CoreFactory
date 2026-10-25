package io.corefactory.paper.listener;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.item.PlaceableItems;
import io.corefactory.core.text.Text;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Keeps a plugin item from becoming its vanilla material in the world. A plugin item that is a block material places its
 * vanilla block, so the placement of each one is allowed only where its own listener places it (see {@link PlaceableItems}).
 * Every other plugin item is refused before any other placement code runs. A dispenser never places a plugin block item,
 * since no placement logic is run for a dispensed item.
 */
public final class PlaceableItemGuard implements Listener {

    private final CoreFactoryCore core;
    private final Predicate<String> machine;

    /**
     * @param core    the core, which reads the identity of an item
     * @param machine true when an id names a machine, a lab tool or a drill
     */
    public PlaceableItemGuard(CoreFactoryCore core, Predicate<String> machine) {
        this.core = core;
        this.machine = machine;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Optional<PlaceableItems.Identity> identity = PlaceableItems.identityOf(core.items(), event.getItemInHand());
        if (identity.isEmpty()) {
            return;
        }
        boolean device = identity.get().kind() == PlaceableItems.Kind.COMPONENT
                && PlaceableItems.isDevice(identity.get().id());
        boolean mainHand = event.getHand() == EquipmentSlot.HAND;
        if (PlaceableItems.allowed(identity.get().kind(), identity.get().id(), machine) && (mainHand || !device)) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(Text.parse("&cThis item cannot be placed."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        ItemStack item = event.getItem();
        if (item.getType().isBlock() && PlaceableItems.identityOf(core.items(), item).isPresent()) {
            event.setCancelled(true);
        }
    }
}
