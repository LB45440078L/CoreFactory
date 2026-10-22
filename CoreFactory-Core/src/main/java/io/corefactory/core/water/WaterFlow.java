package io.corefactory.core.water;

import io.corefactory.core.energy.Pos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shares each network's water among its consumers and works out what each pump delivers.
 *
 * <p>The supply of a network is the sum of the flows of its running pumps. Consumers are served by max-min fair sharing
 * (water filling): a consumer whose request is within the equal share gets all of it, and the rest of the supply is
 * shared equally among the larger requests. When the supply covers every request, each gets its full request. Each
 * pump's output is its flow's proportional share of what the network delivers, so it never exceeds its flow.
 */
public final class WaterFlow {

    private WaterFlow() {
    }

    /**
     * A pump's level, starting at 1, and whether it runs this tick. It runs when its source is above the minimum and its
     * fuel or power is there. A pump that does not run supplies nothing.
     *
     * @param level   the pump's level, at least 1
     * @param running true when the pump runs
     */
    public record PumpSupply(int level, boolean running) {

        public PumpSupply {
            if (level < 1) {
                throw new IllegalArgumentException("pump level starts at 1, got " + level);
            }
        }
    }

    /**
     * The outcome of one solve.
     *
     * @param consumerDelivered units per second delivered to each consumer in the grid, 0 when it has no request or no supply
     * @param pumpOutput        units per second each pump in the grid puts into its network, never more than its flow
     * @param networkSupply     units per second the running pumps supply, by network id
     * @param networkDelivered  units per second the network delivers to its consumers, by network id
     */
    public record Result(
            Map<Pos, Double> consumerDelivered,
            Map<Pos, Double> pumpOutput,
            List<Double> networkSupply,
            List<Double> networkDelivered) {

        public Result {
            consumerDelivered = Map.copyOf(consumerDelivered);
            pumpOutput = Map.copyOf(pumpOutput);
            networkSupply = List.copyOf(networkSupply);
            networkDelivered = List.copyOf(networkDelivered);
        }

        /** The units per second the network with this id delivers to its consumers. */
        public double deliveredIn(int networkId) {
            return networkDelivered.get(networkId);
        }
    }

    /**
     * Solves the flow of every network in the grid.
     *
     * @param flowByLevel units per second for each level; index 0 is level 1
     * @param pumps       each pump's level and running state, by position; a pump not listed does not run
     * @param requests    units per second each consumer asks for, by position; a consumer not listed asks for nothing
     * @return the delivered amounts and the pump outputs
     * @throws IllegalArgumentException when a key is not a pump or consumer of the grid, a level has no flow, or a
     *                                  request is negative or not finite
     */
    public static Result solve(WaterGrid grid, double[] flowByLevel,
                               Map<Pos, PumpSupply> pumps, Map<Pos, Double> requests) {
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(flowByLevel, "flow table");
        Objects.requireNonNull(pumps, "pumps");
        Objects.requireNonNull(requests, "requests");
        for (Map.Entry<Pos, PumpSupply> entry : pumps.entrySet()) {
            requireKind(grid, entry.getKey(), WaterKind.PUMP);
            if (entry.getValue().level() > flowByLevel.length) {
                throw new IllegalArgumentException("no flow for pump level " + entry.getValue().level());
            }
        }
        for (Map.Entry<Pos, Double> entry : requests.entrySet()) {
            requireKind(grid, entry.getKey(), WaterKind.CONSUMER);
            double units = entry.getValue();
            if (!(units >= 0) || Double.isInfinite(units)) {
                throw new IllegalArgumentException("request must be a finite amount, got " + units);
            }
        }

        Map<Pos, Double> consumerDelivered = new HashMap<>();
        Map<Pos, Double> pumpOutput = new HashMap<>();
        List<Double> supplies = new ArrayList<>();
        List<Double> delivered = new ArrayList<>();
        for (List<Pos> network : grid.networks()) {
            List<Pos> pumpsHere = new ArrayList<>();
            List<Pos> consumersHere = new ArrayList<>();
            for (Pos pos : network) {
                WaterKind kind = grid.node(pos).orElseThrow().kind();
                if (kind == WaterKind.PUMP) {
                    pumpsHere.add(pos);
                } else if (kind == WaterKind.CONSUMER) {
                    consumersHere.add(pos);
                }
            }

            double[] flows = new double[pumpsHere.size()];
            double supply = 0;
            for (int i = 0; i < flows.length; i++) {
                PumpSupply state = pumps.get(pumpsHere.get(i));
                if (state != null && state.running()) {
                    flows[i] = flowByLevel[state.level() - 1];
                    supply += flows[i];
                }
            }

            double[] requested = new double[consumersHere.size()];
            for (int i = 0; i < requested.length; i++) {
                requested[i] = requests.getOrDefault(consumersHere.get(i), 0.0);
            }
            double[] given = shareWater(supply, requested);

            double total = 0;
            for (int i = 0; i < given.length; i++) {
                consumerDelivered.put(consumersHere.get(i), given[i]);
                total += given[i];
            }
            for (int i = 0; i < flows.length; i++) {
                pumpOutput.put(pumpsHere.get(i), supply > 0 ? total * flows[i] / supply : 0.0);
            }
            supplies.add(supply);
            delivered.add(total);
        }
        return new Result(consumerDelivered, pumpOutput, supplies, delivered);
    }

    /**
     * Max-min fair sharing of a supply. Returns what each request gets, in the same order as the requests.
     */
    static double[] shareWater(double supply, double[] requested) {
        double total = 0;
        for (double request : requested) {
            total += request;
        }
        if (total <= supply) {
            return requested.clone();
        }
        Integer[] order = new Integer[requested.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingDouble((Integer i) -> requested[i]));

        double[] given = new double[requested.length];
        double remaining = supply;
        int left = requested.length;
        for (int k = 0; k < order.length; k++) {
            int i = order[k];
            double share = remaining / left;
            if (requested[i] <= share) {
                given[i] = requested[i];
                remaining -= requested[i];
                left--;
            } else {
                for (int m = k; m < order.length; m++) {
                    given[order[m]] = share;
                }
                break;
            }
        }
        return given;
    }

    private static void requireKind(WaterGrid grid, Pos pos, WaterKind kind) {
        WaterNode node = grid.node(pos)
                .orElseThrow(() -> new IllegalArgumentException("no water block at " + pos));
        if (node.kind() != kind) {
            throw new IllegalArgumentException(pos + " is " + node.kind() + ", not " + kind);
        }
    }

}
