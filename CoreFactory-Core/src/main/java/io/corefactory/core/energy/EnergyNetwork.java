package io.corefactory.core.energy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The energy solver. One call takes a snapshot for one tick and returns what every grid, consumer and accumulator did, and
 * the grid ids and accumulator states for the next tick. It is pure: the same snapshot and dt always give the same result.
 *
 * <p>Power is in W and energy in Wh. Within one tick the passes run in this order:
 * <ol>
 *   <li>The grids are the connected components of the wires. A grid's deliverable power is the lesser of its supply and its
 *       capacity.</li>
 *   <li>Each consumer draws from each grid that covers it once: the draw is in proportion to each grid's free deliverable power,
 *       and the consumer takes at most its demand in total. Critical consumers draw first, the water pumps that feed nuclear
 *       generators among them; the rest draw in id order, so an earlier consumer can take a short grid's power before a later one.</li>
 *   <li>Each consumer still short takes the shortfall from the accumulators that share a covering grid with it, in id order,
 *       each at most its power limit for the tick and its stored energy.</li>
 *   <li>Each accumulator that did not discharge this tick charges from the grids covering it, in grid id order, from the
 *       free power left after the draws. It takes at most its power limit and its free space, and stores 90 % of what it takes.</li>
 *   <li>Activation is set on each accumulator that discharges now and did not discharge in the previous tick.</li>
 * </ol>
 */
public final class EnergyNetwork {

    private static final double EPS = 1e-9;

    private EnergyNetwork() {
    }

