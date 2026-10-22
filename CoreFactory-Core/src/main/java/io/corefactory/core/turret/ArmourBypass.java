package io.corefactory.core.turret;

/**
 * The armour bypass of a round. The server takes a share of each hit with the entity's armour and armour toughness, as vanilla does: the
 * reduction is the armour less the damage over {@code 2 + toughness / 4}, kept between a fifth of the armour and 20 points, and each point
 * takes 4 per cent. A round that bypasses a share of the armour is dealt the damage it would do if the armour were that much lower. The
 * damage handed to the entity is the amount whose result after that same formula is the wanted damage, so the server's armour gives exactly
 * that result.
 */
public final class ArmourBypass {

    /** The most armour points that take a share of a hit, in vanilla; more armour adds nothing. */
    static final double ARMOUR_CAP = 20.0;

    /** The armour points that make up the whole damage reduction, in vanilla: 25 points is the full 100 per cent. */
    static final double ARMOUR_DIVISOR = 25.0;

    /** The base of the toughness term in vanilla's divisor, which is {@code 2 + toughness / 4}. */
    static final double TOUGHNESS_BASE = 2.0;

    /** The toughness per point of the divisor's toughness term, in vanilla. */
    static final double TOUGHNESS_PER_POINT = 4.0;

    /** The least share of a hit that armour leaves: the reduction never exceeds 20 points, so a hit keeps at least 1 - 20/25. */
    private static final double MIN_KEPT = 1.0 - ARMOUR_CAP / ARMOUR_DIVISOR;

    /** Bisection steps, which narrow the search far below the precision of a double. */
    private static final int SOLVE_STEPS = 64;

    private ArmourBypass() {
    }

    /**
     * The damage to hand to an entity so that, after its armour, the round does the damage it would do with a share of its armour ignored.
     *
     * @param damage    the round's damage
     * @param armour    the entity's armour value
     * @param toughness the entity's armour toughness
     * @param bypass    the share of the armour the round ignores, from 0 to 1
     * @return the damage to deal; equal to {@code damage} when the bypass is 0 or the entity has no armour
     */
    public static double adjustedDamage(double damage, double armour, double toughness, double bypass) {
        if (!(damage > 0.0)) {
            return 0.0;
        }
        double shareBypassed = Math.max(0.0, Math.min(1.0, bypass));
        double armourNow = Math.max(0.0, armour);
        double toughnessNow = Math.max(0.0, toughness);
        if (shareBypassed <= 0.0 || armourNow <= 0.0) {
            return damage;
        }
        double wanted = afterArmour(damage, armourNow * (1.0 - shareBypassed), toughnessNow);
        return handedFor(wanted, armourNow, toughnessNow);
    }

    /**
     * The damage an entity takes from a hit of the given damage, by vanilla's armour formula: the damage times one less the reduction over
     * 25, where the reduction is the armour less the damage over the divisor, kept between a fifth of the armour and 20.
     *
     * @param damage    the damage handed to the entity
     * @param armour    the entity's armour value
     * @param toughness the entity's armour toughness
     */
    static double afterArmour(double damage, double armour, double toughness) {
        if (!(damage > 0.0)) {
            return 0.0;
        }
        double armourNow = Math.max(0.0, armour);
        double divisor = TOUGHNESS_BASE + Math.max(0.0, toughness) / TOUGHNESS_PER_POINT;
        double reduction = Math.min(ARMOUR_CAP, Math.max(armourNow * 0.2, armourNow - damage / divisor));
        return damage * (1.0 - reduction / ARMOUR_DIVISOR);
    }

    /**
     * The damage to hand over whose result after the entity's armour is the wanted damage. The result rises with the damage handed over, so
     * a bisection finds it: it lies between the wanted damage, since armour takes nothing more than the damage, and five times it, since
     * armour takes at most 80 per cent.
     */
    private static double handedFor(double wanted, double armour, double toughness) {
        double low = wanted;
        double high = wanted / MIN_KEPT;
        for (int step = 0; step < SOLVE_STEPS; step++) {
            double middle = (low + high) / 2.0;
            if (afterArmour(middle, armour, toughness) < wanted) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return (low + high) / 2.0;
    }
}
