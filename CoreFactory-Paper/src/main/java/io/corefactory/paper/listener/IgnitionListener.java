package io.corefactory.paper.listener;

import io.corefactory.api.PlacedTnt;
import io.corefactory.core.CoreFactoryCore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Optional;

/**
 * Every way fire, flint, redstone or an explosion can light TNT, routed to the core's ignition.
 */
public final class IgnitionListener implements Listener {

    private final CoreFactoryCore core;

    public IgnitionListener(CoreFactoryCore core) {
        this.core = core;
    }

    /**
     * Flint and steel, a fire arrow, lava, lightning, fire spread, an explosion: every cause that would light a placed custom TNT
     * is taken over when the ignition is still allowed. An ignition that another plugin cancelled (a claim, a region that
     * denies fire) stays cancelled, so protection is honoured. The vanilla ignition is cancelled and the core runs the custom
     * mechanic once.
     */
    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        Block block = event.getBlock();
        if (core.settings().debug()) {
            core.plugin().getLogger().info("[debug] BlockIgniteEvent " + block.getType() + " cause=" + event.getCause()
                    + " igniting=" + (event.getIgnitingEntity() == null ? "none" : event.getIgnitingEntity().getType()));
        }
        if (block.getType() != Material.TNT) {
            return;
        }
        Optional<PlacedTnt> placed = core.placedAt(block.getLocation());
        if (placed.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        core.ignite(placed.get(), event.getIgnitingEntity(), event.getCause());
    }

    /**
     * A flaming arrow or a fireball that hits a placed custom TNT lights it, as a fire source does. Vanilla may also raise a
     * BlockIgniteEvent for the same hit: the core lights a block once, so the second request does nothing.
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Block block = event.getHitBlock();
        Projectile projectile = event.getEntity();
        if (block == null || block.getType() != Material.TNT || !isFlaming(projectile)) {
            return;
        }
        Optional<PlacedTnt> placed = core.placedAt(block.getLocation());
        if (placed.isEmpty()) {
            return;
        }
        Entity shooter = projectile.getShooter() instanceof Entity entity ? entity : null;
        core.ignite(placed.get(), shooter, projectile instanceof Fireball
                ? BlockIgniteEvent.IgniteCause.FIREBALL : BlockIgniteEvent.IgniteCause.ARROW);
    }

    private static boolean isFlaming(Projectile projectile) {
        return projectile.getFireTicks() > 0 || projectile instanceof Fireball;
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
