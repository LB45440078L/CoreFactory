package io.corefactory.core.ledger;

import java.util.Map;
import java.util.Optional;

/**
 * The ore budget of each chunk: what was planned when the chunk was first populated, and what drills have mined since.
 * A chunk is planned once; its mined counts only grow. The Spigot module adapts the database to this interface. Calls block.
 */
public interface ChunkBudgets {

    /** True when the chunk has any budget row, which means it was populated and its ores must not be re-created. */
    boolean isBudgeted(String world, int chunkX, int chunkZ);

    /**
     * Writes the planned counts of a chunk, once. An ore that already has a planned count keeps it. Every ore id of the
     * ore list should be given, with 0 for an ore the chunk does not hold, so the chunk counts as budgeted.
     */
    void plant(String world, int chunkX, int chunkZ, Map<String, Long> planned);

    /** The budget of one ore in one chunk, or empty when the chunk has no row for it (a chunk laid before budgets). */
    Optional<OreBudget> find(String world, int chunkX, int chunkZ, String oreId);

    /**
     * Adds to the mined count of one ore in one chunk.
     *
     * @return true when the chunk has a row for the ore and it was updated
     */
    boolean recordMined(String world, int chunkX, int chunkZ, String oreId, long amount);
}
