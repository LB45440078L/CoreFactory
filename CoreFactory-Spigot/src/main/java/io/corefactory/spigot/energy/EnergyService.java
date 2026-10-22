package io.corefactory.spigot.energy;

import io.corefactory.core.config.Messages;
import io.corefactory.core.energy.Accumulator;
import io.corefactory.core.energy.AccumulatorResult;
import io.corefactory.core.energy.ChartHistory;
import io.corefactory.core.energy.ChartMode;
import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.ConsumerResult;
import io.corefactory.core.energy.DeviceKind;
import io.corefactory.core.energy.EnergyNetwork;
import io.corefactory.core.energy.EnergyResult;
import io.corefactory.core.energy.EnergySnapshot;
import io.corefactory.core.energy.EnergyTable;
import io.corefactory.core.energy.GeneratorEnvironment;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.energy.GeneratorOutput;
import io.corefactory.core.energy.GridPalette;
import io.corefactory.core.energy.GridRegistry;
import io.corefactory.core.energy.GridResult;
import io.corefactory.core.energy.OpenAir;
import io.corefactory.core.energy.OutlineTracer;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.energy.Pylon;
import io.corefactory.core.energy.Wire;
import io.corefactory.core.energy.WirePath;
import io.corefactory.spigot.audio.MachineSounds;
import io.corefactory.spigot.energy.StoredEnergy.StoredAccumulator;
import io.corefactory.spigot.energy.StoredEnergy.StoredChart;
import io.corefactory.spigot.energy.StoredEnergy.StoredPylon;
import io.corefactory.spigot.energy.StoredEnergy.StoredWire;
import io.corefactory.spigot.machine.MachineStore;
import io.corefactory.spigot.machine.PlacedMachine;
import io.corefactory.spigot.water.WaterFeed;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Runs the energy network once per server tick. Each tick the devices are read into a snapshot per world on the main thread, the
 * pure solver runs on it, and the result is kept: the reading of every lab tool and drill, the grid readings and their storage, the
 * new state of every accumulator, and the grid ids. The tick order is: the registry is refreshed every 20 ticks, every loaded
 * generator ticks, the solver runs, every second each grid's history takes a sample, the accumulator holograms take the new state,
 * every 10 ticks the generator and pylon holograms, menus and particles are updated, every 5 ticks the wires are drawn, the wire starts
 * time out, and
 * the file is saved every {@code save-interval-ticks}. It is registered before the machine ticker, so the machines read this tick's
 * satisfaction.
 *
 * <p>The network is solved per world, since a pylon's range does not cross worlds. Grid ids are one id space for all worlds: the
 * registry is threaded through the worlds in order, so no id is issued twice. A generator joins the network through its wires, with
 * the output it gave this tick.
 */
public final class EnergyService {

    /** The item of the energy wire, which is not a device. */
    public static final String WIRE_ITEM = "energy_wire";

    /** Something that takes power and is not a lab tool or a drill, such as the logistics devices. */
    public interface ConsumerSource {

        /** The consumers of each world this tick, from the loaded devices only. */
        Map<UUID, List<Consumer>> consumers();
    }

    /**
     * What the network gave a lab tool or drill at its last solve.
     *
     * @param satisfaction the share of its demand it received, 0 to 1
     * @param voltageV     the voltage of its grid, in V
     * @param demandW      the power it asked for, in W
     */
    public record Reading(double satisfaction, double voltageV, double demandW) {

        /** A machine the solver did not see: nothing received, no voltage, no demand. */
        public static final Reading NONE = new Reading(0.0, 0.0, 0.0);
    }

    private static final double TICK_SECONDS = EnergyTable.SECONDS_PER_TICK;
    private static final int REGISTRY_TICKS = 20;
    private static final int SAMPLE_TICKS = EnergyTable.TICKS_PER_SECOND;
    /** Ticks between the redraws of the wires: a pulse moves a little at each redraw, so the redraw is quick. */
    private static final int WIRE_TICKS = 5;
    private static final int FLASH_TICKS = 40;

    /** The height of a grid's outline above its lowest pylon's block, in blocks. */
    private static final double OUTLINE_HEIGHT = 0.3;

    /** The distance between two particles of a grid's outline, in blocks. */
    private static final double OUTLINE_SPACING = 0.5;

