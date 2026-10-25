package io.corefactory.core.oil;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.WaterGrid;
import io.corefactory.core.water.WaterNode;
import io.corefactory.core.water.WaterRules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The join decision of the oil network: which network each tube belongs to, and which networks each device joins, and in what role. The
 * tubes are joined the way water pipes are, through {@link WaterGrid}. A device that merges its networks, such as a drill, a generator or a
 * pump, is a node of that grid: the tubes it touches become one network. A refinery is separate: it is not a node, so each tube beside it
 * stays in its own network, and it joins each network in turn as an intake (it takes petroleum) or as an output (it gives diesel).
 *
 * <p>A network is an intake of a refinery when it holds petroleum, from its tanks or from a petroleum drill joined to it. It is an output
 * when it holds diesel, or when it is empty and a diesel consumer is joined to it; an empty network with no consumer is neither, so a tube
 * that a petroleum drill will later stand on stays empty and is not made diesel. A network is never both: an intake is never an output.
 * Pure: the caller reads the tubes, their tanks and the devices, and draws the joins.
 */
public final class OilJoinPlan {

    /**
     * A tube as the plan reads it.
     *
     * @param pos   the block the tube stands in
     * @param node  the water node of the tube: a pipe, or a splitter with its faces
     * @param fluid the fluid its tank holds, or null when the tank is empty
     * @throws IllegalArgumentException when the node stands in another block
     */
    public record Tube(Pos pos, WaterNode node, Fluid fluid) {

        public Tube {
            Objects.requireNonNull(pos, "pos");
            Objects.requireNonNull(node, "node");
            if (!node.pos().equals(pos)) {
                throw new IllegalArgumentException("the node of a tube at " + pos + " stands at " + node.pos());
            }
        }
    }

    /**
     * A device that touches the oil network.
     *
     * @param anchor   the block the device's anchor stands in
     * @param height   the device's footprint height: the anchor and the blocks above it
     * @param gives    the fluid the device puts into the networks it joins, or null when it gives none
     * @param takes    the fluid the device takes from the networks it joins, or null when it takes none
     * @param separate true when the device keeps the networks it touches apart, joining each in turn (a refinery); false when it merges
     *                 the networks it touches into one (a drill, a generator, a pump)
     * @throws IllegalArgumentException when the height is below 1, or a separate device does not give and take a fluid
     */
    public record Device(Pos anchor, int height, Fluid gives, Fluid takes, boolean separate) {

        public Device {
            Objects.requireNonNull(anchor, "anchor");
            if (height < 1) {
                throw new IllegalArgumentException("a device is at least one block tall, got " + height);
            }
            if (separate && (gives == null || takes == null)) {
                throw new IllegalArgumentException("a separate device gives and takes a fluid");
            }
        }

        /** The blocks the device fills, from the anchor upward. */
        public List<Pos> footprint() {
            List<Pos> blocks = new ArrayList<>(height);
            for (int dy = 0; dy < height; dy++) {
                blocks.add(new Pos(anchor.x(), anchor.y() + dy, anchor.z()));
            }
            return blocks;
        }
    }

    /** What a join does in its network: a tube is part of it, a source gives a fluid into it, a sink takes oil from it. */
    public enum Kind {
        /** The block is a tube of the network. */
        TUBE,
        /** The device gives its fluid into the network. */
        SOURCE,
        /** The device takes oil from the network. */
        SINK
    }

    /**
     * A device's join to a network, or a tube's join to its own network.
     *
     * @param network the id of the network
     * @param kind    what the join does
     * @param gives   the fluid a source gives, or null for a tube or a sink
     */
    public record Join(int network, Kind kind, Fluid gives) {
    }

    /**
     * One network: its tubes in position order, and the fluids it holds or is given, which together make at most one kind when the
     * network is allowed.
     *
     * @param id     the id of the network
     * @param tubes  the positions of its tubes, in position order
     * @param fluids the fluids of its tanks and of its sources, including the diesel a refinery output gives
     */
    public record Network(int id, List<Pos> tubes, Set<Fluid> fluids) {

