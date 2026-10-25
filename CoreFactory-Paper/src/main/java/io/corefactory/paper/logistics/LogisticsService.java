package io.corefactory.paper.logistics;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.EnergyTable;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.logistics.BeltItem;
import io.corefactory.core.logistics.BeltShape;
import io.corefactory.core.logistics.BeltUnit;
import io.corefactory.core.logistics.Dir;
import io.corefactory.core.logistics.Duplex;
import io.corefactory.core.logistics.Endpoints;
import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.LogisticsLevel;
import io.corefactory.core.logistics.LogisticsNetwork;
import io.corefactory.core.logistics.LogisticsResult;
import io.corefactory.core.logistics.LogisticsTable;
import io.corefactory.core.logistics.Placement;
import io.corefactory.core.logistics.PlacementReason;
import io.corefactory.core.logistics.Picker;
import io.corefactory.core.logistics.Port;
import io.corefactory.core.logistics.Splitter;
import io.corefactory.paper.chunks.ChunkKey;
import io.corefactory.paper.energy.DeviceRegistry;
import io.corefactory.paper.energy.EnergyDevice;
import io.corefactory.paper.energy.EnergyService;
import io.corefactory.paper.energy.EnergySnapshots;
import io.corefactory.paper.energy.GeneratorDevice;
import io.corefactory.paper.hologram.HologramService;
import io.corefactory.paper.hook.ContainerProtection;
import io.corefactory.paper.machine.ItemRefs;
import io.corefactory.paper.machine.MachineStore;
import io.corefactory.paper.machine.PlacedMachine;
import io.corefactory.paper.turret.TurretRegistry;
import io.corefactory.paper.vertical.TowerNeighbours;
import io.corefactory.paper.vertical.VerticalService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Owns the belts, pickers and splitters, the items on the belts, and the logistics networks that carry them. Each world has one
 * network, built from its devices and rebuilt when a device is added, removed or re-filtered; the items are carried over to the new
 * network, so a rebuild moves nothing. Every server tick, each network of a loaded world runs once with the satisfaction the energy
 * service gives its devices, in this order: the satisfaction map is read, the ports at the cells the devices touch are found, the
 * network moves the items, the displays of the items are moved, and the moves are shown (particles, holograms). A device in an
 * unloaded chunk has satisfaction 0, so the items on it wait. The state is written to {@code logistics.yml} every save interval
 * and on shutdown.
 */
public final class LogisticsService implements EnergyService.ConsumerSource {

    private static final int ENTITY_TICKS = 20;
    private static final int HOLOGRAM_TICKS = 10;
    private static final int PUFF_TICKS = 10;
    private static final int MOVE_WINDOW_TICKS = 20;
    private static final double PUFF_RANGE_SQUARED = 32.0 * 32.0;
    private static final int PUFF_COUNT = 3;
    private static final double TICK_SECONDS = EnergyTable.SECONDS_PER_TICK;

    /**
     * A world's network with the belt positions it was built from, and the ids of the splitters and belt duplexes, which hold an item
     * between their input and outputs.
     */
    private record Built(LogisticsNetwork network, Set<Pos> belts, Set<String> holders) {
    }

    /** An item display's key: the world and the serial of the item in its network. */
    private record ItemKey(UUID world, long serial) {
    }

    private final CoreFactoryCore core;
    private final MachineStore machines;
    private final DeviceRegistry energyDevices;
    private final EnergyService energy;
    private final LogisticsStore store;
    private final LogisticsEntities entities;
    private final LogisticsPorts ports;
    private final VerticalService vertical;
    private final DisplayBudget budget;
    private final int saveIntervalTicks;
    private final Logger log;
    private final Map<String, LogisticsDevice> byId = new LinkedHashMap<>();
    private final Map<String, LogisticsDevice> byBlock = new HashMap<>();
    private final Map<UUID, Built> networks = new HashMap<>();
    private final Map<UUID, LogisticsResult> results = new HashMap<>();
    private final Map<String, Double> satisfactionOf = new HashMap<>();
    private final Map<String, Long> lastMoved = new HashMap<>();
    private final Map<String, Long> lastPuff = new HashMap<>();
    private final Map<ItemKey, UUID> displays = new HashMap<>();
    private long tick;

    /** What a vertical tower finds at its output cell: a regular belt of this network, or the port of a machine or storage. */
    private final TowerNeighbours towerNeighbours = new TowerNeighbours() {
        @Override
        public boolean isRegularBelt(UUID world, Pos cell) {
            Built built = networks.get(world);
            return built != null && built.belts().contains(cell);
        }

        @Override
        public boolean putOnRegularBelt(UUID world, Pos cell, Item item) {
            Built built = networks.get(world);
            LogisticsDevice belt = byBlock.get(key(world, cell.x(), cell.y(), cell.z()));
            if (built == null || belt == null || belt.kind() != LogisticsIds.Kind.BELT || !built.belts().contains(cell)) {
                return false;
            }
            if (vertical.contains(world, belt.dir().step(belt.pos()))) {
                return false;
            }
            return built.network().insertAtEntry(cell, item);
        }

        @Override
        public Optional<Port> portAt(UUID world, Pos cell, UUID owner) {
            return ports.at(world, cell, owner, null);
        }
    };