    private final MachineStore machines;
    private final DeviceRegistry devices;
    private final EnergyStore store;
    private final EnergyEffects effects;
    private final WireSelection selection = new WireSelection();
    private final List<StoredWire> wires = new ArrayList<>();
    private final List<StoredChart> charts = new ArrayList<>();
    private final List<ConsumerSource> sources = new ArrayList<>();
    private final Map<Integer, ChartHistory> histories = new HashMap<>();
    private final int saveIntervalTicks;
    private final int labDemandW;
    private final int drillDemandW;
    private final Logger log;
    private GridRegistry grids = GridRegistry.EMPTY;
    private Map<String, ConsumerResult> consumerResults = Map.of();
    private Map<Integer, GridResult> gridResults = Map.of();
    private Map<Integer, ChartSamples.Storage> storage = Map.of();
    private Map<UUID, List<GeneratorOutput>> generatorOutputs = Map.of();
    private WaterFeed water = WaterFeed.NONE;
    private long tick;

    /**
     * @param machines          the lab tools and drills, the consumers
     * @param devices           the pylons and accumulators
     * @param store             the energy file
     * @param effects           the visuals
     * @param saveIntervalTicks ticks between saves, at least 1
     * @param labDemandW        the power a lab tool asks for, in W
     * @param drillDemandW      the power a drill asks for when its level is unknown, in W
     * @param log               receives warnings
     */
    public EnergyService(MachineStore machines, DeviceRegistry devices, EnergyStore store, EnergyEffects effects,
                         int saveIntervalTicks, int labDemandW, int drillDemandW, Logger log) {
        this.machines = machines;
        this.devices = devices;
        this.store = store;
        this.effects = effects;
        this.saveIntervalTicks = Math.max(1, saveIntervalTicks);
        this.labDemandW = labDemandW;
        this.drillDemandW = drillDemandW;
        this.log = log;
    }

    /**
     * Reads the saved devices, wires, charts and grid ids, and removes the entities of an earlier run. Call once, after the worlds are
     * loaded.
     */
    public void load() {
        StoredEnergy saved = store.read();
        devices.sweepLoaded();
        devices.load(saved);
        wires.clear();
        wires.addAll(saved.wires());
        charts.clear();
        charts.addAll(saved.charts());
        grids = EnergySnapshots.grids(saved.pylons(), saved.nextGridId());
        devices.ensureEntities();
    }

    /** One server tick of the network. */
    public void tick() {
        tick++;
        if (tick % REGISTRY_TICKS == 0) {
            devices.materializePending();
            devices.ensureEntities();
        }
        generatorOutputs = tickGenerators();
        solve();
        if (tick % SAMPLE_TICKS == 0) {
            sample();
        }
        for (EnergyDevice device : devices.all()) {
            if (device instanceof AccumulatorDevice accumulator && accumulator.hologram() != null) {
                accumulator.hologram().setLines(EnergyText.accumulator(accumulator.level(), accumulator.stored(),
                        accumulator.accumulatorLevel().capacityWh(), accumulator.charging(), accumulator.discharging(),
                        accumulator.flashing(tick)));
            }
        }
        if (tick % GeneratorLook.PERIOD_TICKS == 0) {
            updateGenerators();
            updatePylons();
        }
        if (tick % WIRE_TICKS == 0) {
            effects.drawWires(wireSegments(), tick);
        }
        for (UUID player : selection.expire(tick)) {
            Player online = Bukkit.getPlayer(player);
            if (online != null) {
                online.sendMessage(Messages.color("&7The wire start timed out."));
            }
        }
        if (tick % saveIntervalTicks == 0) {
            save();
        }
    }

    /**
     * The satisfaction of the network at a lab tool or drill: 0 to 1, from the last solve. A machine the solver did not see reads 0,
     * so an unpowered tool stops.
     *
     * @param block the machine's host block
     */
    public double satisfaction(Location block) {
        return reading(block).satisfaction();
    }

    /**
     * What the network gave a lab tool or drill at the last solve: its satisfaction, the voltage of its grid and its demand.
     *
     * @param block the machine's host block
     */
    public Reading reading(Location block) {
        if (block.getWorld() == null) {
            return Reading.NONE;
        }
        return reading(consumerId(block));
    }

    /**
     * The satisfaction of a consumer by its id, from the last solve: 0 to 1, or 0 for a consumer the solver did not see.
     *
     * @param consumerId the id, as {@link EnergySnapshots#consumerId} makes it
     */
    public double satisfaction(String consumerId) {
        return reading(consumerId).satisfaction();
    }

