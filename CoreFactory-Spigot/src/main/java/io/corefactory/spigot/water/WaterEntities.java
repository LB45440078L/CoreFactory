package io.corefactory.spigot.water;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.PumpLevels;
import io.corefactory.core.water.WaterOrientation;
import io.corefactory.core.water.WaterPart;
import io.corefactory.spigot.hologram.Hologram;
import io.corefactory.spigot.hologram.HologramService;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
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
import java.util.UUID;

/**
 * The entities of the water blocks: the item display that shows a part, the interaction that takes its clicks, and the hologram of a
 * pump. Every device entity carries a PDC marker with its device id, so the entities a previous run left behind are found and removed.
 * The entities are not persistent: they go when their chunk unloads, and the service makes new ones when the chunk loads.
 *
 * <p>A pipe is drawn with a turn of its model: a straight run on the east-west axis is the model as drawn, a run on the north-south axis
 * is turned a quarter about the vertical, a vertical run is rolled a quarter about the north-south axis, and a bend is turned so its open
 * ends match its connections. A pump is turned so its front faces the player who placed it, a T so its input faces that player, and a
 * cross is drawn as it is, since its four faces are alike. Every display is centred on its block.
 */
final class WaterEntities {

    /** The marker of a device's entities, which holds the device id. */
    static final NamespacedKey DEVICE_MARK = new NamespacedKey(ItemFactory.NAMESPACE, "water_device");

    /** The level a pump item carries above level 1. */
    static final NamespacedKey LEVEL = new NamespacedKey(ItemFactory.NAMESPACE, "water_level");

    private final CoreFactoryCore core;
    private final HologramService holograms;

    WaterEntities(CoreFactoryCore core, HologramService holograms) {
        this.core = core;
        this.holograms = holograms;
    }

    /** The item a part gives back: its component item, carrying the level of a pump above 1. */
    Optional<ItemStack> itemOf(WaterPart part, int level) {
        ComponentDefinition definition = core.componentDefinitions().get(part.componentId());
        if (definition == null) {
            return Optional.empty();
        }
        ItemStack item = core.items().component(definition, 1);
        if (part == WaterPart.PUMP && level > 1) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(LEVEL, PersistentDataType.INTEGER, level);
                item.setItemMeta(meta);
            }
        }
        return Optional.of(item);
    }

    /** The level a held item carries: its pump level, or 1 when it carries none. */
    static int levelOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 1;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 1;
        }
        Integer level = meta.getPersistentDataContainer().get(LEVEL, PersistentDataType.INTEGER);
        return level == null ? 1 : Math.max(1, Math.min(level, PumpLevels.MAX_LEVEL));
    }

    /** The key of the model a device is drawn with. */
    static String modelKey(WaterDevice device) {
        String id = switch (device.part()) {
            case PIPE -> WaterOrientation.pipe(device.shown()).shape() == WaterOrientation.PipeShape.BEND
                    ? "water_pipe_bend" : "water_pipe";
            case PUMP -> PumpLevels.modelKey(device.level());
            case TEE -> "water_splitter_t";
            case CROSS -> "water_splitter_x";
        };
        return ItemFactory.displayKey(id);
    }

    /** The transform a device's model takes, from its part, its face and the faces its network joins. */
    static Transformation transformOf(WaterDevice device) {
        Quaternionf turn = new Quaternionf();
        Face face = device.face();
        switch (device.part()) {
            case PIPE -> {
                WaterOrientation.PipeModel model = WaterOrientation.pipe(device.shown());
                if (model.shape() == WaterOrientation.PipeShape.STRAIGHT_Y) {
                    turn.rotationZ(radians(model.roll()));
                } else {
                    turn.rotationY(radians(model.yaw()));
                }
            }
            case PUMP -> turn.rotationY(radians(face == null ? 0 : WaterOrientation.yawOf(Face.NORTH, face)));
            case TEE, CROSS -> turn.rotationY(radians(face == null ? 0 : WaterOrientation.yawOf(Face.WEST, face)));
        }
        return new Transformation(new Vector3f(0f, 0f, 0f), turn, new Vector3f(1f, 1f, 1f), new Quaternionf());
    }

    /** The item a device's display shows: its machine item, with the model of its shape and level. */
    Optional<ItemStack> displayItem(WaterDevice device) {
        ComponentDefinition definition = core.componentDefinitions().get(device.part().componentId());
        if (definition == null) {
            return Optional.empty();
        }
        ItemStack item = core.items().machine(definition);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setItemModel(new NamespacedKey(ItemFactory.NAMESPACE, modelKey(device)));
            item.setItemMeta(meta);
        }
        return Optional.of(item);
    }

    /** Spawns the display and the interaction of a device at its block. */
    void spawn(WaterDevice device, Location block) {
        displayItem(device).ifPresent(item -> device.setDisplay(spawnDisplay(block, item, transformOf(device), device.id())));
        device.setInteraction(spawnInteraction(block, device.id()));
    }

    /** Shows a device's current model and turn on its display, when the display exists. */
    void refresh(WaterDevice device) {
        Entity entity = device.display() == null ? null : Bukkit.getEntity(device.display());
        if (entity instanceof ItemDisplay shown) {
            displayItem(device).ifPresent(shown::setItemStack);
            shown.setTransformation(transformOf(device));
        }
    }

    /** Removes a device's display and interaction, and forgets their ids. The hologram is the service's to release. */
    void release(WaterDevice device) {
        remove(device.display());
        remove(device.interaction());
        device.setDisplay(null);
        device.setInteraction(null);
    }

    /** Makes a hologram above a block with the given lines, when holograms are on. */
    Optional<Hologram> hologram(Location block, List<String> lines) {
        return holograms.create(HologramService.spotAbove(block), lines);
    }

    /** Removes the marked entities of a chunk, so the entities of a previous run do not stay next to the new ones. */
    static void sweep(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity.getPersistentDataContainer().has(DEVICE_MARK, PersistentDataType.STRING)) {
                entity.remove();
            }
        }
    }

    /** The device id an entity belongs to, when it is a water device's entity. */
    static Optional<String> deviceOf(Entity entity) {
        return Optional.ofNullable(entity.getPersistentDataContainer().get(DEVICE_MARK, PersistentDataType.STRING));
    }

    /** True when an entity with this id exists and is valid. */
    static boolean alive(UUID entity) {
        Entity found = entity == null ? null : Bukkit.getEntity(entity);
        return found != null && found.isValid();
    }

    private static UUID spawnDisplay(Location block, ItemStack item, Transformation transform, String deviceId) {
        ItemDisplay display = block.getWorld().spawn(block.clone().add(0.5, 0.5, 0.5), ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setTransformation(transform);
            entity.setPersistent(false);
            entity.getPersistentDataContainer().set(DEVICE_MARK, PersistentDataType.STRING, deviceId);
        });
        return display.getUniqueId();
    }

    private static UUID spawnInteraction(Location block, String deviceId) {
        Interaction interaction = block.getWorld().spawn(block.clone().add(0.5, 0.0, 0.5), Interaction.class, entity -> {
            entity.setInteractionWidth(1.0f);
            entity.setInteractionHeight(1.0f);
            entity.setResponsive(true);
            entity.setPersistent(false);
            entity.getPersistentDataContainer().set(DEVICE_MARK, PersistentDataType.STRING, deviceId);
        });
        return interaction.getUniqueId();
    }

    private static void remove(UUID id) {
        if (id == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(id);
        if (entity != null) {
            entity.remove();
        }
    }

    private static float radians(int degrees) {
        return (float) Math.toRadians(degrees);
    }
}
