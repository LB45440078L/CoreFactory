package io.corefactory.spigot.energy;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.device.Footprint;
import io.corefactory.core.energy.DeviceKind;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.spigot.device.DevicePart;
import io.corefactory.spigot.hologram.Hologram;
import io.corefactory.spigot.hologram.HologramService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
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
 * Keeps the energy devices by id and by block, and owns their entities. A device gets an item display for its look, an
 * interaction entity for its clicks, and an accumulator also a hologram. Every entity carries a PDC marker with its device id, so
 * entities left by an earlier run, or by a chunk that unloaded, are found and removed. A device whose chunk is unloaded keeps its
 * data: only its entity handles are forgotten, and they are made again when the chunk loads. A device whose world is not loaded
 * waits in the pending records until that world loads.
 */
public final class DeviceRegistry {

    private static final NamespacedKey MARK = new NamespacedKey(ItemFactory.NAMESPACE, "energy_device");

    private final CoreFactoryCore core;
    private final HologramService holograms;
    private final Logger log;
    private final GeneratorItems generatorItems;
    private final Map<String, EnergyDevice> byId = new LinkedHashMap<>();
    private final Map<String, EnergyDevice> byBlock = new HashMap<>();
    private StoredEnergy pending = StoredEnergy.empty();

    public DeviceRegistry(CoreFactoryCore core, HologramService holograms, Logger log) {
        this.core = core;
        this.holograms = holograms;
        this.log = log;
        this.generatorItems = new GeneratorItems(core);
    }

    /** Every device, in the order they were added. */
    public List<EnergyDevice> all() {
        return List.copyOf(byId.values());
    }

    /** The device with the given id, if any. */
    public Optional<EnergyDevice> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * The device a block belongs to, if any: the device anchored at the block, or a tall device whose upper block it is. Both blocks
     * of a tall device lead to the same device.
     */
    public Optional<EnergyDevice> at(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        return DevicePart.covering(location, spot -> Optional.ofNullable(byBlock.get(key(spot))), EnergyDevice::height);
    }

    /** The device whose interaction entity this is, if any. */
    public Optional<EnergyDevice> byInteraction(UUID entity) {
        return byId.values().stream().filter(device -> entity.equals(device.interaction())).findFirst();
    }

    /**
     * Reads the saved devices. Those in a loaded world are added; the rest wait in the pending records. Entities are not made here;
     * {@link #ensureEntities} makes them for the loaded chunks.
     */
    public void load(StoredEnergy saved) {
        pending = saved;
        materializePending();
    }

    /** Adds the pending devices whose world has loaded since. */
    public void materializePending() {
        if (pending.pylons().isEmpty() && pending.accumulators().isEmpty() && pending.generators().isEmpty()) {
            return;
        }
        List<StoredEnergy.StoredPylon> waitingPylons = new ArrayList<>();
        for (StoredEnergy.StoredPylon pylon : pending.pylons()) {
            if (Bukkit.getWorld(pylon.world()) == null) {
                waitingPylons.add(pylon);
            } else {
                addDevice(new EnergyDevice(pylon.id(), DeviceKind.PYLON, location(pylon.world(), pylon.pos()), pylon.level()));
            }
        }
        List<StoredEnergy.StoredAccumulator> waitingAccumulators = new ArrayList<>();
        for (StoredEnergy.StoredAccumulator accumulator : pending.accumulators()) {
            if (Bukkit.getWorld(accumulator.world()) == null) {
                waitingAccumulators.add(accumulator);
            } else {
                addDevice(accumulatorOf(accumulator));
            }
        }
        List<StoredEnergy.StoredGenerator> waitingGenerators = new ArrayList<>();
        for (StoredEnergy.StoredGenerator generator : pending.generators()) {
            if (Bukkit.getWorld(generator.world()) == null) {
                waitingGenerators.add(generator);
            } else {
                addDevice(new GeneratorDevice(generator.id(), generator.kind(), location(generator.world(), generator.pos()),
                        generator.fuel(), generator.remainingSeconds(), generator.totalWh(), generatorItems));
            }
        }
        pending = new StoredEnergy(waitingPylons, waitingAccumulators, waitingGenerators, List.of());
    }

    /**
     * A new generator of a kind at a block, not yet registered. Register it with {@link #add}.
     *
     * @param id    the device id
     * @param kind  the generator kind
     * @param block the block the generator stands in
     */
    public GeneratorDevice newGenerator(String id, GeneratorKind kind, Location block) {
        return new GeneratorDevice(id, kind, block, 0, 0.0, 0.0, generatorItems);
    }

    /**
     * Registers a new device and shows it, when its chunk is loaded.
     *
     * @return the device
     */
    public <T extends EnergyDevice> T add(T device) {
        addDevice(device);
        spawnEntities(device);
        return device;
    }

