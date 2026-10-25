package io.corefactory.paper.listener;

import io.corefactory.api.PlacedTnt;
import io.corefactory.api.TntDefinition;
import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.lock.LockRules;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Optional;

/**
 * Placing a custom TNT item registers the block. Breaking a placed one drops its custom item, not vanilla TNT.
 */
public final class PlacementListener implements Listener {

    private final CoreFactoryCore core;

    public PlacementListener(CoreFactoryCore core) {
        this.core = core;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Optional<String> id = core.items().tntId(event.getItemInHand());
        if (id.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        if (core.isDisabledWorld(event.getBlockPlaced().getWorld())) {
            event.setCancelled(true);
            player.sendMessage(core.messages().get("disabled-world"));
            return;
        }
        Optional<TntDefinition> definition = core.definition(id.get());
        if (definition.isEmpty()) {
            event.setCancelled(true);
            player.sendMessage(core.messages().get("unknown-id", "id", id.get()));
            return;
        }
        core.place(event.getBlockPlaced().getLocation(), definition.get(), player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.TNT) {
            return;
        }
        Optional<PlacedTnt> placed = core.placedAt(block.getLocation());
        if (placed.isEmpty()) {
            return;
        }
        if (LockRules.refusesTriggers(placed.get().lock())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(core.messages().get("locked-tnt"));
            return;
        }
        event.setDropItems(false);
        core.unregister(placed.get());
        if (event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            core.definition(placed.get().tntId()).ifPresent(definition ->
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5),
                            core.items().tnt(definition, 1)));
        }
    }
}
