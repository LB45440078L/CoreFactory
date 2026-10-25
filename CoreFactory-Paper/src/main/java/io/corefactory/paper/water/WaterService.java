package io.corefactory.paper.water;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.hologram.Tone;
import io.corefactory.core.oil.Fluid;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.PumpFuel;
import io.corefactory.core.water.PumpHologram;
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
import io.corefactory.paper.audio.MachineSounds;
import io.corefactory.paper.chunks.ChunkKey;
import io.corefactory.paper.energy.DeviceRegistry;
import io.corefactory.paper.energy.EnergyDevice;
import io.corefactory.paper.energy.EnergyService;
import io.corefactory.paper.energy.EnergySnapshots;
import io.corefactory.paper.energy.GeneratorDevice;
import io.corefactory.paper.hologram.HologramService;
import io.corefactory.paper.machine.ItemRefs;
import io.corefactory.paper.machine.MachineStore;
import io.corefactory.paper.machine.PlacedMachine;
import io.corefactory.paper.oil.OilService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final Set<WaterPumpMenu> menus = new HashSet<>();
    private final Map<String, Request> requests = new HashMap<>();
    private final Map<UUID, Solved> solved = new HashMap<>();
    private long tick;
    private OilService oil;

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

    /** True when a water pipe is joined to the block at a location: a nuclear generator that has one may run on the pipe's water. */
    @Override
    public boolean joinedToPipe(Location block) {
        if (block.getWorld() == null) {
            return false;
        }
        Solved current = solved.get(block.getWorld().getUID());
        return current != null && current.grid().pipeAttached(posOf(block));
    }

    /** The water block placed at a location, if one is there. */
    public Optional<WaterDevice> at(Location block) {
        if (block.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byBlock.get(WaterDevice.key(block.getWorld().getUID(), posOf(block))));
    }

    /** Every water block placed. */
    public List<WaterDevice> all() {
        return List.copyOf(byId.values());
    }

    /** The chunk of every pump and pipe block, for the chunk keeper. */
    public Set<ChunkKey> chunkKeys() {
        Set<ChunkKey> keys = new HashSet<>();
        for (WaterDevice device : all()) {
            keys.add(ChunkKey.of(device.world(), device.pos().x(), device.pos().z()));
        }
        return keys;
    }

    /** True when the device's chunk is loaded, so its block can be reached. */
    public boolean isLoaded(WaterDevice device) {
        return loaded(device);
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
     * Whether the block above a location is free for the upper half of a tall pump: no device, machine or water part stands there, and
     * the block is not solid.
     *
     * @param lower the lower block of the tall pump
     */
    public boolean spaceAbove(Location lower) {
        if (lower.getWorld() == null) {
            return false;
        }
        Location above = lower.clone().add(0, 1, 0);
        return at(above).isEmpty() && machines.at(above).isEmpty() && devices.at(above).isEmpty()
                && !above.getBlock().getType().isSolid();
    }

    /**
     * Places a water block at a block the player chose. The caller has checked the spot and the item.
     *
     * @param block the block the device stands in
     * @param part  the part
     * @param owner the player who places it
     * @param face   a splitter's input face or a pump's front; null for a pipe
     * @param output a splitter's output face on another line than its input; null for other parts
     * @param level  the pump's level, 1 for any other part
     * @return the device
     * @throws IllegalStateException when a water block already stands there
     */
    public WaterDevice place(Location block, WaterPart part, UUID owner, Face face, Face output, int level) {
        WaterDevice device = new WaterDevice(UUID.randomUUID().toString(), part, owner, block.getWorld().getUID(), posOf(block),
                face, level, PumpFuel.Burn.empty());
        if (output != null) {
            device.setOutput(output);
        }
        if (!register(device)) {
            throw new IllegalStateException("a water block already stands there");
        }
        ensure(device);
        save();
        recountSources();
        return device;
    }

    /**
     * Removes a water block, and releases its entities. Returns the item it gives back.
     *
     * @param device the device to remove
     * @return the items the removal gives back: one item for the part, empty when its item is not defined
     */
    public List<ItemStack> remove(WaterDevice device) {
        for (WaterPumpMenu menu : List.copyOf(menus)) {
            if (menu.pump().id().equals(device.id())) {
                menu.closeViewers();
            }
        }
        byId.remove(device.id());
        byBlock.remove(device.blockKey(), device);
        if (device.part() == WaterPart.PUMP && PumpLevels.height(device.level()) > 1) {
            byBlock.remove(upperKey(device), device);
        }
        release(device);
        save();
        recountSources();
        return entities.itemOf(device.part(), device.level()).map(List::of).orElse(List.of());
    }

    /**
     * Sets a pump's level and makes its entities again, since a tall level has a second block. The caller has checked the block above
     * with {@link #spaceAbove} when the level is tall.
     */
    public void upgrade(WaterDevice pump, int level) {
        boolean wasTall = PumpLevels.height(pump.level()) > 1;
        pump.setLevel(level);
        if (!wasTall && PumpLevels.height(level) > 1) {
            byBlock.putIfAbsent(upperKey(pump), pump);
        }
        if (loaded(pump)) {
            release(pump);
            ensure(pump);
        }
        save();
    }

    /**
     * Sets the oil network a level 2 pump draws its diesel from. Without one such a pump stops, since it draws nothing.
     */
    public void useOil(OilService oil) {
        this.oil = Objects.requireNonNull(oil, "oil");
    }

    /** Adds fuel items to a pump. The caller has checked the count against {@link PumpFuel#accept}. */
    public void addFuel(WaterDevice pump, int count) {
        PumpFuel.Burn burn = pump.burn();
        pump.setBurn(new PumpFuel.Burn(Math.min(PumpFuel.CAPACITY, burn.items() + count), burn.ticksLeft()));
        save();
    }

    /**
     * The numbers a pump's menu and hologram show: what it gives now and what it can give, its source against the minimum, and its power.
     *
     * @param delivered    units per second the pump puts into its network
     * @param capacity     units per second the pump can give at its level
     * @param source       the water blocks its source counted, up to the cap
     * @param minimum      the water blocks a source needs
     * @param satisfaction the share of its power the grid gives it, 0 to 1; 0 at a level that burns fuel
     * @param demandW      the power it asks for, in W; 0 at a level that burns fuel
     * @param running      true when it runs now
     */
    public record PumpReading(double delivered, double capacity, int source, int minimum, double satisfaction, int demandW,
                              boolean running, String power) {
    }

    /** The current reading of a pump. */
    public PumpReading reading(WaterDevice pump) {
        Solved current = solved.get(pump.world());
        double delivered = current == null ? 0.0 : current.result().pumpOutput().getOrDefault(pump.pos(), 0.0);
        boolean powered = PumpLevels.powered(pump.level());
        return new PumpReading(delivered, settings.pumpFlow().get(pump.level() - 1), pump.source(), settings.sourceMinBlocks(),
                powered ? energy.satisfaction(consumerKey(pump)) : 0.0, powered ? demandOf(pump) : 0, pump.running(),
                powerLine(pump));
    }

    /**
     * The power line of a pump that draws power: no grid when no grid covers it, waiting for a source when it has none, else the share of
     * its demand the grid gives it. A grid gives a consumer with no demand a full share, so the line is never read as full without a grid.
     */
    private String powerLine(WaterDevice pump) {
        if (!energy.connected(consumerKey(pump))) {
            return WaterText.noGrid();
        }
        if (!pump.hasSource(settings.sourceMinBlocks())) {
            return WaterText.noSource();
        }
        return WaterText.power(energy.satisfaction(consumerKey(pump)), demandOf(pump));
    }

    /**
     * Opens a pump's menu for a player. Each opening makes its own menu, which is kept up to date until it closes.
     *
     * @param refs converts the items a player puts into the fuel slot
     */
    public void openMenu(Player player, WaterDevice pump, ItemRefs refs) {
        WaterPumpMenu menu = new WaterPumpMenu(this, pump, refs);
        menus.add(menu);
        menu.open(player);
    }

    /** A menu closed: the fuel in its slot goes to the pump, and the rest back to the player. */
    public void closeMenu(WaterPumpMenu menu, Player player) {
        menus.remove(menu);
        menu.absorb(player);
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
            refreshMenus();
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
            // Critical: the nuclear generators on this network stop when the pump stops, so the pump is served before any other consumer.
            byWorld.computeIfAbsent(device.world(), key -> new ArrayList<>())
                    .add(new Consumer(consumerKey(device), device.pos(), demand, true));
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

    /**
     * Registers a device under its block. A tall pump also claims the block above it, when that block is free. A part already there
     * keeps its block: the pump still works, and its upper half is shared, so nothing a player placed before the tall level is dropped.
     */
    private boolean register(WaterDevice device) {
        if (byBlock.containsKey(device.blockKey())) {
            return false;
        }
        byId.put(device.id(), device);
        byBlock.put(device.blockKey(), device);
        if (device.part() == WaterPart.PUMP && PumpLevels.height(device.level()) > 1) {
            String upper = upperKey(device);
            if (byBlock.containsKey(upper)) {
                log.warning("A level " + device.level() + " pump at " + device.pos().x() + " " + device.pos().y() + " "
                        + device.pos().z() + " has a water part in the block above it. The part is kept, and the pump's upper block is shared.");
            } else {
                byBlock.put(upper, device);
            }
        }
        return true;
    }

    private static String upperKey(WaterDevice device) {
        return WaterDevice.key(device.world(), new Pos(device.pos().x(), device.pos().y() + 1, device.pos().z()));
    }

    /**
     * The source of a pump: the most water that any block of its network reaches, counted up to the minimum plus one. A pipe counts the
     * water it stands in or touches, so a source tube in a lake, or beside one on land, is a source for every pump it is joined to. A pump
     * counts only the water of its own block, so a pump on land with no pipe into water has no source, even beside a lake.
     */
    private int networkSource(WaterDevice pump) {
        World world = Bukkit.getWorld(pump.world());
        if (world == null) {
            return 0;
        }
        int cap = settings.sourceMinBlocks() + 1;
        WaterSource.BlockProbe probe = (x, y, z) -> world.isChunkLoaded(x >> 4, z >> 4)
                && world.getBlockAt(x, y, z).getType() == Material.WATER;
        Solved current = solved.get(pump.world());
        int network = current == null ? -1 : current.grid().networkOf(pump.pos());
        if (network < 0) {
            return WaterSource.sourceBlocks(pump.pos().x(), pump.pos().y(), pump.pos().z(), probe, cap);
        }
        int best = 0;
        for (Pos pos : current.grid().networks().get(network)) {
            Optional<WaterNode> node = current.grid().node(pos);
            if (node.isEmpty() || node.get().kind() == WaterKind.CONSUMER) {
                continue;
            }
            int count = node.get().kind() == WaterKind.PUMP
                    ? WaterSource.sourceBlocks(pos.x(), pos.y(), pos.z(), probe, cap)
                    : WaterSource.sourceAround(pos.x(), pos.y(), pos.z(), probe, cap);
            best = Math.max(best, count);
            if (best >= cap) {
                break;
            }
        }
        return best;
    }

    /**
     * Rebuilds the networks and counts every pump's source again, at once, so a pump's hologram changes in the tick a tube is placed or
     * broken rather than at its next periodic check.
     */
    private void recountSources() {
        solve();
        for (WaterDevice device : byId.values()) {
            if (device.part() == WaterPart.PUMP && loaded(device)) {
                device.setSource(networkSource(device), tick);
            }
        }
    }

    private void updatePump(WaterDevice pump) {
        if (pump.sourceCheckedAt() < 0 || tick - pump.sourceCheckedAt() >= settings.sourceCheckTicks()) {
            pump.setSource(networkSource(pump), tick);
        }
        boolean hasSource = pump.hasSource(settings.sourceMinBlocks());
        boolean running;
        if (PumpLevels.powered(pump.level())) {
            running = hasSource && energy.connected(consumerKey(pump)) && energy.satisfaction(consumerKey(pump)) > 0.0;
        } else if (PumpFuel.drawsFluid(pump.level())) {
            running = hasSource && drawDiesel(pump);
        } else {
            PumpFuel.Step step = PumpFuel.step(pump.burn(), hasSource);
            pump.setBurn(step.burn());
            running = step.running();
        }
        pump.setRunning(running);
        if (running) {
            MachineSounds.pumpRun(location(pump), pump.level(), tick);
        }
    }

    /**
     * Draws the diesel a level 2 pump burns in this tick from the oil network at its block. The pump runs only when the network gave it
     * the whole litres it wanted. With no oil network it draws nothing and stops.
     */
    private boolean drawDiesel(WaterDevice pump) {
        if (oil == null) {
            return false;
        }
        double wanted = PumpFuel.litresWanted(pump.level());
        double drawn = oil.draw(location(pump), Fluid.DIESEL, wanted);
        return drawn + EPSILON >= wanted;
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
                        entities.refresh(device, location(device));
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

    private void refreshMenus() {
        menus.removeIf(menu -> !menu.isOpen());
        for (WaterPumpMenu menu : List.copyOf(menus)) {
            menu.refresh();
        }
    }

    private void refreshText() {
        for (WaterDevice device : byId.values()) {
            if (device.part() == WaterPart.PUMP && device.hologram() != null) {
                device.hologram().setTone(pumpTone(device));
                device.hologram().setLines(pumpLines(device));
            }
        }
    }

    /** A pump's background: green while it pushes water, red while it is stopped for want of a source, fuel, diesel or power. */
    private Tone pumpTone(WaterDevice pump) {
        return PumpHologram.tone(hologramReading(pump));
    }

    /** The lines of a pump's hologram: its title, status, flow, source, and power or fuel. */
    private List<String> pumpLines(WaterDevice pump) {
        return PumpHologram.lines(hologramReading(pump));
    }

    /** The state a pump's hologram is built from, read now. */
    private PumpHologram.Reading hologramReading(WaterDevice pump) {
        Solved current = solved.get(pump.world());
        double delivered = current == null ? 0.0 : current.result().pumpOutput().getOrDefault(pump.pos(), 0.0);
        boolean powered = PumpLevels.powered(pump.level());
        return new PumpHologram.Reading(pump.level(), delivered, settings.pumpFlow().get(pump.level() - 1), pump.source(),
                settings.sourceMinBlocks(), pump.running(), powered && energy.connected(consumerKey(pump)),
                powered ? energy.satisfaction(consumerKey(pump)) : 0.0, demandOf(pump), pump.burn().held());
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
            entities.hologram(block, PumpLevels.height(device.level()), pumpLines(device)).ifPresent(hologram -> {
                hologram.setTone(pumpTone(device));
                device.setHologram(hologram);
            });
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
            case TEE -> WaterNode.splitter(device.pos(), WaterKind.SPLITTER_T, device.face(), device.output());
            case CROSS -> WaterNode.splitter(device.pos(), WaterKind.SPLITTER_X, device.face(), device.output());
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
