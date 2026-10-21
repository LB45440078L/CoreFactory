package io.corefactory.core.turret;

import io.corefactory.core.item.ItemFactory;

/**
 * The item model keys of the turret displays. Level 1 is the turret as placed, and its lower half uses the component's own model.
 * Levels 2 to 5 have a model of their own, {@code <base>_<n>}. A tall level (3 and up) also has an upper half, {@code <base>_<n>_top},
 * which is shown one block above the turret. The names are the resource pack's, so a rename there must follow here. Pure.
 */
public final class ModelKeys {

    /** The component id of the machine gun turret, the base of its models. */
    public static final String MACHINE_GUN = "machine_gun_turret";

    /** The component id of the laser turret, the base of its models. */
    public static final String LASER = "laser_turret";

    /** The suffix of the model of a tall level's upper half. */
    public static final String TOP_SUFFIX = "_top";

    private ModelKeys() {
    }

    /**
     * The model key of one half of a turret at a level.
     *
     * @param type  the kind of turret
     * @param level the level, from 1 to {@link TurretLevels#MAX_LEVEL}
     * @param top   true for the upper half of a tall level
     * @return the key, such as {@code machine/machine_gun_turret_3_top}, without the namespace
     * @throws IllegalArgumentException when the level is out of range, or the upper half is asked for a level that is not tall
     */
    public static String of(TurretType type, int level, boolean top) {
        if (level < 1 || level > TurretLevels.MAX_LEVEL) {
            throw new IllegalArgumentException("no turret level " + level);
        }
        if (top && level < TurretUpgrade.TALL_FROM) {
            throw new IllegalArgumentException("level " + level + " has no upper half");
        }
        String base = type == TurretType.LASER ? LASER : MACHINE_GUN;
        String id = level == 1 ? base : base + "_" + level;
        return ItemFactory.displayKey(top ? id + TOP_SUFFIX : id);
    }
}
