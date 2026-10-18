package io.moretntplus.api;

import org.bukkit.Location;
import org.bukkit.entity.Item;

/**
 * The behaviour of a TNT type. A mechanic is stateless and shared by every TNT type that names it in
 * {@code tnts.yml}; all per-type numbers come from {@link DetonationContext#settings()}.
 *
 * <p>All methods run on the server's main thread.
 */
public interface Mechanic {

    /** Stable identifier used in {@code tnts.yml} as {@code mechanic: <id>}. Lower snake case. */
    String id();

    /** One-line description shown in {@code /mtnt info} and the catalog GUI. */
    String summary();

    /** How a placed block of this mechanic reacts to ignition. Defaults to a vanilla-style primed entity. */
    default IgnitionMode ignitionMode() {
        return IgnitionMode.PRIMED_ENTITY;
    }

    /**
     * Runs the blast (or the mechanic's effect) once the fuse has finished, or immediately for
     * {@link IgnitionMode#PERSISTENT} mechanics.
     */
    void detonate(DetonationContext context);

    /**
     * Called as soon as a block of this TNT is ignited, before the fuse starts. Use it for warnings.
     *
     * @param origin the block that was ignited, centred on its middle
     * @param tnt    the definition being ignited
     * @param api    the service facade
     */
    default void onIgnite(Location origin, TntDefinition tnt, MoreTntPlusApi api) {
    }

    /**
     * Called when a player drops a block of this TNT as an item, such as a thrown napalm charge. The item
     * is still a normal item entity at this point; the mechanic decides whether to remove or detonate it.
     */
    default void onDropped(Item item, TntDefinition tnt, MoreTntPlusApi api) {
    }

    /**
     * Interval, in server ticks, between calls to {@link #onPlacedTick}. Zero disables the periodic call.
     */
    default int placedTickInterval() {
        return 0;
    }

    /**
     * Periodic work for every placed block of this mechanic (proximity arming, anchor fields, fuse lines).
     */
    default void onPlacedTick(PlacedTnt placed, TntDefinition tnt, MoreTntPlusApi api) {
    }

    /** What ignition does to the placed block. */
    enum IgnitionMode {
        /** The block turns into a primed TNT entity that explodes when its fuse ends. */
        PRIMED_ENTITY,
        /** The block is removed and {@link #detonate} runs after the fuse, without an entity or sound. */
        DELAYED,
        /** The block stays where it is and {@link #detonate} runs immediately (relays, markers). */
        PERSISTENT,
        /** Ignition does nothing (anchors, which only work while placed). */
        INERT
    }
}
