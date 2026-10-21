package io.corefactory.core.turret;

/**
 * The stats of one upgrade level of a turret. Level 1 is a turret as placed; each upgrade adds one level, up to
 * {@link TurretLevels#MAX_LEVEL}. Every value is clamped to a sane range, so an odd config value never breaks a turret.
 *
 * @param level              the level, from 1 to {@link TurretLevels#MAX_LEVEL}
 * @param damage             the damage per hit before the boost: the regular round's damage for a machine gun, the beam's for a laser
 * @param damageBoostPercent the boost on the damage, in per cent; 0 is no boost, and each level raises it
 * @param fireIntervalTicks  the ticks between two shots at full power
 * @param range              the reach in blocks
 * @param ammoCapacity       the rounds the ammunition belt holds; 0 for a laser, which has no magazine
 * @param cannons            the cannons that fire, from 1 to 2; a machine gun has two from level 4, a laser has one
 * @param powerDemandW       the power the level asks for, in W
 * @param tall               true when the turret is two blocks high, as a door: levels 3 and up
 */
public record TurretLevel(
        int level,
        double damage,
        int damageBoostPercent,
        int fireIntervalTicks,
        double range,
        int ammoCapacity,
        int cannons,
        int powerDemandW,
        boolean tall) {

    /** The most base damage one hit may deal. */
    private static final double MAX_DAMAGE = 1000.0;

    public TurretLevel {
        level = Math.max(1, Math.min(level, TurretLevels.MAX_LEVEL));
        damage = clamp(damage, 0.0, MAX_DAMAGE);
        damageBoostPercent = Math.max(0, Math.min(damageBoostPercent, 1000));
        fireIntervalTicks = Math.max(1, Math.min(fireIntervalTicks, 1200));
        range = clamp(range, 1.0, 64.0);
        ammoCapacity = Math.max(0, Math.min(ammoCapacity, 4096));
        cannons = Math.max(1, Math.min(cannons, DualTargeting.MAX_CANNONS));
        powerDemandW = Math.max(0, Math.min(powerDemandW, 1_000_000));
    }

    /** The damage per hit with the boost applied: the base times one plus the boost in per cent. */
    public double effectiveDamage() {
        return damage * (100 + damageBoostPercent) / 100.0;
    }

    /** True when the level has an ammunition belt: a machine gun does, a laser does not. */
    public boolean hasMagazine() {
        return ammoCapacity > 0;
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
