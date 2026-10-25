package io.corefactory.core.artillery;

/**
 * The behaviour of a white phosphorus cloud over time and over distance. The cloud is at full strength at its burst, and its strength
 * halves every half-life. A creature inside the cloud's radius takes the damage per tick scaled by that strength, and nothing outside it.
 * The cloud stands for its duration and then clears. The platform keeps the timing and applies the damage; these are the numbers.
 */
public final class ArtilleryCloud {

    private ArtilleryCloud() {
    }

    /**
     * The strength of a cloud after a time: 1 at the burst, halved each half-life. A time before the burst gives full strength.
     *
     * @param seconds         the time since the burst, in seconds
     * @param halfLifeSeconds the time the strength takes to halve, in seconds, above 0
     * @return the strength, from 0 to 1
     * @throws IllegalArgumentException when the half-life is not finite and above 0
     */
    public static double strength(double seconds, double halfLifeSeconds) {
        if (!(halfLifeSeconds > 0) || !Double.isFinite(halfLifeSeconds)) {
            throw new IllegalArgumentException("half-life must be finite and above 0: " + halfLifeSeconds);
        }
        double elapsed = Math.max(0.0, seconds);
        return Math.pow(0.5, elapsed / halfLifeSeconds);
    }

    /**
     * The damage a creature takes in one tick: the damage per tick at full strength, scaled by the strength, inside the cloud's radius;
     * none outside it.
     *
     * @param damagePerTick the damage per tick at full strength, in HP
     * @param strength      the cloud's strength, from 0 to 1
     * @param distance      the distance of the creature from the cloud's centre on the ground, in blocks
     * @param radius        the cloud's radius, in blocks
     * @return the damage for the tick, in HP
     */
    public static double damagePerTick(double damagePerTick, double strength, double distance, double radius) {
        return distance <= radius ? damagePerTick * strength : 0.0;
    }

    /**
     * Whether the cloud still stands at a time: it stands until its duration has passed.
     *
     * @param seconds         the time since the burst, in seconds
     * @param durationSeconds how long the cloud lasts, in seconds
     * @return true while the cloud stands
     */
    public static boolean standing(double seconds, double durationSeconds) {
        return seconds < durationSeconds;
    }
}
