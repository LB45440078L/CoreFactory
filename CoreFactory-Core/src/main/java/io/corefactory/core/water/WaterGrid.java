package io.corefactory.core.water;

import io.corefactory.core.energy.Pos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The water blocks of one world, joined into networks. Two blocks are joined when {@link WaterRules#connects} holds on
 * the face they share. A network is a connected component, found by a breadth-first walk. Network ids are the
 * components' indices in {@link #networks()}, and components are ordered by their lowest position.
 */
public final class WaterGrid {

    private static final Comparator<Pos> ORDER =
            Comparator.comparingInt(Pos::x).thenComparingInt(Pos::y).thenComparingInt(Pos::z);

    private final Map<Pos, WaterNode> nodes;
    private final Map<Pos, Set<Face>> connected;
    private final Map<Pos, Integer> networkIds;
    private final List<List<Pos>> networks;

    private WaterGrid(Map<Pos, WaterNode> byPos) {
        nodes = Map.copyOf(byPos);
        Map<Pos, Set<Face>> joined = new HashMap<>();
        for (WaterNode node : nodes.values()) {
            Set<Face> faces = EnumSet.noneOf(Face.class);
            for (Face face : Face.values()) {
                WaterNode neighbour = nodes.get(face.step(node.pos()));
                if (neighbour != null && WaterRules.connects(node, face, neighbour)) {
                    faces.add(face);
                }
            }
            joined.put(node.pos(), Collections.unmodifiableSet(faces));
        }
        connected = Map.copyOf(joined);

        Map<Pos, Integer> ids = new HashMap<>();
        List<List<Pos>> found = new ArrayList<>();
        for (Pos start : nodes.keySet().stream().sorted(ORDER).toList()) {
            if (ids.containsKey(start)) {
                continue;
            }
            int id = found.size();
            List<Pos> component = new ArrayList<>();
            Deque<Pos> queue = new ArrayDeque<>();
            ids.put(start, id);
            queue.add(start);
            while (!queue.isEmpty()) {
                Pos current = queue.poll();
                component.add(current);
                for (Face face : connected.get(current)) {
                    Pos next = face.step(current);
                    if (ids.putIfAbsent(next, id) == null) {
                        queue.add(next);
                    }
                }
            }
            component.sort(ORDER);
            found.add(List.copyOf(component));
        }
        networks = List.copyOf(found);
        networkIds = Map.copyOf(ids);
    }

    /**
     * Builds the grid.
     *
     * @param blocks the water blocks; at most one per position
     * @throws IllegalArgumentException when two blocks share a position
     */
    public static WaterGrid of(Collection<WaterNode> blocks) {
        Objects.requireNonNull(blocks, "blocks");
        Map<Pos, WaterNode> byPos = new HashMap<>();
        for (WaterNode node : blocks) {
            if (byPos.putIfAbsent(node.pos(), node) != null) {
                throw new IllegalArgumentException("two water blocks at " + node.pos());
            }
        }
        return new WaterGrid(byPos);
    }

    /** The block at a position, if there is one. */
    public Optional<WaterNode> node(Pos pos) {
        return Optional.ofNullable(nodes.get(pos));
    }

    /** The faces of the block at {@code pos} that join a neighbour. Empty when there is no block there. */
    public Set<Face> connectedFaces(Pos pos) {
        return connected.getOrDefault(pos, Set.of());
    }

    /** The id of the network the block at {@code pos} belongs to, or -1 when there is no block there. */
    public int networkOf(Pos pos) {
        return networkIds.getOrDefault(pos, -1);
    }

    /** The networks, by id. Each lists its positions in position order. */
    public List<List<Pos>> networks() {
        return networks;
    }

    /** True when a pipe joins the block at {@code pos} on any face. */
    public boolean pipeAttached(Pos pos) {
        for (Face face : connectedFaces(pos)) {
            WaterNode neighbour = nodes.get(face.step(pos));
            if (neighbour != null && neighbour.kind() == WaterKind.PIPE) {
                return true;
            }
        }
        return false;
    }
}
