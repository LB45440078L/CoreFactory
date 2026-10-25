package io.corefactory.core.artillery;

/**
 * The charge a shell is fired with. Tier I and tier II charges differ only in range, reload and scatter: a tier II charge
 * is a bigger propellant load, so it reaches further, reloads more slowly and spreads its shells more. Tier III is the
 * charge the most powerful shells need. Each tier's multipliers are in {@link ArtillerySettings}.
 */
public enum ChargeTier {
    TIER_I, TIER_II, TIER_III;

    /** The key of the tier in the config section {@code artillery.charges}, such as {@code tier-2}. */
    public String key() {
        return "tier-" + (ordinal() + 1);
    }
}
