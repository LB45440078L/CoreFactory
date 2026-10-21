package io.corefactory.core.engine;

import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;

import java.util.EnumSet;
import java.util.Set;

/**
 * The decision taken when something lights a block. Every vanilla ignition cause lights a placed custom TNT, and a block
 * lights once: a request for a block that is already lit, or that detonated on the current tick, is dropped.
 */
public final class IgnitionRules {

    /** What the core does with one ignition request. */
    public enum Verdict {
        /** The custom mechanic runs. */
        IGNITE,
        /** No placed custom TNT is at the block, so vanilla handles it. */
        NOT_CUSTOM,
        /** The cause is not one the core handles. */
        UNHANDLED_CAUSE,
        /** The block is already lit, or the placed record is no longer the one at the block. */
        ALREADY_IGNITED,
        /** The block detonated on this tick, so this request is the second one for the same ignition. */
        SAME_TICK
    }

    /** The vanilla causes that light a placed custom TNT. Every cause is handled. */
    private static final Set<IgniteCause> HANDLED = EnumSet.allOf(IgniteCause.class);

    private IgnitionRules() {
    }

    /**
     * Decides one ignition request. Checks run in this order, and the first that fails gives the verdict.
     *
     * @param customTnt         true when a placed custom TNT is at the block
     * @param cause             the vanilla cause, or {@code null} for a request without one (the API, redstone, a listener)
     * @param alreadyIgnited    true when the placed record handed in is not the one at the block: it is lit or replaced
     * @param detonatedThisTick true when the block detonated on the current tick
     */
    public static Verdict decide(boolean customTnt, IgniteCause cause, boolean alreadyIgnited, boolean detonatedThisTick) {
        if (!customTnt) {
            return Verdict.NOT_CUSTOM;
        }
        if (cause != null && !HANDLED.contains(cause)) {
            return Verdict.UNHANDLED_CAUSE;
        }
        if (alreadyIgnited) {
            return Verdict.ALREADY_IGNITED;
        }
        if (detonatedThisTick) {
            return Verdict.SAME_TICK;
        }
        return Verdict.IGNITE;
    }
}