    /**
     * @param core              the core, which builds the component items
     * @param machines          the lab tools, whose barrels are ports
     * @param energyDevices     the energy devices, whose generators are ports and which take cells
     * @param energy            the energy service, which gives the satisfaction of each device
     * @param holograms         the hologram service, for the pickers' and splitters' lines
     * @param refs              converts items to stacks
     * @param containers        the claim systems that decide whether a belt, picker or splitter may fill a protected container
     * @param vertical          the vertical belts: their towers take items from a belt that flows into them, and give items to the
     *                          cell beyond their output end
     * @param file              {@code logistics.yml}
     * @param maxDisplays       the most item displays on belts at once
     * @param saveIntervalTicks ticks between saves, at least 1
     * @param log               receives warnings
     */
    public LogisticsService(CoreFactoryCore core, MachineStore machines, DeviceRegistry energyDevices, EnergyService energy,
                            HologramService holograms, TurretRegistry turrets, ItemRefs refs, ContainerProtection containers,
                            VerticalService vertical, File file, int maxDisplays, int saveIntervalTicks, Logger log) {
        this.core = core;
        this.machines = machines;
        this.energyDevices = energyDevices;
        this.energy = energy;
        this.store = new LogisticsStore(file, log);
        this.entities = new LogisticsEntities(core, holograms, refs);
        this.ports = new LogisticsPorts(machines, energyDevices, turrets, refs, containers);
        this.vertical = vertical;
        this.budget = new DisplayBudget(maxDisplays);
        this.saveIntervalTicks = Math.max(1, saveIntervalTicks);
        this.log = log;
    }

    /** Reads the saved devices and items, and builds the networks. Call once, after the worlds are loaded. */
    public void load() {
        LogisticsFile.State saved = store.read();
        for (LogisticsFile.Belt belt : saved.belts()) {
            register(new LogisticsDevice(UUID.randomUUID().toString(), LogisticsIds.Kind.BELT, belt.world(), belt.pos(),
                    belt.level(), belt.flow(), belt.owner()));
        }
        for (LogisticsFile.Picker picker : saved.pickers()) {
            register(new LogisticsDevice(picker.id(), LogisticsIds.Kind.PICKER, picker.world(), picker.pos(), picker.level(),
                    picker.inDir(), picker.owner()));
        }
        for (LogisticsFile.Splitter splitter : saved.splitters()) {
            LogisticsDevice device = new LogisticsDevice(splitter.id(), LogisticsIds.Kind.SPLITTER, splitter.world(), splitter.pos(),
                    splitter.level(), splitter.inDir(), splitter.owner());
            try {
                device.setFilter(SplitterFilter.of(splitter.left(), splitter.right()));
                register(device);
            } catch (IllegalArgumentException e) {
                log.warning("A splitter in logistics.yml has a filter that is not valid and is skipped: " + e.getMessage());
            }
        }
        for (LogisticsFile.Duplex duplex : saved.duplexes()) {
            register(new LogisticsDevice(duplex.id(), LogisticsIds.Kind.DUPLEX, duplex.world(), duplex.pos(), duplex.level(),
                    duplex.flow(), duplex.owner()));
        }
        Set<UUID> worlds = new HashSet<>();
        for (LogisticsDevice device : byId.values()) {
            worlds.add(device.world());
        }
        for (UUID world : worlds) {
            rebuild(world);
        }
        int lost = 0;
        for (LogisticsFile.Carried carried : saved.items()) {
            Built built = networks.get(carried.world());
            if (built == null || !built.belts().contains(carried.pos())
                    || !built.network().insertAt(carried.pos(), carried.item(), carried.position())) {
                lost++;
            }
        }
        for (LogisticsFile.Splitter splitter : saved.splitters()) {
            if (splitter.held() == null) {
                continue;
            }
            Built built = networks.get(splitter.world());
            if (built == null || !built.holders().contains(splitter.id())
                    || !built.network().holdAt(splitter.id(), splitter.held())) {
                lost++;
            }
        }
        for (LogisticsFile.Duplex duplex : saved.duplexes()) {
            if (duplex.held() == null) {
                continue;
            }
            Built built = networks.get(duplex.world());
            if (built == null || !built.holders().contains(duplex.id())
                    || !built.network().holdAt(duplex.id(), duplex.held())) {
                lost++;
            }
        }
        if (lost > 0) {
            log.warning("Logistics: " + lost + " item(s) saved on a belt or splitter could not be put back and are not kept.");
        }
        vertical.load();
    }

    /** One server tick of the logistics. Call after the energy service has solved this tick. */
    public void tick() {
        tick++;
        if (tick % ENTITY_TICKS == 0) {
            ensureEntities();
        }
        for (Map.Entry<UUID, Built> entry : networks.entrySet()) {
            if (Bukkit.getWorld(entry.getKey()) == null) {
                continue;
            }
            try {
                tickWorld(entry.getKey(), entry.getValue());
            } catch (RuntimeException e) {
                log.severe("The logistics of a world failed this tick and will run again next tick: " + e);
            }
        }
        try {
            vertical.tick(towerNeighbours);
        } catch (RuntimeException e) {
            log.severe("The vertical belts failed this tick and will run again next tick: " + e);
        }
        if (tick % HOLOGRAM_TICKS == 0) {
            updateHolograms();
        }
        if (tick % saveIntervalTicks == 0) {
            save();
        }
    }

    /** The consumers of the belts, pickers and splitters in loaded chunks, for the energy solver. */
    @Override
    public Map<UUID, List<Consumer>> consumers() {
        Map<UUID, List<Consumer>> byWorld = new HashMap<>();
        for (LogisticsDevice device : byId.values()) {
            if (isLoaded(device)) {
                byWorld.computeIfAbsent(device.world(), key -> new ArrayList<>())
                        .add(new Consumer(consumerId(device), device.pos(), demandOf(device)));
            }
        }
        return byWorld;
    }

