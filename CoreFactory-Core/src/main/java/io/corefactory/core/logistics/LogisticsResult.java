package io.corefactory.core.logistics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * What one logistics tick did, keyed by node id (see {@link LogisticsNetwork#beltId} and its siblings).
 *
 * @param moved   the items each node moved out this tick: a belt the items it handed on, a picker the items it moved, a
 *                splitter the items it delivered
 * @param blocked the nodes that could not move something they held: a belt with an item waiting at its exit, a picker
 *                whose item its output refused, a splitter holding an item it cannot deliver
 */
public record LogisticsResult(Map<String, Integer> moved, Set<String> blocked) {

    public LogisticsResult {
        moved = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(moved, "moved")));
        blocked = Collections.unmodifiableSet(new TreeSet<>(Objects.requireNonNull(blocked, "blocked")));
    }

    /** The items a node moved this tick, zero for an unknown id. */
    public int movedBy(String nodeId) {
        return moved.getOrDefault(nodeId, 0);
    }

    /** Whether a node was blocked this tick. */
    public boolean isBlocked(String nodeId) {
        return blocked.contains(nodeId);
    }
}
