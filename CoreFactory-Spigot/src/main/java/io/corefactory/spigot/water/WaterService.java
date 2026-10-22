package io.corefactory.spigot.water;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.PumpFuel;
import io.corefactory.core.water.PumpLevels;
import io.corefactory.core.water.WaterFlow;
import io.corefactory.core.water.WaterGrid;
import io.corefactory.core.water.WaterKind;
import io.corefactory.core.water.WaterNode;
import io.corefactory.core.water.WaterPart;
import io.corefactory.core.water.WaterRules;
import io.corefactory.core.water.WaterSettings;
import io.corefactory.core.water.WaterSource;
import io.corefactory.core.water.WaterText;
import io.corefactory.spigot.energy.DeviceRegistry;
import io.corefactory.spigot.energy.EnergyDevice;
import io.corefactory.spigot.energy.EnergyService;
import io.corefactory.spigot.energy.EnergySnapshots;
import io.corefactory.spigot.energy.GeneratorDevice;
import io.corefactory.spigot.hologram.HologramService;
import io.corefactory.spigot.machine.MachineStore;
import io.corefactory.spigot.machine.PlacedMachine;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The runtime of the water network: the pipes, pumps and splitters placed in the world, the lab machines and nuclear generators that take
 * water, and the solve that shares the water among them. One scheduled task calls {@link #tick} every server tick. Each pump checks its
 * source every {@code source-check-ticks}, burns its fuel or draws its power, and is wanted by the solve only when it has a source and runs.
 * Every {@code solve-ticks} the network is rebuilt from the placed blocks of the loaded chunks, and {@link WaterFlow} gives each consumer its
 * share and each pump its output. The results are read by the machines and the nuclear generators through {@link WaterFeed}.
 *
 * <p>The networks are solved per world, since a pipe does not cross worlds. A pump's power is a consumer of the energy network at levels
 * 3 to 5, and its satisfaction gates the pump the same way as its fuel.
 */
public final class WaterService implements EnergyService.ConsumerSource, WaterFeed {

    private static final double TICKS_PER_SECOND = 20.0;
    private static final long TEXT_TICKS = 10L;
    private static final long SAVE_TICKS = 300L;
    private static final double EPSILON = 1e-9;

    /** A consumer's ask for water, in units per second, and the tick it was made. */
    private record Request(double unitsPerSecond, long tick) {
    }

    /** One world's network: its grid and what the solve gave it. */
    private record Solved(WaterGrid grid, WaterFlow.Result result) {
    }

    private final MachineStore machines;
    private final DeviceRegistry devices;
    private final EnergyService energy;
    private final HologramService holograms;
    private final WaterStore store;
    private final WaterSettings settings;
    private final WaterEntities entities;
    private final Logger log;
    private final Map<String, WaterDevice> byId = new LinkedHashMap<>();
    private final Map<String, WaterDevice> byBlock = new HashMap<>();
    private final Map<String, Request> requests = new HashMap<>();
    private final Map<UUID, Solved> solved = new HashMap<>();
    private long tick;

    /**
     * @param core       the core, which reads the item definitions
     * @param machines   the lab machines, which take water
     * @param devices    the energy devices, whose nuclear generators take water
     * @param energy     the energy network, which gives the pumps' power and takes their demand
     * @param holograms  the holograms of the pumps
     * @param store      the water file
     * @param settings   the water settings from config.yml
     * @param log        receives the warnings of the file
     */
    public WaterService(CoreFactoryCore core, MachineStore machines, DeviceRegistry devices, EnergyService energy,
                        HologramService holograms, WaterStore store, WaterSettings settings, Logger log) {
        this.machines = machines;
        this.devices = devices;
        this.energy = energy;
        this.holograms = holograms;
        this.store = store;
        this.settings = settings;
        this.entities = new WaterEntities(core, holograms);
        this.log = log;
    }

    /** Reads the saved devices. Their entities are made by {@link #start} and by the chunk loads. */
    public void load() {
        for (WaterDevice device : store.read()) {
            if (!register(device)) {
                log.warning("water.yml: two water blocks share a position. The second is dropped.");
            }
        }
    }

    /** Makes the entities of the devices whose chunks are loaded now. */
    public void start() {
        for (WaterDevice device : List.copyOf(byId.values())) {
            ensure(device);
        }
    }

    /** Saves the devices, and releases their entities and holograms. Called when the plugin is disabled. */
    public void shutdown() {
        save();
        for (WaterDevice device : byId.values()) {
            release(device);
        }
    }

    /** The water blocks a pump's source needs, from config.yml. */
    public int sourceMinBlocks() {
        return settings.sourceMinBlocks();
    }

    /** The water block placed at a location, if one is there. */
    public Optional<WaterDevice> at(Location block) {
        if (block.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byBlock.get(WaterDevice.key(block.getWorld().getUID(), posOf(block))));
    }

    /** The water block with a device id, if there is one. */
    public Optional<WaterDevice> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** The location of a device's block. Its world must be loaded. */
    public Location location(WaterDevice device) {
        Pos pos = device.pos();
        return new Location(Bukkit.getWorld(device.world()), pos.x(), pos.y(), pos.z());
    }

    /**
     * The water blocks 6-connected to a location, counted up to a cap: the source a pump would have there. Zero when the location is not water.
     *
     * @param block the location
     * @param cap   the largest count wanted, at least 1
     */
    public int sourceAt(Location block, int cap) {
        World world = block.getWorld();
        if (world == null) {
            return 0;
        }
        return WaterSource.sourceBlocks(block.getBlockX(), block.getBlockY(), block.getBlockZ(),
                (x, y, z) -> world.isChunkLoaded(x >> 4, z >> 4) && world.getBlockAt(x, y, z).getType() == Material.WATER, cap);
    }

    /**
     * Places a water block at a block the player chose. The caller has checked the spot and the item.
     *
     * @param block the block the device stands in
     * @param part  the part
     * @param owner the player who places it
     * @param face  a splitter's input face or a pump's front; null for a pipe
     * @param level the pump's level, 1 for any other part
     * @return the device
     * @throws IllegalStateException when a water block already stands there
     */
    public WaterDevice place(Location block, WaterPart part, UUID owner, Face face, int level) {
        WaterDevice device = new WaterDevice(UUID.randomUUID().toString(), part, owner, block.getWorld().getUID(), posOf(block),
                face, level, PumpFuel.Burn.empty());
        if (!register(device)) {
            throw new IllegalStateException("a water block already stands there");
        }
        ensure(device);
        save();
        return device;
    }

    /**
     * Removes a water block, and releases its entities. Returns the item it gives back.
     *
     * @param device the device to remove
     * @return the items the removal gives back: one item for the part, empty when its item is not defined
     */
    public List<ItemStack> remove(WaterDevice device) {
        byId.remove(device.id());
        byBlock.remove(device.blockKey());
        release(device);
        save();
        return entities.itemOf(device.part(), device.level()).map(List::of).orElse(List.of());
    }

    /** Sets a pump's level, and redraws it. */
    public void upgrade(WaterDevice pump, int level) {
        pump.setLevel(level);
        entities.refresh(pump);
        save();
    }

    /** Adds fuel items to a pump. The caller has checked the count against {@link PumpFuel#accept}. */
    public void addFuel(WaterDevice pump, int count) {
        PumpFuel.Burn burn = pump.burn();
        pump.setBurn(new PumpFuel.Burn(Math.min(PumpFuel.CAPACITY, burn.items() + count), burn.ticksLeft()));
        save();
    }

    /** The lines a pump shows: its flow, its source, and its fuel or power. */
    public List<String> status(WaterDevice pump) {
        return pumpLines(pump);
    }

    /** A chunk loaded: the marked entities left in it are removed, and the devices in it get their entities. */
    public void onChunkLoad(Chunk chunk) {
        WaterEntities.sweep(chunk);
        for (WaterDevice device : List.copyOf(byId.values())) {
            if (inChunk(device, chunk)) {
                ensure(device);
            }
        }
    }

    /** A chunk unloaded: the devices in it release their entities and holograms. The devices stay in the file. */
    public void onChunkUnload(Chunk chunk) {
        for (WaterDevice device : byId.values()) {
            if (inChunk(device, chunk)) {
                release(device);
            }
        }
    }

    /** Every server tick: the pumps' sources and fuel, the solve every {@code solve-ticks}, the texts and the save. */
    public void tick() {
        tick++;
        for (WaterDevice device : byId.values()) {
            if (device.part() == WaterPart.PUMP && loaded(device)) {
                updatePump(device);
            }
        }
        if (tick % settings.solveTicks() == 0) {
            solve();
        }
        if (tick % TEXT_TICKS == 0) {
            refreshText();
        }
        if (tick % SAVE_TICKS == 0 && !byId.isEmpty()) {
            save();
        }
    }

    @Override
    public Map<UUID, List<Consumer>> consumers() {
        Map<UUID, List<Consumer>> byWorld = new HashMap<>();
        for (WaterDevice device : byId.values()) {
            if (device.part() != WaterPart.PUMP || !PumpLevels.powered(device.level()) || !loaded(device)) {
                continue;
            }
            double demand = device.hasSource(settings.sourceMinBlocks()) ? demandOf(device) : 0.0;
            byWorld.computeIfAbsent(device.world(), key -> new ArrayList<>())
                    .add(new Consumer(consumerKey(device), device.pos(), demand));
        }
        return byWorld;
    }

    @Override
    public boolean canSupply(Location block, int processTicks) {
        if (block.getWorld() == null) {
            return false;
        }
        double rate = settings.unitPerProcessRun() * TICKS_PER_SECOND / Math.max(1, processTicks);
        UUID world = block.getWorld().getUID();
        Pos pos = posOf(block);
        requests.put(EnergySnapshots.consumerId(world, pos.x(), pos.y(), pos.z()), new Request(rate, tick));
        Solved current = solved.get(world);
        if (current == null) {
            return false;
        }
        Optional<WaterNode> node = current.grid().node(pos);
        if (node.isEmpty() || node.get().kind() != WaterKind.CONSUMER) {
            return false;
        }
        return current.result().consumerDelivered().getOrDefault(pos, 0.0) + EPSILON >= rate;
    }

    @Override
    public Optional<String> waterLine(Location block) {
        if (block.getWorld() == null) {
            return Optional.empty();
        }
        UUID world = block.getWorld().getUID();
        Pos pos = posOf(block);
        Solved current = solved.get(world);
        if (current == null || !current.grid().pipeAttached(pos)) {
            return Optional.empty();
        }
        Request request = requests.get(EnergySnapshots.consumerId(world, pos.x(), pos.y(), pos.z()));
        double requested = request != null && tick - request.tick() <= requestWindow() ? request.unitsPerSecond() : 0.0;
        return Optional.of(WaterText.consumerLine(current.result().consumerDelivered().getOrDefault(pos, 0.0), requested));
    }

    @Override
    public boolean nuclearMayRun(Location block, boolean underwaterRuleMet) {
        if (block.getWorld() == null) {
            return underwaterRuleMet;
        }
        boolean ownWater = block.getBlock().getType() == Material.WATER;
        Solved current = solved.get(block.getWorld().getUID());
        Pos pos = posOf(block);
        boolean pipe = current != null && current.grid().pipeAttached(pos);
        double delivered = pipe ? current.result().deliveredIn(current.grid().networkOf(pos)) : 0.0;
        return WaterRules.nuclearMayRun(ownWater, underwaterRuleMet, pipe, delivered);
    }

    private boolean register(WaterDevice device) {
        if (byBlock.containsKey(device.blockKey())) {
            return false;
        }
        byId.put(device.id(), device);
        byBlock.put(device.blockKey(), device);
        return true;
    }

    private void updatePump(WaterDevice pump) {
        if (pump.sourceCheckedAt() < 0 || tick - pump.sourceCheckedAt() >= settings.sourceCheckTicks()) {
            pump.setSource(sourceAt(location(pump), settings.sourceMinBlocks() + 1), tick);
        }
        boolean hasSource = pump.hasSource(settings.sourceMinBlocks());
        boolean running;
        if (PumpLevels.powered(pump.level())) {
            running = hasSource && energy.satisfaction(consumerKey(pump)) > 0.0;
        } else {
            PumpFuel.Step step = PumpFuel.step(pump.burn(), hasSource);
            pump.setBurn(step.burn());
            running = step.running();
        }
        pump.setRunning(running);
    }

    /** Rebuilds every world's network from the loaded blocks and solves it. */
    private void solve() {
        Map<UUID, List<WaterNode>> nodes = new HashMap<>();
        Map<UUID, Set<Pos>> taken = new HashMap<>();
        Map<UUID, List<WaterDevice>> parts = new HashMap<>();
        for (WaterDevice device : byId.values()) {
            if (loaded(device)) {
                parts.computeIfAbsent(device.world(), key -> new ArrayList<>()).add(device);
                add(nodes, taken, device.world(), nodeOf(device));
            }
        }
        for (PlacedMachine placed : machines.all()) {
            if (placed.isLoaded() && placed.drill() == null && placed.block().getWorld() != null) {
                Location block = placed.block();
                add(nodes, taken, block.getWorld().getUID(), WaterNode.consumer(posOf(block), EnumSet.allOf(Face.class)));
            }
        }
        Set<String> nuclear = new HashSet<>();
        for (EnergyDevice device : devices.all()) {
            if (device instanceof GeneratorDevice generator && generator.generatorKind() == GeneratorKind.NUCLEAR
                    && generator.isLoaded()) {
                Pos pos = generator.pos();
                if (add(nodes, taken, generator.world(), WaterNode.consumer(pos, EnumSet.allOf(Face.class)))) {
                    nuclear.add(EnergySnapshots.consumerId(generator.world(), pos.x(), pos.y(), pos.z()));
                }
            }
        }

        Map<UUID, Solved> next = new HashMap<>();
        for (Map.Entry<UUID, List<WaterNode>> entry : nodes.entrySet()) {
            UUID world = entry.getKey();
            List<WaterNode> list = entry.getValue();
            WaterGrid grid = WaterGrid.of(list);
            Map<Pos, WaterFlow.PumpSupply> pumps = new HashMap<>();
            Map<Pos, Double> requested = new HashMap<>();
            for (WaterNode node : list) {
                Pos pos = node.pos();
                if (node.kind() == WaterKind.PUMP) {
                    WaterDevice pump = byBlock.get(WaterDevice.key(world, pos));
                    if (pump != null) {
                        pumps.put(pos, new WaterFlow.PumpSupply(pump.level(), pump.running()));
                    }
                } else if (node.kind() == WaterKind.CONSUMER) {
                    String key = EnergySnapshots.consumerId(world, pos.x(), pos.y(), pos.z());
                    if (nuclear.contains(key)) {
                        requested.put(pos, (double) settings.unitPerProcessRun());
                    } else {
                        Request request = requests.get(key);
                        if (request != null && tick - request.tick() <= requestWindow()) {
                            requested.put(pos, request.unitsPerSecond());
                        }
                    }
                }
            }
            WaterFlow.Result result = WaterFlow.solve(grid, settings.flowTable(), pumps, requested);
            next.put(world, new Solved(grid, result));
            for (WaterDevice device : parts.getOrDefault(world, List.of())) {
                if (device.part() == WaterPart.PIPE) {
                    Set<Face> faces = Set.copyOf(grid.connectedFaces(device.pos()));
                    if (!faces.equals(device.shown())) {
                        device.setShown(faces);
                        entities.refresh(device);
                    }
                }
            }
        }
        solved.clear();
        solved.putAll(next);
    }

    private long requestWindow() {
        return 2L * settings.solveTicks();
    }

    private void refreshText() {
        for (WaterDevice device : byId.values()) {
            if (device.part() == WaterPart.PUMP && device.hologram() != null) {
                device.hologram().setLines(pumpLines(device));
            }
        }
    }

    private List<String> pumpLines(WaterDevice pump) {
        Solved current = solved.get(pump.world());
        double delivered = current == null ? 0.0 : current.result().pumpOutput().getOrDefault(pump.pos(), 0.0);
        List<String> lines = new ArrayList<>();
        lines.add(WaterText.pumpFlow(pump.level(), delivered));
        lines.add(WaterText.source(pump.source(), settings.sourceMinBlocks()));
        if (PumpLevels.powered(pump.level())) {
            lines.add(WaterText.power(energy.satisfaction(consumerKey(pump)), demandOf(pump)));
        } else {
            lines.add(WaterText.fuel(pump.level(), pump.burn().items(), pump.running()));
        }
        return lines;
    }

    private int demandOf(WaterDevice pump) {
        return settings.pumpPowerW().get(pump.level() - 1);
    }

    /** Makes a device's entities when they are missing and its chunk is loaded. A pump also gets its hologram. */
    private void ensure(WaterDevice device) {
        if (!loaded(device) || (WaterEntities.alive(device.display()) && WaterEntities.alive(device.interaction()))) {
            return;
        }
        release(device);
        Location block = location(device);
        entities.spawn(device, block);
        if (device.part() == WaterPart.PUMP) {
            entities.hologram(block, pumpLines(device)).ifPresent(device::setHologram);
        }
    }

    private void release(WaterDevice device) {
        entities.release(device);
        if (device.hologram() != null) {
            holograms.remove(device.hologram());
            device.setHologram(null);
        }
    }

    private void save() {
        store.write(List.copyOf(byId.values()));
    }

    private static WaterNode nodeOf(WaterDevice device) {
        return switch (device.part()) {
            case PIPE -> WaterNode.pipe(device.pos());
            case PUMP -> WaterNode.pump(device.pos());
            case TEE -> WaterNode.splitter(device.pos(), WaterKind.SPLITTER_T, device.face());
            case CROSS -> WaterNode.splitter(device.pos(), WaterKind.SPLITTER_X, device.face());
        };
    }

    private static boolean add(Map<UUID, List<WaterNode>> nodes, Map<UUID, Set<Pos>> taken, UUID world, WaterNode node) {
        if (!taken.computeIfAbsent(world, key -> new HashSet<>()).add(node.pos())) {
            return false;
        }
        nodes.computeIfAbsent(world, key -> new ArrayList<>()).add(node);
        return true;
    }

    private static boolean loaded(WaterDevice device) {
        World world = Bukkit.getWorld(device.world());
        return world != null && world.isChunkLoaded(device.pos().x() >> 4, device.pos().z() >> 4);
    }

    private static boolean inChunk(WaterDevice device, Chunk chunk) {
        return device.world().equals(chunk.getWorld().getUID())
                && (device.pos().x() >> 4) == chunk.getX() && (device.pos().z() >> 4) == chunk.getZ();
    }

    private static String consumerKey(WaterDevice device) {
        Pos pos = device.pos();
        return EnergySnapshots.consumerId(device.world(), pos.x(), pos.y(), pos.z());
    }

    private static Pos posOf(Location block) {
        return new Pos(block.getBlockX(), block.getBlockY(), block.getBlockZ());
    }
}
