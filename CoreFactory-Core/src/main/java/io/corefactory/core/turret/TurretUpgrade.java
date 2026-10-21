package io.corefactory.core.turret;

/**
 * The rule that decides whether an upgrade kit may upgrade a turret. A kit of level {@code n} upgrades any turret of either kind whose
 * level is exactly {@code n - 1}, so a turret moves one level at a time. A turret at the highest level takes no kit, and a kit of the wrong
 * level is refused. Moving to a tall level, {@link #TALL_FROM} and up, also needs the block above the turret to be free; the runtime
 * supplies that check, and the rule only reads its result. Pure.
 */
public final class TurretUpgrade {

    /** The first level that is two blocks high, as a door. Moving to this level or higher needs the block above free. */
    public static final int TALL_FROM = 3;

    /** The outcome of the rule. Only {@link #OK} lets the kit upgrade the turret. */
    public enum Reason {
        /** The kit may upgrade the turret. */
        OK,
        /** The kit is not of the level just above the turret's, or the turret's level is below 1. */
        WRONG_LEVEL,
        /** The turret is already at the highest level. */
        MAX_LEVEL,
        /** The turret moves to a tall level and the block above it is not free. */
        BLOCKED_ABOVE
    }

    private TurretUpgrade() {
    }

    /**
     * Whether a kit may upgrade a turret, from the level change alone. A turret moving to a tall level also needs
     * {@link #canUpgrade(int, int, boolean)}, which checks the block above.
     *
     * @param currentLevel the turret's level now
     * @param kitLevel     the level of the upgrade kit
     */
    public static Reason canUpgrade(int currentLevel, int kitLevel) {
        if (currentLevel >= TurretLevels.MAX_LEVEL) {
            return Reason.MAX_LEVEL;
        }
        if (currentLevel < 1 || kitLevel != currentLevel + 1) {
            return Reason.WRONG_LEVEL;
        }
        return Reason.OK;
    }

    /**
     * Whether a kit may upgrade a turret, from the level change and whether the block above the turret is free.
     *
     * @param currentLevel the turret's level now
     * @param kitLevel     the level of the upgrade kit, which is the level the turret moves to
     * @param spaceAbove   true when the block above the turret is free; the runtime checks it
     */
    public static Reason canUpgrade(int currentLevel, int kitLevel, boolean spaceAbove) {
        Reason level = canUpgrade(currentLevel, kitLevel);
        if (level != Reason.OK) {
            return level;
        }
        if (kitLevel >= TALL_FROM && !spaceAbove) {
            return Reason.BLOCKED_ABOVE;
        }
        return Reason.OK;
    }
}
