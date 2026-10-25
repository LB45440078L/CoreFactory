package io.corefactory.paper.artillery;

import io.corefactory.paper.artillery.TerrainCache.ChunkId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Brings the terrain of the chunks the Set Target map shows into the {@link TerrainCache}, within a {@link TerrainBudget}. A chunk that is wanted
 * is first read without generating it: when it is generated, its terrain is sampled and kept. A chunk that is not generated is asked for again
 * with generation, when a generation is free. A wanted chunk that no redraw has asked for within {@link #WANT_TICKS} is dropped before it starts,
 * so the map does not generate chunks the player has turned away from. The loader runs on the main thread, where the chunk source calls back.
 *
 * <p>The loader keeps no chunk loaded: the source loads a chunk only for its callback, and the loader keeps the terrain, not the chunk.
 */
final class TerrainLoader {

    /** The ticks a wanted chunk stays wanted after a redraw asked for it. A redraw of a loading view asks every few ticks. */
    static final long WANT_TICKS = 3L * LoadingAnimation.STEP_TICKS;

    /** Reads and generates chunks for the loader. */
    interface ChunkSource {

        /**
         * Gives the terrain of a chunk to the callback, on the main thread. The chunk is loaded for the callback only.
         *
         * @param id       the chunk
         * @param generate true to generate the chunk when it is not generated
         * @param done     receives the terrain, {@link TerrainCache#COLUMNS} bytes, or null when the chunk is not generated (a read) or could
         *                 not be generated
         */
        void fetch(ChunkId id, boolean generate, Consumer<byte[]> done);
    }

    /** The step a wanted chunk is at: a read first, a generation when the read found no generated chunk. */
    private enum Step {
        READ, GENERATE
    }

    /** A wanted chunk: its step, its place in the queue, the tick a redraw last asked for it, and whether its request is in flight. */
    private static final class Want {
        final long token;
        Step step = Step.READ;
        double priority;
        long asked;
        boolean inFlight;

        Want(long token, double priority, long asked) {
            this.token = token;
            this.priority = priority;
            this.asked = asked;
        }
    }

    private final ChunkSource source;
    private final TerrainCache cache;
    private final TerrainBudget budget;
    private final Map<ChunkId, Want> wanted = new HashMap<>();
    private long tokens;

    /**
     * @param source the chunk source
     * @param cache  the terrain kept
     * @param budget the reads and generations in flight
     */
    TerrainLoader(ChunkSource source, TerrainCache cache, TerrainBudget budget) {
        this.source = source;
        this.cache = cache;
        this.budget = budget;
    }

    /**
     * Asks for a chunk's terrain. A chunk whose terrain is kept is not wanted. A chunk asked for again keeps its place, and its priority is the
     * latest one.
     *
     * @param id       the chunk
     * @param priority the chunk's distance from the view's centre, squared: the nearer, the sooner
     * @param tick     the tick of the redraw that asks
     * @return true when the chunk's terrain is not kept, so its tile is loading
     */
    boolean want(ChunkId id, double priority, long tick) {
        if (cache.contains(id)) {
            return false;
        }
        Want want = wanted.get(id);
        if (want == null) {
            wanted.put(id, new Want(++tokens, priority, tick));
        } else {
            want.priority = priority;
            want.asked = tick;
        }
        return true;
    }

    /**
     * The colour of a column of a chunk for the map: the kept terrain's colour, -1 when the chunk could not be read, or the loading tile's colour
     * when the chunk's terrain is not kept yet.
     *
     * @param id    the chunk
     * @param x     the block x of the column
     * @param z     the block z of the column
     * @param phase the loading tile's step, from {@link LoadingAnimation#phase}
     * @return the palette byte as 0 to 255, or -1
     */
    int colourAt(ChunkId id, int x, int z, int phase) {
        byte[] colours = cache.get(id);
        if (colours == null) {
            return LoadingAnimation.colour(x, z, phase) & 0xFF;
        }
        if (colours == TerrainCache.UNAVAILABLE) {
            return -1;
        }
        return colours[TerrainCache.index(x, z)] & 0xFF;
    }

    /**
     * Replaces a kept chunk's terrain with a new sample, when the chunk is kept: a chunk that was loaded while its terrain was kept has changed
     * since it was sampled.
     *
     * @param id     the chunk
     * @param sample the new terrain, read only when the chunk is kept
     */
    void refresh(ChunkId id, Supplier<byte[]> sample) {
        if (cache.contains(id)) {
            cache.put(id, sample.get());
        }
    }

    /**
     * Starts the requests that the budget has room for, nearest chunk first, and drops the wanted chunks that no redraw asked for lately. Called
     * once a tick while a session is open.
     *
     * @param tick the current tick
     */
    void tick(long tick) {
        List<ChunkId> ready = new ArrayList<>();
        Iterator<Map.Entry<ChunkId, Want>> entries = wanted.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<ChunkId, Want> entry = entries.next();
            Want want = entry.getValue();
            if (want.inFlight) {
                continue;
            }
            if (tick - want.asked > WANT_TICKS) {
                entries.remove();
            } else {
                ready.add(entry.getKey());
            }
        }
        ready.sort(Comparator.comparingDouble(id -> wanted.get(id).priority));
        for (ChunkId id : ready) {
            Want want = wanted.get(id);
            if (want == null || want.inFlight) {
                continue;
            }
            boolean generate = want.step == Step.GENERATE;
            if (generate ? budget.startGeneration() : budget.startRead()) {
                want.inFlight = true;
                long token = want.token;
                source.fetch(id, generate, colours -> finish(id, token, colours));
            }
        }
    }

    /** Takes back the chunks of a world that unloads: their in-flight requests give their places back, and their terrain is dropped. */
    void forget(UUID world) {
        cache.forget(world);
        wanted.entrySet().removeIf(entry -> {
            if (!entry.getKey().world().equals(world)) {
                return false;
            }
            Want want = entry.getValue();
            if (want.inFlight) {
                release(want.step);
            }
            return true;
        });
    }

    /** The number of chunks wanted and not kept: the tiles still loading. */
    int pending() {
        return wanted.size();
    }

    /** An answer from the source. An answer for a request that was cancelled is ignored: its place was given back when it was cancelled. */
    private void finish(ChunkId id, long token, byte[] colours) {
        Want want = wanted.get(id);
        if (want == null || want.token != token) {
            return;
        }
        Step step = want.step;
        want.inFlight = false;
        release(step);
        if (colours != null) {
            cache.put(id, colours);
            wanted.remove(id);
        } else if (step == Step.GENERATE) {
            cache.put(id, TerrainCache.UNAVAILABLE);
            wanted.remove(id);
        } else {
            want.step = Step.GENERATE;
        }
    }

    private void release(Step step) {
        if (step == Step.GENERATE) {
            budget.finishGeneration();
        } else {
            budget.finishRead();
        }
    }
}
