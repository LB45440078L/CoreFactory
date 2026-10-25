package io.corefactory.paper.artillery;

/**
 * How many chunks the Set Target map may have in flight at once: the reads of explored chunks, which load a chunk only to read it, and the
 * generations of unexplored chunks, which are slow. A request starts only when its count has room; it is finished when its answer comes back,
 * or when it is cancelled. Pure, so the limits are tested without a server.
 */
final class TerrainBudget {

    private final int maxReads;
    private final int maxGenerations;
    private int reads;
    private int generations;

    /**
     * @param maxReads       the reads in flight at once, at least 1
     * @param maxGenerations the generations in flight at once, at least 1
     */
    TerrainBudget(int maxReads, int maxGenerations) {
        this.maxReads = maxReads;
        this.maxGenerations = maxGenerations;
    }

    /** Takes a read's place, when one is free. */
    boolean startRead() {
        if (reads >= maxReads) {
            return false;
        }
        reads++;
        return true;
    }

    /** Gives back a read's place. */
    void finishRead() {
        reads--;
    }

    /** Takes a generation's place, when one is free. */
    boolean startGeneration() {
        if (generations >= maxGenerations) {
            return false;
        }
        generations++;
        return true;
    }

    /** Gives back a generation's place. */
    void finishGeneration() {
        generations--;
    }

    /** The reads in flight. */
    int reads() {
        return reads;
    }

    /** The generations in flight. */
    int generations() {
        return generations;
    }
}
