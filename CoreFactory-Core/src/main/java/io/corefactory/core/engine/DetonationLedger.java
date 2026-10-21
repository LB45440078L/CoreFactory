package io.corefactory.core.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * Remembers the server tick of the last detonation of each block, so that one ignition detonates once. A block may detonate
 * once per tick: a second claim for the same block on the same tick is refused. Entries from earlier ticks are dropped
 * as the map grows, since only the current tick can refuse a claim.
 *
 * <p>Keys are the position keys of the placed TNT registry. Main thread only.
 */
public final class DetonationLedger {

    private static final int PRUNE_AT = 256;

    private final Map<String, Long> lastTick = new HashMap<>();

    /**
     * Records a detonation of the block at {@code key} on {@code tick}.
     *
     * @return false when that block already detonated on this tick, in which case nothing is recorded
     */
    public boolean claim(String key, long tick) {
        if (detonatedOn(key, tick)) {
            return false;
        }
        if (lastTick.size() >= PRUNE_AT) {
            lastTick.values().removeIf(past -> past < tick);
        }
        lastTick.put(key, tick);
        return true;
    }

    /** True when the block at {@code key} detonated on {@code tick}. */
    public boolean detonatedOn(String key, long tick) {
        Long last = lastTick.get(key);
        return last != null && last == tick;
    }
}
