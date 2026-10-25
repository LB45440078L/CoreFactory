package io.corefactory.core.turret;

/**
 * The rule that decides whether a player may change a turret: they must be allowed to use it and hold the permission to change turrets. The
 * caller says whether the player may use it: the owner and a friend of the owner may load, eject, power, upgrade and break a turret, while the
 * targeting switches are the owner's alone. Every action that changes a turret takes this rule, so a player who is not allowed never changes
 * another player's turret, whatever their permission. The allowance is checked first. Pure.
 */
public final class TurretAccess {

    /** The outcome of the rule. Only {@link #ALLOW} lets the action go on. */
    public enum Verdict {
        /** The player is allowed to change the turret, and holds the permission. */
        ALLOW,
        /** The player is not allowed to change the turret: not the owner, and for an action a friend may take, not a friend either. */
        NOT_ALLOWED,
        /** The player is allowed, but holds no permission to change turrets. */
        NO_PERMISSION
    }

    private TurretAccess() {
    }

    /**
     * The verdict for a player and a turret.
     *
     * @param allowed   true when the player may change this turret for the action: the owner, a friend of the owner, or the owner alone for a
     *                  targeting switch
     * @param permitted true when the player holds the permission to change turrets
     */
    public static Verdict decide(boolean allowed, boolean permitted) {
        if (!allowed) {
            return Verdict.NOT_ALLOWED;
        }
        return permitted ? Verdict.ALLOW : Verdict.NO_PERMISSION;
    }
}
