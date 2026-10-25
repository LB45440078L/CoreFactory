package io.corefactory.core.turret;

import io.corefactory.core.item.ItemFactory;

/**
 * The item model keys of the turret displays. Level 1 is the turret as placed, and its lower half uses the component's own model.
 * Levels 2 up to the kind's top level have a model of their own, {@code <base>_<n>}. A tall level (3 and up) also has an upper half,
 * {@code <base>_<n>_top}, which is shown one block above the turret. The names are the resource pack's, so a rename there must follow here.
 *
 * <p>A rail gun is built of separate models instead: a body at every level, {@code railgun_<n>_body}, which turns to the aim, a barrel,
 * {@code railgun_<n>_barrel}, which tilts about its trunnion, and for level 3 the top, {@code railgun_3_top}. Its level 1 has no model of the
 * component's own id. Pure.
 */
public final class ModelKeys {

    /** The component id of the machine gun turret, the base of its models. */
    public static final String MACHINE_GUN = "machine_gun_turret";

    /** The component id of the laser turret, the base of its models. */
    public static final String LASER = "laser_turret";

    /** The component id of the rail gun. Its models are named by {@link #barrel} and {@link #of}, not by this id. */
    public static final String RAILGUN = "railgun_turret";

    /** The suffix of the model of a tall level's upper half. */
    public static final String TOP_SUFFIX = "_top";

    /** The suffix of a rail gun's body model: the part that turns to the aim. */
    public static final String BODY_SUFFIX = "_body";

    /** The suffix of a rail gun's barrel model: the part that tilts about its trunnion. */
    public static final String BARREL_SUFFIX = "_barrel";

    /** The base of a rail gun's model ids, which are {@code railgun_<n>_body} and so on. */
    private static final String RAILGUN_BASE = "railgun";

    private ModelKeys() {
    }

    /**
     * The model key of one half of a turret at a level.
     *
     * @param type  the kind of turret
     * @param level the level, from 1 to {@link TurretType#maxLevel()} of the kind
     * @param top   true for the upper half of a tall level
     * @return the key, such as {@code machine/machine_gun_turret_3_top}, without the namespace
     * @throws IllegalArgumentException when the level is out of range for the kind, or the upper half is asked for a level that is not tall
     */
    public static String of(TurretType type, int level, boolean top) {
        if (level < 1 || level > type.maxLevel()) {
            throw new IllegalArgumentException("no " + type.name().toLowerCase(java.util.Locale.ROOT) + " level " + level);
        }
        if (top && level < TurretUpgrade.TALL_FROM) {
            throw new IllegalArgumentException("level " + level + " has no upper half");
        }
        if (type == TurretType.RAILGUN) {
            return ItemFactory.displayKey(RAILGUN_BASE + "_" + level + (top ? TOP_SUFFIX : BODY_SUFFIX));
        }
        String id = level == 1 ? baseOf(type) : baseOf(type) + "_" + level;
        return ItemFactory.displayKey(top ? id + TOP_SUFFIX : id);
    }

    /**
     * The model key of a rail gun's barrel at a level. Only a rail gun has a barrel model; a machine gun and a laser show their barrel in
     * their one model.
     *
     * @param type  the kind of turret, which must be the rail gun
     * @param level the level, from 1 to {@link TurretType#maxLevel()} of the kind
     * @return the key, such as {@code machine/railgun_2_barrel}, without the namespace
     * @throws IllegalArgumentException when the kind is not the rail gun, or the level is out of range for it
     */
    public static String barrel(TurretType type, int level) {
        if (type != TurretType.RAILGUN) {
            throw new IllegalArgumentException(type.name().toLowerCase(java.util.Locale.ROOT) + " has no barrel model");
        }
        if (level < 1 || level > type.maxLevel()) {
            throw new IllegalArgumentException("no railgun level " + level);
        }
        return ItemFactory.displayKey(RAILGUN_BASE + "_" + level + BARREL_SUFFIX);
    }

    private static String baseOf(TurretType type) {
        return switch (type) {
            case MACHINE_GUN -> MACHINE_GUN;
            case LASER -> LASER;
            case RAILGUN -> RAILGUN;
        };
    }
}
