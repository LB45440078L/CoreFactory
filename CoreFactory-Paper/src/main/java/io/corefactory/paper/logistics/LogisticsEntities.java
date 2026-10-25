package io.corefactory.paper.logistics;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.device.Footprint;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.logistics.BeltShape;
import io.corefactory.core.logistics.Dir;
import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.LogisticsLevel;
import io.corefactory.core.logistics.PortItems;
import io.corefactory.paper.hologram.Hologram;
import io.corefactory.paper.hologram.HologramService;
import io.corefactory.paper.machine.ItemRefs;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The entities of the logistics devices: the item display that shows a belt, a picker or a splitter, the interaction that takes
 * its clicks, the hologram of a picker or splitter, and the item displays that show the items on the belts. Every device entity
 * carries a PDC marker with its device id, so the entities a previous run or an unloaded chunk left behind are found and removed.
 * An item display is not persistent: it goes when its chunk unloads, and the service makes a new one when the chunk loads.
 */
final class LogisticsEntities {

    /** The marker of a device's entities, which holds the device id. */
    static final NamespacedKey DEVICE_MARK = new NamespacedKey(ItemFactory.NAMESPACE, "logistics_device");

    /** The marker of an item display on a belt. */
    static final NamespacedKey ITEM_MARK = new NamespacedKey(ItemFactory.NAMESPACE, "logistics_item");

    /** The scale of an item on a belt. */
    static final float ITEM_SCALE = 0.3f;

    /** The height of an item's centre above the block centre, in blocks: just above the belt's slab. */
    static final double ITEM_HEIGHT = -0.3;

    private final CoreFactoryCore core;
    private final HologramService holograms;
    private final ItemRefs refs;

    LogisticsEntities(CoreFactoryCore core, HologramService holograms, ItemRefs refs) {
        this.core = core;
        this.holograms = holograms;
        this.refs = refs;
    }

    /** The item a belt's display shows: its straight, left corner or right corner model. Empty when the belt's component is not defined. */
    Optional<ItemStack> beltItem(LogisticsLevel level, BeltShape shape) {
        String id = LogisticsIds.componentId(LogisticsIds.Kind.BELT, level);
        ComponentDefinition definition = core.componentDefinitions().get(id);
        if (definition == null) {
            return Optional.empty();
        }
        ItemStack item = new ItemStack(definition.material(), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        meta.setItemModel(new NamespacedKey(ItemFactory.NAMESPACE, BeltPath.modelKeyOf(id, shape)));
        item.setItemMeta(meta);
        return Optional.of(item);
    }

    /** The item a picker or splitter's display shows: its machine model. Empty when the component is not defined. */
    Optional<ItemStack> machineItem(LogisticsLevel level, LogisticsIds.Kind kind) {
        ComponentDefinition definition = core.componentDefinitions().get(LogisticsIds.componentId(kind, level));
        return definition == null ? Optional.empty() : Optional.of(core.items().machine(definition));
    }

    /** The item a device gives back when it is broken: its component item. */
    Optional<ItemStack> deviceItem(LogisticsIds.Kind kind, LogisticsLevel level) {
        ComponentDefinition definition = core.componentDefinitions().get(LogisticsIds.componentId(kind, level));
        return definition == null ? Optional.empty() : Optional.of(core.items().component(definition, 1));
    }

    /** A stack of one item for a logistics item, or empty when the item names no stack. */
    Optional<ItemStack> stackOf(Item item) {
        return Optional.ofNullable(refs.create(PortItems.refOf(item), 1));
    }

    /**
     * The transform of a belt's display: turned so its front follows the flow, or, for a corner, its input port faces the input side
     * and its front faces the output. The display is only turned, never mirrored: a right corner has its own model, so a negative
     * scale is not needed and would turn the faces inside out.
     */
    static Transformation beltTransform(Dir flow, BeltShape shape, Dir input) {
        double yaw = shape == BeltShape.STRAIGHT ? Orientation.beltYaw(flow) : Orientation.cornerYaw(shape, input);
        return new Transformation(new Vector3f(0f, 0f, 0f), turn(yaw), new Vector3f(1f, 1f, 1f), new Quaternionf());
    }

    /**
     * The transform of a belt duplex's display: turned so its north face points along the flow, the model's north being the flow. The
     * display is not scaled, as a belt's.
     */
    static Transformation duplexTransform(Dir flow) {
        return new Transformation(new Vector3f(0f, 0f, 0f), turn(Orientation.northYaw(flow)), new Vector3f(1f, 1f, 1f),
                new Quaternionf());
    }

    /** The item a duplex's display shows: its model for the anchor cell's half or the second cell's half. Empty when not defined. */
    Optional<ItemStack> duplexItem(LogisticsLevel level, boolean second) {
        String id = LogisticsIds.componentId(LogisticsIds.Kind.DUPLEX, level);
        ComponentDefinition definition = core.componentDefinitions().get(id);
        if (definition == null) {
            return Optional.empty();
        }
        ItemStack item = new ItemStack(definition.material(), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        meta.setItemModel(new NamespacedKey(ItemFactory.NAMESPACE, BeltPath.duplexModelKeyOf(id, second)));
        item.setItemMeta(meta);
        return Optional.of(item);
    }

    /** The transform of a picker or splitter display: turned so its north face points to the given side. */
    static Transformation machineTransform(Dir northSide, float scale) {
        return new Transformation(new Vector3f(0f, 0f, 0f), turn(Orientation.northYaw(northSide)),
                new Vector3f(scale, scale, scale), new Quaternionf());
    }

    /** The transform of an item on a belt: a display offset from the block centre, at the item's scale. */
    static Transformation itemTransform(double[] offset) {
        return new Transformation(new Vector3f((float) offset[0], (float) ITEM_HEIGHT, (float) offset[1]), new Quaternionf(),
                new Vector3f(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE), new Quaternionf());
    }

    /**
     * Spawns the item display of a belt, picker or splitter at its block, with its model turned.
     *
     * @return the display's id
     */
    UUID spawnDevice(Location block, ItemStack item, Transformation transform, String deviceId) {
        World world = block.getWorld();
        ItemDisplay display = world.spawn(block.clone().add(0.5, 0.5, 0.5), ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setTransformation(transform);
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(DEVICE_MARK, PersistentDataType.STRING, deviceId);
        });
        return display.getUniqueId();
    }

    /** Spawns the interaction of a device at its block: a one-block box that takes its clicks. */
    UUID spawnInteraction(Location block, String deviceId) {
        Interaction interaction = block.getWorld().spawn(block.clone().add(0.5, 0.0, 0.5), Interaction.class, entity -> {
            entity.setInteractionWidth(1.0f);
            entity.setInteractionHeight(1.0f);
            entity.setResponsive(true);
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(DEVICE_MARK, PersistentDataType.STRING, deviceId);
        });
        return interaction.getUniqueId();
    }

    /**
     * Spawns the display of one item on a belt, at the block's centre, with its offset set. Teleports and transforms glide over one
     * tick. The display is not persistent and cannot be picked up.
     *
     * @return the display's id
     */
    UUID spawnItem(Location block, ItemStack item, double[] offset) {
        ItemDisplay display = block.getWorld().spawn(block.clone().add(0.5, 0.5, 0.5), ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setTransformation(itemTransform(offset));
            entity.setInterpolationDelay(0);
            entity.setInterpolationDuration(1);
            entity.setTeleportDuration(1);
            entity.setPersistent(false);
            entity.getPersistentDataContainer().set(ITEM_MARK, PersistentDataType.BYTE, (byte) 1);
        });
        return display.getUniqueId();
    }

