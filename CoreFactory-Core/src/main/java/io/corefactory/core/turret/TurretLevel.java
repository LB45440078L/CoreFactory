package io.corefactory.core.turret;

/**
 * The stats of one upgrade level of a turret. Level 1 is a turret as placed; each upgrade adds one level, up to the kind's
 * {@link TurretType#maxLevel()}. Every value is clamped to a sane range, so an odd config value never breaks a turret. The ranges are the
 * widest any kind may have; {@link TurretLevels} narrows the machine gun's and the laser's to the ceilings they have always had.
 *
 * @param level              the level, from 1 to {@link TurretLevels#MAX_LEVEL}
 * @param damage             the damage per hit before the boost: the regular round's damage for a machine gun, the beam's for a laser,
 *                           the shot's for a railgun
 * @param damageBoostPercent the boost on the damage, in per cent; 0 is no boost, and each level raises it
 * @param fireIntervalTicks  the ticks between two shots at full power
 * @param range              the reach in blocks
 * @param ammoCapacity       the rounds the ammunition belt holds; 0 for a laser or a railgun, which have no magazine
 * @param cannons            the cannons that fire, from 1 to 2; a machine gun has two from level 4, a laser and a railgun have one
 * @param powerDemandW       the power the level asks for, in W
 * @param tall               true when the turret is two blocks high, as a door: levels 3 and up
 * @param rotationDegreesPerTick the most the level turns in one tick, in degrees; 0 keeps the turrets' shared rotation speed
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
        boolean tall,
        double rotationDegreesPerTick) {

    /** The most base damage one hit may deal: the widest of the kinds, a railgun's level III. */
    private static final double MAX_DAMAGE = 10_000.0;

    /** The most reach a level may have, in blocks: the widest of the kinds, a railgun's level III. */
    private static final double MAX_RANGE_BLOCKS = 128.0;

    /** The most power a level may ask for, in W: the widest of the kinds, a railgun's level III. */
    private static final int MAX_POWER_W = 10_000_000;

    /** The most a level may turn in one tick. */
    private static final double MAX_ROTATION_DEGREES = 90.0;

    /**
     * A level that keeps the turrets' shared rotation speed, with no rotation of its own.
     */
    public TurretLevel(int level, double damage, int damageBoostPercent, int fireIntervalTicks, double range, int ammoCapacity,
                       int cannons, int powerDemandW, boolean tall) {
        this(level, damage, damageBoostPercent, fireIntervalTicks, range, ammoCapacity, cannons, powerDemandW, tall, 0.0);
    }

    public TurretLevel {
        level = Math.max(1, Math.min(level, TurretLevels.MAX_LEVEL));
        damage = clamp(damage, 0.0, MAX_DAMAGE);
        damageBoostPercent = Math.max(0, Math.min(damageBoostPercent, 1000));
        fireIntervalTicks = Math.max(1, Math.min(fireIntervalTicks, 1200));
        range = clamp(range, 1.0, MAX_RANGE_BLOCKS);
        ammoCapacity = Math.max(0, Math.min(ammoCapacity, 4096));
        cannons = Math.max(1, Math.min(cannons, DualTargeting.MAX_CANNONS));
        powerDemandW = Math.max(0, Math.min(powerDemandW, MAX_POWER_W));
        rotationDegreesPerTick = clamp(rotationDegreesPerTick, 0.0, MAX_ROTATION_DEGREES);
    }

    /** The damage per hit with the boost applied: the base times one plus the boost in per cent. */
    public double effectiveDamage() {
        return damage * (100 + damageBoostPercent) / 100.0;
    }

    /**
     * The most this level turns in one tick: its own rotation when it has one, and the given speed otherwise.
     *
     * @param shared the turrets' shared rotation speed, in degrees per tick
     */
    public double rotationDegreesOr(double shared) {
        return rotationDegreesPerTick > 0.0 ? rotationDegreesPerTick : shared;
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
