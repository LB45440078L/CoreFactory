package io.corefactory.core.deposit;

/**
 * The items a drill has produced and not yet taken from its deposit. Each take is a store call, so a drill takes in batches:
 * a take is due when {@link #size} items are pending, or when the oldest pending item has waited {@link #maxTicks} ticks.
 * The clock starts at the first item of a batch and keeps running after a partial take, so a few items never wait longer
 * than a second. Pure and not thread-safe: each drill owns one.
 */
public final class TakeBatch {

    /** The pending items that make a take due at once. */
    public static final int DEFAULT_SIZE = 10;

    /** The ticks the oldest pending item may wait before a take is due: one second. */
    public static final int DEFAULT_TICKS = 20;

    private final int size;
    private final int maxTicks;
    private long pending;
    private int waited;

    /**
     * @param size     the pending items that make a take due, at least 1
     * @param maxTicks the ticks the oldest pending item may wait, at least 1
     * @throws IllegalArgumentException when either is below 1
     */
    public TakeBatch(int size, int maxTicks) {
        if (size < 1 || maxTicks < 1) {
            throw new IllegalArgumentException("A batch needs a size and a wait of at least 1, not " + size + " and " + maxTicks);
        }
        this.size = size;
        this.maxTicks = maxTicks;
    }

    /**
     * Adds produced items to the batch.
     *
     * @throws IllegalArgumentException when the count is negative
     */
    public void produce(long items) {
        if (items < 0) {
            throw new IllegalArgumentException("A drill cannot produce a negative amount: " + items);
        }
        if (items == 0) {
            return;
        }
        if (pending == 0) {
            waited = 0;
        }
        pending += items;
    }

    /** Advances the wait clock by one tick, while items are pending. */
    public void tick() {
        if (pending > 0 && waited < maxTicks) {
            waited++;
        }
    }

    /** True when a take should run now: enough items are pending, or the oldest one has waited long enough. */
    public boolean due() {
        return pending >= size || (pending > 0 && waited >= maxTicks);
    }

    /** The items produced and not yet taken. */
    public long pending() {
        return pending;
    }

    /**
     * Records a take of the pending items. The store removed {@code taken} of the {@code asked} items. Items the store did not
     * have are gone, so a short take empties the batch.
     *
     * @param asked the amount asked of the store, at most the pending items
     * @param taken the amount the store removed, from 0 to {@code asked}
     * @throws IllegalArgumentException when the amounts do not fit the pending items
     */
    public void settle(long asked, long taken) {
        if (asked < 0 || asked > pending || taken < 0 || taken > asked) {
            throw new IllegalArgumentException("A take of " + asked + " with " + taken + " taken does not fit "
                    + pending + " pending");
        }
        pending = taken < asked ? 0 : pending - asked;
        if (pending == 0) {
            waited = 0;
        }
    }

    /** Drops the pending items without a take, for an item that has no deposit to go back to. */
    public void discard() {
        pending = 0;
        waited = 0;
    }
}
