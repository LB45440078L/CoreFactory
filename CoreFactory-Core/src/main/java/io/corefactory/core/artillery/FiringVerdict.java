package io.corefactory.core.artillery;

/**
 * The verdict of a firing rule: {@link #OK}, or the one reason the shot is refused. A shot that breaks several rules gets the first one
 * the check finds, in the order of {@link ArtilleryFiringRules#check}.
 */
public enum FiringVerdict {
    /** The shot may be fired. */
    OK,
    /** The shell needs a bigger charge tier than the piece has loaded. */
    CHARGE_TOO_LOW,
    /** The shell needs a higher artillery level than the piece is. */
    LEVEL_TOO_LOW,
    /** The barrel is lower than the least elevation any shot may have. */
    ELEVATION_TOO_LOW,
    /** The barrel is higher than the greatest elevation any shot may have. */
    ELEVATION_TOO_HIGH,
    /** A cluster enriched uranium shot needs a steeper elevation, so the bombs fall from above. */
    CLUSTER_ELEVATION_TOO_LOW,
    /** The target is beyond the range of the piece at its level and charge. */
    OUT_OF_RANGE,
    /** The target is nearer to the piece than the shell's least distance. */
    TOO_CLOSE,
    /** The blast would reach the piece, with its margin and its scatter. */
    BLAST_REACHES_ARTILLERY,
    /** The blast would reach the player who fired, with its margin and its scatter. */
    BLAST_REACHES_PLAYER,
    /** A nuclear shell may not be fired at a target this near a player. */
    NUCLEAR_NEAR_PLAYER,
    /** The antimatter shell was fired too recently by this player. */
    COOLDOWN_ACTIVE,
    /** The piece does not have its full power, so it may not fire. */
    POWER_SHORT;

    /** True when the shot may be fired. */
    public boolean ok() {
        return this == OK;
    }
}
