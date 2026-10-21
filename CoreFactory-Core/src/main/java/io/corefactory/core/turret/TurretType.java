package io.corefactory.core.turret;

/**
 * The two turret kinds. Each has its section in config.yml, and its {@code levels} entries there set its upgrade levels.
 */
public enum TurretType {

    /** The machine gun turret: fires rounds from an ammunition belt, with one cannon, or two from level 4. */
    MACHINE_GUN("turrets"),

    /** The laser turret: electric, fires an instant beam, one cannon at every level. */
    LASER("laser-turret");

    private final String section;

    TurretType(String section) {
        this.section = section;
    }

    /** The section of config.yml that holds this kind's settings, such as {@code turrets}. */
    public String section() {
        return section;
    }
}
