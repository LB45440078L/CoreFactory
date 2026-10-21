package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The layout of a set of belts, pickers and splitters: who feeds whom, the rules that keep the layout valid, the shape of
 * every belt and the order its nodes are updated in. It is built from plain configuration and never changes after that.
 *
 * <p>A node feeds a belt when its output cell is the belt's cell. A feeder from the belt's front is refused (head-on).
 * A belt takes at most one feeder. A picker is fed only by a belt at its input cell, and a splitter only by a belt at
 * its back cell. Pickers and splitters give to belts and ports only.
 */
final class Topology {

    /** The three kinds of node that have an output. */
    enum NodeKind {
        BELT,
        PICKER,
        SPLITTER
    }

    /** A node with its kind, its id and its block. */
    record Node(NodeKind kind, String id, Pos pos) {
    }

    /** A node that outputs into a cell, seen from that cell: the direction it is in and its kind. */
    record Feeder(Dir side, NodeKind kind) {
    }

    private final Map<Pos, BeltUnit> belts = new HashMap<>();
    private final Map<Pos, Picker> pickers = new HashMap<>();
    private final Map<Pos, Splitter> splitters = new HashMap<>();

    Topology(Collection<BeltUnit> belts, Collection<Picker> pickers, Collection<Splitter> splitters) {
        for (BeltUnit belt : belts) {
            claim(belt.pos());
            this.belts.put(belt.pos(), belt);
        }
        Set<String> pickerIds = new HashSet<>();
        for (Picker picker : pickers) {
            if (!pickerIds.add(picker.id())) {
                throw new IllegalArgumentException("duplicate picker id: " + picker.id());
            }
            claim(picker.pos());
            this.pickers.put(picker.pos(), picker);
        }
        Set<String> splitterIds = new HashSet<>();
        for (Splitter splitter : splitters) {
            if (!splitterIds.add(splitter.id())) {
                throw new IllegalArgumentException("duplicate splitter id: " + splitter.id());
            }
            claim(splitter.pos());
            this.splitters.put(splitter.pos(), splitter);
        }
    }

    private void claim(Pos pos) {
        if (occupied(pos)) {
            throw new IllegalArgumentException("cell is occupied: " + pos);
        }
    }

    boolean occupied(Pos pos) {
        return belts.containsKey(pos) || pickers.containsKey(pos) || splitters.containsKey(pos);
    }

    BeltUnit belt(Pos pos) {
        return belts.get(pos);
    }

    Picker picker(Pos pos) {
        return pickers.get(pos);
    }

    Splitter splitter(Pos pos) {
        return splitters.get(pos);
    }

    Collection<BeltUnit> belts() {
        return Collections.unmodifiableCollection(belts.values());
    }

    Collection<Picker> pickers() {
        return Collections.unmodifiableCollection(pickers.values());
    }

    Collection<Splitter> splitters() {
        return Collections.unmodifiableCollection(splitters.values());
    }

    /** The nodes that output into {@code cell}, each with the side of the cell it sits on. */
    List<Feeder> feedersOf(Pos cell) {
        List<Feeder> feeders = new ArrayList<>();
        for (Dir side : Dir.values()) {
            Pos neighbour = side.step(cell);
            BeltUnit belt = belts.get(neighbour);
            if (belt != null && belt.flow().step(neighbour).equals(cell)) {
                feeders.add(new Feeder(side, NodeKind.BELT));
            }
            Picker picker = pickers.get(neighbour);
            if (picker != null && picker.outputCell().equals(cell)) {
                feeders.add(new Feeder(side, NodeKind.PICKER));
            }
            Splitter splitter = splitters.get(neighbour);
            if (splitter != null && splitter.outputCells().contains(cell)) {
                feeders.add(new Feeder(side, NodeKind.SPLITTER));
            }
        }
        return feeders;
    }

    /** Checks every node against its feeders. MERGE is reported before BLOCKED. */
    PlacementReason validate() {
        for (BeltUnit belt : belts.values()) {
            if (feedersOf(belt.pos()).size() > 1) {
                return PlacementReason.MERGE;
            }
        }
        for (BeltUnit belt : belts.values()) {
            for (Feeder feeder : feedersOf(belt.pos())) {
                if (feeder.side() == belt.flow()) {
                    return PlacementReason.BLOCKED;
                }
            }
        }
        for (Picker picker : pickers.values()) {
            for (Feeder feeder : feedersOf(picker.pos())) {
                if (feeder.kind() != NodeKind.BELT || feeder.side() != picker.inDir()) {
                    return PlacementReason.BLOCKED;
                }
            }
        }
        for (Splitter splitter : splitters.values()) {
            for (Feeder feeder : feedersOf(splitter.pos())) {
                if (feeder.kind() != NodeKind.BELT || feeder.side() != splitter.inDir().opposite()) {
                    return PlacementReason.BLOCKED;
                }
            }
        }
        return PlacementReason.OK;
    }