        /** True when the network holds at most one fluid kind. A network that holds two moves nothing. */
        public boolean allowed() {
            return FluidRules.allowed(fluids);
        }
    }

    private final List<Network> networks;
    private final Map<Pos, Integer> networkOfTube;
    private final Map<Pos, List<Join>> joins;

    private OilJoinPlan(List<Network> networks, Map<Pos, Integer> networkOfTube, Map<Pos, List<Join>> joins) {
        this.networks = networks;
        this.networkOfTube = networkOfTube;
        this.joins = joins;
    }

    /**
     * Builds the plan of one world.
     *
     * @param tubes   the tubes of the world, one per block
     * @param devices the devices beside the tubes, one per anchor
     * @return the plan
     * @throws IllegalArgumentException when two tubes stand in one block
     */
    public static OilJoinPlan of(Collection<Tube> tubes, Collection<Device> devices) {
        Map<Pos, Tube> tubeAt = new HashMap<>();
        List<WaterNode> nodes = new ArrayList<>();
        for (Tube tube : tubes) {
            if (tubeAt.putIfAbsent(tube.pos(), tube) != null) {
                throw new IllegalArgumentException("two oil tubes at " + tube.pos());
            }
            nodes.add(tube.node());
        }
        Set<Pos> taken = new HashSet<>(tubeAt.keySet());
        for (Device device : devices) {
            if (device.separate()) {
                continue;
            }
            for (Pos block : device.footprint()) {
                if (taken.add(block)) {
                    nodes.add(WaterNode.consumer(block, EnumSet.allOf(Face.class)));
                }
            }
        }
        WaterGrid grid = WaterGrid.of(nodes);

        List<List<Pos>> members = new ArrayList<>();
        Map<Pos, Integer> networkOfTube = new HashMap<>();
        for (List<Pos> component : grid.networks()) {
            List<Pos> inNetwork = component.stream().filter(tubeAt::containsKey).toList();
            if (inNetwork.isEmpty()) {
                continue;
            }
            int id = members.size();
            members.add(inNetwork);
            for (Pos pos : inNetwork) {
                networkOfTube.put(pos, id);
            }
        }

        List<EnumSet<Fluid>> fluids = new ArrayList<>(members.size());
        for (List<Pos> network : members) {
            EnumSet<Fluid> held = EnumSet.noneOf(Fluid.class);
            for (Pos pos : network) {
                if (tubeAt.get(pos).fluid() != null) {
                    held.add(tubeAt.get(pos).fluid());
                }
            }
            fluids.add(held);
        }

        Map<Pos, Set<Join>> joined = new HashMap<>();
        for (Map.Entry<Pos, Integer> tube : networkOfTube.entrySet()) {
            joined.computeIfAbsent(tube.getKey(), key -> new LinkedHashSet<>())
                    .add(new Join(tube.getValue(), Kind.TUBE, null));
        }

        Map<Fluid, Set<Integer>> takenBy = new HashMap<>();
        for (Device device : devices) {
            if (device.separate()) {
                continue;
            }
            Set<Join> own = new LinkedHashSet<>();
            for (Pos block : device.footprint()) {
                for (Face face : Face.values()) {
                    Integer network = networkOfTube.get(face.step(block));
                    if (network == null || !grid.connectedFaces(block).contains(face)) {
                        continue;
                    }
                    if (device.gives() != null) {
                        own.add(new Join(network, Kind.SOURCE, device.gives()));
                        fluids.get(network).add(device.gives());
                    } else {
                        own.add(new Join(network, Kind.SINK, null));
                    }
                    if (device.takes() != null) {
                        takenBy.computeIfAbsent(device.takes(), key -> new HashSet<>()).add(network);
                    }
                }
            }
            for (Pos block : device.footprint()) {
                joined.computeIfAbsent(block, key -> new LinkedHashSet<>()).addAll(own);
            }
        }

        List<EnumSet<Fluid>> before = new ArrayList<>(fluids.size());
        for (EnumSet<Fluid> held : fluids) {
            before.add(EnumSet.copyOf(held));
        }
        for (Device device : devices) {
            if (!device.separate()) {
                continue;
            }
            Set<Join> own = new LinkedHashSet<>();
            for (Pos block : device.footprint()) {
                WaterNode consumer = WaterNode.consumer(block, EnumSet.allOf(Face.class));
                for (Face face : Face.values()) {
                    Tube tube = tubeAt.get(face.step(block));
                    if (tube == null || !WaterRules.connects(consumer, face, tube.node())) {
                        continue;
                    }
                    int network = networkOfTube.get(tube.pos());
                    if (before.get(network).contains(device.takes())) {
                        own.add(new Join(network, Kind.SINK, null));
                    } else if (before.get(network).contains(device.gives())
                            || takenBy.getOrDefault(device.gives(), Set.of()).contains(network)) {
                        own.add(new Join(network, Kind.SOURCE, device.gives()));
                        fluids.get(network).add(device.gives());
                    }
                }
            }
            for (Pos block : device.footprint()) {
                joined.computeIfAbsent(block, key -> new LinkedHashSet<>()).addAll(own);
            }
        }

        List<Network> built = new ArrayList<>(members.size());
        for (int id = 0; id < members.size(); id++) {
            built.add(new Network(id, List.copyOf(members.get(id)), Collections.unmodifiableSet(fluids.get(id))));
        }
        Map<Pos, List<Join>> frozen = new HashMap<>();
        for (Map.Entry<Pos, Set<Join>> entry : joined.entrySet()) {
            frozen.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return new OilJoinPlan(List.copyOf(built), Map.copyOf(networkOfTube), Map.copyOf(frozen));
    }

    /**
     * True when a tube placed at a block would make a network that holds two fluids. The tube joins the networks of the tubes beside it, and
     * through a merging device beside it, the networks that device joins; the networks are those of {@link #of} with the tube added.
     *
     * @param tubes the tubes of the world as they stand
     * @param devices the devices beside them
     * @param pos   the block the tube would stand in
     * @return true when the placement would mix two fluids
     */
    public static boolean mixesIfTubePlaced(Collection<Tube> tubes, Collection<Device> devices, Pos pos) {
        List<Tube> with = new ArrayList<>(tubes);
        with.add(new Tube(pos, WaterNode.pipe(pos), null));
        OilJoinPlan plan = of(with, devices);
        return !plan.networks().get(plan.networkOf(pos)).allowed();
    }

    /**
     * True when a device placed would make a network that holds two fluids. A merging device joins every network it touches into one, so
     * the networks it touches, with the device added, must hold one fluid kind between them. A separate device, the refinery, never merges
     * the networks it touches, so it never mixes.
     *
     * @param tubes     the tubes of the world as they stand
     * @param devices   the devices beside them
     * @param candidate the device that would be placed
     * @return true when the placement would mix two fluids
     */
    public static boolean mixesIfDevicePlaced(Collection<Tube> tubes, Collection<Device> devices, Device candidate) {
        if (candidate.separate()) {
            return false;
        }
        List<Device> with = new ArrayList<>(devices);
        with.add(candidate);
        OilJoinPlan plan = of(tubes, with);
        for (Join join : plan.joinsAt(candidate.anchor())) {
            if (join.kind() != Kind.TUBE && !plan.networks().get(join.network()).allowed()) {
                return true;
            }
        }
        return false;
    }

    /** The networks, by id. Each lists its tubes in position order. */
    public List<Network> networks() {
        return networks;
    }

    /** The id of the network a tube belongs to, or -1 when the position holds no tube of the plan. */
    public int networkOf(Pos tube) {
        return networkOfTube.getOrDefault(tube, -1);
    }

    /** The joins of a block: a tube's own network, or the networks a device joins from any of its blocks. Empty for other blocks. */
    public List<Join> joinsAt(Pos block) {
        return joins.getOrDefault(block, List.of());
    }
}
