package io.corefactory.core.ledger;

/**
 * The budget of one ore in one chunk. What is left to mine is the planned count less the mined count, and never less
 * than zero: a mined count above the plan (after a crash that lost a write) leaves nothing to mine, not a negative.
 *
 * @param planned the count planned when the chunk was populated, at least 0
 * @param mined   the count mined since, at least 0
 */
public record OreBudget(long planned, long mined) {

    public OreBudget {
        if (planned < 0 || mined < 0) {
            throw new IllegalArgumentException("planned and mined must not be negative: " + planned + ", " + mined);
        }
    }

    /** The count still to mine: planned less mined, never negative. */
    public long remaining() {
        return Math.max(0, planned - mined);
    }

    /** The budget after more has been mined. */
    public OreBudget withMined(long amount) {
        return new OreBudget(planned, mined + amount);
    }
}
