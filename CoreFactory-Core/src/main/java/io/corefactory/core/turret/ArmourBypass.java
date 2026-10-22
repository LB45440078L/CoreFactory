package io.corefactory.core.turret;

/**
 * The armour a hit meets. The game takes a share of each hit with the entity's armour and armour toughness: the reduction is the armour
 * less the damage over {@code 2 + toughness / 4}, kept between a fifth of the armour and 20 points, and each point takes 4 per cent. A round
 * that bypasses a share of the armour meets only the armour that is left, so its reduction is worked out with the rest. The turret removes
 * its hits from the health itself, so the damage it applies is the damage this rule gives; the server's armour does not act on it again.
 */
public final class ArmourBypass {

    /** The most armour points that take a share of a hit, in vanilla; more armour adds nothing. */
    private static final double ARMOUR_CAP = 20.0;

    /** The armour points that make up the whole damage reduction, in vanilla: 25 points is the full 100 per cent. */
    private static final double ARMOUR_DIVISOR = 25.0;

    /** The least share of the armour a hit's reduction takes, in vanilla: a fifth of the armour. */
    private static final double ARMOUR_FLOOR_DIVISOR = 5.0;

    /** The base of the toughness term in vanilla's divisor, which is {@code 2 + toughness / 4}. */
    private static final double TOUGHNESS_BASE = 2.0;

    /** The toughness per point of the divisor's toughness term, in vanilla. */
    private static final double TOUGHNESS_PER_POINT = 4.0;

    private ArmourBypass() {
    }

    /**
     * The damage that reaches an entity from a hit, after the share of its armour that the hit bypasses is ignored. The armour and the
     * toughness that remain are the given ones times one less the bypass; the reduction is the remaining armour less the damage over
     * {@code 2 + remaining toughness / 4}, kept between a fifth of the remaining armour and 20 points, and the damage is taken down by that
     * reduction over 25.
     *
     * @param damage    the hit's damage before the entity's armour
     * @param armour    the entity's armour value
     * @param toughness the entity's armour toughness
     * @param bypass    the share of the armour the hit ignores, from 0 to 1; a bypass of 1 leaves the damage whole
     * @return the damage that reaches the entity, never negative; 0 when the hit has no damage
     */
    public static double finalDamage(double damage, double armour, double toughness, double bypass) {
        if (!(damage > 0.0)) {
            return 0.0;
        }
        double share = bypass > 0.0 ? Math.min(1.0, bypass) : 0.0;
        double armourLeft = Math.max(0.0, armour) * (1.0 - share);
        double toughnessLeft = Math.max(0.0, toughness) * (1.0 - share);
        double divisor = TOUGHNESS_BASE + toughnessLeft / TOUGHNESS_PER_POINT;
        double reduction = Math.min(ARMOUR_CAP, Math.max(armourLeft / ARMOUR_FLOOR_DIVISOR, armourLeft - damage / divisor));
        return Math.max(0.0, damage * (1.0 - reduction / ARMOUR_DIVISOR));
    }
}