    /** Writes the state and releases every entity. Called when the plugin is disabled. */
    public void shutdown() {
        save();
        for (LogisticsDevice device : byId.values()) {
            entities.destroy(device);
        }
        vertical.shutdown();
        for (ItemKey key : List.copyOf(displays.keySet())) {
            removeDisplay(key);
        }
    }

    /** Writes the devices and the items on the belts to {@code logistics.yml}. */
    public void save() {
        List<LogisticsFile.Belt> belts = new ArrayList<>();
        List<LogisticsFile.Picker> pickers = new ArrayList<>();
        List<LogisticsFile.Splitter> splitters = new ArrayList<>();
        List<LogisticsFile.Duplex> duplexes = new ArrayList<>();
        List<LogisticsFile.Carried> items = new ArrayList<>();
        for (LogisticsDevice device : byId.values()) {
            switch (device.kind()) {
                case BELT -> {
                    belts.add(new LogisticsFile.Belt(device.world(), device.pos(), device.dir(), device.level(), device.owner()));
                    Built built = networks.get(device.world());
                    if (built != null) {
                        for (BeltItem view : built.network().itemsOn(device.pos())) {
                            items.add(new LogisticsFile.Carried(device.world(), device.pos(), view.item(), view.position()));
                        }
                    }
                }
                case PICKER -> pickers.add(new LogisticsFile.Picker(device.id(), device.world(), device.pos(), device.dir(),
                        device.level(), device.owner()));
                case SPLITTER -> {
                    Built built = networks.get(device.world());
                    Item held = built == null ? null : built.network().heldBy(device.id()).orElse(null);
                    splitters.add(new LogisticsFile.Splitter(device.id(), device.world(), device.pos(), device.dir(),
                            device.level(), device.filter().items(SplitterFilter.Side.LEFT),
                            device.filter().items(SplitterFilter.Side.RIGHT), held, device.owner()));
                }
                case DUPLEX -> {
                    Built built = networks.get(device.world());
                    Item held = built == null ? null : built.network().heldBy(device.id()).orElse(null);
                    duplexes.add(new LogisticsFile.Duplex(device.id(), device.world(), device.pos(), device.dir(), device.level(),
                            held, device.owner()));
                }
            }
        }
        store.write(new LogisticsFile.State(belts, pickers, splitters, duplexes, items));
        vertical.save();
    }

