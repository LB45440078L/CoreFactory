package io.corefactory.core.turret;

/**
 * The armour bypass of a round. Vanilla armour takes a share of each hit: each armour point takes 4 per cent, up to 20 points, which is
 * 80 per cent. A round that bypasses a share of the armour is dealt the damage it would do if the armour were that much lower. The
 * damage handed to the entity is scaled so that the armour the server then applies gives exactly that result.
 */
public final class ArmourBypass {

    /** The armour points that count in vanilla; the rest add nothing. */
    static final double ARMOUR_CAP = 20.0;

    /** The armour points that make up the whole damage reduction, in vanilla: 25 points is the full 100 per cent. */
    static final double ARMOUR_DIVISOR = 25.0;

    private ArmourBypass() {
    }

    /**
     * The damage to hand to an entity so that, after its armour, the round does the damage it would do with less armour.
     *
     * @param damage the round's damage
     * @param armour the entity's armour value
     * @param bypass the share of the armour the round ignores, from 0 to 1
     * @return the damage to deal; equal to {@code damage} when the bypass is 0
     */
    public static double adjustedDamage(double damage, double armour, double bypass) {
        if (damage <= 0 || bypass <= 0) {
            return Math.max(0.0, damage);
        }
        double shareBypassed = Math.max(0.0, Math.min(1.0, bypass));
        double armourNow = Math.max(0.0, Math.min(ARMOUR_CAP, armour));
        double armourIgnored = armourNow * (1.0 - shareBypassed);
        double keptNow = 1.0 - armourNow / ARMOUR_DIVISOR;
        double keptIgnored = 1.0 - armourIgnored / ARMOUR_DIVISOR;
        return damage * keptIgnored / keptNow;
    }
}
