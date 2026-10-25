package io.corefactory.paper.logistics;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * The satisfaction map a logistics tick reads: each node's satisfaction by node id. A node in a chunk that is not loaded gets 0,
 * so an item on an unloaded belt waits; a loaded node takes the energy service's satisfaction for its consumer id. Pure.
 */
public final class SatisfactionMap {

    /**
     * A node of a network as the map sees it.
     *
     * @param nodeId     the node's id in the logistics network
     * @param consumerId the id of the node's consumer in the energy service
     * @param loaded     true when the node's chunk is loaded
     */
    public record Node(String nodeId, String consumerId, boolean loaded) {
    }

    private SatisfactionMap() {
    }

    /**
     * Builds the map.
     *
     * @param nodes          the nodes of the networks this tick
     * @param satisfactionOf the energy satisfaction of a consumer id; a value outside 0 to 1 is clamped, NaN counts as 0
     * @return the satisfaction of every node, in the order of the nodes
     */
    public static Map<String, Double> build(Collection<Node> nodes, ToDoubleFunction<String> satisfactionOf) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (Node node : nodes) {
            double value = node.loaded() ? clamp(satisfactionOf.applyAsDouble(node.consumerId())) : 0.0;
            map.put(node.nodeId(), value);
        }
        return map;
    }

    /** A value from 0 to 1; NaN is 0. */
    public static double clamp(double value) {
        if (Double.isNaN(value) || value < 0.0) {
            return 0.0;
        }
        return Math.min(1.0, value);
    }
}
