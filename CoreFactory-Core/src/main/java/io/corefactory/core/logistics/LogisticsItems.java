package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Item counts for conservation checks. */
public final class LogisticsItems {

    private LogisticsItems() {
    }

    /**
     * Every item in the network (on belts and in splitters) and in the given ports. Each port is counted once, even when
     * it is keyed by several positions. Between ticks the total changes only when the caller moves items in or out of
     * the set of ports.
     */
    public static int totalItems(LogisticsNetwork network, Map<Pos, Port> ports) {
        Set<Port> distinct = Collections.newSetFromMap(new IdentityHashMap<>());
        distinct.addAll(ports.values());
        int total = network.itemCount();
        for (Port port : distinct) {
            total += port.storedCount();
        }
        return total;
    }
}
