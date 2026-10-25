package io.corefactory.core.artillery;

import java.util.Locale;

/**
 * The shells an artillery piece fires, with the rules that do not change with the config: the charge tier and the
 * artillery level each needs, how it detonates, what it does, and whether it is nuclear. Its yield, burst altitudes and
 * the sizes of its patterns are in {@link ArtillerySettings}, so they can be tuned without a new build.
 */
public enum ArtilleryShell {

    /** A normal shell. It explodes on impact, with about 1 ton of TNT. */
    EXPLOSIVE(ChargeTier.TIER_I, ArtilleryLevel.I, Detonation.CONTACT, Effect.BLAST, false),

    /** A shell with ten TNT blocks of filler. It explodes exactly where it lands, so it digs its own crater. */
    HIGH_EXPLOSIVE(ChargeTier.TIER_I, ArtilleryLevel.I, Detonation.CONTACT, Effect.BLAST, false),

    /** Burns in the air, high above the ground, and its smoke settles where the particles fall. Deals no blast. */
    WHITE_PHOSPHORUS(ChargeTier.TIER_I, ArtilleryLevel.I, Detonation.AIR_BURST, Effect.SMOKE, false),

    /** Bursts on impact and sets a long strip of ground on fire, with no damage to the terrain. */
    NAPALM(ChargeTier.TIER_I, ArtilleryLevel.I, Detonation.CONTACT, Effect.FIRE, false),

    /** Opens in mid-air and scatters small submunitions over a wide area. Each one breaks a few blocks. */
    CLUSTER(ChargeTier.TIER_II, ArtilleryLevel.II, Detonation.AIR_BURST, Effect.SUBMUNITIONS, false),

    /** A Little Boy class warhead, burst a few blocks above the ground. */
    ENRICHED_URANIUM(ChargeTier.TIER_II, ArtilleryLevel.VI, Detonation.AIR_BURST, Effect.BLAST, true),

    /** A megaton-class thermonuclear warhead, burst high above the ground. */
    HYDROGEN_THERMONUCLEAR(ChargeTier.TIER_III, ArtilleryLevel.VI, Detonation.AIR_BURST, Effect.BLAST, true),

    /** Five small uranium bombs, spread over a wide area. It needs a steep shot, so the bombs fall from above. */
    CLUSTER_ENRICHED_URANIUM(ChargeTier.TIER_III, ArtilleryLevel.VII, Detonation.AIR_BURST, Effect.SUBMUNITIONS, true),

    /** Half a gigaton of condensed antimatter. It may be fired only far away, and only once per cooldown. */
    CONDENSED_ANTIMATTER(ChargeTier.TIER_III, ArtilleryLevel.VII, Detonation.AIR_BURST, Effect.BLAST, true);

    /** When a shell detonates: on the ground it hits, or in the air at an altitude. */
    public enum Detonation {
        /** Explodes where it lands. */
        CONTACT,
        /** Explodes in the air, at an altitude above the ground given by the shell's burst range. */
        AIR_BURST
    }

    /** What a shell does where it detonates. */
    public enum Effect {
        /** A blast that breaks blocks and hurts whatever is in its radius. */
        BLAST,
        /** A cloud of smoke that hurts the living creatures that stand in it. */
        SMOKE,
        /** A strip of fire that burns the ground and leaves the blocks standing. */
        FIRE,
        /** Small detonations spread over an area, each one a blast of its own. */
        SUBMUNITIONS
    }

    private final ChargeTier requiredTier;
    private final ArtilleryLevel minimumLevel;
    private final Detonation detonation;
    private final Effect effect;
    private final boolean nuclear;

    ArtilleryShell(ChargeTier requiredTier, ArtilleryLevel minimumLevel, Detonation detonation, Effect effect,
                   boolean nuclear) {
        this.requiredTier = requiredTier;
        this.minimumLevel = minimumLevel;
        this.detonation = detonation;
        this.effect = effect;
        this.nuclear = nuclear;
    }

    /** The lowest charge tier the shell can be fired with. A higher tier is also accepted. */
    public ChargeTier requiredTier() {
        return requiredTier;
    }

    /** The lowest artillery level the shell can be fired from. A higher level is also accepted. */
    public ArtilleryLevel minimumLevel() {
        return minimumLevel;
    }

    /** When the shell detonates. */
    public Detonation detonation() {
        return detonation;
    }

    /** What the shell does where it detonates. */
    public Effect effect() {
        return effect;
    }

    /** True for a nuclear shell, which needs the nuclear distance rules and may not be fired near a player. */
    public boolean nuclear() {
        return nuclear;
    }

    /** The key of the shell in the config section {@code artillery.shells}: its name in lower case with dashes. */
    public String key() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
