package io.corefactory.core.ledger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Chunk budgets held in memory. The Spigot module uses it when the database cannot open; the budgets are lost at the next
 * start, so a chunk laid during such a start can be laid again by a forced retrofit.
 */
public final class MemoryChunkBudgets implements ChunkBudgets {

    private final Set<String> budgeted = new HashSet<>();
    private final Map<String, OreBudget> budgets = new HashMap<>();

    @Override
    public synchronized boolean isBudgeted(String world, int chunkX, int chunkZ) {
        return budgeted.contains(chunkKey(world, chunkX, chunkZ));
    }

    @Override
    public synchronized void plant(String world, int chunkX, int chunkZ, Map<String, Long> planned) {
        String chunk = chunkKey(world, chunkX, chunkZ);
        budgeted.add(chunk);
        for (Map.Entry<String, Long> entry : planned.entrySet()) {
            budgets.putIfAbsent(oreKey(chunk, entry.getKey()), new OreBudget(entry.getValue(), 0));
        }
    }

    @Override
    public synchronized Optional<OreBudget> find(String world, int chunkX, int chunkZ, String oreId) {
        return Optional.ofNullable(budgets.get(oreKey(chunkKey(world, chunkX, chunkZ), oreId)));
    }

    @Override
    public synchronized boolean recordMined(String world, int chunkX, int chunkZ, String oreId, long amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be at least 1, not " + amount);
        }
        String key = oreKey(chunkKey(world, chunkX, chunkZ), oreId);
        OreBudget current = budgets.get(key);
        if (current == null) {
            return false;
        }
        budgets.put(key, current.withMined(amount));
        return true;
    }

    private static String chunkKey(String world, int chunkX, int chunkZ) {
        return world + ":" + chunkX + ":" + chunkZ;
    }

    private static String oreKey(String chunk, String oreId) {
        return chunk + ":" + oreId;
    }
}
