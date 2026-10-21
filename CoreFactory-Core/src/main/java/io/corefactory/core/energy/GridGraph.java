package io.corefactory.core.energy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Finds the grids of a snapshot: the connected components of pylons and generators linked by valid wires. A generator that
 * is wired to no pylon belongs to no grid. Nothing is kept between calls: the components are computed from the snapshot
 * each time.
 */
public final class GridGraph {

    private GridGraph() {
    }

    /**
     * One connected group of pylons and generators, before its id is known.
     *
     * @param pylonIds   the ids of its pylons, in id order; never empty
     * @param generatorIds the ids of its generators, in id order
     * @param wireBlocks the longest wire path from one of its generators to a pylon, in blocks; 0 without generators
     */
    public record Component(List<String> pylonIds, List<String> generatorIds, double wireBlocks) {

        public Component {
            pylonIds = List.copyOf(pylonIds);
            generatorIds = List.copyOf(generatorIds);
        }
    }

    private record Node(DeviceKind kind, String id) {
    }

    private record Link(Node to, double lengthBlocks) {
    }

    /**
     * The components of a snapshot, ordered by their smallest pylon id.
     *
     * @param snapshot the devices and wires of one tick
     */
    public static List<Component> components(EnergySnapshot snapshot) {
        Map<String, Pylon> pylons = new TreeMap<>();
        for (Pylon pylon : snapshot.pylons()) {
            pylons.put(pylon.id(), pylon);
        }
        Map<String, GeneratorOutput> generators = new TreeMap<>();
        for (GeneratorOutput generator : snapshot.generators()) {
            generators.put(generator.id(), generator);
        }
        Map<Node, Pos> positions = new HashMap<>();
        for (Pylon pylon : pylons.values()) {
            positions.put(new Node(DeviceKind.PYLON, pylon.id()), pylon.pos());
        }
        for (GeneratorOutput generator : generators.values()) {
            positions.put(new Node(DeviceKind.GENERATOR, generator.id()), generator.pos());
        }
        Map<Node, List<Link>> links = links(snapshot, positions);

        Set<Node> seen = new HashSet<>();
        List<Component> components = new ArrayList<>();
        for (String pylonId : pylons.keySet()) {
            Node start = new Node(DeviceKind.PYLON, pylonId);
            if (!seen.add(start)) {
                continue;
            }
            List<String> pylonIds = new ArrayList<>();
            List<String> generatorIds = new ArrayList<>();
            Deque<Node> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                Node current = queue.poll();
                if (current.kind() == DeviceKind.PYLON) {
                    pylonIds.add(current.id());
                } else {
                    generatorIds.add(current.id());
                }
                for (Link link : links.getOrDefault(current, List.of())) {
                    if (seen.add(link.to())) {
                        queue.add(link.to());
                    }
                }
            }
            pylonIds.sort(null);
            generatorIds.sort(null);
            components.add(new Component(pylonIds, generatorIds, wireBlocks(generatorIds, links)));
        }
        return List.copyOf(components);
    }

    private static Map<Node, List<Link>> links(EnergySnapshot snapshot, Map<Node, Pos> positions) {
        Map<Node, List<Link>> links = new HashMap<>();
        for (Wire wire : snapshot.wires()) {
            Node from = nodeOf(wire.from(), positions);
            Node to = nodeOf(wire.to(), positions);
            if (from == null || to == null) {
                continue;
            }
            Endpoint fromEnd = new Endpoint(from.id(), from.kind(), positions.get(from));
            Endpoint toEnd = new Endpoint(to.id(), to.kind(), positions.get(to));
            if (WireRules.validate(fromEnd, toEnd) != WireCheck.OK) {
                continue;
            }
            double length = positions.get(from).horizontalDistance(positions.get(to));
            links.computeIfAbsent(from, k -> new ArrayList<>()).add(new Link(to, length));
            links.computeIfAbsent(to, k -> new ArrayList<>()).add(new Link(from, length));
        }
        return links;
    }

    private static Node nodeOf(String id, Map<Node, Pos> positions) {
        Node pylon = new Node(DeviceKind.PYLON, id);
        if (positions.containsKey(pylon)) {
            return pylon;
        }
        Node generator = new Node(DeviceKind.GENERATOR, id);
        return positions.containsKey(generator) ? generator : null;
    }

    private static double wireBlocks(List<String> generatorIds, Map<Node, List<Link>> links) {
        double longest = 0.0;
        for (String generatorId : generatorIds) {
            double nearest = Double.POSITIVE_INFINITY;
            for (Link link : links.getOrDefault(new Node(DeviceKind.GENERATOR, generatorId), List.of())) {
                nearest = Math.min(nearest, link.lengthBlocks());
            }
            if (Double.isFinite(nearest)) {
                longest = Math.max(longest, nearest);
            }
        }
        return longest;
    }
}
