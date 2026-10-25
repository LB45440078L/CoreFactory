package io.corefactory.paper.machine;

import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DrillAccumulator;
import io.corefactory.core.deposit.TakeBatch;

import java.util.List;

/**
 * The mining state of one drill, kept while the drill is loaded and never saved: the accumulator that turns its rate into whole
 * items or litres, the batch of items waiting for a take, the deposit it works on, whether the oil network refused a petroleum drill's
 * last push, and what its last search found. A search runs every
 * {@link #SEARCH_TICKS} ticks, and again at once when the deposit it works on runs out.
 */
final class DrillMining {

    /** Ticks between two searches of the store for deposits: one second. */
    static final int SEARCH_TICKS = 20;

    private final DrillAccumulator accumulator = new DrillAccumulator(DrillAccumulator.SECONDS_PER_TICK);
    private final TakeBatch batch = new TakeBatch(TakeBatch.DEFAULT_SIZE, TakeBatch.DEFAULT_TICKS);
    private List<Deposit> listed = List.of();
    private boolean targetInRange;
    private Deposit current;
    private long nextSearch;
    private long takesBlockedUntil = Long.MIN_VALUE;
    private boolean networkRefused;

    /** The whole items this drill has made from its rate, not yet taken from the deposit. */
    DrillAccumulator accumulator() {
        return accumulator;
    }

    /** The items made and not yet taken from the deposit, batched so the store is called a few times a second at most. */
    TakeBatch batch() {
        return batch;
    }

    /** The deposits of the drill's square that its kind may mine, as the last search saw them, for the resources menu. */
    List<Deposit> listed() {
        return listed;
    }

    /** True when a deposit the drill's kind and selection allow touches its square. */
    boolean targetInRange() {
        return targetInRange;
    }

    /** The deposit the drill works on, with the amount left in it as this drill sees it, or null when it works on none. */
    Deposit current() {
        return current;
    }

    /** True when the last search is due: at the start, every {@link #SEARCH_TICKS} ticks, or when {@link #searchSoon} was called. */
    boolean searchDue(long tick) {
        return tick >= nextSearch;
    }

    /** Records a search: what the square holds and the deposit to work on now. The next search is {@link #SEARCH_TICKS} later. */
    void searched(long tick, List<Deposit> listed, boolean targetInRange, Deposit current) {
        this.listed = List.copyOf(listed);
        this.targetInRange = targetInRange;
        this.current = current;
        this.nextSearch = tick + SEARCH_TICKS;
    }

    /** Asks for a search at this tick, which the next tick then runs. */
    void searchSoon(long tick) {
        this.nextSearch = tick;
    }

    /** True when the oil network refused the petroleum a petroleum drill last pushed into it, so it takes no more for now. */
    boolean networkRefused() {
        return networkRefused;
    }

    /** Records whether the oil network refused the petroleum the drill last pushed. */
    void setNetworkRefused(boolean refused) {
        this.networkRefused = refused;
    }

    /** True while takes are held back after the store refused one. */
    boolean takesBlocked(long tick) {
        return tick < takesBlockedUntil;
    }

    /** Holds back takes for one search interval, after the store refused one. */
    void blockTakes(long tick) {
        this.takesBlockedUntil = tick + SEARCH_TICKS;
    }

    /**
     * Applies a take to the deposit the drill works on. A short take means the deposit is empty, so the drill looks for another one
     * at once; a deposit that reaches zero is dropped the same way.
     *
     * @param asked the amount asked of the store
     * @param taken the amount the store removed
     */
    void took(long tick, long asked, long taken) {
        if (current == null) {
            return;
        }
        long left = taken < asked ? 0 : Math.max(0, current.remaining() - taken);
        if (left == 0) {
            current = null;
            searchSoon(tick);
        } else {
            current = current.withRemaining(left);
        }
    }
}