    /** The side items enter a belt from: its feeder's side, or behind it when nothing feeds it. */
    Dir inputSideOf(BeltUnit belt) {
        List<Feeder> feeders = feedersOf(belt.pos());
        return feeders.isEmpty() ? belt.flow().opposite() : feeders.get(0).side();
    }

    /** STRAIGHT when the input is behind or absent, otherwise a corner turning from the input side into the flow. */
    BeltShape shapeOf(BeltUnit belt) {
        Dir input = inputSideOf(belt);
        if (input == belt.flow().opposite()) {
            return BeltShape.STRAIGHT;
        }
        Dir travel = input.opposite();
        return belt.flow() == travel.turnLeft() ? BeltShape.CORNER_LEFT : BeltShape.CORNER_RIGHT;
    }

    /** The node at a cell, or null when the cell holds no node. */
    Node nodeAt(Pos pos) {
        BeltUnit belt = belts.get(pos);
        if (belt != null) {
            return new Node(NodeKind.BELT, LogisticsNetwork.beltId(pos), pos);
        }
        Picker picker = pickers.get(pos);
        if (picker != null) {
            return new Node(NodeKind.PICKER, LogisticsNetwork.pickerId(picker.id()), pos);
        }
        Splitter splitter = splitters.get(pos);
        if (splitter != null) {
            return new Node(NodeKind.SPLITTER, LogisticsNetwork.splitterId(splitter.id()), pos);
        }
        return null;
    }

    /** Every node, in id order. */
    List<Node> nodes() {
        List<Node> nodes = new ArrayList<>();
        for (Pos pos : belts.keySet()) {
            nodes.add(nodeAt(pos));
        }
        for (Pos pos : pickers.keySet()) {
            nodes.add(nodeAt(pos));
        }
        for (Pos pos : splitters.keySet()) {
            nodes.add(nodeAt(pos));
        }
        nodes.sort(Comparator.comparing(Node::id));
        return nodes;
    }

    private List<Pos> outputsOf(Node node) {
        return switch (node.kind()) {
            case BELT -> List.of(belts.get(node.pos()).flow().step(node.pos()));
            case PICKER -> List.of(pickers.get(node.pos()).outputCell());
            case SPLITTER -> splitters.get(node.pos()).outputCells();
        };
    }

    /**
     * The order nodes are updated in: downstream first, so a full line fills from its end backwards. Nodes on a cycle
     * are placed by id, which keeps the order fixed.
     */
    List<Node> processingOrder() {
        List<Node> all = nodes();
        Map<String, Node> byId = new HashMap<>();
        Map<String, Set<String>> successors = new HashMap<>();
        Map<String, Integer> indegree = new HashMap<>();
        for (Node node : all) {
            byId.put(node.id(), node);
            indegree.put(node.id(), 0);
        }
        for (Node node : all) {
            Set<String> next = new TreeSet<>();
            for (Pos out : outputsOf(node)) {
                Node target = nodeAt(out);
                if (target != null) {
                    next.add(target.id());
                }
            }
            successors.put(node.id(), next);
            for (String id : next) {
                indegree.merge(id, 1, Integer::sum);
            }
        }
        TreeSet<String> ready = new TreeSet<>();
        for (Node node : all) {
            if (indegree.get(node.id()) == 0) {
                ready.add(node.id());
            }
        }
        Set<String> done = new HashSet<>();
        List<Node> upstreamFirst = new ArrayList<>();
        while (upstreamFirst.size() < all.size()) {
            if (ready.isEmpty()) {
                for (Node node : all) {
                    if (!done.contains(node.id())) {
                        ready.add(node.id());
                        break;
                    }
                }
            }
            String id = ready.pollFirst();
            if (!done.add(id)) {
                continue;
            }
            upstreamFirst.add(byId.get(id));
            for (String next : successors.get(id)) {
                if (!done.contains(next) && indegree.merge(next, -1, Integer::sum) == 0) {
                    ready.add(next);
                }
            }
        }
        Collections.reverse(upstreamFirst);
        return upstreamFirst;
    }
}
