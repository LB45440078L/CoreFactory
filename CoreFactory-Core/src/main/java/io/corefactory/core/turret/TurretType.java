package io.corefactory.core.turret;

/**
 * The turret kinds. Each has its section in config.yml, and its {@code levels} entries there set its upgrade levels. The machine gun and
 * the laser have five levels each; the railgun has three. A kind's levels are counted by {@link #maxLevel()}, so a level outside that
 * range is refused for that kind alone.
 */
public enum TurretType {

    /** The machine gun turret: fires rounds from an ammunition belt, with one cannon, or two from level 4. */
    MACHINE_GUN("turrets", 5),

    /** The laser turret: electric, fires an instant beam, one cannon at every level. */
    LASER("laser-turret", 5),

    /** The rail gun: electric, charges for a long wind-up and fires one heavy shot that travels as a helix. One cannon, three levels. */
    RAILGUN("railgun", 3);

    private final String section;
    private final int maxLevel;

    TurretType(String section, int maxLevel) {
        this.section = section;
        this.maxLevel = maxLevel;
    }

    /** The section of config.yml that holds this kind's settings, such as {@code turrets}. */
    public String section() {
        return section;
    }

    /** The highest level this kind can be upgraded to: 5 for a machine gun and a laser, 3 for a railgun. Level 1 is the lowest. */
    public int maxLevel() {
        return maxLevel;
    }

    /** True when the kind fires rounds from an ammunition belt. Only the machine gun does; the others are electric. */
    public boolean hasMagazine() {
        return this == MACHINE_GUN;
    }
}
