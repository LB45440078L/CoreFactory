package io.moretntplus.core.engine;

import io.moretntplus.api.DetonationContext;
import io.moretntplus.api.Mechanic;
import io.moretntplus.api.PlacedTnt;
import io.moretntplus.api.TntDefinition;
import io.moretntplus.api.event.TntDetonateEvent;
import io.moretntplus.api.event.TntIgniteEvent;
import io.moretntplus.core.MoreTntPlusCore;
import io.moretntplus.core.placement.PlacedTntStore;
import io.moretntplus.core.item.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Ignition, detonation, primed-entity bookkeeping and chain reactions.
 *
 * <p>Ignition removes the placed block and, unless the TNT is silent or its mechanic is delayed or persistent,
 * spawns a vanilla {@link TNTPrimed} tagged with the TNT's id. When that entity explodes the vanilla explosion
 * is cancelled and the mechanic runs instead. Vanilla explosions that reach a placed TNT block take it out
 * of the block list and ignite it after a short delay, which produces real chains.
 */
public final class TntManager {

    static final NamespacedKey PRIMED_ID = new NamespacedKey(ItemFactory.NAMESPACE, "primed_tnt_id");
    static final NamespacedKey PRIMED_DIR = new NamespacedKey(ItemFactory.NAMESPACE, "primed_tnt_dir");

    private static final long RATE_WINDOW_TICKS = 20L;

    private final MoreTntPlusCore core;
    private final Map<UUID, PlacedTnt> primed = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private long windowStart;
    private int windowCount;

    public TntManager(MoreTntPlusCore core) {
        this.core = core;
    }

    /** Lights a placed TNT block. A stale entry (already lit, or overwritten) is ignored, so no TNT doubles up. */
    public void ignite(PlacedTnt placed, Entity source) {
        if (core.placedAt(placed.location()).filter(current -> current == placed).isEmpty()) {
            return;
        }
        Optional<TntDefinition> found = core.definition(placed.tntId());
        if (found.isEmpty()) {
            core.plugin().getLogger().warning("Placed TNT of unknown type " + placed.tntId() + " was ignited.");
            return;
        }
        TntDefinition definition = found.get();
        Optional<Mechanic> mechanic = core.mechanic(definition.mechanic());
        if (mechanic.isEmpty()) {
            return;
        }
        TntIgniteEvent event = new TntIgniteEvent(placed, definition, source);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }

        Location origin = placed.location().clone().add(0.5, 0.5, 0.5);
        Mechanic.IgnitionMode mode = mechanic.get().ignitionMode();
        if (mode == Mechanic.IgnitionMode.INERT || core.protection().inAnchorField(origin)) {
            return;
        }
        if (mode == Mechanic.IgnitionMode.PERSISTENT) {
            String key = PlacedTntStore.key(placed.location());
            long now = core.currentTick();
            Long until = cooldowns.get(key);
            if (until != null && now < until) {
                return;
            }
            cooldowns.put(key, now + Math.max(20, definition.fuseTicks()));
            mechanic.get().onIgnite(origin, definition, core);
            detonate(definition, origin, source, placed.direction(), placed.chainDepth(), placed);
            return;
        }

