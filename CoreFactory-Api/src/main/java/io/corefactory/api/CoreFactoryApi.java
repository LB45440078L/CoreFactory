package io.corefactory.api;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Optional;

/**
 * The services CoreFactory offers to mechanics and addons. Obtain it from {@link CoreFactoryProvider}
 * or {@link DetonationContext#api()}.
 */
public interface CoreFactoryApi {

    Plugin plugin();

    /** The current server tick. */
    long currentTick();

    Collection<TntDefinition> definitions();

    Optional<TntDefinition> definition(String id);

    Collection<Mechanic> mechanics();

    Optional<Mechanic> mechanic(String id);

    /**
     * Adds a mechanic, or replaces the built-in one with the same id. TNT types in {@code tnts.yml} that name it
     * become usable straight away, and their recipes are registered. Call it from your plugin's enable method,
     * after CoreFactory has enabled.
     */
    void registerMechanic(Mechanic mechanic);

    /** The placed TNT at a block position, if any. The location's yaw and pitch are ignored. */
    Optional<PlacedTnt> placedAt(Location blockLocation);

    Collection<PlacedTnt> placedTnts();

    /** The TNT id stored on an item, if the item is a CoreFactory TNT. */
    Optional<String> tntIdOf(ItemStack item);

    Optional<ItemStack> createTntItem(String id, int amount);

    Optional<ItemStack> createComponentItem(String id, int amount);

    ClientEffects clientEffects();

    ProtectionService protection();

    /**
     * Applies block writes in batches, spread over ticks when the set is large. Placed TNT that a write
     * overwrites is unregistered automatically. {@code onComplete} runs on the main thread after the last batch.
     */
    void submitBlockChanges(World world, BlockChangeSet changes, Runnable onComplete);

    /**
     * Like {@link #submitBlockChanges} but pulls the writes from a generator, one slice per tick, so the
     * whole change never sits in memory.
     */
    void submitBlockGenerator(World world, BlockGenerator generator, Runnable onComplete);

    /** Ignites a placed TNT block, as if a player had lit it. Fires {@code TntIgniteEvent}. */
    void ignite(PlacedTnt placed, Entity source);

    /** Runs a detonation of the given TNT type at a position. Fires {@code TntDetonateEvent}. */
    void detonate(String tntId, Location origin, Entity source, Vector direction, int depth);

    /** Runs a task on the main thread after {@code delayTicks} ticks. */
    void scheduleLater(long delayTicks, Runnable task);
}