    /**
     * Solves one tick.
     *
     * @param snapshot the devices, wires, states and grid ids of the tick
     * @param dt       the length of the tick in seconds, positive; one server tick is {@link EnergyTable#SECONDS_PER_TICK}
     * @return the result of the tick
     */
    public static EnergyResult solve(EnergySnapshot snapshot, double dt) {
        if (!(dt > 0) || !Double.isFinite(dt)) {
            throw new IllegalArgumentException("dt must be positive and finite: " + dt);
        }
        List<GridGraph.Component> components = GridGraph.components(snapshot);
        List<Set<String>> pylonSets = components.stream().map(component -> Set.copyOf(component.pylonIds())).toList();
        GridRegistry registry = snapshot.grids().assign(pylonSets);
        List<Grid> grids = grids(snapshot, components, registry);

        int gridCount = grids.size();
        Map<String, Integer> gridOfPylon = new HashMap<>();
        for (int k = 0; k < gridCount; k++) {
            for (String pylonId : grids.get(k).pylonIds()) {
                gridOfPylon.put(pylonId, k);
            }
        }
        double[] free = new double[gridCount];
        double[] drawn = new double[gridCount];
        double[] storageShare = new double[gridCount];
        double[] loadShare = new double[gridCount];
        for (int k = 0; k < gridCount; k++) {
            free[k] = grids.get(k).deliverableW();
        }

        List<Consumer> consumers = snapshot.consumers();
        int consumerCount = consumers.size();
        int[][] consumerGrids = new int[consumerCount][];
        double[][] consumerDraws = new double[consumerCount][];
        double[] fromGrids = new double[consumerCount];
        double[] fromStorage = new double[consumerCount];
        for (int j = 0; j < consumerCount; j++) {
            Consumer consumer = consumers.get(j);
            consumerGrids[j] = coveringGrids(snapshot.pylons(), gridOfPylon, consumer.pos());
            consumerDraws[j] = new double[consumerGrids[j].length];
            if (consumerGrids[j].length > 0) {
                double share = consumer.demandW() / consumerGrids[j].length;
                for (int k : consumerGrids[j]) {
                    loadShare[k] += share;
                }
            }
        }
        for (int j : drawOrder(consumers)) {
            fromGrids[j] = drawFromGrids(consumers.get(j).demandW(), consumerGrids[j], free, drawn, consumerDraws[j]);
        }

        List<Accumulator> accumulators = snapshot.accumulators();
        int accumulatorCount = accumulators.size();
        int[][] accumulatorGrids = new int[accumulatorCount][];
        double[] stored = new double[accumulatorCount];
        double[] dischargeUsedW = new double[accumulatorCount];
        double[] discharged = new double[accumulatorCount];
        double[] charged = new double[accumulatorCount];
        for (int a = 0; a < accumulatorCount; a++) {
            Accumulator accumulator = accumulators.get(a);
            accumulatorGrids[a] = coveringGrids(snapshot.pylons(), gridOfPylon, accumulator.pos());
            stored[a] = Math.min(accumulator.level().capacityWh(), snapshot.accumulatorStates().get(a).storedWh());
        }

        for (int j = 0; j < consumerCount; j++) {
            double shortfall = consumers.get(j).demandW() - fromGrids[j];
            if (shortfall <= EPS || consumerGrids[j].length == 0) {
                continue;
            }
            for (int a = 0; a < accumulatorCount && shortfall > EPS; a++) {
                if (!intersects(accumulatorGrids[a], consumerGrids[j])) {
                    continue;
                }
                double room = Math.min(shortfall, accumulators.get(a).level().powerW() - dischargeUsedW[a]);
                if (room <= EPS) {
                    continue;
                }
                double energy = Math.min(Units.wattHours(room, dt), stored[a]);
                if (energy <= 0) {
                    continue;
                }
                double rate = Units.watts(energy, dt);
                stored[a] = Math.max(0.0, stored[a] - energy);
                discharged[a] += energy;
                dischargeUsedW[a] += rate;
                fromStorage[j] += rate;
                shortfall -= rate;
            }
            if (fromStorage[j] > 0) {
                double share = fromStorage[j] / consumerGrids[j].length;
                for (int k : consumerGrids[j]) {
                    storageShare[k] += share;
                }
            }
        }

        for (int a = 0; a < accumulatorCount; a++) {
            if (discharged[a] > 0 || accumulatorGrids[a].length == 0) {
                continue;
            }
            AccumulatorLevel level = accumulators.get(a).level();
            double budget = Math.min(Units.wattHours(level.powerW(), dt),
                    (level.capacityWh() - stored[a]) / EnergyTable.ACCUMULATOR_ROUND_TRIP);
            for (int k : accumulatorGrids[a]) {
                if (budget <= EPS) {
                    break;
                }
                double take = Math.min(Units.wattHours(free[k], dt), budget);
                if (take <= 0) {
                    continue;
                }
                free[k] = Math.max(0.0, free[k] - Units.watts(take, dt));
                charged[a] += take;
                budget -= take;
            }
            stored[a] = Math.min(level.capacityWh(), stored[a] + charged[a] * EnergyTable.ACCUMULATOR_ROUND_TRIP);
        }

        List<GridResult> gridResults = new ArrayList<>(gridCount);
        double[] voltage = new double[gridCount];
        for (int k = 0; k < gridCount; k++) {
            Grid grid = grids.get(k);
            double delivered = drawn[k];
            double load = loadShare[k];
            double satisfaction = load <= EPS ? 1.0 : Math.min(1.0, (delivered + storageShare[k]) / load);
            voltage[k] = Units.voltageV(delivered, grid.resistanceOhm());
            gridResults.add(new GridResult(grid, delivered, storageShare[k], load, satisfaction, voltage[k],
                    Units.currentA(delivered, voltage[k])));
        }

        List<ConsumerResult> consumerResults = new ArrayList<>(consumerCount);
        for (int j = 0; j < consumerCount; j++) {
            Consumer consumer = consumers.get(j);
            double demand = consumer.demandW();
            double absorbed = Math.min(demand, fromGrids[j] + fromStorage[j]);
            double satisfaction = demand <= 0 ? 1.0 : Math.min(1.0, absorbed / demand);
            consumerResults.add(new ConsumerResult(consumer.id(), demand, absorbed, satisfaction,
                    voltageOf(consumerGrids[j], consumerDraws[j], voltage), fromGrids[j], fromStorage[j]));
        }

        List<AccumulatorResult> accumulatorResults = new ArrayList<>(accumulatorCount);
        for (int a = 0; a < accumulatorCount; a++) {
            Accumulator accumulator = accumulators.get(a);
            boolean discharging = discharged[a] > 0;
            boolean wasDischarging = snapshot.accumulatorStates().get(a).discharging();
            AccumulatorState next = new AccumulatorState(accumulator.id(), Math.max(0.0, stored[a]), discharging);
            accumulatorResults.add(new AccumulatorResult(accumulator.id(), charged[a], discharged[a],
                    discharging && !wasDischarging, next));
        }

        return new EnergyResult(gridResults, consumerResults, accumulatorResults, registry);
    }

