package io.corefactory.spigot.turret;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.device.Footprint;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.turret.Magazine;
import io.corefactory.core.turret.ModelKeys;
import io.corefactory.spigot.device.DevicePart;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
 * Keeps the turrets by id and by block, and owns their entities. A turret gets an item display for its look and an interaction entity for
 * its clicks; a hologram is added when holograms are on. A tall turret (level 3 and up) also gets a second display over the block above,
 * and its interaction is as tall as both blocks. Every entity carries a marker with the turret's id, so entities left by an earlier run are
 * found and removed. A turret whose chunk unloads keeps its data, and its entity handles are forgotten until the chunk loads again. A turret
 * whose world is not loaded waits in the pending records until that world loads.
 *
 * <p>A turret is keyed by its anchor, the lower block. The block above a tall turret belongs to it too: {@link #at} finds the turret from
 * either block, through {@link DevicePart#covering}, so a placement that asks for that block is refused, and a logistics port at it is the
 * turret's.
 */
public final class TurretRegistry {

    private final CoreFactoryCore core;

    private Runnable persister = () -> { };
    private final HologramService holograms;
    private final Logger log;
    private final Map<String, TurretDevice> byId = new LinkedHashMap<>();
    private final Map<String, TurretDevice> byBlock = new HashMap<>();
    private final Map<UUID, TurretDevice> byInteraction = new HashMap<>();
    private List<StoredTurret> pending = List.of();
    private final Set<TurretKind> reportedMissingItem = EnumSet.noneOf(TurretKind.class);

    /**
     * @param core      the core, for the turret's item and the display scale
     * @param holograms the hologram service
     * @param log       receives the warnings
     */
    public TurretRegistry(CoreFactoryCore core, HologramService holograms, Logger log) {
        this.core = core;
        this.holograms = holograms;
        this.log = log;
    }

    /** Sets what saves the turrets. The service sets it when it is built; a port calls {@link #persist()} after it changes a turret. */
    void onPersist(Runnable save) {
        this.persister = save;
    }

    /** Saves the turrets through the service that owns them. */
    public void persist() {
        persister.run();
    }

    /** Every turret, in the order they were added. */
    public List<TurretDevice> all() {
        return List.copyOf(byId.values());
    }

    /** The turret with the given id, if any. */
    public Optional<TurretDevice> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * The turret a block belongs to: the turret anchored at the block, or a tall turret whose upper block it is. Both blocks of a tall turret
     * lead to the same turret.
     */
    public Optional<TurretDevice> at(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        return DevicePart.covering(location, spot -> Optional.ofNullable(byBlock.get(key(spot))), TurretDevice::height);
    }

    /** The turret whose interaction entity this is, if any. */
    public Optional<TurretDevice> byInteraction(UUID entity) {
        return Optional.ofNullable(byInteraction.get(entity));
    }

    /**
     * Reads the saved turrets. Those in a loaded world are added; the rest wait. Entities are made by {@link #ensureEntities}.
     *
     * @param saved the saved turrets
     * @param stats the stats the magazines take their capacity from
     */
    public void load(List<StoredTurret> saved, TurretStats stats) {
        pending = List.copyOf(saved);
        materializePending(stats);
    }

    /** Adds the pending turrets whose world has loaded since. */
    public void materializePending(TurretStats stats) {
        List<StoredTurret> waiting = new ArrayList<>();
        for (StoredTurret saved : pending) {
            if (Bukkit.getWorld(saved.world()) == null) {
                waiting.add(saved);
            } else if (!byId.containsKey(saved.id())) {
                addDevice(deviceOf(saved, stats));
            }
        }
        pending = List.copyOf(waiting);
    }

    /**
     * Registers a new turret and shows it, when its chunk is loaded.
     *
     * @return the turret
     */
    public TurretDevice add(TurretDevice device) {
        addDevice(device);
        spawnEntities(device);
        return device;
    }

    /**
     * Removes a turret from the registry and removes its entities and hologram. The caller drops the items it gives back.
     */
    public void remove(TurretDevice device) {
        if (device.existingMenu() != null) {
            device.existingMenu().closeViewers();
        }
        byId.remove(device.id());
        byBlock.remove(key(device.block()));
        destroyEntities(device);
    }

    /**
     * Makes the entities of a turret again after its level changed: its displays, its interaction and its hologram, the last two for the
     * new height.
     */
    public void rebuild(TurretDevice device) {
        destroyEntities(device);
        spawnEntities(device);
    }

    /**
     * The item a turret gives back when it is broken, carrying its level, or empty when the item is not defined.
     */
    public Optional<ItemStack> itemOf(TurretDevice device) {
        ComponentDefinition definition = core.componentDefinitions().get(device.kind().itemId());
        return definition == null ? Optional.empty()
                : Optional.of(TurretIds.withLevel(core.items().component(definition, 1), device.level()));
    }

    /** The rounds a turret holds, as stacks of at most 64. Empty when it holds none or the round is not defined. */
    public List<ItemStack> roundStacks(Magazine magazine) {
        List<ItemStack> stacks = new ArrayList<>();
        if (magazine.isEmpty() || magazine.type() == null) {
            return stacks;
        }
        ComponentDefinition definition = core.componentDefinitions().get(magazine.type().itemId());
        if (definition == null) {
            return stacks;
        }
        int left = magazine.count();
        while (left > 0) {
            int amount = Math.min(64, left);
            stacks.add(core.items().component(definition, amount));
            left -= amount;
        }
        return stacks;
    }

    /** Makes the missing display, interaction or hologram of every turret whose chunk is loaded. */
    public void ensureEntities() {
        for (TurretDevice device : byId.values()) {
            if (device.isLoaded()) {
                spawnEntities(device);
            }
        }
    }

    /** Removes the marked entities of a chunk that no turret owns. Called when a chunk loads. */
    public void sweep(Chunk chunk) {
        Set<UUID> owned = ownedEntities();
        for (Entity entity : chunk.getEntities()) {
            if (entity.getPersistentDataContainer().has(TurretIds.MARK, PersistentDataType.STRING)
                    && !owned.contains(entity.getUniqueId())) {
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

    /** A chunk is unloading: the entity handles of its turrets are forgotten and their holograms released. The data stays. */
    public void unloaded(Chunk chunk) {
        for (TurretDevice device : byId.values()) {
            if (device.inChunk(chunk)) {
                releaseEntities(device);
            }
        }
    }

    /** Removes the entities and holograms of every turret. The data stays. */
    public void releaseAll() {
        for (TurretDevice device : byId.values()) {
            destroyEntities(device);
        }
    }

    /** The saved form of every turret, with the pending records kept. */
    public List<StoredTurret> snapshot() {
        List<StoredTurret> turrets = new ArrayList<>(pending);
        for (TurretDevice device : byId.values()) {
            turrets.add(device.toStored());
        }
        return turrets;
    }

    /** Makes the turret of a saved record, with its level and its rounds clamped to the level's capacity. */
    static TurretDevice deviceOf(StoredTurret saved, TurretStats stats) {
        int capacity = stats.magazineCapacity(saved.kind(), saved.level());
        int count = Math.min(saved.rounds(), capacity);
        Magazine magazine = saved.round() == null || count == 0 ? Magazine.empty(capacity)
                : new Magazine(saved.round(), count, capacity);
        Location block = new Location(Bukkit.getWorld(saved.world()), saved.pos().x(), saved.pos().y(), saved.pos().z());
        TurretDevice device = new TurretDevice(saved.id(), saved.kind(), saved.owner(), block, saved.facing(), magazine, saved.level());
        device.setPolicy(saved.policy());
        return device;
    }

    private void addDevice(TurretDevice device) {
        byId.put(device.id(), device);
        byBlock.put(key(device.block()), device);
    }

    private void spawnEntities(TurretDevice device) {
        if (!device.isLoaded()) {
            return;
        }
        Location block = device.block();
        World world = block.getWorld();
        int level = device.level();
        boolean tall = device.height() > Footprint.SHORT;
        if (device.display() == null || Bukkit.getEntity(device.display()) == null) {
            Optional<ItemStack> item = displayOf(device.kind(), level, false);
            if (item.isEmpty()) {
                if (reportedMissingItem.add(device.kind())) {
                    log.warning("No item is defined for " + device.kind().itemId() + ", so no display is shown for the turret.");
                }
            } else {
                device.setDisplay(spawnDisplay(world, block, device.id(), item.get()));
                device.setShown(Double.NaN, Double.NaN);
            }
        }
        if (tall && (device.topDisplay() == null || Bukkit.getEntity(device.topDisplay()) == null)) {
            displayOf(device.kind(), level, true).ifPresent(item -> {
                device.setTopDisplay(spawnDisplay(world, block.clone().add(0, Footprint.SHORT, 0), device.id(), item));
                device.setShown(Double.NaN, Double.NaN);
            });
        }
        if (device.interaction() == null || Bukkit.getEntity(device.interaction()) == null) {
            UUID interaction = spawnInteraction(world, block, device.id(), device.height());
            device.setInteraction(interaction);
            byInteraction.put(interaction, device);
        }
        if (device.hologram() == null && holograms.enabled()) {
            holograms.create(HologramService.spotAbove(block.clone().add(0, device.height() - Footprint.SHORT, 0)),
                    TurretText.lines(device.kind(), level, device.magazine(), device.status(), device.satisfaction()))
                    .ifPresent(device::setHologram);
        }
    }

    /**
     * The display item of one half of a turret at a level: the machine model of its component, with the model of that level and half.
     */
    private Optional<ItemStack> displayOf(TurretKind kind, int level, boolean top) {
        ComponentDefinition definition = core.componentDefinitions().get(kind.itemId());
        if (definition == null) {
            return Optional.empty();
        }
        ItemStack item = core.items().machine(definition);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setItemModel(new NamespacedKey(ItemFactory.NAMESPACE, ModelKeys.of(kind.type(), level, top)));
            item.setItemMeta(meta);
        }
        return Optional.of(item);
    }

    private UUID spawnDisplay(World world, Location block, String turretId, ItemStack item) {
        float scale = core.settings().displayScale();
        ItemDisplay display = world.spawn(block.clone().add(0.5, 0.5, 0.5), ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setTransformation(new Transformation(new Vector3f(0f, 0f, 0f), new Quaternionf(),
                    new Vector3f(scale, scale, scale), new Quaternionf()));
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(TurretIds.MARK, PersistentDataType.STRING, turretId);
        });
        return display.getUniqueId();
    }

    /** One interaction over the turret: as tall as its footprint, so a click on either block reaches the turret. */
    private static UUID spawnInteraction(World world, Location block, String turretId, int height) {
        Interaction interaction = world.spawn(block.clone().add(0.5, 0.0, 0.5), Interaction.class, entity -> {
            entity.setInteractionWidth(1.0f);
            entity.setInteractionHeight(height > Footprint.SHORT ? 2.5f : 1.5f);
            entity.setResponsive(true);
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(TurretIds.MARK, PersistentDataType.STRING, turretId);
        });
        return interaction.getUniqueId();
    }

    private void releaseEntities(TurretDevice device) {
        if (device.interaction() != null) {
            byInteraction.remove(device.interaction());
        }
        device.setDisplay(null);
        device.setTopDisplay(null);
        device.setInteraction(null);
        releaseHologram(device);
    }

    private void destroyEntities(TurretDevice device) {
        removeEntity(device.display());
        removeEntity(device.topDisplay());
        removeEntity(device.interaction());
        if (device.interaction() != null) {
            byInteraction.remove(device.interaction());
        }
        device.setDisplay(null);
        device.setTopDisplay(null);
        device.setInteraction(null);
        releaseHologram(device);
    }

    private void releaseHologram(TurretDevice device) {
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
        for (TurretDevice device : byId.values()) {
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

    private static String key(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":"
                + location.getBlockZ();
    }
}