    private Reading reading(String consumerId) {
        ConsumerResult result = consumerResults.get(consumerId);
        return result == null ? Reading.NONE : new Reading(result.satisfaction(), result.voltageV(), result.demandW());
    }

    /**
     * Adds a source of consumers that are not lab tools or drills, such as the logistics devices. Its consumers are solved with the
     * others every tick, and their satisfaction is read by id.
     */
    public void addConsumerSource(ConsumerSource source) {
        sources.add(Objects.requireNonNull(source, "source"));
    }

    /**
     * Sets the water network a nuclear generator may run on, when it has a pipe and the network delivers water. Without one a nuclear
     * generator keeps the underwater rule alone.
     */
    public void useWater(WaterFeed feed) {
        water = Objects.requireNonNull(feed, "feed");
    }

    /**
     * The generators of a world with their output this tick, as the solver reads them. A generator that is not loaded gives nothing.
     *
     * @param world the id of the world
     */
    public List<GeneratorOutput> generators(UUID world) {
        return generatorOutputs.getOrDefault(world, List.of());
    }

    /** Places a pylon on a block, and saves. */
    public EnergyDevice addPylon(Location block, int level) {
        EnergyDevice device = devices.add(new EnergyDevice(UUID.randomUUID().toString(), DeviceKind.PYLON, block, level));
        save();
        return device;
    }

    /** Places a generator of a kind on a block, and saves. */
    public GeneratorDevice addGenerator(Location block, GeneratorKind kind) {
        GeneratorDevice device = devices.add(devices.newGenerator(UUID.randomUUID().toString(), kind, block));
        save();
        return device;
    }

    /** Places an accumulator on a block, and saves. */
    public AccumulatorDevice addAccumulator(Location block, int level) {
        AccumulatorDevice device = new AccumulatorDevice(UUID.randomUUID().toString(), block, level, 0.0, false);
        devices.add(device);
        save();
        return device;
    }

    /**
     * Removes a device with its wires and any wire start on it. Nothing is refunded for the wires. A pylon's grid is worked out again at
     * the next solve, so a grid it held may split.
     *
     * @return true when the device was registered
     */
    public boolean removeDevice(EnergyDevice device) {
        if (devices.byId(device.id()).isEmpty()) {
            return false;
        }
        devices.remove(device);
        wires.removeIf(wire -> wire.from().equals(device.id()) || wire.to().equals(device.id()));
        selection.forgetDevice(device.id());
        save();
        return true;
    }

    /**
     * One click of a player with the wire on a device. A wire may join a generator to a pylon, or a pylon to another pylon.
     *
     * @param player   the player
     * @param device   the device clicked
     * @param sneaking true when the player is sneaking
     * @return what the click did
     */
    public WireSelection.Result clickWire(UUID player, EnergyDevice device, boolean sneaking) {
        WireSelection.Result result = selection.click(player, wireDevice(device), sneaking, tick, wiresNow());
        if (result.verdict() == WireSelection.Verdict.MADE) {
            wires.add(new StoredWire(UUID.randomUUID().toString(), result.start().id(), device.id()));
            save();
        }
        return result;
    }

    /** The start of a player's wire, or empty when none is selected. */
    public Optional<WireSelection.Device> wireStart(UUID player) {
        return selection.selected(player, tick);
    }

    /** Clears the wire start of a player who quit. */
    public void forgetPlayer(UUID player) {
        selection.forget(player);
    }

    /** The name of a device, as the wire messages show it. */
    public String label(EnergyDevice device) {
        String at = " at " + device.pos().x() + " " + device.pos().y() + " " + device.pos().z();
        if (device instanceof GeneratorDevice generator) {
            return GeneratorText.name(generator.generatorKind()) + at;
        }
        String name = device.kind() == DeviceKind.PYLON ? "Pylon " : "Accumulator ";
        return name + EnergyLevels.roman(device.level()) + at;
    }

    /** The grid id of the pylon's grid, or empty when the pylon has none yet. */
    public OptionalInt gridOf(String pylonId) {
        Integer id = grids.idByPylon().get(pylonId);
        return id == null ? OptionalInt.empty() : OptionalInt.of(id);
    }

