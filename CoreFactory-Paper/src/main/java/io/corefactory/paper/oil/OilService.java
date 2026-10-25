package io.corefactory.paper.oil;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.device.Footprint;
import io.corefactory.core.drill.DrillKind;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.oil.Fluid;
import io.corefactory.core.oil.FluidTank;
import io.corefactory.core.oil.OilJoinPlan;
import io.corefactory.core.oil.OilJoinPlan.Device;
import io.corefactory.core.oil.OilJoinPlan.Kind;
import io.corefactory.core.oil.OilNetwork;
import io.corefactory.core.oil.OilPart;
import io.corefactory.core.oil.OilSettings;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.PumpLevels;
import io.corefactory.core.water.WaterGrid;
import io.corefactory.core.water.WaterNode;
import io.corefactory.core.water.WaterPart;
import io.corefactory.paper.chunks.ChunkKey;
import io.corefactory.paper.energy.DeviceRegistry;
import io.corefactory.paper.energy.EnergyDevice;
import io.corefactory.paper.energy.GeneratorDevice;
import io.corefactory.paper.machine.MachineStore;
import io.corefactory.paper.machine.PlacedMachine;
import io.corefactory.paper.water.WaterDevice;
import io.corefactory.paper.water.WaterService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * The runtime of the oil network: the oil tubes placed in the world, each with a tank of petroleum or diesel, and the devices they join. A
 * network is a set of tubes joined the way a water pipe is joined, and {@link OilJoinPlan} decides the joins. Its tanks hold one fluid at a
 * time, and the fluid a source gives is the fluid of the network it joins. Each server tick the networks are rebuilt from the loaded blocks,
 * and the tubes whose joins changed are drawn again.
 *
 * <p>A device joins the networks that touch its footprint. A petroleum drill gives petroleum into every network it joins; a petroleum or
 * diesel generator, an ore drill and a diesel pump take from them. These devices merge the networks they touch into one. A crude oil refinery
 * keeps the networks it touches apart: it takes petroleum from each network that holds petroleum, and gives diesel into each network that
 * holds diesel, or is empty with a diesel consumer on it, on any of its faces. Devices move fluid through {@link #supply}, {@link #draw} and
 * {@link #available}. Each network moves at most the pipe flow of the settings per server tick, counted over supply and draw together. A
 * network whose fluids disagree is kept as it is and moves nothing.
 *
 * <p>The tubes are saved, with their fluid; the entities and the networks are not. A tube's block is kept in the world it stands in, and its
 * entities are made when its chunk loads and removed when it unloads.
 */
public final class OilService {

    /** Ticks between two saves of the tubes. */
    private static final long SAVE_TICKS = 300L;

    /** The length of one server tick, in seconds. */
    private static final double TICK_SECONDS = 0.05;

    /** The pump level that burns diesel. Levels 1 and 2 burn fuel; level 1 burns coal or charcoal. */
    private static final int DIESEL_PUMP_LEVEL = 2;

    /** The component id of the crude oil refinery, the lab machine that joins the oil network. */
    private static final String REFINERY = "crude_oil_refinery";

    /** A device's join to a network, or a tube's join to its own network. */
    private record Join(Net net, Kind kind, Fluid gives) {
    }

    /**
     * One network: the tanks of its tubes, and its oil network. The oil network is null when the fluids disagree; such a network moves nothing.
     */
    private static final class Net {

        private final String key;
        private final List<FluidTank> tanks;
        private OilNetwork network;

        Net(String key, List<FluidTank> tanks) {
            this.key = key;
            this.tanks = tanks;
        }
    }

    /** The litres a network may still move in a server tick, and the tick they were counted for. */
    private static final class Flow {

        private long tick = -1L;
        private double litres;
    }

    private final MachineStore machines;
    private final DeviceRegistry devices;
    private final WaterService water;
    private final OilStore store;
    private final OilSettings settings;
    private final OilEntities entities;
    private final Logger log;
    private final Map<String, OilDevice> byId = new LinkedHashMap<>();
    private final Map<String, OilDevice> byBlock = new HashMap<>();
    private final Map<String, Flow> flows = new HashMap<>();
    private Map<String, List<Join>> joins = Map.of();
    private long tick;

    /**
     * @param core     the core, which reads the item definitions
     * @param machines the lab machines and drills, which give and take oil
     * @param devices  the energy devices, which take oil or give it
     * @param water    the water devices, whose diesel pumps take oil
     * @param store    the oil file
     * @param settings the oil settings from config.yml
     * @param log      receives the warnings of the file
     */
    public OilService(CoreFactoryCore core, MachineStore machines, DeviceRegistry devices, WaterService water, OilStore store,
                      OilSettings settings, Logger log) {
        this.machines = machines;
        this.devices = devices;
        this.water = water;
        this.store = store;
        this.settings = settings;
        this.entities = new OilEntities(core);
        this.log = log;
    }

    /**
     * Reads the saved tubes. Their entities are made by {@link #start} and by the chunk loads. The networks that hold two fluids are settled
     * here, once, before the first tick: each is warned about, and its tubes of the fluid it does not keep are emptied.
     */
    public void load() {
        for (OilDevice device : store.read(settings.tubeCapacityLitres())) {
            if (!register(device)) {
                log.warning("oil.yml: two oil blocks share a position. The second is dropped.");
            }
        }
        settleMixed();
    }

    /** Makes the entities of the tubes whose chunks are loaded now. */
    public void start() {
        for (OilDevice device : List.copyOf(byId.values())) {
            ensure(device);
        }
    }

    /** Saves the tubes, and releases their entities. Called when the plugin is disabled. */
    public void shutdown() {
        save();
        for (OilDevice device : byId.values()) {
            entities.release(device);
        }
    }

    /** The oil tube placed at a location, if one is there. */
    public Optional<OilDevice> at(Location block) {
        if (block.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byBlock.get(OilDevice.key(block.getWorld().getUID(), posOf(block))));
    }

    /** Every oil tube placed. */
    public List<OilDevice> all() {
        return List.copyOf(byId.values());
    }

    /** The chunk of every oil tube, source and sink block, for the chunk keeper. */
    public Set<ChunkKey> chunkKeys() {
        Set<ChunkKey> keys = new HashSet<>();
        for (OilDevice device : all()) {
            keys.add(ChunkKey.of(device.world(), device.pos().x(), device.pos().z()));
        }
        return keys;
    }

    /** True when the tube's chunk is loaded, so its block can be reached. */
    public boolean isLoaded(OilDevice device) {
        return loaded(device);
    }

    /** The oil tube with a device id, if there is one. */
    public Optional<OilDevice> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** The location of a tube's block. Its world must be loaded. */
    public Location location(OilDevice device) {
        Pos pos = device.pos();
        return new Location(Bukkit.getWorld(device.world()), pos.x(), pos.y(), pos.z());
    }

    /**
     * Places an oil tube at a block the player chose. The caller has checked the spot, the item and that {@link #wouldMixTube} is false.
     * The tube's tank starts empty.
     *
     * @param block  the block the tube stands in
     * @param part   the part
     * @param owner  the player who places it
     * @param face   a splitter's input face; null for a pipe
     * @param output a splitter's output face on another line than its input; null for a pipe
     * @return the tube
     * @throws IllegalStateException when an oil tube already stands there
     */
    public OilDevice place(Location block, OilPart part, UUID owner, Face face, Face output) {
        OilDevice device = new OilDevice(UUID.randomUUID().toString(), part, owner, block.getWorld().getUID(), posOf(block), face,
                settings.tubeCapacityLitres());
        if (output != null) {
            device.setOutput(output);
        }
        if (!register(device)) {
            throw new IllegalStateException("an oil tube already stands there");
        }
        ensure(device);
        sync();
        save();
        return device;
    }

    /**
     * Removes an oil tube, and releases its entities. The fluid in its tank goes with it. Returns the item it gives back.
     *
     * @param device the tube to remove
     * @return the item the removal gives back: one item for the part, empty when its item is not defined
     */
    public List<ItemStack> remove(OilDevice device) {
        byId.remove(device.id());
        byBlock.remove(device.blockKey(), device);
        entities.release(device);
        save();
        sync();
        return entities.itemOf(device.part()).map(List::of).orElse(List.of());
    }

    /** A chunk loaded: the marked entities left in it are removed, and the tubes in it get their entities. */
    public void onChunkLoad(Chunk chunk) {
        OilEntities.sweep(chunk);
        for (OilDevice device : List.copyOf(byId.values())) {
            if (inChunk(device, chunk)) {
                ensure(device);
            }
        }
    }

    /** A chunk unloaded: the tubes in it release their entities. The tubes stay in the file. */
    public void onChunkUnload(Chunk chunk) {
        for (OilDevice device : byId.values()) {
            if (inChunk(device, chunk)) {
                entities.release(device);
            }
        }
    }

    /**
     * Every server tick: the networks are rebuilt, and the tubes drawn again where their joins changed. The tick counter moves first, so
     * the flow of each network is counted afresh for this tick by the devices that run after it.
     */
    public void tick() {
        tick++;
        sync();
        if (tick % SAVE_TICKS == 0) {
            save();
        }
    }

    /**
     * Gives litres of a fluid to the network a block joins. A tube's block joins its own network; a device's block joins the networks its
     * role gives into. The network must hold no other fluid, and it moves no more than its flow budget left for this tick.
     *
     * @param block  the block of the tube or device that gives
     * @param fluid  the fluid given
     * @param litres the litres offered, finite and 0 or more
     * @return the litres taken in by the network; 0 when no network takes them
     */
    public double supply(Location block, Fluid fluid, double litres) {
        Objects.requireNonNull(fluid, "fluid");
        if (!(litres > 0)) {
            return 0.0;
        }
        for (Net net : netsAt(block, join -> join.kind() == Kind.TUBE || (join.kind() == Kind.SOURCE && join.gives() == fluid))) {
            double amount = Math.min(litres, allowance(net));
            if (amount <= 0) {
                continue;
            }
            double accepted = net.network.supply(fluid, amount);
            if (accepted > 0) {
                spend(net, accepted);
                return accepted;
            }
        }
        return 0.0;
    }

    /**
     * Takes litres of a fluid out of the network a block joins. A tube's block draws from its own network; a device's block draws from the
     * networks its role takes from. The litres count against the network's flow budget for this tick.
     *
     * @param block  the block of the tube or device that takes
     * @param fluid  the fluid asked for
     * @param litres the litres asked for, finite and 0 or more
     * @return the litres given out; 0 when no network holds the fluid
     */
    public double draw(Location block, Fluid fluid, double litres) {
        Objects.requireNonNull(fluid, "fluid");
        if (!(litres > 0)) {
            return 0.0;
        }
        for (Net net : netsAt(block, join -> join.kind() == Kind.TUBE || join.kind() == Kind.SINK)) {
            double amount = Math.min(litres, allowance(net));
            if (amount <= 0) {
                continue;
            }
            double given = net.network.draw(fluid, amount);
            if (given > 0) {
                spend(net, given);
                return given;
            }
        }
        return 0.0;
    }

    /**
     * The litres of a fluid the networks a block joins hold. It takes nothing and counts against no budget.
     *
     * @param block the block of the tube or device
     * @param fluid the fluid asked about
     * @return the litres held, summed over the networks of the block that hold the fluid
     */
    public double available(Location block, Fluid fluid) {
        Objects.requireNonNull(fluid, "fluid");
        double total = 0.0;
        for (Net net : netsAt(block, join -> true)) {
            if (net.network.kinds().contains(fluid)) {
                total += net.network.litres();
            }
        }
        return total;
    }

    /**
     * The diesel outputs a block gives into: the networks it joins as a source of the fluid, each counted once. Zero when none is joined.
     *
     * @param block the block of the device
     * @param fluid the fluid the outputs take
     * @return the number of networks the block gives the fluid into
     */
    public int outputs(Location block, Fluid fluid) {
        Objects.requireNonNull(fluid, "fluid");
        return netsAt(block, join -> join.kind() == Kind.SOURCE && join.gives() == fluid).size();
    }

    /**
     * True when a device of the given height placed on the block would join networks that hold two fluids. It counts the footprint of the
     * tallest device, so a short device placed under a tube can be refused although it would not reach that tube; use
     * {@link #wouldMix(Location, int, Fluid)} when the device is known.
     *
     * @param block the block the device stands in
     * @return true when the placement would mix two fluids
     */
    public boolean wouldMix(Location block) {
        return wouldMix(block, Footprint.MAX_HEIGHT, null);
    }

    /**
     * True when a device placed on the block would make a network that holds two fluids. The tubes and devices beside the block are read as
     * {@link #sync} reads them, and the device is added to them: a merging device joins every network it touches, so the networks it touches
     * must hold one fluid kind between them. A refinery never mixes, since it keeps its networks apart, and it is not checked here.
     *
     * @param block  the block the device's anchor stands in
     * @param height the device's footprint height, from {@link Footprint#heightOf}
     * @param gives  the fluid the device gives into the networks it joins, or null when it gives none
     * @return true when the placement would mix two fluids
     */
    public boolean wouldMix(Location block, int height, Fluid gives) {
        World world = block.getWorld();
        if (world == null) {
            return false;
        }
        Device candidate = new Device(posOf(block), height, gives, null, false);
        return OilJoinPlan.mixesIfDevicePlaced(tubesOf(world.getUID()), devicesOf(world.getUID()).values(), candidate);
    }

    /**
     * True when a tube placed on the block would make a network that holds two fluids. The tube joins the tubes beside it, and the networks
     * of the merging devices beside it; a refinery beside it joins no network through the tube. A tube holds no fluid of its own.
     *
     * @param block the block the tube stands in
     * @return true when the tube would mix two fluids
     */
    public boolean wouldMixTube(Location block) {
        World world = block.getWorld();
        if (world == null) {
            return false;
        }
        return OilJoinPlan.mixesIfTubePlaced(tubesOf(world.getUID()), devicesOf(world.getUID()).values(), posOf(block));
    }

    /**
     * Rebuilds every world's networks from the loaded tubes and the devices beside them, through {@link OilJoinPlan}, and draws again each
     * tube whose joins changed.
     */
    private void sync() {
        Map<UUID, Map<Pos, OilDevice>> tubes = new HashMap<>();
        for (OilDevice tube : byId.values()) {
            if (loaded(tube)) {
                tubes.computeIfAbsent(tube.world(), key -> new HashMap<>()).put(tube.pos(), tube);
            }
        }

        Map<String, Net> netOfTube = new HashMap<>();
        Map<String, List<Join>> built = new HashMap<>();
        Map<UUID, WaterGrid> shown = new HashMap<>();
        List<Net> nets = new ArrayList<>();
        for (Map.Entry<UUID, Map<Pos, OilDevice>> world : tubes.entrySet()) {
            Map<Pos, OilDevice> worldTubes = world.getValue();
            Map<Pos, Device> devices = devicesOf(world.getKey(), worldTubes.keySet());
            List<OilJoinPlan.Tube> planned = new ArrayList<>();
            for (OilDevice tube : worldTubes.values()) {
                planned.add(planTube(tube));
            }
            OilJoinPlan plan = OilJoinPlan.of(planned, devices.values());

            Net[] made = new Net[plan.networks().size()];
            for (OilJoinPlan.Network network : plan.networks()) {
                List<OilDevice> members = new ArrayList<>();
                for (Pos pos : network.tubes()) {
                    members.add(worldTubes.get(pos));
                }
                members.sort(Comparator.comparing(OilDevice::blockKey));
                List<FluidTank> tanks = new ArrayList<>();
                for (OilDevice tube : members) {
                    tanks.add(tube.tank());
                }
                Net net = new Net(members.get(0).blockKey(), tanks);
                net.network = network.allowed() ? new OilNetwork(tanks) : null;
                nets.add(net);
                made[network.id()] = net;
                for (OilDevice tube : members) {
                    netOfTube.put(tube.blockKey(), net);
                }
            }

            for (OilDevice tube : worldTubes.values()) {
                built.put(tube.blockKey(), joinsOf(plan.joinsAt(tube.pos()), made));
            }
            for (Device device : devices.values()) {
                for (Pos block : device.footprint()) {
                    built.computeIfAbsent(OilDevice.key(world.getKey(), block), key -> new ArrayList<>())
                            .addAll(joinsOf(plan.joinsAt(block), made));
                }
            }
            shown.put(world.getKey(), shownGrid(worldTubes, devices));
        }

        Set<String> keys = new LinkedHashSet<>();
        for (Net net : nets) {
            keys.add(net.key);
        }
        flows.keySet().retainAll(keys);
        joins = built;

        for (OilDevice device : byId.values()) {
            WaterGrid grid = shown.get(device.world());
            if (device.part() != OilPart.PIPE || grid == null || !loaded(device)) {
                continue;
            }
            Set<Face> faces = Set.copyOf(grid.connectedFaces(device.pos()));
            if (!faces.equals(device.shown())) {
                device.setShown(faces);
                entities.refresh(device, location(device));
            }
        }
    }

    /** A tube as the join plan reads it: its block, its shape and the fluid its tank holds. */
    private static OilJoinPlan.Tube planTube(OilDevice tube) {
        return new OilJoinPlan.Tube(tube.pos(), nodeOf(tube), tube.tank().fluid().orElse(null));
    }

    /** The joins of the plan, each with its network as the Net it was made into. */
    private static List<Join> joinsOf(List<OilJoinPlan.Join> planned, Net[] made) {
        List<Join> joins = new ArrayList<>(planned.size());
        for (OilJoinPlan.Join join : planned) {
            joins.add(new Join(made[join.network()], join.kind(), join.gives()));
        }
        return joins;
    }

    /**
     * The water grid the tubes are drawn by: the tubes, and every block of a device beside them, so a tube shows an arm to each device it
     * joins, a refinery included on every face.
     */
    private static WaterGrid shownGrid(Map<Pos, OilDevice> tubes, Map<Pos, Device> devices) {
        List<WaterNode> nodes = new ArrayList<>();
        for (OilDevice tube : tubes.values()) {
            nodes.add(nodeOf(tube));
        }
        Set<Pos> taken = new HashSet<>(tubes.keySet());
        for (Device device : devices.values()) {
            for (Pos block : device.footprint()) {
                if (taken.add(block)) {
                    nodes.add(WaterNode.consumer(block, EnumSet.allOf(Face.class)));
                }
            }
        }
        return WaterGrid.of(nodes);
    }

    /**
     * The networks a block joins through the joins that pass the test, each once and only when it has a network.
     */
    private List<Net> netsAt(Location block, Predicate<Join> wanted) {
        World world = block.getWorld();
        if (world == null) {
            return List.of();
        }
        Set<Net> found = new LinkedHashSet<>();
        for (Join join : joins.getOrDefault(OilDevice.key(world.getUID(), posOf(block)), List.of())) {
            if (join.net().network != null && wanted.test(join)) {
                found.add(join.net());
            }
        }
        return List.copyOf(found);
    }

    /** The litres a network may still move this tick. The budget is the pipe flow of one tick, and it resets when the tick changes. */
    private double allowance(Net net) {
        Flow flow = flows.computeIfAbsent(net.key, key -> new Flow());
        if (flow.tick != tick) {
            flow.tick = tick;
            flow.litres = settings.pipeFlowLitresPerSecond() * TICK_SECONDS;
        }
        return flow.litres;
    }

    private void spend(Net net, double litres) {
        Flow flow = flows.get(net.key);
        flow.litres = Math.max(0.0, flow.litres - litres);
    }

    /**
     * The device a block belongs to when it may join the oil network: a lab machine that is a petroleum or ore drill or the refinery, an
     * energy generator that burns a fluid, or a water pump at the diesel level. Empty for any other block.
     */
    private Optional<Device> endpointAt(Location block) {
        Optional<PlacedMachine> placed = machines.at(block);
        if (placed.isPresent()) {
            return deviceOf(placed.get());
        }
        Optional<EnergyDevice> energy = devices.at(block);
        if (energy.isPresent()) {
            EnergyDevice device = energy.get();
            if (device instanceof GeneratorDevice generator && generator.generatorKind().fluid().isPresent() && device.isLoaded()) {
                Fluid fuel = generator.generatorKind().fluid().orElseThrow();
                return Optional.of(new Device(device.pos(), device.height(), null, fuel, false));
            }
            return Optional.empty();
        }
        Optional<WaterDevice> pump = water.at(block);
        if (pump.isPresent() && pump.get().part() == WaterPart.PUMP && pump.get().level() == DIESEL_PUMP_LEVEL
                && water.isLoaded(pump.get())) {
            WaterDevice device = pump.get();
            return Optional.of(new Device(device.pos(), PumpLevels.height(device.level()), null, Fluid.DIESEL, false));
        }
        return Optional.empty();
    }

    /**
     * The device of a placed lab machine in the oil network: a petroleum drill gives petroleum, an ore drill joins without a fluid, and the
     * refinery takes petroleum and gives diesel, keeping each network it touches apart. Empty for a machine that does not join the network.
     */
    private static Optional<Device> deviceOf(PlacedMachine placed) {
        if (!placed.isLoaded()) {
            return Optional.empty();
        }
        Pos anchor = posOf(placed.block());
        int height = Footprint.heightOf(placed.machine().id());
        if (placed.drill() == null) {
            if (!REFINERY.equals(placed.machine().id())) {
                return Optional.empty();
            }
            return Optional.of(new Device(anchor, height, Fluid.DIESEL, Fluid.PETROLEUM, true));
        }
        DrillKind kind = placed.drill().definition().kind();
        if (kind == DrillKind.PETROLEUM) {
            return Optional.of(new Device(anchor, height, Fluid.PETROLEUM, null, false));
        }
        if (kind == DrillKind.ORE) {
            return Optional.of(new Device(anchor, height, null, null, false));
        }
        return Optional.empty();
    }

    /** The loaded tubes of a world, by position. */
    private Map<Pos, OilDevice> loadedIn(UUID world) {
        Map<Pos, OilDevice> found = new HashMap<>();
        for (OilDevice tube : byId.values()) {
            if (tube.world().equals(world) && loaded(tube)) {
                found.put(tube.pos(), tube);
            }
        }
        return found;
    }

    /** The loaded tubes of a world, as the join plan reads them. */
    private List<OilJoinPlan.Tube> tubesOf(UUID world) {
        List<OilJoinPlan.Tube> planned = new ArrayList<>();
        for (OilDevice tube : loadedIn(world).values()) {
            planned.add(planTube(tube));
        }
        return planned;
    }

    /** The devices beside the loaded tubes of a world, by anchor. */
    private Map<Pos, Device> devicesOf(UUID world) {
        return devicesOf(world, loadedIn(world).keySet());
    }

    /** The devices beside the given tubes of a world, by anchor, each device once. */
    private Map<Pos, Device> devicesOf(UUID world, Set<Pos> tubes) {
        Map<Pos, Device> found = new LinkedHashMap<>();
        for (Pos tube : tubes) {
            for (Face face : Face.values()) {
                Pos next = face.step(tube);
                if (tubes.contains(next)) {
                    continue;
                }
                endpointAt(blockAt(world, next)).ifPresent(device -> found.putIfAbsent(device.anchor(), device));
            }
        }
        return found;
    }

    /**
     * Registers a tube under its block. A part already there keeps its block, and the second is refused.
     */
    private boolean register(OilDevice device) {
        if (byBlock.containsKey(device.blockKey())) {
            return false;
        }
        byId.put(device.id(), device);
        byBlock.put(device.blockKey(), device);
        return true;
    }

    /** Makes a tube's entities when they are missing and its chunk is loaded. */
    private void ensure(OilDevice device) {
        if (!loaded(device) || (OilEntities.alive(device.display()) && OilEntities.alive(device.interaction()))) {
            return;
        }
        entities.release(device);
        entities.spawn(device, location(device));
    }

    private void save() {
        store.write(List.copyOf(byId.values()));
    }

    /**
     * Settles the networks of the registered tubes that hold two fluids. The networks are the components of each world's water grid over the
     * tube blocks, the same grid {@link #sync} builds. Devices are not placed yet when the file loads, so only tube-to-tube joins are seen here.
     * Each mixed network is warned about once, and the tubes of the fluid it does not keep are emptied through their tanks.
     */
    private void settleMixed() {
        Map<UUID, List<OilDevice>> byWorld = new LinkedHashMap<>();
        for (OilDevice tube : byId.values()) {
            byWorld.computeIfAbsent(tube.world(), key -> new ArrayList<>()).add(tube);
        }
        for (Map.Entry<UUID, List<OilDevice>> world : byWorld.entrySet()) {
            List<WaterNode> nodes = new ArrayList<>();
            List<MixedNetworks.Tube> tubes = new ArrayList<>();
            Map<Pos, OilDevice> atPos = new HashMap<>();
            for (OilDevice tube : world.getValue()) {
                nodes.add(nodeOf(tube));
                atPos.put(tube.pos(), tube);
                tubes.add(new MixedNetworks.Tube(tube.pos(), tube.tank().fluid().map(Fluid::id).orElse(null)));
            }
            for (MixedNetworks.Mixed mixed : MixedNetworks.decide(tubes, WaterGrid.of(nodes).networks())) {
                Pos first = mixed.first();
                log.warning("oil.yml: the oil network whose first tube is at " + first.x() + ", " + first.y() + ", " + first.z()
                        + " (world " + world.getKey() + ") holds " + mixed.kept() + " and " + mixed.emptied()
                        + ". Its " + mixed.tubes().size() + " tube(s) holding " + mixed.emptied() + " are emptied.");
                for (Pos pos : mixed.tubes()) {
                    atPos.get(pos).empty();
                }
            }
        }
    }

    private static Location blockAt(UUID world, Pos pos) {
        return new Location(Bukkit.getWorld(world), pos.x(), pos.y(), pos.z());
    }

    private static WaterNode nodeOf(OilDevice device) {
        if (device.part() == OilPart.PIPE) {
            return WaterNode.pipe(device.pos());
        }
        return WaterNode.splitter(device.pos(), device.part().kind(), device.face(), device.output());
    }

    private static boolean loaded(OilDevice device) {
        World world = Bukkit.getWorld(device.world());
        return world != null && world.isChunkLoaded(device.pos().x() >> 4, device.pos().z() >> 4);
    }

    private static boolean inChunk(OilDevice device, Chunk chunk) {
        return device.world().equals(chunk.getWorld().getUID())
                && (device.pos().x() >> 4) == chunk.getX() && (device.pos().z() >> 4) == chunk.getZ();
    }

    private static Pos posOf(Location block) {
        return new Pos(block.getBlockX(), block.getBlockY(), block.getBlockZ());
    }
}
