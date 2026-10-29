package io.corefactory.paper.logistics;

/**
 * The rule that limits the console warning about the item display cap on belts. A warning may be logged when the throttle is enabled
 * and either nothing was logged yet, or at least the interval has passed since the last one. An interval of zero or less means one
 * warning only. A disabled throttle never allows a warning. Pure: the caller passes the time, in milliseconds.
 */
public final class CapWarningThrottle {

    private final boolean enabled;
    private final long intervalMillis;
    private boolean logged;
    private long lastMillis;

    /**
     * @param enabled        whether the warning is logged at all
     * @param intervalMillis the least time between two warnings in milliseconds; zero or less means one warning only
     */
    public CapWarningThrottle(boolean enabled, long intervalMillis) {
        this.enabled = enabled;
        this.intervalMillis = Math.max(0, intervalMillis);
    }

    /**
     * Builds the throttle from the config values.
     *
     * @param enabled the {@code logistics.cap-warning} toggle
     * @param minutes the {@code logistics.cap-warning-minutes} value; zero, a negative value or a value that is not a number means once only
     */
    public static CapWarningThrottle of(boolean enabled, double minutes) {
        long millis = minutes > 0 && Double.isFinite(minutes) ? Math.round(minutes * 60_000.0) : 0L;
        return new CapWarningThrottle(enabled, millis);
    }

    /**
     * Asks whether a warning may be logged now, and if so records it.
     *
     * @param nowMillis the current server time in milliseconds
     * @return true when the caller should log the warning
     */
    public boolean allow(long nowMillis) {
        if (!enabled) {
            return false;
        }
        if (logged && (intervalMillis <= 0 || nowMillis - lastMillis < intervalMillis)) {
            return false;
        }
        logged = true;
        lastMillis = nowMillis;
        return true;
    }
}