        mechanic.get().onIgnite(origin, definition, core);
        core.forget(placed, true);
        if (mode == Mechanic.IgnitionMode.DELAYED || definition.hasEffect("silent")) {
            core.scheduleLater(definition.fuseTicks(),
                    () -> detonate(definition, origin, source, placed.direction(), placed.chainDepth(), placed));
            return;
        }
        TNTPrimed entity = origin.getWorld().spawn(origin, TNTPrimed.class, tnt -> {
            tnt.setFuseTicks(definition.fuseTicks());
            tnt.getPersistentDataContainer().set(PRIMED_ID, PersistentDataType.STRING, definition.id());
            if (placed.direction() != null) {
                tnt.getPersistentDataContainer().set(PRIMED_DIR, PersistentDataType.STRING,
                        encode(placed.direction()));
            }
            if (source instanceof LivingEntity living) {
                tnt.setSource(living);
            }
        });
        primed.put(entity.getUniqueId(), placed);
    }

    /** Runs a detonation of {@code definition}. The mechanic decides what that means. */
    public void detonate(TntDefinition definition, Location origin, Entity source, Vector direction,
                         int depth, PlacedTnt placed) {
        if (depth > core.settings().maxChainDepth() || !allowDetonation()) {
            // The block (or primed entity) is already gone, so hand its item back instead of losing it.
            refund(definition, origin);
            return;
        }
        Optional<Mechanic> mechanic = core.mechanic(definition.mechanic());
        if (mechanic.isEmpty()) {
            return;
        }
        if (core.protection().inAnchorField(origin)) {
            return;
        }
        TntDetonateEvent event = new TntDetonateEvent(definition, origin, source, direction, depth);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        try {
            mechanic.get().detonate(new DetonationContext(definition, origin, source, direction, placed, depth, core));
        } catch (RuntimeException e) {
            core.plugin().getLogger().log(Level.SEVERE,
                    "Mechanic " + mechanic.get().id() + " failed for TNT " + definition.id(), e);
        }
    }

    /** Entry point for {@link EntityExplodeEvent}. Handles our primed TNT and chains vanilla explosions. */
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (entity != null) {
            String id = entity.getPersistentDataContainer().get(PRIMED_ID, PersistentDataType.STRING);
            if (id != null) {
                // Another plugin that cancelled this explosion (a region flag, say) keeps its verdict:
                // the TNT is removed but nothing detonates.
                boolean blockedElsewhere = event.isCancelled();
                event.setCancelled(true);
                event.blockList().clear();
                Location at = entity.getLocation();
                Vector direction = decode(entity.getPersistentDataContainer()
                        .get(PRIMED_DIR, PersistentDataType.STRING));
                PlacedTnt placed = primed.remove(entity.getUniqueId());
                Entity source = entity instanceof TNTPrimed tnt ? tnt.getSource() : null;
                entity.remove();
                if (!blockedElsewhere) {
                    core.definition(id).ifPresent(definition ->
                            detonate(definition, at, source, direction, placed == null ? 0 : placed.chainDepth(), placed));
                }
                return;
            }
        }
        if (event.isCancelled()) {
            // Another plugin refused this explosion: it must not chain into placed TNT either.
            return;
        }
        if (core.protection().inAnchorField(event.getLocation())) {
            event.setCancelled(true);
            return;
        }
        event.blockList().removeIf(block -> core.protection().inAnchorField(block.getLocation()));
        chainFrom(event.blockList(), entity);
    }

    /** Entry point for {@link BlockExplodeEvent}, such as beds and respawn anchors. */
    public void onBlockExplode(BlockExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }
        event.blockList().removeIf(block -> core.protection().inAnchorField(block.getLocation()));
        chainFrom(event.blockList(), null);
    }

    private void chainFrom(List<Block> blocks, Entity source) {
        Iterator<Block> iterator = blocks.iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            Optional<PlacedTnt> placed = core.placedAt(block.getLocation());
            if (placed.isEmpty()) {
                continue;
            }
            iterator.remove();
            PlacedTnt target = placed.get();
            target.setChainDepth(1);
            core.scheduleLater(core.settings().chainIgniteDelayTicks(), () -> ignite(target, source));
        }
    }

    /** Drops the item of a TNT whose detonation was refused. */
    private void refund(TntDefinition definition, Location origin) {
        if (origin.getWorld() != null) {
            origin.getWorld().dropItemNaturally(origin.clone().add(0.5, 0.5, 0.5),
                    core.items().tnt(definition, 1));
        }
    }

    /** Clears the per-key cooldowns of persistent mechanics; called on reload. */
    public void resetCooldowns() {
        cooldowns.clear();
    }

    private boolean allowDetonation() {
        long now = core.currentTick();
        if (now - windowStart >= RATE_WINDOW_TICKS) {
            windowStart = now;
            windowCount = 0;
        }
        windowCount++;
        return windowCount <= core.settings().maxDetonationsPerSecond();
    }

    static String encode(Vector vector) {
        return vector.getX() + "," + vector.getY() + "," + vector.getZ();
    }

    static Vector decode(String text) {
        if (text == null) {
            return null;
        }
        String[] parts = text.split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new Vector(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
