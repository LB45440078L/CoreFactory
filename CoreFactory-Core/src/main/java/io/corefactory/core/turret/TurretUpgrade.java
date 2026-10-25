package io.corefactory.core.turret;

/**
 * The rule that decides whether an upgrade kit may upgrade a turret. A kit of level {@code n} upgrades a turret whose level is exactly
 * {@code n - 1}, so a turret moves one level at a time. A turret at the highest level of its kind takes no kit, and a kit of the wrong level
 * is refused. The highest level is {@link TurretType#maxLevel()}: 5 for a machine gun and a laser, 3 for a railgun. Moving to a tall level,
 * {@link #TALL_FROM} and up, also needs the block above the turret to be free; the runtime supplies that check, and the rule only reads its
 * result. Pure.
 *
 * <p>The overloads without a kind apply the machine gun's and the laser's five levels, as they always have. A caller that knows the kind
 * should pass it, so a railgun is refused above its level 3.
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
        /** The turret is already at the highest level of its kind. */
        MAX_LEVEL,
        /** The turret moves to a tall level and the block above it is not free. */
        BLOCKED_ABOVE
    }

    private TurretUpgrade() {
    }

    /**
     * Whether a kit may upgrade a machine gun or a laser, from the level change alone. A turret moving to a tall level also needs
     * {@link #canUpgrade(int, int, boolean)}, which checks the block above.
     *
     * @param currentLevel the turret's level now
     * @param kitLevel     the level of the upgrade kit
     */
    public static Reason canUpgrade(int currentLevel, int kitLevel) {
        return reasonOf(TurretLevels.MAX_LEVEL, currentLevel, kitLevel);
    }

    /**
     * Whether a kit may upgrade a machine gun or a laser, from the level change and whether the block above the turret is free.
     *
     * @param currentLevel the turret's level now
     * @param kitLevel     the level of the upgrade kit, which is the level the turret moves to
     * @param spaceAbove   true when the block above the turret is free; the runtime checks it
     */
    public static Reason canUpgrade(int currentLevel, int kitLevel, boolean spaceAbove) {
        return reasonOf(TurretLevels.MAX_LEVEL, currentLevel, kitLevel, spaceAbove);
    }

    /**
     * Whether a kit may upgrade a turret of a kind, from the level change alone.
     *
     * @param type         the kind of the turret
     * @param currentLevel the turret's level now
     * @param kitLevel     the level of the upgrade kit
     */
    public static Reason canUpgrade(TurretType type, int currentLevel, int kitLevel) {
        return reasonOf(type.maxLevel(), currentLevel, kitLevel);
    }

    /**
     * Whether a kit may upgrade a turret of a kind, from the level change and whether the block above the turret is free.
     *
     * @param type         the kind of the turret
     * @param currentLevel the turret's level now
     * @param kitLevel     the level of the upgrade kit, which is the level the turret moves to
     * @param spaceAbove   true when the block above the turret is free; the runtime checks it
     */
    public static Reason canUpgrade(TurretType type, int currentLevel, int kitLevel, boolean spaceAbove) {
        return reasonOf(type.maxLevel(), currentLevel, kitLevel, spaceAbove);
    }

    private static Reason reasonOf(int maxLevel, int currentLevel, int kitLevel) {
        if (currentLevel >= maxLevel) {
            return Reason.MAX_LEVEL;
        }
        if (currentLevel < 1 || kitLevel != currentLevel + 1) {
            return Reason.WRONG_LEVEL;
        }
        return Reason.OK;
    }

    private static Reason reasonOf(int maxLevel, int currentLevel, int kitLevel, boolean spaceAbove) {
        Reason level = reasonOf(maxLevel, currentLevel, kitLevel);
        if (level != Reason.OK) {
            return level;
        }
        if (kitLevel >= TALL_FROM && !spaceAbove) {
            return Reason.BLOCKED_ABOVE;
        }
        return Reason.OK;
    }
}