    /**
     * The grid covering a block: the smallest id of the grids of the pylons whose squares cover it, in its world.
     *
     * @param world the id of the world
     * @param pos   the block
     */
    public OptionalInt gridCovering(UUID world, Pos pos) {
        OptionalInt found = OptionalInt.empty();
        for (EnergyDevice device : devices.all()) {
            if (device.kind() != DeviceKind.PYLON || !device.world().equals(world)) {
                continue;
            }
            Pylon pylon = new Pylon(device.id(), device.pos(), EnergyLevels.pylonType(device.level()));
            OptionalInt id = gridOf(device.id());
            if (id.isPresent() && pylon.covers(pos) && (found.isEmpty() || id.getAsInt() < found.getAsInt())) {
                found = id;
            }
        }
        return found;
    }

    /** The last solve's reading of a grid, or empty when the grid did not exist at the last solve. */
    public Optional<GridResult> grid(int gridId) {
        return Optional.ofNullable(gridResults.get(gridId));
    }

    /** The last solve's storage of a grid, or empty when the grid did not exist at the last solve. */
    public Optional<ChartSamples.Storage> storage(int gridId) {
        return Optional.ofNullable(storage.get(gridId));
    }

    /** The grid's sample for this tick, or empty when the grid did not exist at the last solve. */
    public Optional<ChartHistory.Sample> liveSample(int gridId) {
        GridResult grid = gridResults.get(gridId);
        if (grid == null) {
            return Optional.empty();
        }
        return Optional.of(ChartSamples.sample(grid, storage.getOrDefault(gridId, new ChartSamples.Storage(0.0, 0.0))));
    }

    /** The samples of a grid, one per second, oldest first. Empty for a grid with no history. */
    public List<ChartHistory.Sample> samples(int gridId) {
        ChartHistory history = histories.get(gridId);
        return history == null ? List.of() : history.samples();
    }

    /** The charts, in the order they were made. */
    public List<StoredChart> charts() {
        return List.copyOf(charts);
    }

    /** The chart with a name, if any. */
    public Optional<StoredChart> chart(String name) {
        return charts.stream().filter(chart -> chart.name().equals(name)).findFirst();
    }

    /** The number of charts on a grid. */
    public int chartsOn(int gridId) {
        return (int) charts.stream().filter(chart -> chart.grid() == gridId).count();
    }

    /** Adds a chart, and saves. The caller has checked the name and the limits. */
    public void addChart(StoredChart chart) {
        charts.add(chart);
        save();
    }

    /**
     * Removes a chart by name, and saves.
     *
     * @return the chart removed, or empty when there was none
     */
    public Optional<StoredChart> removeChart(String name) {
        Optional<StoredChart> chart = chart(name);
        chart.ifPresent(found -> {
            charts.remove(found);
            save();
        });
        return chart;
    }

    /**
     * Sets the mode of a chart, and saves.
     *
     * @return the chart with its new mode, or empty when there is no chart with the name
     */
    public Optional<StoredChart> setChartMode(String name, ChartMode mode) {
        Optional<StoredChart> found = chart(name);
        if (found.isEmpty()) {
            return found;
        }
        int index = charts.indexOf(found.get());
        StoredChart changed = found.get().withMode(mode);
        charts.set(index, changed);
        save();
        return Optional.of(changed);
    }

    /**
     * The outlines of the grids that have a pylon within a distance of a location, in its world, for the outline command. Each grid's
     * outline is the boundary of the union of its pylons' squares, at a height of 0.3 blocks above its lowest pylon.
     *
     * @param centre   the location
     * @param distance the distance in blocks, on x and z
     */
    public List<EnergyEffects.Outline> outlinesNear(Location centre, double distance) {
        List<EnergyEffects.Outline> outlines = new ArrayList<>();
        if (centre.getWorld() == null) {
            return outlines;
        }
        UUID world = centre.getWorld().getUID();
        Map<Integer, List<EnergyDevice>> byGrid = new HashMap<>();
        for (EnergyDevice device : devices.all()) {
            if (device.kind() != DeviceKind.PYLON || !device.world().equals(world)) {
                continue;
            }
            OptionalInt id = gridOf(device.id());
            if (id.isPresent()) {
                byGrid.computeIfAbsent(id.getAsInt(), key -> new ArrayList<>()).add(device);
            }
        }
        for (Map.Entry<Integer, List<EnergyDevice>> entry : new TreeMap<>(byGrid).entrySet()) {
            List<EnergyDevice> pylons = entry.getValue();
            boolean near = pylons.stream().anyMatch(pylon -> {
                double dx = pylon.pos().x() + 0.5 - centre.getX();
                double dz = pylon.pos().z() + 0.5 - centre.getZ();
                return Math.sqrt(dx * dx + dz * dz) <= distance;
            });
            if (!near) {
                continue;
            }
            List<Pylon> corePylons = pylons.stream()
                    .map(pylon -> new Pylon(pylon.id(), pylon.pos(), EnergyLevels.pylonType(pylon.level()))).toList();
            int lowest = pylons.stream().mapToInt(pylon -> pylon.pos().y()).min().orElse(0);
            List<double[]> points = EnergyGeometry.loops(OutlineTracer.trace(OutlineTracer.coverOf(corePylons)),
                    lowest + OUTLINE_HEIGHT, OUTLINE_SPACING);
            outlines.add(new EnergyEffects.Outline(world, GridPalette.colour(entry.getKey()), points));
        }
        return outlines;
    }

