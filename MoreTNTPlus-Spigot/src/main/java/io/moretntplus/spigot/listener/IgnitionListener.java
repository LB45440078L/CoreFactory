package io.moretntplus.spigot.listener;

import io.moretntplus.api.PlacedTnt;
import io.moretntplus.core.MoreTntPlusCore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Optional;

/**
 * Every way fire, flint, redstone or an explosion can light TNT, routed to the core's ignition.
 */
public final class IgnitionListener implements Listener {

    private final MoreTntPlusCore core;

    public IgnitionListener(MoreTntPlusCore core) {
        this.core = core;
    }

    /** Flint and steel, lava, lightning, fire spread: a placed custom TNT is lit by the core instead. */
    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.TNT) {
            return;
        }
        Optional<PlacedTnt> placed = core.placedAt(block.getLocation());
        if (placed.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        core.ignite(placed.get(), event.getPlayer());
    }

    /** Redstone power reaching a placed TNT lights it, the way it lights vanilla TNT. */
    @EventHandler(ignoreCancelled = true)
    public void onPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.TNT || !block.isBlockIndirectlyPowered()) {
            return;
        }
        Optional<PlacedTnt> placed = core.placedAt(block.getLocation());
        if (placed.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        core.ignite(placed.get(), null);
    }

    /** An entity explosion: our primed TNT detonates its mechanic, and vanilla explosions chain into placed TNT. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent event) {
        core.manager().onEntityExplode(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        core.manager().onBlockExplode(event);
    }
}
