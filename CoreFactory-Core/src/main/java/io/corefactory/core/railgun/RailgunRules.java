package io.corefactory.core.railgun;

import io.corefactory.core.turret.LaserRules;

/**
 * The rules of a rail gun shot that are not its geometry: how its damage meets the target's armour, and how bright the rail gun glows while
 * it charges. Pure.
 */
public final class RailgunRules {

    private RailgunRules() {
    }

    /**
     * The damage a shot leaves in its target after the target's armour. The rule is the laser's, {@link LaserRules#damageAfterArmour}: the
     * level's damage, less the reduction of the armour the shot does not ignore. The share ignored is the section's {@code armour-bypass}.
     *
     * @param settings  the rail gun's settings
     * @param level     the level of the rail gun that fired
     * @param armour    the target's armour value
     * @param toughness the target's armour toughness
     * @return the damage the turret removes from the target, in HP; never negative
     */
    public static double damageAfterArmour(RailgunSettings settings, RailgunLevel level, double armour, double toughness) {
        return LaserRules.damageAfterArmour(settings.level(level).damage(), armour, toughness, settings.armourBypass());
    }

    /**
     * The light level of a rail gun that has been charging for a time. It rises in a straight line from 0 at the start of the charge to the
     * section's {@code max-light} when the charge is complete, and stays there while the rail gun is ready to fire. The light is rounded down.
     *
     * @param settings       the rail gun's settings
     * @param level          the level of the rail gun
     * @param chargedSeconds the seconds since the target was acquired; 0 or less, or not a number, is no charge
     * @return the light level, from 0 to {@code max-light}
     */
    public static int lightLevel(RailgunSettings settings, RailgunLevel level, double chargedSeconds) {
        int max = settings.maxLight();
        double charge = settings.level(level).chargeSeconds();
        if (Double.isNaN(chargedSeconds) || chargedSeconds <= 0.0) {
            return 0;
        }
        if (charge <= 0.0 || chargedSeconds >= charge) {
            return max;
        }
        return (int) Math.floor(max * chargedSeconds / charge);
    }
}