    /** Writes the devices, wires, charts and grid ids to the energy file. */
    public void save() {
        StoredEnergy snapshot = devices.snapshot(grids.idByPylon());
        store.write(new StoredEnergy(snapshot.pylons(), snapshot.accumulators(), snapshot.generators(), List.copyOf(wires),
                grids.nextFreshId(), charts));
    }

    /** Saves and releases the entities. Called when the plugin is disabled. */
    public void shutdown() {
        save();
        devices.releaseAll();
    }

    /** Ticks every loaded generator once, and returns the outputs by world for the solver. A generator whose chunk is unloaded does not run. */
    private Map<UUID, List<GeneratorOutput>> tickGenerators() {
        Map<UUID, List<GeneratorOutput>> byWorld = new HashMap<>();
        for (EnergyDevice device : devices.all()) {
            if (!(device instanceof GeneratorDevice generator) || !generator.isLoaded()) {
                continue;
            }
            Location block = generator.block();
            World world = generator.loadedWorld();
            GeneratorKind kind = generator.generatorKind();
            OpenAir air = kind == GeneratorKind.WIND ? GeneratorSites.openAir(block) : new OpenAir(0, block.getBlockY());
            boolean underwater = kind == GeneratorKind.NUCLEAR && water.nuclearMayRun(block, GeneratorSites.underwater(block));
            GeneratorEnvironment env = GeneratorEnvironment.at(world.getFullTime(), world.getSeed(), air, underwater);
            generator.tick(env, TICK_SECONDS);
            MachineSounds.generatorHum(generator.producing(), kind, block, tick);
            byWorld.computeIfAbsent(generator.world(), key -> new ArrayList<>())
                    .add(new GeneratorOutput(generator.id(), generator.pos(), generator.outputW()));
        }
        return byWorld;
    }

    /** Every 10 ticks: the generators' holograms take their text, their open menus are redrawn, and their particles are sent. */
    private void updateGenerators() {
        List<EnergyEffects.GeneratorVisual> visuals = new ArrayList<>();
        for (EnergyDevice device : devices.all()) {
            if (!(device instanceof GeneratorDevice generator)) {
                continue;
            }
            if (generator.hologram() != null) {
                generator.hologram().setLines(generator.hologramLines());
            }
            generator.refreshMenu();
            if (generator.isLoaded()) {
                visuals.add(new EnergyEffects.GeneratorVisual(generator.world(), topOf(generator), generator.generatorKind(),
                        generator.burning(), generator.producing()));
            }
        }
        effects.drawGenerators(visuals);
    }

    /** Every 10 ticks: each pylon's hologram shows the voltage, current and power of its grid. */
    private void updatePylons() {
        for (EnergyDevice device : devices.all()) {
            if (device.kind() != DeviceKind.PYLON || device.hologram() == null) {
                continue;
            }
            OptionalInt id = gridOf(device.id());
            if (id.isPresent()) {
                device.hologram().setLines(EnergyText.pylon(id.getAsInt(), gridResults.get(id.getAsInt())));
            }
        }
    }

    /** Every second: each grid's history takes its sample, and the histories of grids that are gone are dropped. */
    private void sample() {
        for (Map.Entry<Integer, GridResult> entry : gridResults.entrySet()) {
            ChartSamples.Storage stored = storage.getOrDefault(entry.getKey(), new ChartSamples.Storage(0.0, 0.0));
            histories.computeIfAbsent(entry.getKey(), key -> new ChartHistory())
                    .add(ChartSamples.sample(entry.getValue(), stored));
        }
        histories.keySet().retainAll(gridResults.keySet());
    }