    /**
     * Removes a device from the registry, removes its entities and hologram, and leaves nothing behind. A generator's menu is closed
     * first, so its fuel is back in the count. The caller removes the wires and the selections that name it.
     */
    public void remove(EnergyDevice device) {
        if (device instanceof GeneratorDevice generator) {
            generator.closeMenu();
        }
        byId.remove(device.id());
        byBlock.remove(key(device.block()));
        destroyEntities(device);
    }

    /**
     * What a device gives back when it is broken: its item, and for a generator the fuel it keeps, in stacks. Call after
     * {@link #remove}, when the generator's fuel is in its count.
     */
    public List<ItemStack> drops(EnergyDevice device) {
        List<ItemStack> drops = new ArrayList<>();
        itemOf(device).ifPresent(drops::add);
        if (device instanceof GeneratorDevice generator) {
            drops.addAll(generatorItems.fuelStacks(generator.generatorKind(), generator.fuelItems()));
        }
        return drops;
    }

    /**
     * Makes the missing display, interaction or hologram of every device whose chunk is loaded. A tall device gets a second display
     * over its upper block, and its interaction is as tall as both blocks.
     */
    public void ensureEntities() {
        for (EnergyDevice device : byId.values()) {
            if (device.isLoaded()) {
                spawnEntities(device);
            }
        }
    }

    /**
     * Removes the marked entities of a chunk that no device owns. Called when a chunk loads, so the entities a previous run or an
     * earlier load left behind do not stay beside the new ones.
     */
    public void sweep(Chunk chunk) {
        Set<UUID> owned = ownedEntities();
        for (Entity entity : chunk.getEntities()) {
            if (isMarked(entity) && !owned.contains(entity.getUniqueId())) {
                entity.remove();
            }
        }
    }

