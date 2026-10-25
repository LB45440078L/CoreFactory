package io.corefactory.core.research;

import java.util.Set;
import java.util.UUID;

/**
 * Where research progress is kept permanently. The Paper module adapts the database to this interface, so the core
 * has no database types. Calls block, so the research service only makes them off the main thread.
 */
public interface ResearchStore {

    /** The ids of the nodes a player has finished. */
    Set<String> unlocked(UUID player);

    /** Records a finished node. Returns true only when it was not finished before. */
    boolean unlock(UUID player, String node, long epochMillis);

    /**
     * How many units of a node's cost a player has fed in so far. The key names the node and pack, as
     * {@code node/pack}, so one number is kept per pack.
     */
    int progress(UUID player, String node);

    /** Stores how many units of a node's cost a player has fed in. */
    void setProgress(UUID player, String node, int units);
}
