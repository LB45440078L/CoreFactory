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
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
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
        if (core.settings().debug()) {
            core.plugin().getLogger().info("[debug] BlockIgniteEvent " + block.getType() + " cause=" + event.getCause()
                    + " player=" + (event.getPlayer() == null ? "none" : event.getPlayer().getName()));
        }
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

    /**
     * Flint and steel or a fire charge used on a placed TNT. Vanilla does not raise BlockIgniteEvent for this, so the
     * interaction itself is taken over: the item is not used and the core lights the placed TNT.
     */
    @EventHandler(ignoreCancelled = true)
    public void onLight(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Material item = event.getMaterial();
        if (item != Material.FLINT_AND_STEEL && item != Material.FIRE_CHARGE) {
            return;
        }
        if (event.getClickedBlock().getType() != Material.TNT) {
            return;
        }
        Optional<PlacedTnt> placed = core.placedAt(event.getClickedBlock().getLocation());
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
