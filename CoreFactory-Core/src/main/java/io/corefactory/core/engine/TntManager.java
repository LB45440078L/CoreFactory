package io.corefactory.core.engine;

import io.corefactory.api.DetonationContext;
import io.corefactory.api.Mechanic;
import io.corefactory.api.PlacedTnt;
import io.corefactory.api.TntDefinition;
import io.corefactory.api.event.TntDetonateEvent;
import io.corefactory.api.event.TntIgniteEvent;
import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.placement.PlacedTntStore;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.lock.LockRules;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private final CoreFactoryCore core;
    private final Map<UUID, PlacedTnt> primed = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final DetonationLedger ledger = new DetonationLedger();
    private final List<DetonationObserver> observers = new ArrayList<>();
    private long windowStart;
    private int windowCount;

    public TntManager(CoreFactoryCore core) {
        this.core = core;
    }

    /** Adds an observer that is told of every detonation, in the order observers were added. */
    public void addObserver(DetonationObserver observer) {
        observers.add(Objects.requireNonNull(observer, "observer"));
    }

    /** Lights a placed TNT block for a request without a vanilla cause, such as the API or redstone. */
    public void ignite(PlacedTnt placed, Entity source) {
        ignite(placed, source, null);
    }

    /**
     * Lights a placed TNT block. The {@link IgnitionRules} decide: a block that is already lit, or that detonated on this
     * tick, is ignored, so one ignition detonates once and no TNT doubles up.
     */
    public void ignite(PlacedTnt placed, Entity source, IgniteCause cause) {
        Optional<PlacedTnt> current = core.placedAt(placed.location());
        boolean detonatedNow = ledger.detonatedOn(PlacedTntStore.key(placed.location()), core.currentTick());
        IgnitionRules.Verdict verdict = IgnitionRules.decide(current.isPresent(), cause,
                current.isPresent() && current.get() != placed, detonatedNow);
        if (verdict != IgnitionRules.Verdict.IGNITE || refusedByLock(placed, source)) {
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
        if (refusedByLock(placed, source)) {
            return;
        }
        // A second detonation of the same block on this tick is the same ignition: it is dropped, with no refund.
        if (origin.getWorld() != null && !ledger.claim(PlacedTntStore.key(origin), core.currentTick())) {
            return;
        }
        if (depth > core.settings().maxChainDepth() || !allowDetonation()) {
            // The block (or primed entity) is already gone, so hand its item back instead of losing it.
            refund(definition, origin);
            return;
        }
        Optional<Mechanic> mechanic = core.mechanic(definition.mechanic());
        if (core.settings().debug()) {
            core.plugin().getLogger().info("[debug] detonate " + definition.id() + " mechanic="
                    + definition.mechanic() + " depth=" + depth + " origin=" + origin.getBlockX() + ","
                    + origin.getBlockY() + "," + origin.getBlockZ() + " placed=" + (placed != null));
        }
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
        notifyObservers(definition, origin, source);
    }

    private void notifyObservers(TntDefinition definition, Location origin, Entity source) {
        for (DetonationObserver observer : observers) {
            try {
                observer.onDetonation(origin.getWorld(), origin, definition.radius(), source);
            } catch (RuntimeException e) {
                core.plugin().getLogger().log(Level.SEVERE,
                        "A detonation observer failed for TNT " + definition.id(), e);
            }
        }
    }

    /**
     * True when an explosion of this entity is run by the core: a primed custom TNT, or a vanilla primed TNT that stands on a
     * placed custom TNT. Such an explosion is not a vanilla one, so the vanilla rules for explosions do not apply to it.
     */
    public boolean isCustomExplosion(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.getPersistentDataContainer().has(PRIMED_ID, PersistentDataType.STRING)) {
            return true;
        }
        return entity instanceof TNTPrimed && core.placedAt(entity.getLocation()).isPresent();
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
        if (core.settings().debug()) {
            core.plugin().getLogger().info("[debug] explosion entity=" + (entity == null ? "none" : entity.getType())
                    + " cancelled=" + event.isCancelled() + " blocks=" + event.blockList().size());
        }
        if (event.isCancelled()) {
            // Another plugin refused this explosion: it must not chain into placed TNT either.
            return;
        }
        if (entity instanceof TNTPrimed) {
            Optional<PlacedTnt> lit = core.placedAt(entity.getLocation());
            if (lit.isPresent()) {
                // A placed custom TNT that vanilla primed (arrows, fire, lava): run its mechanic, not a vanilla blast.
                PlacedTnt placed = lit.get();
                event.setCancelled(true);
                event.blockList().clear();
                Location at = entity.getLocation();
                entity.remove();
                if (!refusedByLock(placed, null)) {
                    core.unregister(placed);
                    core.definition(placed.tntId()).ifPresent(definition ->
                            detonate(definition, at, null, placed.direction(), placed.chainDepth(), placed));
                }
                return;
            }
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
            core.scheduleLater(core.settings().chainIgniteDelayTicks(), () -> ignite(target, source, IgniteCause.EXPLOSION));
        }
    }

    /**
     * The one place that refuses a trigger on a locked placed TNT. Ignition and detonation both pass through it, and so does the
     * vanilla explosion that primes a placed TNT. A refused trigger changes nothing: the TNT stays placed and unlit. The message
     * goes to the player who triggered it, when there is one.
     *
     * @return true when the trigger is refused
     */
    private boolean refusedByLock(PlacedTnt placed, Entity source) {
        if (placed == null || !LockRules.refusesTriggers(placed.lock())) {
            return false;
        }
        if (source instanceof Player player) {
            player.sendMessage(core.messages().get("locked-tnt"));
        }
        return true;
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
