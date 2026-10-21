package io.corefactory.core.lock;

/**
 * The wrong-PIN count of one locked TNT. After {@code maxAttempts} wrong PINs in a row the keypad is locked out for
 * {@code lockoutMillis}. The lockout ends at a fixed time, and the count starts again from zero once it has ended. A correct
 * PIN clears the count. Pure: the caller passes the time, so it is tested without a server. Held in memory only.
 */
public final class PinAttempts {

    private final int maxAttempts;
    private final long lockoutMillis;
    private int failures;
    private Long lockedUntil;

    /**
     * @param maxAttempts    wrong PINs allowed before the lockout, at least 1
     * @param lockoutMillis  length of the lockout, in milliseconds
     */
    public PinAttempts(int maxAttempts, long lockoutMillis) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.lockoutMillis = Math.max(0, lockoutMillis);
    }

    /** Milliseconds of lockout left at {@code now}, or 0 when the keypad may be used. */
    public long remainingMillis(long now) {
        return lockedUntil != null && now < lockedUntil ? lockedUntil - now : 0;
    }

    /** Wrong PINs left before the next lockout, counted from the last lockout or from the last correct PIN. */
    public int attemptsLeft() {
        return maxAttempts - failures;
    }

    /**
     * Records a wrong PIN at {@code now}. A wrong PIN during a lockout is not counted: the caller should refuse the keypad
     * until {@link #remainingMillis} is 0.
     *
     * @return true when this wrong PIN starts a lockout
     */
    public boolean wrong(long now) {
        if (remainingMillis(now) > 0) {
            return false;
        }
        if (lockedUntil != null) {
            lockedUntil = null;
            failures = 0;
        }
        failures++;
        if (failures >= maxAttempts) {
            failures = 0;
            lockedUntil = now + lockoutMillis;
            return true;
        }
        return false;
    }

    /** Clears the count after a correct PIN. */
    public void succeeded() {
        failures = 0;
        lockedUntil = null;
    }
}
