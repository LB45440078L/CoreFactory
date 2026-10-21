package io.corefactory.core.ledger;

import java.util.Map;

/**
 * What each drill has mined, per ore id. The units are blocks, or pumped units for a deposit. The Spigot module adapts the
 * database to this interface, so the core has no database types. Calls block, so they are made from the main thread only
 * when they are needed, and never per tick.
 */
public interface MiningLedger {

    /**
     * Adds to the count a drill holds for one ore.
     *
     * @param drillKey the drill, as {@code world:x:y:z}
     * @param oreId    the ore id
     * @param amount   how many to add, at least 1
     */
    void add(String drillKey, String oreId, long amount);

    /** How many of the ore the drill has mined, 0 when none. */
    long mined(String drillKey, String oreId);

    /** The mined count per ore id; empty when the drill has mined nothing. */
    Map<String, Long> minedBy(String drillKey);
}
