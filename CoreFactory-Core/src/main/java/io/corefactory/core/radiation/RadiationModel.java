package io.corefactory.core.radiation;

import java.util.Locale;

/**
 * The radiation numbers of the plugin, without any server types, so they can be tested directly.
 *
 * <p>Units: the dose is in sieverts (Sv), the dose rate in sieverts per second (Sv/s), and the displayed rate in
 * millisieverts per hour (mSv/h) or microsieverts per hour (uSv/h), as a Geiger counter shows it. The scale is
 * compressed for play: a few minutes in the fallout of a large blast is enough to make a player sick, and the numbers
 * stay consistent from one second to the next.
 *
 * <p>A zone's rate at distance {@code d} from its centre, for a zone of radius {@code R}, is
 * {@code centreRate * (1 - d / R)}, and it halves every zone half-life. A player's accumulated dose halves every
 * {@link #PLAYER_HALF_LIFE_SECONDS} outside the fallout, so a short exposure is recovered from.
 */
public final class RadiationModel {

    /** Millisieverts per sievert. */
    public static final double MSV_PER_SV = 1000.0;

    /** Seconds per hour, for converting a rate per second to a rate per hour. */
    public static final double SECONDS_PER_HOUR = 3600.0;

    /** The natural background shown on a Geiger counter with no fallout nearby: 0.1 uSv/h, in Sv/s. */
    public static final double BACKGROUND_SV_PER_SECOND = 1e-7 / SECONDS_PER_HOUR;

    /** How long the body takes to clear half of the accumulated dose outside the fallout. */
    public static final double PLAYER_HALF_LIFE_SECONDS = 20 * 60;

    /** Dose at which nausea starts. */
    public static final double NAUSEA_SV = 1.0;

    /** Dose at which damage starts, one quarter of a heart per second. */
    public static final double DAMAGE_SV = 2.0;

    /** Dose at which damage gets severe, one heart per second. */
    public static final double SEVERE_SV = 5.0;

    /** The fallout a blast leaves: about 0.01 Sv/s at the centre for each square root of a megaton of yield. */
    public static final double FALLOUT_RATE_PER_SQRT_MEGATON = 0.01;

    private RadiationModel() {
    }

    /** The centre rate a blast of {@code megatons} leaves behind, in Sv/s. */
    public static double centreRate(double megatons) {
        return FALLOUT_RATE_PER_SQRT_MEGATON * Math.sqrt(Math.max(0.0, megatons));
    }

    /** The fraction of the original amount left after {@code ageSeconds}, given a half-life. */
    public static double decay(double ageSeconds, double halfLifeSeconds) {
        if (ageSeconds <= 0) {
            return 1.0;
        }
        if (halfLifeSeconds <= 0) {
            return 0.0;
        }
        return Math.pow(0.5, ageSeconds / halfLifeSeconds);
    }

    /**
     * The dose rate a zone gives at a distance from its centre.
     *
     * @return the rate in Sv/s, zero at or beyond the zone's radius
     */
    public static double rateAt(double centreRate, double radius, double distance, double ageSeconds,
                                double halfLifeSeconds) {
        if (radius <= 0 || distance >= radius) {
            return 0.0;
        }
        double falloff = 1.0 - Math.max(0.0, distance) / radius;
        return centreRate * falloff * decay(ageSeconds, halfLifeSeconds);
    }

    /** Dose added by standing in a rate for {@code seconds}. */
    public static double absorb(double dose, double rateSvPerSecond, double seconds) {
        return dose + Math.max(0.0, rateSvPerSecond) * Math.max(0.0, seconds);
    }

    /** The dose left after {@code seconds} of the body clearing it. */
    public static double recover(double dose, double seconds) {
        return dose * decay(seconds, PLAYER_HALF_LIFE_SECONDS);
    }

    /** Hit points lost per second at a dose. */
    public static double damagePerSecond(double dose) {
        if (dose >= SEVERE_SV) {
            return 1.0;
        }
        if (dose >= DAMAGE_SV) {
            return 0.25;
        }
        return 0.0;
    }

    public static boolean causesNausea(double dose) {
        return dose >= NAUSEA_SV;
    }

    /** A rate per second, converted to millisieverts per hour. */
    public static double mSvPerHour(double svPerSecond) {
        return svPerSecond * SECONDS_PER_HOUR * MSV_PER_SV;
    }

    /** The colour code of a dose rate: green is background, red is lethal within the hour. */
    public static String severityColour(double mSvPerHour) {
        if (mSvPerHour < 0.1) {
            return "&a";
        }
        if (mSvPerHour < 10) {
            return "&e";
        }
        if (mSvPerHour < 1000) {
            return "&6";
        }
        return "&c";
    }

    /** A rate per second, shown in microsieverts per hour below 1 mSv/h and in millisieverts per hour above. */
    public static String formatRate(double svPerSecond) {
        double mSvPerHour = mSvPerHour(svPerSecond);
        if (mSvPerHour < 1.0) {
            return String.format(Locale.ROOT, "%.2f uSv/h", mSvPerHour * 1000.0);
        }
        return String.format(Locale.ROOT, "%.1f mSv/h", mSvPerHour);
    }
}