    private void solve() {
        Map<UUID, List<EnergyDevice>> byWorld = new HashMap<>();
        for (EnergyDevice device : devices.all()) {
            byWorld.computeIfAbsent(device.world(), key -> new ArrayList<>()).add(device);
        }
        Map<UUID, List<PlacedMachine>> consumersByWorld = new HashMap<>();
        for (PlacedMachine placed : machines.all()) {
            if (placed.isLoaded() && placed.block().getWorld() != null) {
                consumersByWorld.computeIfAbsent(placed.block().getWorld().getUID(), key -> new ArrayList<>()).add(placed);
            }
        }
        Map<UUID, List<Consumer>> others = new HashMap<>();
        for (ConsumerSource source : sources) {
            source.consumers().forEach((world, consumers) ->
                    others.computeIfAbsent(world, key -> new ArrayList<>()).addAll(consumers));
        }
        Set<UUID> worlds = new HashSet<>(byWorld.keySet());
        worlds.addAll(consumersByWorld.keySet());
        worlds.addAll(others.keySet());
        List<UUID> ordered = new ArrayList<>(worlds);
        ordered.sort(Comparator.comparing(UUID::toString));

        GridRegistry before = grids;
        GridRegistry current = before;
        Map<Integer, GridResult> readings = new HashMap<>();
        Map<Integer, ChartSamples.Storage> storages = new HashMap<>();
        Map<String, ConsumerResult> consumers = new HashMap<>();
        for (UUID world : ordered) {
            current = solveWorld(world, byWorld.getOrDefault(world, List.of()),
                    consumersByWorld.getOrDefault(world, List.of()), others.getOrDefault(world, List.of()), current,
                    readings, storages, consumers);
        }
        grids = current;
        gridResults = Map.copyOf(readings);
        storage = Map.copyOf(storages);
        consumerResults = Map.copyOf(consumers);
        remapCharts(before.idByPylon(), current.idByPylon());
    }

    private GridRegistry solveWorld(UUID world, List<EnergyDevice> worldDevices, List<PlacedMachine> machinesHere,
                                    List<Consumer> otherConsumers, GridRegistry before, Map<Integer, GridResult> readings,
                                    Map<Integer, ChartSamples.Storage> storages, Map<String, ConsumerResult> consumers) {
        List<StoredPylon> pylons = new ArrayList<>();
        List<StoredAccumulator> accumulators = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (EnergyDevice device : worldDevices) {
            ids.add(device.id());
            if (device instanceof AccumulatorDevice accumulator) {
                accumulators.add(new StoredAccumulator(accumulator.id(), world, accumulator.pos(), accumulator.level(),
                        accumulator.stored(), accumulator.discharging()));
            } else if (device.kind() == DeviceKind.PYLON) {
                pylons.add(new StoredPylon(device.id(), world, device.pos(), device.level(), gridIdOrNone(device.id())));
            }
        }
        List<StoredWire> here = new ArrayList<>();
        for (StoredWire wire : wires) {
            if (ids.contains(wire.from()) && ids.contains(wire.to())) {
                here.add(wire);
            }
        }
        List<Consumer> demands = new ArrayList<>();
        for (PlacedMachine placed : machinesHere) {
            demands.add(new Consumer(consumerId(placed.block()), pos(placed.block()), demandOf(placed)));
        }
        demands.addAll(otherConsumers);
        EnergySnapshot snapshot = EnergySnapshots.build(pylons, accumulators, here, demands, generators(world), before);
        EnergyResult result;
        try {
            result = EnergyNetwork.solve(snapshot, TICK_SECONDS);
        } catch (RuntimeException e) {
            log.warning("The energy network of a world could not be solved this tick: " + e.getMessage());
            return before;
        }
        for (GridResult grid : result.grids()) {
            readings.put(grid.grid().id(), grid);
        }
        storages.putAll(ChartSamples.storageByGrid(snapshot, result));
        for (ConsumerResult consumer : result.consumers()) {
            consumers.put(consumer.id(), consumer);
        }
        for (AccumulatorResult outcome : result.accumulators()) {
            devices.byId(outcome.id()).ifPresent(device -> {
                if (device instanceof AccumulatorDevice accumulator) {
                    accumulator.update(outcome.state().storedWh(), outcome.chargedWh() > 0, outcome.dischargedWh() > 0);
                    if (outcome.activated() && accumulator.isLoaded()) {
                        effects.burst(accumulator.block());
                        accumulator.flashUntil(tick + FLASH_TICKS);
                    }
                }
            });
        }
        Map<String, Integer> merged = new HashMap<>(before.idByPylon());
        merged.putAll(result.gridIds().idByPylon());
        return new GridRegistry(merged, result.gridIds().nextFreshId());
    }