    /** Moves an item display to an offset from the block centre, gliding over one tick. False when the display is gone. */
    boolean moveItem(UUID display, double[] offset) {
        Entity entity = Bukkit.getEntity(display);
        if (!(entity instanceof ItemDisplay item)) {
            return false;
        }
        item.setTransformation(itemTransform(offset));
        return true;
    }

    /** Sets a display's transform and item, for a belt whose shape changed. */
    void refresh(UUID display, ItemStack item, Transformation transform) {
        Entity entity = display == null ? null : Bukkit.getEntity(display);
        if (entity instanceof ItemDisplay shown) {
            shown.setItemStack(item);
            shown.setTransformation(transform);
        }
    }

    /** Makes a hologram above a block with the given lines, when holograms are on. */
    Optional<Hologram> hologram(Location block, List<String> lines) {
        return holograms.enabled() ? holograms.create(HologramService.spotAbove(block, Footprint.SHORT), lines) : Optional.empty();
    }

    /** Removes an entity by id, when it exists. */
    static void remove(UUID id) {
        if (id == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(id);
        if (entity != null) {
            entity.remove();
        }
    }

    /** Removes a device's displays, interaction and hologram. */
    void destroy(LogisticsDevice device) {
        remove(device.display());
        remove(device.secondDisplay());
        remove(device.interaction());
        device.setDisplay(null);
        device.setSecondDisplay(null);
        device.setInteraction(null);
        release(device);
    }

    /** Releases a device's hologram, and forgets the handle. */
    void release(LogisticsDevice device) {
        if (device.hologram() != null) {
            holograms.remove(device.hologram());
            device.setHologram(null);
        }
    }

    /**
     * Removes the marked entities of a chunk that nothing owns: device entities not held by a device, and item displays not in
     * {@code shownItems}.
     */
    void sweep(Chunk chunk, Set<UUID> ownedDevices, Set<UUID> shownItems) {
        for (Entity entity : chunk.getEntities()) {
            if (entity.getPersistentDataContainer().has(DEVICE_MARK, PersistentDataType.STRING)) {
                if (!ownedDevices.contains(entity.getUniqueId())) {
                    entity.remove();
                }
            } else if (entity.getPersistentDataContainer().has(ITEM_MARK, PersistentDataType.BYTE)) {
                if (!shownItems.contains(entity.getUniqueId())) {
                    entity.remove();
                }
            }
        }
    }

    /** The rotation of a yaw about the vertical axis, as the display takes it. */
    private static Quaternionf turn(double yawDegrees) {
        return new Quaternionf().rotationY((float) Math.toRadians(yawDegrees));
    }
}