    /** Sweeps every loaded chunk. Called once when the plugin starts. */
    public void sweepLoaded() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                sweep(chunk);
            }
        }
    }

    /** A chunk is unloading: the entity handles of its devices are forgotten and their holograms released. The data stays. */
    public void unloaded(Chunk chunk) {
        for (EnergyDevice device : byId.values()) {
            if (device.inChunk(chunk)) {
                device.setDisplay(null);
                device.setTopDisplay(null);
                device.setInteraction(null);
                releaseHologram(device);
            }
        }
    }

    /** Removes the entities and holograms of every device. The data stays, and the entities are made again on the next start. */
    public void releaseAll() {
        for (EnergyDevice device : byId.values()) {
            destroyEntities(device);
        }
    }

    /**
     * The saved form of every device, with the pending records kept.
     *
     * @param gridOf the grid id of each pylon, by pylon id; a pylon not in it is saved with no grid
     */
    public StoredEnergy snapshot(Map<String, Integer> gridOf) {
        List<StoredEnergy.StoredPylon> pylons = new ArrayList<>(pending.pylons());
        List<StoredEnergy.StoredAccumulator> accumulators = new ArrayList<>(pending.accumulators());
        List<StoredEnergy.StoredGenerator> generators = new ArrayList<>(pending.generators());
        for (EnergyDevice device : byId.values()) {
            if (device instanceof AccumulatorDevice accumulator) {
                accumulators.add(new StoredEnergy.StoredAccumulator(accumulator.id(), accumulator.world(), accumulator.pos(),
                        accumulator.level(), accumulator.stored(), accumulator.discharging()));
            } else if (device instanceof GeneratorDevice generator) {
                generators.add(generator.toStored());
            } else if (device.kind() == DeviceKind.PYLON) {
                pylons.add(new StoredEnergy.StoredPylon(device.id(), device.world(), device.pos(), device.level(),
                        gridOf.getOrDefault(device.id(), StoredEnergy.NO_GRID)));
            }
        }
        return new StoredEnergy(pylons, accumulators, generators, List.of());
    }

    /** The item a device drops when it is broken: its component item, or empty when the component is not defined. */
    public Optional<ItemStack> itemOf(EnergyDevice device) {
        ComponentDefinition definition = core.componentDefinitions().get(device.componentId());
        return definition == null ? Optional.empty() : Optional.of(core.items().component(definition, 1));
    }

    /**
     * The item a device's display shows: its machine model, the 3D model of the device. Empty when the component is not defined. A tall
     * device's upper display shows the upper half of the same model, see {@link DevicePart#topItem}.
     */
    public Optional<ItemStack> displayOf(EnergyDevice device) {
        ComponentDefinition definition = core.componentDefinitions().get(device.componentId());
        return definition == null ? Optional.empty() : Optional.of(core.items().machine(definition));
    }

    private void addDevice(EnergyDevice device) {
        byId.put(device.id(), device);
        byBlock.put(key(device.block()), device);
    }

    private void spawnEntities(EnergyDevice device) {
        if (!device.isLoaded()) {
            return;
        }
        Location block = device.block();
        World world = block.getWorld();
        int height = device.height();
        if (device.display() == null || Bukkit.getEntity(device.display()) == null) {
            Optional<ItemStack> item = displayOf(device);
            if (item.isEmpty()) {
                log.warning("No item is defined for " + device.componentId() + ", so no display is shown for it.");
            } else {
                device.setDisplay(spawnDisplay(world, block, device.id(), item.get()));
            }
        }
        if (height > Footprint.SHORT && (device.topDisplay() == null || Bukkit.getEntity(device.topDisplay()) == null)) {
            displayOf(device).ifPresent(item -> device.setTopDisplay(spawnDisplay(world, block.clone().add(0, Footprint.SHORT, 0),
                    device.id(), DevicePart.topItem(item, device.componentId()))));
        }
        if (device.interaction() == null || Bukkit.getEntity(device.interaction()) == null) {
            device.setInteraction(spawnInteraction(world, block, device.id(), height));
        }
        Location above = HologramService.spotAbove(block.clone().add(0, height - Footprint.SHORT, 0));
        if (device instanceof AccumulatorDevice accumulator && accumulator.hologram() == null && holograms.enabled()) {
            List<String> lines = EnergyText.accumulator(accumulator.level(), accumulator.stored(),
                    accumulator.accumulatorLevel().capacityWh(), accumulator.charging(), accumulator.discharging(),
                    accumulator.flashing(0));
            holograms.create(above, lines).ifPresent(accumulator::setHologram);
        }
        if (device.kind() == DeviceKind.PYLON && device.hologram() == null && holograms.enabled()) {
            holograms.create(above, EnergyText.pylonWaiting()).ifPresent(device::setHologram);
        }
        if (device instanceof GeneratorDevice generator && generator.hologram() == null && holograms.enabled()) {
            holograms.create(above, generator.hologramLines()).ifPresent(generator::setHologram);
        }
    }

    private UUID spawnDisplay(World world, Location block, String deviceId, ItemStack item) {
        float scale = core.settings().displayScale();
        ItemDisplay display = world.spawn(block.clone().add(0.5, 0.5, 0.5), ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setTransformation(new Transformation(new Vector3f(0f, 0f, 0f), new Quaternionf(),
                    new Vector3f(scale, scale, scale), new Quaternionf()));
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(MARK, PersistentDataType.STRING, deviceId);
        });
        return display.getUniqueId();
    }

    /** One interaction over the device: as tall as its footprint, so a click on any of its blocks reaches the device. */
    private static UUID spawnInteraction(World world, Location block, String deviceId, int height) {
        Interaction interaction = world.spawn(block.clone().add(0.5, 0.0, 0.5), Interaction.class, entity -> {
            entity.setInteractionWidth(1.0f);
            entity.setInteractionHeight((float) height);
            entity.setResponsive(true);
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(MARK, PersistentDataType.STRING, deviceId);
        });
        return interaction.getUniqueId();
    }

    private void destroyEntities(EnergyDevice device) {
        removeEntity(device.display());
        removeEntity(device.topDisplay());
        removeEntity(device.interaction());
        device.setDisplay(null);
        device.setTopDisplay(null);
        device.setInteraction(null);
        releaseHologram(device);
    }

    private void releaseHologram(EnergyDevice device) {
        if (device.hologram() != null) {
            holograms.remove(device.hologram());
            device.setHologram(null);
        }
    }

    private static void removeEntity(UUID id) {
        if (id == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(id);
        if (entity != null) {
            entity.remove();
        }
    }

    private Set<UUID> ownedEntities() {
        Set<UUID> owned = new HashSet<>();
        for (EnergyDevice device : byId.values()) {
            if (device.display() != null) {
                owned.add(device.display());
            }
            if (device.topDisplay() != null) {
                owned.add(device.topDisplay());
            }
            if (device.interaction() != null) {
                owned.add(device.interaction());
            }
        }
        return owned;
    }

    private static boolean isMarked(Entity entity) {
        return entity.getPersistentDataContainer().has(MARK, PersistentDataType.STRING);
    }

    private static AccumulatorDevice accumulatorOf(StoredEnergy.StoredAccumulator saved) {
        return new AccumulatorDevice(saved.id(), location(saved.world(), saved.pos()), saved.level(), saved.stored(),
                saved.discharging());
    }

    private static Location location(UUID world, io.corefactory.core.energy.Pos pos) {
        return new Location(Bukkit.getWorld(world), pos.x(), pos.y(), pos.z());
    }

    private static String key(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":"
                + location.getBlockZ();
    }
}
