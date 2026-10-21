package io.corefactory.core.research;

import java.util.HashMap;
import java.util.Map;

/**
 * The rule for feeding packs into a node. A node takes packs up to its cost and no further, so surplus packs stay with
 * the player. Pure logic, so it can be tested without a server.
 */
public final class Progression {

    /**
     * The outcome of one feeding.
     *
     * @param consumed the packs taken from the offer, by id
     * @param done     the units fed in so far, by pack id, after this feeding
     * @param finished true when every part of the cost is covered
     */
    public record Result(Map<String, Integer> consumed, Map<String, Integer> done, boolean finished) {
    }

    private Progression() {
    }

    /**
     * Feeds the offered packs into a node.
     *
     * @param cost    the node's cost, by pack id
     * @param done    the units already fed in, by pack id
     * @param offered the packs available to feed, by pack id
     */
    public static Result apply(Map<String, Integer> cost, Map<String, Integer> done, Map<String, Integer> offered) {
        Map<String, Integer> consumed = new HashMap<>();
        Map<String, Integer> total = new HashMap<>(done);
        for (Map.Entry<String, Integer> part : cost.entrySet()) {
            String pack = part.getKey();
            int missing = part.getValue() - total.getOrDefault(pack, 0);
            if (missing <= 0) {
                continue;
            }
            int take = Math.min(missing, offered.getOrDefault(pack, 0));
            if (take > 0) {
                consumed.put(pack, take);
                total.merge(pack, take, Integer::sum);
            }
        }
        boolean finished = cost.entrySet().stream()
                .allMatch(part -> total.getOrDefault(part.getKey(), 0) >= part.getValue());
        return new Result(consumed, total, finished);
    }
}
