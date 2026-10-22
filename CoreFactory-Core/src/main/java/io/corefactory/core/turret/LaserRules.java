package io.corefactory.core.turret;

/**
 * The rules of a laser shot that are not its geometry: how its damage meets the target's armour, and how long its glow and its light last.
 * A laser's damage is set by its share of the armour it ignores, which is configured; it does not use the round table of the machine gun.
 */
public final class LaserRules {

    /** The ticks the turret's model glows, and the light block stands in front of the barrel, after one shot. */
    public static final int GLOW_TICKS = 4;

    /** The light level of the light block in front of the barrel, from 0 to 15. */
    public static final int LIGHT_LEVEL = 12;

    /** The block and sky light of a glowing model: the most vanilla gives. */
    public static final int FULL_LIGHT = 15;

    private LaserRules() {
    }

    /**
     * The damage to hand to an entity so that, after its armour and toughness, a laser does the damage it would do with the given share of
     * its armour ignored. With a share of 1 the entity's armour takes nothing, and the damage is the configured damage.
     *
     * @param damage       the laser's damage per hit
     * @param armour       the entity's armour value
     * @param toughness    the entity's armour toughness
     * @param armourBypass the share of the armour the beam ignores, from 0 to 1
     */
    public static double damageAfterArmour(double damage, double armour, double toughness, double armourBypass) {
        return ArmourBypass.adjustedDamage(damage, armour, toughness, armourBypass);
    }
}