    /** The device whose block is at this location, if any. */
    public Optional<LogisticsDevice> at(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byBlock.get(key(location.getWorld().getUID(), location.getBlockX(), location.getBlockY(),
                location.getBlockZ())));
    }

    /** The device whose interaction entity this is, if any. */
    public Optional<LogisticsDevice> byInteraction(UUID entity) {
        return byId.values().stream().filter(device -> entity.equals(device.interaction())).findFirst();
    }

    /**
     * What a picker or a splitter placed on the face {@code toward} of a cell takes from: a belt at the cell that flows into that
     * face or flows past its side (a straight belt), a machine or a generator at the cell, or nothing.
     *
     * @param cell   the aimed block
     * @param toward the direction from the aimed block to the new device, or null for a top or bottom face
     */
    public SidePlacement.Input inputAt(Location cell, Dir toward) {
        Optional<LogisticsDevice> device = at(cell);
        if (toward != null && device.isPresent() && device.get().kind() == LogisticsIds.Kind.BELT) {
            Built built = networks.get(device.get().world());
            boolean straight = built != null && built.network().shapeOf(device.get().pos()) == BeltShape.STRAIGHT;
            SidePlacement.Input belt = SidePlacement.beltInput(device.get().dir(), toward, straight);
            if (belt != SidePlacement.Input.OTHER) {
                return belt;
            }
        }
        if (machines.at(cell).isPresent() || energyDevices.at(cell).filter(GeneratorDevice.class::isInstance).isPresent()) {
            return SidePlacement.Input.PORT;
        }
        return SidePlacement.Input.OTHER;
    }

    /** What stands in the spot a device would fill: a device or a machine, a solid block, an entity, or nothing. */
    public SidePlacement.Space spaceAt(Location spot) {
        if (at(spot).isPresent() || energyDevices.at(spot).isPresent() || machines.at(spot).isPresent()) {
            return SidePlacement.Space.DEVICE;
        }
        Block block = spot.getBlock();
        if (!block.isEmpty() && !block.isLiquid() && !block.isPassable()) {
            return SidePlacement.Space.SOLID;
        }
        boolean entities = !block.getWorld().getNearbyEntities(block.getBoundingBox(), entity -> !(entity instanceof Display))
                .isEmpty();
        return entities ? SidePlacement.Space.ENTITY : SidePlacement.Space.FREE;
    }

    /** Every device. */
    public List<LogisticsDevice> all() {
        return List.copyOf(byId.values());
    }

    /** The chunk of every cell of every belt, picker and splitter. A duplex's second cell can lie in another chunk, so it counts too. */
    public Set<ChunkKey> chunkKeys() {
        Set<ChunkKey> keys = new HashSet<>();
        for (LogisticsDevice device : all()) {
            for (Pos cell : device.cells()) {
                keys.add(ChunkKey.of(device.world(), cell.x(), cell.z()));
            }
        }
        return keys;
    }

    /** Whether every chunk a device's cells stand in is loaded, so its entities exist and its items move. */
    public boolean isLoaded(LogisticsDevice device) {
        World world = Bukkit.getWorld(device.world());
        if (world == null) {
            return false;
        }
        for (Pos cell : device.cells()) {
            if (!world.isChunkLoaded(cell.x() >> 4, cell.z() >> 4)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a device of a kind can be placed at a cell, facing a direction. A belt's direction is its flow; a picker's and a
     * splitter's is the direction from the device to its input.
     */
    public PlacementReason check(LogisticsIds.Kind kind, UUID world, Pos pos, Dir dir) {
        List<BeltUnit> units = unitsOf(world);
        Endpoints endpoints = endpointsOf(world);
        return switch (kind) {
            case BELT -> Placement.canPlace(pos, dir, units, endpoints);
            case PICKER -> Placement.canPlacePicker(
                    new Picker(UUID.randomUUID().toString(), pos, dir, dir.opposite(), LogisticsLevel.I), units, endpoints);
            case SPLITTER -> Placement.canPlaceSplitter(
                    new Splitter(UUID.randomUUID().toString(), pos, dir, Set.of(), Set.of(), LogisticsLevel.I),
                    units, endpoints);
            case DUPLEX -> Placement.canPlaceDuplex(
                    new Duplex(UUID.randomUUID().toString(), pos, dir, LogisticsLevel.I), units, endpoints);
        };
    }

    /**
     * What a belt duplex placed at {@code anchor}, flowing {@code flow}, takes its input from: a belt flowing into the back of one of its
     * two cells, {@code BELT_INTO}; otherwise {@code OTHER}. A duplex is refused without one (see {@link SidePlacement}).
     */
    public SidePlacement.Input duplexInputAt(Location anchor, Dir flow) {
        if (anchor.getWorld() == null) {
            return SidePlacement.Input.OTHER;
        }
        Pos anchorCell = new Pos(anchor.getBlockX(), anchor.getBlockY(), anchor.getBlockZ());
        for (Pos cell : List.of(anchorCell, Duplex.secondCell(anchorCell, flow))) {
            Pos back = flow.opposite().step(cell);
            Optional<LogisticsDevice> belt = at(new Location(anchor.getWorld(), back.x(), back.y(), back.z()));
            if (belt.isPresent() && belt.get().kind() == LogisticsIds.Kind.BELT && belt.get().dir() == flow) {
                return SidePlacement.Input.BELT_INTO;
            }
        }
        return SidePlacement.Input.OTHER;
    }

    /**
     * Adds a device that {@link #check} accepted, shows it and saves.
     *
     * @param owner the id of the player who placed it, or null when none is known
     * @return the device
     */
    public LogisticsDevice add(LogisticsIds.Kind kind, UUID world, Pos pos, Dir dir, LogisticsLevel level, UUID owner) {
        LogisticsDevice device = new LogisticsDevice(UUID.randomUUID().toString(), kind, world, pos, level, dir, owner);
        if (kind == LogisticsIds.Kind.SPLITTER) {
            device.setFilter(SplitterFilter.empty());
        }
        register(device);
        rebuild(world);
        ensureEntities();
        save();
        return device;
    }

    /**
     * Removes a device, with its entities and its item displays, and rebuilds its world's network. The items on a belt and the item
     * a splitter holds are returned as stacks to drop, with the device's own item. The filter is not kept.
     *
     * @return the stacks to drop at the device's block
     */
    public List<ItemStack> remove(LogisticsDevice device) {
        List<ItemStack> drops = new ArrayList<>();
        Built built = networks.get(device.world());
        if (built != null) {
            if (device.kind() == LogisticsIds.Kind.BELT) {
                for (long serial : built.network().serialsOn(device.pos())) {
                    removeDisplay(new ItemKey(device.world(), serial));
                }
                for (BeltItem view : built.network().itemsOn(device.pos())) {
                    entities.stackOf(view.item()).ifPresent(drops::add);
                }
            } else if (device.kind() == LogisticsIds.Kind.SPLITTER || device.kind() == LogisticsIds.Kind.DUPLEX) {
                built.network().heldBy(device.id()).flatMap(entities::stackOf).ifPresent(drops::add);
            }
        }
        entities.deviceItem(device.kind(), device.level()).ifPresent(drops::add);
        entities.destroy(device);
        unregister(device);
        satisfactionOf.remove(device.id());
        lastMoved.remove(device.id());
        lastPuff.remove(device.id());
        rebuild(device.world());
        save();
        return drops;
    }

    /** Sets a splitter's filter, rebuilds its world's network with the same items, and saves. */
    public void setFilter(LogisticsDevice device, SplitterFilter filter) {
        device.setFilter(filter);
        rebuild(device.world());
        save();
    }

    /** A chunk is unloading: its devices forget their entities, and the items shown on its belts are removed. The data stays. */
    public void unloaded(Chunk chunk) {
        for (LogisticsDevice device : byId.values()) {
            if (!inChunk(device, chunk)) {
                continue;
            }
            device.setDisplay(null);
            device.setSecondDisplay(null);
            device.setInteraction(null);
            entities.release(device);
            Built built = networks.get(device.world());
            if (built != null && device.kind() == LogisticsIds.Kind.BELT) {
                for (long serial : built.network().serialsOn(device.pos())) {
                    removeDisplay(new ItemKey(device.world(), serial));
                }
            }
        }
    }

    /** A chunk has loaded: its marked entities that nothing owns are removed. */
    public void sweep(Chunk chunk) {
        Set<UUID> owned = new HashSet<>();
        for (LogisticsDevice device : byId.values()) {
            if (device.display() != null) {
                owned.add(device.display());
            }
            if (device.secondDisplay() != null) {
                owned.add(device.secondDisplay());
            }
            if (device.interaction() != null) {
                owned.add(device.interaction());
            }
        }
        entities.sweep(chunk, owned, new HashSet<>(displays.values()));
    }

    private void tickWorld(UUID world, Built built) {
        List<LogisticsDevice> here = devicesIn(world);
        List<SatisfactionMap.Node> nodes = new ArrayList<>();
        Set<Pos> occupied = new HashSet<>();
        for (LogisticsDevice device : here) {
            occupied.addAll(device.cells());
            nodes.add(new SatisfactionMap.Node(nodeId(device), consumerId(device), isLoaded(device)));
        }
        Map<String, Double> satisfaction = SatisfactionMap.build(nodes, id -> energy.satisfaction(id));
        for (LogisticsDevice device : here) {
            satisfactionOf.put(device.id(), satisfaction.getOrDefault(nodeId(device), 0.0));
        }
        Map<Pos, UUID> owners = deliveryOwners(here);
        Map<Pos, UUID> takers = takeOwners(here);
        Map<Pos, Port> portMap = new HashMap<>();
        for (LogisticsDevice device : here) {
            for (Pos cell : cellsOf(device)) {
                if (!occupied.contains(cell) && !portMap.containsKey(cell)) {
                    Optional<Port> tower = vertical.inputPort(world, cell);
                    (tower.isPresent() ? tower : ports.at(world, cell, owners.get(cell), takers.get(cell)))
                            .ifPresent(port -> portMap.put(cell, port));
                }
            }
        }
        LogisticsResult result = built.network().tick(TICK_SECONDS, satisfaction, portMap);
        results.put(world, result);
        showMoves(world, here, result);
        updateItemDisplays(world, built, here);
    }

    /** The pickers that moved an item this tick are marked, and a faint puff shows at their output for players nearby. */
    private void showMoves(UUID world, List<LogisticsDevice> here, LogisticsResult result) {
        for (LogisticsDevice device : here) {
            if (device.kind() != LogisticsIds.Kind.PICKER || result.movedBy(nodeId(device)) <= 0) {
                continue;
            }
            lastMoved.put(device.id(), tick);
            if (tick - lastPuff.getOrDefault(device.id(), Long.MIN_VALUE / 2) < PUFF_TICKS) {
                continue;
            }
            lastPuff.put(device.id(), tick);
            World loaded = Bukkit.getWorld(world);
            if (loaded == null) {
                continue;
            }
            Pos output = device.dir().opposite().step(device.pos());
            Location cell = new Location(loaded, output.x() + 0.5, output.y() + 0.5, output.z() + 0.5);
            for (Player player : loaded.getPlayers()) {
                if (player.getLocation().distanceSquared(cell) <= PUFF_RANGE_SQUARED) {
                    player.spawnParticle(Particle.CRIT, cell, PUFF_COUNT, 0.15, 0.15, 0.15, 0.0);
                }
            }
        }
    }

    /**
     * Moves the displays of the items on the belts of a world, and creates the displays of new items while the cap allows. A display
     * that is gone is made again. Displays of items that left the network are removed.
     */
    private void updateItemDisplays(UUID world, Built built, List<LogisticsDevice> here) {
        Set<ItemKey> live = new HashSet<>();
        for (LogisticsDevice device : here) {
            if (device.kind() != LogisticsIds.Kind.BELT) {
                continue;
            }
            List<BeltItem> views = built.network().itemsOn(device.pos());
            List<Long> serials = built.network().serialsOn(device.pos());
            Dir input = device.input() == null ? device.dir().opposite() : device.input();
            for (int i = 0; i < views.size(); i++) {
                ItemKey key = new ItemKey(world, serials.get(i));
                live.add(key);
                if (!isLoaded(device)) {
                    continue;
                }
                double[] offset = BeltPath.offsetOn(device.shape(), device.dir(), input, views.get(i).position());
                UUID shown = displays.get(key);
                if (shown != null) {
                    if (!entities.moveItem(shown, offset)) {
                        displays.remove(key);
                        budget.giveBack();
                    }
                    continue;
                }
                showItem(key, device, views.get(i).item(), offset);
            }
        }
        for (ItemKey key : new ArrayList<>(displays.keySet())) {
            if (key.world().equals(world) && !live.contains(key)) {
                removeDisplay(key);
            }
        }
    }

    private void showItem(ItemKey key, LogisticsDevice device, Item item, double[] offset) {
        Optional<ItemStack> stack = entities.stackOf(item);
        if (stack.isEmpty()) {
            return;
        }
        DisplayBudget.Take take = budget.take();
        if (take == DisplayBudget.Take.REFUSED_WARN) {
            log.warning("Logistics: the item displays on belts are capped at " + budget.cap() + " (logistics.max-item-displays). "
                    + "More items are carried but not shown until some leave the belts.");
            return;
        }
        if (take == DisplayBudget.Take.TAKEN) {
            Location block = blockOf(device);
            displays.put(key, entities.spawnItem(block, stack.get(), offset));
        }
    }

    private void removeDisplay(ItemKey key) {
        UUID display = displays.remove(key);
        if (display != null) {
            LogisticsEntities.remove(display);
            budget.giveBack();
        }
    }

    /** Makes the missing entities of every device in a loaded chunk: its display, its interaction and, for a picker or splitter, its hologram. */
    private void ensureEntities() {
        for (LogisticsDevice device : byId.values()) {
            if (!isLoaded(device)) {
                continue;
            }
            Location block = blockOf(device);
            if (device.kind() == LogisticsIds.Kind.DUPLEX) {
                if (isMissing(device.display())) {
                    device.setDisplay(spawnDuplex(device, false));
                }
                if (isMissing(device.secondDisplay())) {
                    device.setSecondDisplay(spawnDuplex(device, true));
                }
            } else if (isMissing(device.display())) {
                device.setDisplay(spawnDisplay(device, block));
            }
            if (isMissing(device.interaction())) {
                device.setInteraction(entities.spawnInteraction(block, device.id()));
            }
            boolean sided = device.kind() == LogisticsIds.Kind.PICKER || device.kind() == LogisticsIds.Kind.SPLITTER;
            if (sided && device.hologram() == null) {
                entities.hologram(block, linesOf(device)).ifPresent(device::setHologram);
            }
        }
    }

    private UUID spawnDisplay(LogisticsDevice device, Location block) {
        if (device.kind() == LogisticsIds.Kind.BELT) {
            Optional<ItemStack> item = beltItem(device);
            if (item.isEmpty()) {
                return null;
            }
            Dir input = device.input() == null ? device.dir().opposite() : device.input();
            return entities.spawnDevice(block, item.get(), LogisticsEntities.beltTransform(device.dir(), device.shape(), input),
                    device.id());
        }
        Optional<ItemStack> item = entities.machineItem(device.level(), device.kind());
        if (item.isEmpty()) {
            return null;
        }
        Dir northSide = device.kind() == LogisticsIds.Kind.PICKER ? device.dir() : device.dir().opposite();
        float scale = core.settings().displayScale();
        return entities.spawnDevice(block, item.get(), LogisticsEntities.machineTransform(northSide, scale), device.id());
    }

    /** The display of one half of a belt duplex: the anchor cell's half, or the second cell's. Null when its item is not defined. */
    private UUID spawnDuplex(LogisticsDevice device, boolean second) {
        Optional<ItemStack> item = entities.duplexItem(device.level(), second);
        if (item.isEmpty()) {
            return null;
        }
        Pos cell = second ? device.cells().get(1) : device.pos();
        Location block = new Location(Bukkit.getWorld(device.world()), cell.x(), cell.y(), cell.z());
        return entities.spawnDevice(block, item.get(), LogisticsEntities.duplexTransform(device.dir()), device.id());
    }

    /** True when an entity id is not set, or names an entity that no longer exists. */
    private static boolean isMissing(UUID entity) {
        return entity == null || Bukkit.getEntity(entity) == null;
    }

    private void updateHolograms() {
        for (LogisticsDevice device : byId.values()) {
            if (device.hologram() != null && isLoaded(device)) {
                device.hologram().setLines(linesOf(device));
            }
        }
    }

    private List<String> linesOf(LogisticsDevice device) {
        boolean moved = tick - lastMoved.getOrDefault(device.id(), Long.MIN_VALUE / 2) <= MOVE_WINDOW_TICKS;
        LogisticsResult result = results.get(device.world());
        boolean blocked = result != null && result.isBlocked(nodeId(device));
        if (device.kind() == LogisticsIds.Kind.PICKER) {
            double rate = LogisticsTable.pickerItemsPerSecond(device.level()) * satisfactionOf.getOrDefault(device.id(), 0.0);
            return LogisticsText.pickerLines(device.level(), demandOf(device),
                    LogisticsText.pickerStatus(moved, blocked, rate));
        }
        return LogisticsText.splitterLines(device.level(), demandOf(device), device.filter().items(SplitterFilter.Side.LEFT).size(),
                device.filter().items(SplitterFilter.Side.RIGHT).size(), LogisticsText.splitterStatus(moved, blocked));
    }

    private Optional<ItemStack> beltItem(LogisticsDevice device) {
        return entities.beltItem(device.level(), device.shape());
    }

    /** Refreshes the shape and input side of every belt in a world, and the display of each that changed. */
    private void refreshBelts(UUID world) {
        Built built = networks.get(world);
        if (built == null) {
            return;
        }
        for (LogisticsDevice device : devicesIn(world)) {
            if (device.kind() != LogisticsIds.Kind.BELT) {
                continue;
            }
            BeltShape shape = built.network().shapeOf(device.pos());
            Dir input = built.network().inputSideOf(device.pos());
            if (shape == device.shape() && input == device.input()) {
                continue;
            }
            device.setShape(shape);
            device.setInput(input);
            if (device.display() != null && Bukkit.getEntity(device.display()) != null) {
                Optional<ItemStack> item = beltItem(device);
                item.ifPresent(stack -> entities.refresh(device.display(), stack,
                        LogisticsEntities.beltTransform(device.dir(), device.shape(), input)));
            }
        }
    }

    /**
     * Rebuilds a world's network from its devices. The items it carried are put back on the new network at their places, and the
     * displays of its items are made again. A device the network refuses is removed and reported.
     */
    private void rebuild(UUID world) {
        clearDisplays(world);
        Built previous = networks.get(world);
        Built next = build(world, previous);
        if (next == null) {
            networks.remove(world);
            results.remove(world);
        } else {
            networks.put(world, next);
        }
        refreshBelts(world);
    }

    private Built build(UUID world, Built previous) {
        List<LogisticsDevice> here = devicesIn(world);
        if (here.isEmpty()) {
            return null;
        }
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder();
        List<LogisticsDevice> refused = new ArrayList<>();
        for (LogisticsDevice device : here) {
            try {
                addNode(builder, device);
            } catch (IllegalArgumentException e) {
                refused.add(device);
                log.warning("A " + LogisticsText.name(device.kind(), device.level()) + " at " + device.pos().x() + " "
                        + device.pos().y() + " " + device.pos().z() + " is refused by its network and is removed: "
                        + e.getMessage());
            }
        }
        int dropped = 0;
        for (LogisticsDevice device : refused) {
            dropped += itemsOf(previous, device);
            entities.destroy(device);
            unregister(device);
        }
        if (refused.size() == here.size()) {
            log.warning("Logistics: every device of a world is refused by its network, so the network is removed"
                    + (dropped > 0 ? " and " + dropped + " item(s) on it are lost." : "."));
            return null;
        }
        if (dropped > 0) {
            log.warning("Logistics: " + dropped + " item(s) on the refused devices were lost.");
        }
        Set<Pos> belts = new HashSet<>();
        Set<String> holders = new HashSet<>();
        for (LogisticsDevice device : devicesIn(world)) {
            if (device.kind() == LogisticsIds.Kind.BELT) {
                belts.add(device.pos());
            } else if (device.kind() == LogisticsIds.Kind.SPLITTER || device.kind() == LogisticsIds.Kind.DUPLEX) {
                holders.add(device.id());
            }
        }
        Built built = new Built(builder.build(), belts, holders);
        if (previous != null) {
            carry(previous, built);
        }
        return built;
    }

    /** The items a device holds in a network: the items on a belt, or the one a splitter holds. Zero when there is no network. */
    private static int itemsOf(Built network, LogisticsDevice device) {
        if (network == null) {
            return 0;
        }
        if (device.kind() == LogisticsIds.Kind.BELT) {
            return network.network().itemsOn(device.pos()).size();
        }
        if (device.kind() == LogisticsIds.Kind.SPLITTER || device.kind() == LogisticsIds.Kind.DUPLEX) {
            return network.network().heldBy(device.id()).isPresent() ? 1 : 0;
        }
        return 0;
    }

    private void carry(Built from, Built to) {
        int lost = 0;
        for (Pos belt : to.belts()) {
            if (!from.belts().contains(belt)) {
                continue;
            }
            for (BeltItem view : from.network().itemsOn(belt)) {
                if (!to.network().insertAt(belt, view.item(), view.position())) {
                    lost++;
                }
            }
        }
        for (String id : to.holders()) {
            if (!from.holders().contains(id)) {
                continue;
            }
            Optional<Item> held = from.network().heldBy(id);
            if (held.isPresent() && !to.network().holdAt(id, held.get())) {
                lost++;
            }
        }
        if (lost > 0) {
            log.warning("Logistics: " + lost + " item(s) could not be carried over to the rebuilt network and were lost.");
        }
    }

    private static void addNode(LogisticsNetwork.Builder builder, LogisticsDevice device) {
        switch (device.kind()) {
            case BELT -> builder.belt(device.pos(), device.dir(), device.level());
            case PICKER -> builder.picker(device.id(), device.pos(), device.dir(), device.dir().opposite(), device.level());
            case SPLITTER -> builder.splitter(device.id(), device.pos(), device.dir(), device.filter().leftSet(),
                    device.filter().rightSet(), device.level());
            case DUPLEX -> builder.duplex(device.id(), device.pos(), device.dir(), device.level());
        }
    }

    private void clearDisplays(UUID world) {
        for (ItemKey key : new ArrayList<>(displays.keySet())) {
            if (key.world().equals(world)) {
                removeDisplay(key);
            }
        }
    }

    private void register(LogisticsDevice device) {
        byId.put(device.id(), device);
        for (Pos cell : device.cells()) {
            byBlock.put(device.blockKey(cell), device);
        }
    }

    /** Forgets a device: its id and every block key it occupies. */
    private void unregister(LogisticsDevice device) {
        byId.remove(device.id());
        for (Pos cell : device.cells()) {
            byBlock.remove(device.blockKey(cell));
        }
    }

    private List<LogisticsDevice> devicesIn(UUID world) {
        List<LogisticsDevice> here = new ArrayList<>();
        for (LogisticsDevice device : byId.values()) {
            if (device.world().equals(world)) {
                here.add(device);
            }
        }
        return here;
    }

    private List<BeltUnit> unitsOf(UUID world) {
        List<BeltUnit> units = new ArrayList<>();
        for (LogisticsDevice device : devicesIn(world)) {
            if (device.kind() == LogisticsIds.Kind.BELT) {
                units.add(new BeltUnit(device.pos(), device.dir(), device.level()));
            }
        }
        return units;
    }

    /**
     * The cells a device gives to or takes from: a belt its output, a picker both sides, a splitter its three outputs, a belt duplex its
     * two outputs.
     */
    private static List<Pos> cellsOf(LogisticsDevice device) {
        return switch (device.kind()) {
            case BELT -> List.of(device.dir().step(device.pos()));
            case PICKER -> List.of(device.dir().step(device.pos()), device.dir().opposite().step(device.pos()));
            case SPLITTER -> List.of(device.dir().step(device.pos()), device.dir().turnLeft().step(device.pos()),
                    device.dir().turnRight().step(device.pos()));
            case DUPLEX -> duplexOutputs(device);
        };
    }

    /**
     * The cells a device delivers into: a belt its output, a picker its output side, a splitter its three outputs, a belt duplex its two
     * outputs.
     */
    private static List<Pos> outputsOf(LogisticsDevice device) {
        return switch (device.kind()) {
            case BELT -> List.of(device.dir().step(device.pos()));
            case PICKER -> List.of(device.dir().opposite().step(device.pos()));
            case SPLITTER -> List.of(device.dir().step(device.pos()), device.dir().turnLeft().step(device.pos()),
                    device.dir().turnRight().step(device.pos()));
            case DUPLEX -> duplexOutputs(device);
        };
    }

    /** The two front cells of a belt duplex: each cell of the duplex, one step along its flow. */
    private static List<Pos> duplexOutputs(LogisticsDevice device) {
        return device.cells().stream().map(cell -> device.dir().step(cell)).toList();
    }

    /**
     * The owner a container cell is delivered for: the owner of the devices that deliver into it. When those devices have different
     * owners, or one of them has none, the cell gets a null owner, which a protected container refuses. A cell no device delivers into
     * has no entry.
     */
    private static Map<Pos, UUID> deliveryOwners(List<LogisticsDevice> devices) {
        return ownersOf(devices, LogisticsService::outputsOf);
    }

    /**
     * The owner a container cell is taken for: the owner of the pickers that take from it, by the rule of {@link #deliveryOwners}. A
     * cell no picker takes from has no entry.
     */
    private static Map<Pos, UUID> takeOwners(List<LogisticsDevice> devices) {
        return ownersOf(devices, device -> device.kind() == LogisticsIds.Kind.PICKER
                ? List.of(device.dir().step(device.pos())) : List.of());
    }

    /**
     * The owner of each cell the devices use, as {@code cellsOf} names them. When the devices on a cell have different owners, or one of
     * them has none, the cell gets a null owner, which a protected container refuses.
     */
    private static Map<Pos, UUID> ownersOf(List<LogisticsDevice> devices, Function<LogisticsDevice, List<Pos>> cellsOf) {
        Map<Pos, UUID> owners = new HashMap<>();
        for (LogisticsDevice device : devices) {
            for (Pos cell : cellsOf.apply(device)) {
                if (!owners.containsKey(cell)) {
                    owners.put(cell, device.owner());
                } else if (!Objects.equals(owners.get(cell), device.owner())) {
                    owners.put(cell, null);
                }
            }
        }
        return owners;
    }

    /** The cells that hold something other than a logistics device: a lab tool, a drill, an energy device. */
    private Endpoints endpointsOf(UUID world) {
        List<Picker> pickers = new ArrayList<>();
        List<Splitter> splitters = new ArrayList<>();
        List<Duplex> duplexes = new ArrayList<>();
        for (LogisticsDevice device : devicesIn(world)) {
            if (device.kind() == LogisticsIds.Kind.PICKER) {
                pickers.add(new Picker(device.id(), device.pos(), device.dir(), device.dir().opposite(), device.level()));
            } else if (device.kind() == LogisticsIds.Kind.SPLITTER) {
                splitters.add(new Splitter(device.id(), device.pos(), device.dir(), device.filter().leftSet(),
                        device.filter().rightSet(), device.level()));
            } else if (device.kind() == LogisticsIds.Kind.DUPLEX) {
                duplexes.add(new Duplex(device.id(), device.pos(), device.dir(), device.level()));
            }
        }
        Set<Pos> taken = new HashSet<>();
        for (PlacedMachine placed : machines.all()) {
            Location block = placed.block();
            if (block.getWorld() != null && block.getWorld().getUID().equals(world)) {
                taken.add(new Pos(block.getBlockX(), block.getBlockY(), block.getBlockZ()));
            }
        }
        for (EnergyDevice device : energyDevices.all()) {
            if (device.world().equals(world)) {
                taken.add(device.pos());
            }
        }
        return new Endpoints(pickers, splitters, duplexes, taken);
    }

    private double demandOf(LogisticsDevice device) {
        return switch (device.kind()) {
            case BELT -> LogisticsTable.beltPowerW(device.level());
            case PICKER -> LogisticsTable.pickerPowerW(device.level());
            case SPLITTER -> LogisticsTable.splitterPowerW(device.level());
            case DUPLEX -> LogisticsTable.duplexPowerW(device.level());
        };
    }

    private static String nodeId(LogisticsDevice device) {
        return switch (device.kind()) {
            case BELT -> LogisticsNetwork.beltId(device.pos());
            case PICKER -> LogisticsNetwork.pickerId(device.id());
            case SPLITTER -> LogisticsNetwork.splitterId(device.id());
            case DUPLEX -> LogisticsNetwork.duplexId(device.id());
        };
    }

    private static String consumerId(LogisticsDevice device) {
        return EnergySnapshots.consumerId(device.world(), device.pos().x(), device.pos().y(), device.pos().z());
    }

    /** True when a device's world is the chunk's and one of its cells stands in the chunk. */
    private static boolean inChunk(LogisticsDevice device, Chunk chunk) {
        if (!device.world().equals(chunk.getWorld().getUID())) {
            return false;
        }
        for (Pos cell : device.cells()) {
            if (cell.x() >> 4 == chunk.getX() && cell.z() >> 4 == chunk.getZ()) {
                return true;
            }
        }
        return false;
    }

    private static Location blockOf(LogisticsDevice device) {
        return new Location(Bukkit.getWorld(device.world()), device.pos().x(), device.pos().y(), device.pos().z());
    }

    private static String key(UUID world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }
}
