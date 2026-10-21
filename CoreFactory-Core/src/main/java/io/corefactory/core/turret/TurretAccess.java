package io.corefactory.core.turret;

/**
 * The rule that decides whether a player may change a turret: they must be its owner and hold the permission to change turrets. Every
 * action that changes a turret (targeting, rounds, upgrade, break) takes this rule, so a player who is not the owner never changes
 * another player's turret, whatever their permission. The owner is checked first. Pure.
 */
public final class TurretAccess {

    /** The outcome of the rule. Only {@link #ALLOW} lets the action go on. */
    public enum Verdict {
        /** The player is the owner and holds the permission. */
        ALLOW,
        /** The player is not the owner of the turret. */
        NOT_OWNER,
        /** The player is the owner but holds no permission to change turrets. */
        NO_PERMISSION
    }

    private TurretAccess() {
    }

    /**
     * The verdict for a player and a turret.
     *
     * @param owner     true when the player placed the turret
     * @param permitted true when the player holds the permission to change turrets
     */
    public static Verdict decide(boolean owner, boolean permitted) {
        if (!owner) {
            return Verdict.NOT_OWNER;
        }
        return permitted ? Verdict.ALLOW : Verdict.NO_PERMISSION;
    }
}