    /**
     * Moves a chart to the grid its old grid became, when grids join or split: the chart follows the grid that now holds the smallest
     * pylon the old grid held. A chart whose grid is gone and has no such pylon keeps its id, and shows no grid.
     */
    private void remapCharts(Map<String, Integer> before, Map<String, Integer> after) {
        for (int i = 0; i < charts.size(); i++) {
            StoredChart chart = charts.get(i);
            Optional<Integer> moved = before.entrySet().stream()
                    .filter(entry -> entry.getValue() == chart.grid())
                    .map(Map.Entry::getKey)
                    .sorted()
                    .map(after::get)
                    .filter(Objects::nonNull)
                    .findFirst();
            if (moved.isPresent() && moved.get().intValue() != chart.grid()) {
                charts.set(i, chart.withGrid(moved.get()));
            }
        }
    }

    private int gridIdOrNone(String pylonId) {
        return gridOf(pylonId).orElse(StoredEnergy.NO_GRID);
    }

    private int demandOf(PlacedMachine placed) {
        if (placed.drill() == null) {
            int own = placed.machine().powerW();
            return own > 0 ? own : labDemandW;
        }
        int level = placed.drill().definition().spec().level();
        if (level < 1 || level > 5) {
            return drillDemandW;
        }
        return (int) Math.round(EnergyTable.drillDemandW(level));
    }

    /**
     * The wires to draw: those whose two ends are loaded in one world and whose grid was solved this tick. A wire's grid is the grid of
     * its pylon. The pulses leave from the generator end, and carry the flow of the grid; a pylon-to-pylon wire carries no pulse.
     */
    private List<EnergyEffects.Segment> wireSegments() {
        List<EnergyEffects.Segment> segments = new ArrayList<>();
        for (StoredWire wire : wires) {
            Optional<EnergyDevice> from = devices.byId(wire.from());
            Optional<EnergyDevice> to = devices.byId(wire.to());
            if (from.isEmpty() || to.isEmpty() || !from.get().world().equals(to.get().world())) {
                continue;
            }
            EnergyDevice pylon = from.get().kind() == DeviceKind.PYLON ? from.get() : to.get();
            OptionalInt gridId = gridOf(pylon.id());
            GridResult grid = gridId.isPresent() ? gridResults.get(gridId.getAsInt()) : null;
            if (grid == null) {
                continue;
            }
            EnergyDevice start = from.get();
            EnergyDevice end = to.get();
            if (end.kind() == DeviceKind.GENERATOR) {
                start = to.get();
                end = from.get();
            }
            double flow = start.kind() == DeviceKind.GENERATOR
                    ? WirePath.flow(grid.satisfaction(), grid.loadW(), grid.deliveredW() + grid.fromStorageW())
                    : 0.0;
            segments.add(new EnergyEffects.Segment(start.world(), topOf(start), topOf(end), grid.grid().colour(), flow));
        }
        return segments;
    }

    private List<Wire> wiresNow() {
        return wires.stream().map(wire -> new Wire(wire.id(), wire.from(), wire.to())).toList();
    }

    private WireSelection.Device wireDevice(EnergyDevice device) {
        return new WireSelection.Device(device.id(), device.kind(), device.pos(), device.world(), label(device));
    }

    private static double[] topOf(EnergyDevice device) {
        Pos pos = device.pos();
        return new double[] {pos.x() + 0.5, pos.y() + 1.0, pos.z() + 0.5};
    }

    private static Pos pos(Location block) {
        return new Pos(block.getBlockX(), block.getBlockY(), block.getBlockZ());
    }

    private static String consumerId(Location block) {
        return EnergySnapshots.consumerId(block.getWorld().getUID(), block.getBlockX(), block.getBlockY(), block.getBlockZ());
    }
}