    private static List<Grid> grids(EnergySnapshot snapshot, List<GridGraph.Component> components, GridRegistry registry) {
        Map<String, GeneratorOutput> generators = new HashMap<>();
        for (GeneratorOutput generator : snapshot.generators()) {
            generators.put(generator.id(), generator);
        }
        Map<String, Pylon> pylons = new HashMap<>();
        for (Pylon pylon : snapshot.pylons()) {
            pylons.put(pylon.id(), pylon);
        }
        List<Grid> grids = new ArrayList<>(components.size());
        for (GridGraph.Component component : components) {
            int id = registry.idOf(component.pylonIds().get(0));
            double supply = 0.0;
            for (String generatorId : component.generatorIds()) {
                supply += generators.get(generatorId).outputW();
            }
            double capacity = 0.0;
            for (String pylonId : component.pylonIds()) {
                capacity += pylons.get(pylonId).capacityW();
            }
            grids.add(new Grid(id, GridPalette.colour(id), component.pylonIds(), component.generatorIds(), supply,
                    capacity, Units.resistanceOhm(component.wireBlocks())));
        }
        grids.sort(Comparator.comparingInt(Grid::id));
        return grids;
    }

    /** The order the consumers draw in: the critical ones first, then the rest, each group in id order. */
    private static List<Integer> drawOrder(List<Consumer> consumers) {
        List<Integer> order = new ArrayList<>(consumers.size());
        for (int j = 0; j < consumers.size(); j++) {
            if (consumers.get(j).critical()) {
                order.add(j);
            }
        }
        for (int j = 0; j < consumers.size(); j++) {
            if (!consumers.get(j).critical()) {
                order.add(j);
            }
        }
        return order;
    }

    private static double drawFromGrids(double demand, int[] covering, double[] free, double[] drawn, double[] draws) {
        if (demand <= 0 || covering.length == 0) {
            return 0.0;
        }
        double freeTotal = 0.0;
        for (int k : covering) {
            freeTotal += free[k];
        }
        if (freeTotal <= 0) {
            return 0.0;
        }
        double fraction = Math.min(1.0, demand / freeTotal);
        double total = 0.0;
        for (int i = 0; i < covering.length; i++) {
            int k = covering[i];
            double take = free[k] * fraction;
            free[k] = Math.max(0.0, free[k] - take);
            drawn[k] += take;
            draws[i] = take;
            total += take;
        }
        return Math.min(demand, total);
    }

    private static double voltageOf(int[] covering, double[] draws, double[] voltage) {
        if (covering.length == 0) {
            return 0.0;
        }
        int best = 0;
        for (int i = 1; i < covering.length; i++) {
            if (draws[i] > draws[best]) {
                best = i;
            }
        }
        return voltage[covering[best]];
    }

    private static int[] coveringGrids(List<Pylon> pylons, Map<String, Integer> gridOfPylon, Pos pos) {
        TreeSet<Integer> found = new TreeSet<>();
        for (Pylon pylon : pylons) {
            if (pylon.covers(pos)) {
                found.add(gridOfPylon.get(pylon.id()));
            }
        }
        return found.stream().mapToInt(Integer::intValue).toArray();
    }

    private static boolean intersects(int[] a, int[] b) {
        for (int x : a) {
            for (int y : b) {
                if (x == y) {
                    return true;
                }
            }
        }
        return false;
    }
}
