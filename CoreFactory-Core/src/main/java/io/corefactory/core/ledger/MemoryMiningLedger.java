package io.corefactory.core.ledger;

import java.util.HashMap;
import java.util.Map;

/**
 * A mining ledger held in memory. The Spigot module uses it when the database cannot open, so drills still mine for the
 * rest of the start; the counts are lost at the next start.
 */
public final class MemoryMiningLedger implements MiningLedger {

    private final Map<String, Map<String, Long>> drills = new HashMap<>();

    @Override
    public synchronized void add(String drillKey, String oreId, long amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be at least 1, not " + amount);
        }
        drills.computeIfAbsent(drillKey, key -> new HashMap<>()).merge(oreId, amount, Long::sum);
    }

    @Override
    public synchronized long mined(String drillKey, String oreId) {
        return drills.getOrDefault(drillKey, Map.of()).getOrDefault(oreId, 0L);
    }

    @Override
    public synchronized Map<String, Long> minedBy(String drillKey) {
        return Map.copyOf(drills.getOrDefault(drillKey, Map.of()));
    }
}
