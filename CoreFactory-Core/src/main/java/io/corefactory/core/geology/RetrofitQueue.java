package io.corefactory.core.geology;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A first-in, first-out queue of chunks waiting for ore placement, with a per-tick budget. A chunk is queued once:
 * queuing it again keeps its place, and a forced request upgrades the entry to forced. Not thread-safe; the Spigot
 * module uses it from the main thread only.
 */
public final class RetrofitQueue {

    /**
     * A chunk in one world.
     *
     * @param world  the world name
     * @param chunkX chunk x
     * @param chunkZ chunk z
     */
    public record ChunkRef(String world, int chunkX, int chunkZ) {
    }

    /**
     * A queued chunk.
     *
     * @param chunk the chunk
     * @param force true when the chunk is re-planned even if its marker is current
     */
    public record Entry(ChunkRef chunk, boolean force) {
    }

    private final int perTick;
    private final Map<ChunkRef, Boolean> pending = new LinkedHashMap<>();

    /** @param perTick the most chunks {@link #take} returns at once; values below 1 are raised to 1 */
    public RetrofitQueue(int perTick) {
        this.perTick = Math.max(1, perTick);
    }

    /** The budget: how many chunks each tick may take. */
    public int perTick() {
        return perTick;
    }

    /**
     * Queues a chunk.
     *
     * @return true when the chunk was not queued before
     */
    public boolean offer(String world, int chunkX, int chunkZ, boolean force) {
        ChunkRef ref = new ChunkRef(world, chunkX, chunkZ);
        Boolean existing = pending.get(ref);
        if (existing == null) {
            pending.put(ref, force);
            return true;
        }
        if (force && !existing) {
            pending.put(ref, true);
        }
        return false;
    }

    /** Removes and returns up to {@link #perTick()} chunks, oldest first. */
    public List<Entry> take() {
        List<Entry> out = new ArrayList<>(Math.min(perTick, pending.size()));
        Iterator<Map.Entry<ChunkRef, Boolean>> it = pending.entrySet().iterator();
        while (it.hasNext() && out.size() < perTick) {
            Map.Entry<ChunkRef, Boolean> next = it.next();
            out.add(new Entry(next.getKey(), next.getValue()));
            it.remove();
        }
        return out;
    }

    /** The number of chunks still waiting. */
    public int size() {
        return pending.size();
    }

    /** True when nothing waits. */
    public boolean isEmpty() {
        return pending.isEmpty();
    }

    /** Drops everything that waits. */
    public void clear() {
        pending.clear();
    }
}
