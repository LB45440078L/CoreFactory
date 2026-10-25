package io.corefactory.core.logistics;

import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.Pos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * A logistics network: belts, pickers and splitters, and the items on them. The configuration is fixed once built; only
 * the items and the timers of the nodes change, in {@link #tick}.
 *
 * <p>Each tick updates the nodes downstream first, so a full line fills from its end backwards. A node with satisfaction
 * 0 does nothing; otherwise its speed, its rate and its delays run at satisfaction times normal speed. Items leave a belt
 * into the next belt or into a port at its output cell. A picker or splitter takes the item at its input (a belt's exit,
 * or a port for a picker) only when the output accepts it. A picker placed beside a straight belt takes an item from
 * that belt's side (see {@link PickSide}); the items it does not take keep moving.
 */
public final class LogisticsNetwork {

    private static final double EPS = 1e-9;

    private final Topology topology;
    private final List<Topology.Node> order;
    private final Map<Pos, Lane> lanes = new HashMap<>();
    private final Map<String, PickerState> pickerStates = new HashMap<>();
    private final Map<String, SplitterState> splitterStates = new HashMap<>();
    private final Map<String, DuplexState> duplexStates = new HashMap<>();
    private long serialCount;

    private LogisticsNetwork(Topology topology) {
        this.topology = topology;
        this.order = topology.processingOrder();
        for (BeltUnit belt : topology.belts()) {
            lanes.put(belt.pos(), new Lane(this::nextSerial));
        }
        for (Picker picker : topology.pickers()) {
            pickerStates.put(pickerId(picker.id()), new PickerState());
        }
        for (Splitter splitter : topology.splitters()) {
            splitterStates.put(splitterId(splitter.id()), new SplitterState());
        }
        for (Duplex duplex : topology.duplexes()) {
            duplexStates.put(duplexId(duplex.id()), new DuplexState());
        }
    }

    /** A new, empty builder. */
    public static Builder builder() {
        return new Builder();
    }

    /** The node id of the belt at {@code pos}. */
    public static String beltId(Pos pos) {
        return "belt:" + pos.x() + ":" + pos.y() + ":" + pos.z();
    }

    /** The node id of the picker with this id. */
    public static String pickerId(String id) {
        return "picker:" + id;
    }

    /** The node id of the splitter with this id. */
    public static String splitterId(String id) {
        return "splitter:" + id;
    }

    /** The node id of the belt duplex with this id. */
    public static String duplexId(String id) {
        return "duplex:" + id;
    }

    /** Every belt, picker and splitter as a power consumer, in id order. */
    public List<Consumer> consumers() {
        List<Consumer> consumers = new ArrayList<>();
        for (Topology.Node node : topology.nodes()) {
            consumers.add(new Consumer(node.id(), node.pos(), demandOf(node)));
        }
        return consumers;
    }

    /** The shape of the belt at {@code belt}. */
    public BeltShape shapeOf(Pos belt) {
        return topology.shapeOf(requireBelt(belt));
    }

    /** The side the belt at {@code belt} takes its items from: its feeder's side, or behind it when nothing feeds it. */
    public Dir inputSideOf(Pos belt) {
        return topology.inputSideOf(requireBelt(belt));
    }

    /**
     * Puts an item at the entry of a belt, when there is room. Used to load stored items and by tests.
     *
     * @return false when the entry has no room
     */
    public boolean insertAtEntry(Pos belt, Item item) {
        Objects.requireNonNull(item, "item");
        Lane lane = laneAt(belt);
        if (!lane.hasRoom()) {
            return false;
        }
        lane.insertAtEntry(item);
        return true;
    }

    /** The items on a belt, front (exit side) first. */
    public List<BeltItem> itemsOn(Pos belt) {
        return laneAt(belt).view();
    }

    /**
     * The serial of each item on a belt, in the order of {@link #itemsOn}. A serial is unique within this network and stays
     * with its item while the item moves, so a view can follow the item from tick to tick.
     */
    public List<Long> serialsOn(Pos belt) {
        return laneAt(belt).serialList();
    }

    /**
     * Puts an item back on a belt at a position, as when items are restored from a save or carried into a rebuilt network.
     *
     * @param position where the item sits along the belt, from 0 at the entry to 1 at the exit
     * @return false, and nothing changes, when the position overlaps an item already on the belt or is outside the belt
     */
    public boolean insertAt(Pos belt, Item item, double position) {
        Objects.requireNonNull(item, "item");
        return laneAt(belt).insertAt(item, position);
    }

    /**
     * The item a splitter or a belt duplex holds between its input and its outputs, or empty when it holds none. The id is the
     * device's id, the same for either kind.
     *
     * @throws IllegalArgumentException when no splitter or duplex has this id
     */
    public Optional<Item> heldBy(String deviceId) {
        return Optional.ofNullable(heldState(deviceId).held());
    }

    /**
     * Puts an item into a splitter or a belt duplex that holds none, as when a held item is restored.
     *
     * @return false, and nothing changes, when the device already holds an item
     * @throws IllegalArgumentException when no splitter or duplex has this id
     */
    public boolean holdAt(String deviceId, Item item) {
        Objects.requireNonNull(item, "item");
        HeldState state = heldState(deviceId);
        if (state.held() != null) {
            return false;
        }
        state.hold(item);
        return true;
    }

    /** The items held by belts, splitters and duplexes. Pickers hold nothing between ticks. */
    public int itemCount() {
        int count = 0;
        for (Lane lane : lanes.values()) {
            count += lane.size();
        }
        for (SplitterState state : splitterStates.values()) {
            if (state.held != null) {
                count++;
            }
        }
        for (DuplexState state : duplexStates.values()) {
            if (state.held != null) {
                count++;
            }
        }
        return count;
    }

    /**
     * Advances the network by one tick.
     *
     * @param dt           the length of the tick in seconds, positive
     * @param satisfaction each node's satisfaction by node id, from 0 to 1; a missing node counts as 1
     * @param ports        the storages at the block positions the network touches, keyed by position
     * @return what each node moved and which nodes were blocked
     * @throws IllegalArgumentException when dt, a satisfaction or a port cell is invalid
     */
    public LogisticsResult tick(double dt, Map<String, Double> satisfaction, Map<Pos, Port> ports) {
        if (!(dt > 0) || !Double.isFinite(dt)) {
            throw new IllegalArgumentException("dt must be positive and finite: " + dt);
        }
        Objects.requireNonNull(satisfaction, "satisfaction");
        Objects.requireNonNull(ports, "ports");
        for (Map.Entry<String, Double> entry : satisfaction.entrySet()) {
            Double value = entry.getValue();
            if (value == null || !(value >= 0.0 && value <= 1.0)) {
                throw new IllegalArgumentException("satisfaction of " + entry.getKey() + " must be within 0 and 1: " + value);
            }
        }
        for (Map.Entry<Pos, Port> entry : ports.entrySet()) {
            Objects.requireNonNull(entry.getValue(), "port");
            if (topology.occupied(entry.getKey())) {
                throw new IllegalArgumentException("a port cannot share a cell with a belt, picker or splitter: " + entry.getKey());
            }
        }
        Map<String, Integer> moved = new LinkedHashMap<>();
        Set<String> blocked = new TreeSet<>();
        for (Topology.Node node : order) {
            double satisfied = satisfaction.getOrDefault(node.id(), 1.0);
            int count = switch (node.kind()) {
                case BELT -> stepBelt(node, dt, satisfied, ports, blocked);
                case PICKER -> stepPicker(node, dt, satisfied, ports, blocked);
                case SPLITTER -> stepSplitter(node, dt, satisfied, ports, blocked);
                case DUPLEX -> stepDuplex(node, dt, satisfied, ports, blocked);
            };
            moved.put(node.id(), count);
        }
        return new LogisticsResult(moved, blocked);
    }

    private int stepBelt(Topology.Node node, double dt, double satisfied, Map<Pos, Port> ports, Set<String> blocked) {
        Lane lane = lanes.get(node.pos());
        int count = 0;
        if (satisfied > 0) {
            BeltUnit unit = topology.belt(node.pos());
            Pos next = unit.flow().step(unit.pos());
            double distance = LogisticsTable.beltSpeedBlocksPerSecond(unit.level()) * satisfied * dt;
            count = lane.advance(distance, item -> offer(next, item, ports));
        }
        if (lane.exitWaiting()) {
            blocked.add(node.id());
        }
        return count;
    }

    private int stepPicker(Topology.Node node, double dt, double satisfied, Map<Pos, Port> ports, Set<String> blocked) {
        if (satisfied <= 0) {
            return 0;
        }
        Picker picker = topology.picker(node.pos());
        PickerState state = pickerStates.get(node.id());
        double run = dt * satisfied;
        if (state.wait > EPS) {
            state.wait -= run;
            if (state.wait > EPS) {
                return 0;
            }
        }
        Source source = pickerSource(picker, ports);
        Target target = targetAt(picker.outputCell(), ports);
        Item item = source == null ? null : source.peek();
        if (item == null || target == null || !target.accepts(item)) {
            state.primed = false;
            state.delayLeft = 0.0;
            if (item != null) {
                blocked.add(node.id());
            }
            return 0;
        }
        if (!state.primed) {
            if (state.delayLeft <= 0) {
                state.delayLeft = LogisticsTable.PICKER_DELAY_SECONDS;
            }
            state.delayLeft -= run;
            if (state.delayLeft > EPS) {
                return 0;
            }
            state.delayLeft = 0.0;
            state.primed = true;
        }
        source.take();
        target.put(item);
        state.wait = 1.0 / LogisticsTable.pickerItemsPerSecond(picker.level()) + Math.min(0.0, state.wait);
        return 1;
    }

    private int stepSplitter(Topology.Node node, double dt, double satisfied, Map<Pos, Port> ports, Set<String> blocked) {
        SplitterState state = splitterStates.get(node.id());
        if (satisfied <= 0) {
            if (state.held != null) {
                blocked.add(node.id());
            }
            return 0;
        }
        Splitter splitter = topology.splitter(node.pos());
        double run = dt * satisfied;
        if (state.held == null) {
            if (state.wait > EPS) {
                state.wait -= run;
                if (state.wait > EPS) {
                    return 0;
                }
            }
            Source source = splitterSource(splitter);
            Item item = source == null ? null : source.peek();
            if (item == null) {
                return 0;
            }
            source.take();
            state.held = item;
        }
        Target target = targetAt(splitter.outputCell(state.held), ports);
        if (target == null || !target.accepts(state.held)) {
            blocked.add(node.id());
            return 0;
        }
        target.put(state.held);
        state.held = null;
        state.wait = 1.0 / LogisticsTable.splitterItemsPerSecond(splitter.level()) + Math.min(0.0, state.wait);
        return 1;
    }

    /**
     * A belt duplex: it takes one item from its input belt when it holds none, and gives it to its two outputs in turn. The output
     * whose turn it is goes first; when it refuses, the other output is tried. After an item is given to one output, the turn passes
     * to the other, so with both outputs open the items alternate. When neither takes the item, the duplex holds it and is blocked.
     * It takes the next item after its wait, which is 1 / rate seconds for each item, so the total rate is the duplex's rate.
     */
    private int stepDuplex(Topology.Node node, double dt, double satisfied, Map<Pos, Port> ports, Set<String> blocked) {
        DuplexState state = duplexStates.get(node.id());
        if (satisfied <= 0) {
            if (state.held != null) {
                blocked.add(node.id());
            }
            return 0;
        }
        Duplex duplex = topology.duplexAt(node.pos());
        double run = dt * satisfied;
        if (state.held == null) {
            if (state.wait > EPS) {
                state.wait -= run;
                if (state.wait > EPS) {
                    return 0;
                }
            }
            Source source = duplexSource(duplex);
            Item item = source == null ? null : source.peek();
            if (item == null) {
                return 0;
            }
            source.take();
            state.held = item;
        }
        List<Pos> outputs = duplex.outputCells();
        for (int turn = 0; turn < outputs.size(); turn++) {
            int side = (state.next + turn) % outputs.size();
            Target target = targetAt(outputs.get(side), ports);
            if (target != null && target.accepts(state.held)) {
                target.put(state.held);
                state.held = null;
                state.next = 1 - side;
                state.wait = 1.0 / LogisticsTable.duplexItemsPerSecond(duplex.level()) + Math.min(0.0, state.wait);
                return 1;
            }
        }
        blocked.add(node.id());
        return 0;
    }

    private boolean offer(Pos cell, Item item, Map<Pos, Port> ports) {
        Target target = targetAt(cell, ports);
        if (target == null || !target.accepts(item)) {
            return false;
        }
        target.put(item);
        return true;
    }

    private Target targetAt(Pos cell, Map<Pos, Port> ports) {
        Lane lane = lanes.get(cell);
        if (lane != null) {
            return new LaneEnd(lane);
        }
        Port port = ports.get(cell);
        return port == null ? null : new PortEnd(port);
    }

    private Source pickerSource(Picker picker, Map<Pos, Port> ports) {
        Pos input = picker.inputCell();
        BeltUnit belt = topology.belt(input);
        if (belt != null) {
            return switch (PickSide.of(picker, belt, topology.shapeOf(belt))) {
                case FRONT -> new LaneEnd(lanes.get(input));
                case SIDE -> new LaneSide(lanes.get(input));
                case NONE -> null;
            };
        }
        Port port = ports.get(input);
        return port == null ? null : new PortEnd(port);
    }

    private Source splitterSource(Splitter splitter) {
        Pos back = splitter.backCell();
        BeltUnit belt = topology.belt(back);
        return belt != null && belt.flow() == splitter.inDir() ? new LaneEnd(lanes.get(back)) : null;
    }

    /** The belt that feeds a duplex: the one at a back cell that flows into the duplex. The topology allows at most one. */
    private Source duplexSource(Duplex duplex) {
        for (Pos back : duplex.backCells()) {
            BeltUnit belt = topology.belt(back);
            if (belt != null && belt.flow() == duplex.flow()) {
                return new LaneEnd(lanes.get(back));
            }
        }
        return null;
    }

    private double demandOf(Topology.Node node) {
        return switch (node.kind()) {
            case BELT -> LogisticsTable.beltPowerW(topology.belt(node.pos()).level());
            case PICKER -> LogisticsTable.pickerPowerW(topology.picker(node.pos()).level());
            case SPLITTER -> LogisticsTable.splitterPowerW(topology.splitter(node.pos()).level());
            case DUPLEX -> LogisticsTable.duplexPowerW(topology.duplexAt(node.pos()).level());
        };
    }

    private BeltUnit requireBelt(Pos pos) {
        BeltUnit belt = topology.belt(pos);
        if (belt == null) {
            throw new IllegalArgumentException("no belt at " + pos);
        }
        return belt;
    }

    private Lane laneAt(Pos pos) {
        Lane lane = lanes.get(pos);
        if (lane == null) {
            throw new IllegalArgumentException("no belt at " + pos);
        }
        return lane;
    }

    /** The held-item state of a splitter or a duplex with this device id. */
    private HeldState heldState(String id) {
        SplitterState splitter = splitterStates.get(splitterId(id));
        if (splitter != null) {
            return splitter;
        }
        DuplexState duplex = duplexStates.get(duplexId(id));
        if (duplex != null) {
            return duplex;
        }
        throw new IllegalArgumentException("no splitter or duplex with id " + id);
    }

    private long nextSerial() {
        return ++serialCount;
    }

    /** Something a picker or splitter takes from. */
    private interface Source {

        Item peek();

        void take();
    }

    /** Something a picker or splitter, or a belt, gives to. */
    private interface Target {

        boolean accepts(Item item);

        void put(Item item);
    }

    /** A belt lane, as the end a picker or splitter takes from and gives to. */
    private record LaneEnd(Lane lane) implements Source, Target {

        @Override
        public Item peek() {
            return lane.exitItem();
        }

        @Override
        public void take() {
            lane.takeExit();
        }

        @Override
        public boolean accepts(Item item) {
            return lane.hasRoom();
        }

        @Override
        public void put(Item item) {
            lane.insertAtEntry(item);
        }
    }

    /** A belt lane, as the side a picker takes from: any item on the unit, the item nearest the exit first. */
    private record LaneSide(Lane lane) implements Source {

        @Override
        public Item peek() {
            return lane.frontItem();
        }

        @Override
        public void take() {
            lane.takeFront();
        }
    }

    /** A port, as the end a picker or splitter, or a belt, takes from and gives to. */
    private record PortEnd(Port port) implements Source, Target {

        @Override
        public Item peek() {
            return port.peekOutput();
        }

        @Override
        public void take() {
            port.takeOutput();
        }

        @Override
        public boolean accepts(Item item) {
            return port.accepts(item);
        }

        @Override
        public void put(Item item) {
            port.accept(item);
        }
    }

    private static final class PickerState {

        private double wait;
        private double delayLeft;
        private boolean primed;
    }

    /** A device that holds one item between its input and its outputs: a splitter or a belt duplex. */
    private interface HeldState {

        Item held();

        void hold(Item item);
    }

    private static final class SplitterState implements HeldState {

        private Item held;
        private double wait;

        @Override
        public Item held() {
            return held;
        }

        @Override
        public void hold(Item item) {
            held = item;
        }
    }

    private static final class DuplexState implements HeldState {

        private Item held;
        private double wait;
        /** The output whose turn it is to go first: 0 for the anchor cell's output, 1 for the second cell's. */
        private int next;

        @Override
        public Item held() {
            return held;
        }

        @Override
        public void hold(Item item) {
            held = item;
        }
    }

    /** Builds a network. Each method checks the new node against the nodes already added and refuses a bad one. */
    public static final class Builder {

        private final List<BeltUnit> belts = new ArrayList<>();
        private final List<Picker> pickers = new ArrayList<>();
        private final List<Splitter> splitters = new ArrayList<>();
        private final List<Duplex> duplexes = new ArrayList<>();

        private Builder() {
        }

        /**
         * Adds a belt unit.
         *
         * @throws IllegalArgumentException when the cell is occupied or the belt is refused (merge, head-on or a wrong feed)
         */
        public Builder belt(Pos pos, Dir flow, LogisticsLevel level) {
            requireFree(pos);
            BeltUnit unit = new BeltUnit(pos, flow, level);
            List<BeltUnit> next = new ArrayList<>(belts);
            next.add(unit);
            require(new Topology(next, pickers, splitters, duplexes).validate(), "belt at " + pos);
            belts.add(unit);
            return this;
        }

        /**
         * Adds a picker.
         *
         * @throws IllegalArgumentException when the id is taken, the cell is occupied or the picker is refused
         */
        public Builder picker(String id, Pos pos, Dir inDir, Dir outDir, LogisticsLevel level) {
            requireFree(pos);
            Picker picker = new Picker(id, pos, inDir, outDir, level);
            List<Picker> next = new ArrayList<>(pickers);
            next.add(picker);
            require(new Topology(belts, next, splitters, duplexes).validate(), "picker " + id + " at " + pos);
            pickers.add(picker);
            return this;
        }

        /**
         * Adds a splitter. The lists are copied; an item on both lists is refused.
         *
         * @throws IllegalArgumentException when an item is on both lists, the id is taken, the cell is occupied or the
         *                                  splitter is refused
         */
        public Builder splitter(String id, Pos pos, Dir inDir, Set<Item> leftSet, Set<Item> rightSet, LogisticsLevel level) {
            requireFree(pos);
            Splitter splitter = new Splitter(id, pos, inDir, leftSet, rightSet, level);
            List<Splitter> next = new ArrayList<>(splitters);
            next.add(splitter);
            require(new Topology(belts, pickers, next, duplexes).validate(), "splitter " + id + " at " + pos);
            splitters.add(splitter);
            return this;
        }

        /**
         * Adds a belt duplex. Both of its cells, the anchor and {@code anchor + right(flow)}, must be free.
         *
         * @throws IllegalArgumentException when the id is taken, a cell is occupied or the duplex is refused (a second input,
         *                                  or a feed from a side of either cell)
         */
        public Builder duplex(String id, Pos anchor, Dir flow, LogisticsLevel level) {
            Duplex duplex = new Duplex(id, anchor, flow, level);
            for (Pos cell : duplex.cells()) {
                requireFree(cell);
            }
            List<Duplex> next = new ArrayList<>(duplexes);
            next.add(duplex);
            require(new Topology(belts, pickers, splitters, next).validate(), "duplex " + id + " at " + anchor);
            duplexes.add(duplex);
            return this;
        }

        /** The network with everything added so far. */
        public LogisticsNetwork build() {
            return new LogisticsNetwork(new Topology(belts, pickers, splitters, duplexes));
        }

        private void requireFree(Pos pos) {
            Objects.requireNonNull(pos, "pos");
            for (BeltUnit belt : belts) {
                if (belt.pos().equals(pos)) {
                    throw new IllegalArgumentException("cell is occupied: " + pos);
                }
            }
            for (Picker picker : pickers) {
                if (picker.pos().equals(pos)) {
                    throw new IllegalArgumentException("cell is occupied: " + pos);
                }
            }
            for (Splitter splitter : splitters) {
                if (splitter.pos().equals(pos)) {
                    throw new IllegalArgumentException("cell is occupied: " + pos);
                }
            }
            for (Duplex duplex : duplexes) {
                if (duplex.cells().contains(pos)) {
                    throw new IllegalArgumentException("cell is occupied: " + pos);
                }
            }
        }

        private static void require(PlacementReason reason, String what) {
            if (reason != PlacementReason.OK) {
                throw new IllegalArgumentException(what + " refused: " + reason);
            }
        }
    }
}
