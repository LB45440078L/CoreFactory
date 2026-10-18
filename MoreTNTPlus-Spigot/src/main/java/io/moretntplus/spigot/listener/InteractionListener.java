package io.moretntplus.spigot.listener;

import io.moretntplus.api.PlacedTnt;
import io.moretntplus.core.MoreTntPlusCore;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Right-click mechanics: the Fuse Linker tool, and handing dropped TNT to its mechanic (napalm lands and bursts).
 */
public final class InteractionListener implements Listener {

    /** The component id of the Fuse Linker tool, from items.yml. */
    public static final String FUSE_LINKER = "fuse_linker";

    private final MoreTntPlusCore core;
    private final Map<UUID, Location> selected = new HashMap<>();

    public InteractionListener(MoreTntPlusCore core) {
        this.core = core;
    }

    /**
     * With the Fuse Linker: right-click one placed TNT, then a second, to link them. Sneak-right-click a placed TNT
     * with the tool to remove all its links.
     */
    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!FUSE_LINKER.equals(core.items().componentId(event.getItem()).orElse(null))) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        Optional<PlacedTnt> clicked = core.placedAt(event.getClickedBlock().getLocation());
        if (clicked.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            core.unlinkAll(clicked.get());
            selected.remove(player.getUniqueId());
            player.sendMessage(core.messages().get("link-cleared"));
            return;
        }
        Location first = selected.get(player.getUniqueId());
        if (first == null) {
            selected.put(player.getUniqueId(), clicked.get().location());
            player.sendMessage(core.messages().get("link-start"));
            return;
        }
        selected.remove(player.getUniqueId());
        Optional<PlacedTnt> other = core.placedAt(first);
        if (other.isEmpty()) {
            player.sendMessage(core.messages().get("not-placed"));
            return;
        }
        player.sendMessage(core.messages().get(core.link(other.get(), clicked.get()) ? "link-made" : "link-same"));
    }

    /** A dropped TNT item is handed to its mechanic, which may detonate it on landing (napalm). */
    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        Optional<String> id = core.items().tntId(item.getItemStack());
        if (id.isEmpty()) {
            return;
        }
        core.definition(id.get()).ifPresent(definition ->
                core.mechanicOf(definition).ifPresent(mechanic -> mechanic.onDropped(item, definition, core)));
    }
}
