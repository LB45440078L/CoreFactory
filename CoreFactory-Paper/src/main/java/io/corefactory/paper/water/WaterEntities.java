package io.corefactory.paper.water;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.PumpLevels;
import io.corefactory.core.water.WaterModels;
import io.corefactory.core.water.WaterOrientation;
import io.corefactory.core.water.WaterPart;
import io.corefactory.paper.hologram.Hologram;
import io.corefactory.paper.hologram.HologramService;
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
import java.util.Set;
import java.util.UUID;

/**
 * The entities of the water blocks: the item display that shows a part, the interaction that takes its clicks, and the hologram of a
 * pump. A tall pump (levels 4 and 5) has a second display on the block above, which shows the upper half of its model. Every device
 * entity carries a PDC marker with its device id, so the entities a previous run left behind are found and removed. The entities are
 * not persistent: they go when their chunk unloads, and the service makes new ones when the chunk loads.
 *
 * <p>A pipe and a splitter are turned by their frame: the frame maps the model's axes to world faces, so a bend or a T lies in whatever
 * plane its connections give, and a vertical pipe stands on end. A pump is turned by a yaw, so its front faces the player who placed it.
 * Every display is centred on its block.
 */
public final class WaterEntities {

    /** The marker of a device's entities, which holds the device id. */
    static final NamespacedKey DEVICE_MARK = new NamespacedKey(ItemFactory.NAMESPACE, "water_device");

    /** The level a pump item carries above level 1. */
    static final NamespacedKey LEVEL = new NamespacedKey(ItemFactory.NAMESPACE, "water_level");

    /** The height of a tall pump's interaction, in blocks: both of its blocks. */
    private static final float TALL_INTERACTION = 2.0f;

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

    /**
     * The key of the model a device is drawn with. Every orientation has a model of its own, with the orientation baked into it (see
     * {@link WaterModels}), so the display takes no rotation. A tall pump's key is that of its lower block.
     */
    static String modelKey(WaterDevice device) {
        return ItemFactory.displayKey(modelId(device));
    }

    /** The id of the model a device is drawn with, from its part, its shape, its faces and its level. */
    static String modelId(WaterDevice device) {
        return switch (device.part()) {
            case PIPE -> pipeModelId(device.shown());
            case PUMP -> WaterModels.pump(device.level(), device.face());
            case TEE -> WaterModels.tee(device.face(), device.output());
            case CROSS -> WaterModels.cross(device.face(), device.output());
        };
    }

    /** The model of a pipe: straight along its run, a bend between its two faces, or the junction hub. */
    static String pipeModelId(Set<Face> connections) {
        return WaterModels.pipe(connections);
    }

    /** The key of the model of a tall pump's upper block: the lower model's key with the top suffix. */
    static String topModelKey(WaterDevice device) {
        return ItemFactory.displayKey(WaterModels.pumpTop(device.level(), device.face()));
    }

    /**
     * The transform of every water display: a half turn about the vertical axis, about the block centre. The game draws the water models
     * turned half a turn from how they are authored, so every model is turned back by the same half turn. A half turn is the same whichever
     * way the game counts its angles, so this does not depend on that convention, and the models' ends sit on the faces they name.
     */
    static Transformation transformOf(WaterDevice device) {
        return halfTurn();
    }

    /** The half turn about the vertical axis that every water display takes. */
    public static Transformation halfTurn() {
        return new Transformation(new Vector3f(0f, 0f, 0f), new Quaternionf().rotationY((float) Math.PI),
                new Vector3f(1f, 1f, 1f), new Quaternionf());
    }

    /** The item a device's display shows: its machine item, with the model of its shape and level. */
    Optional<ItemStack> displayItem(WaterDevice device) {
        return modelItem(device, modelKey(device));
    }

    /** The item a tall pump's upper display shows: the same machine item, with the upper half of the model. */
    Optional<ItemStack> topItem(WaterDevice device) {
        return modelItem(device, topModelKey(device));
    }

    /** Spawns the display, the upper display of a tall pump, and the interaction of a device at its block. */
    void spawn(WaterDevice device, Location block) {
        displayItem(device).ifPresent(item -> device.setDisplay(spawnDisplay(block, item, transformOf(device), device.id())));
        if (device.part() == WaterPart.PUMP && PumpLevels.height(device.level()) > 1) {
            Location upper = block.clone().add(0, 1, 0);
            topItem(device).ifPresent(item -> device.setTopDisplay(spawnDisplay(upper, item, transformOf(device), device.id())));
        }
        syncArms(device, block);
        device.setInteraction(spawnInteraction(block, device.id(), interactionHeight(device)));
    }

    /**
     * Makes a junction's arms match its connections: one arm display to each connected face. A pipe that is not a junction has none. The
     * arms are made again only when the set of connected faces changes.
     */
    void syncArms(WaterDevice device, Location block) {
        boolean junction = device.part() == WaterPart.PIPE
                && WaterOrientation.pipe(device.shown()).shape() == WaterOrientation.PipeShape.JUNCTION;
        Set<Face> wanted = junction ? device.shown() : Set.of();
        if (device.arms().keySet().equals(wanted)) {
            return;
        }
        removeArms(device);
        if (wanted.isEmpty()) {
            return;
        }
        for (Face face : wanted) {
            modelItem(device, ItemFactory.displayKey(WaterModels.arm(face)))
                    .ifPresent(item -> device.arms().put(face, spawnDisplay(block, item, halfTurn(), device.id())));
        }
    }

    /** Shows a device's current model and turn on its displays, when they exist. A junction's arms follow its connections. */
    void refresh(WaterDevice device, Location block) {
        syncArms(device, block);
        Entity entity = device.display() == null ? null : Bukkit.getEntity(device.display());
        if (entity instanceof ItemDisplay shown) {
            displayItem(device).ifPresent(shown::setItemStack);
            shown.setTransformation(transformOf(device));
        }
        Entity top = device.topDisplay() == null ? null : Bukkit.getEntity(device.topDisplay());
        if (top instanceof ItemDisplay shown) {
            topItem(device).ifPresent(shown::setItemStack);
            shown.setTransformation(transformOf(device));
        }
    }

    /** Removes a device's displays and interaction, and forgets their ids. The hologram is the service's to release. */
    void release(WaterDevice device) {
        remove(device.display());
        remove(device.topDisplay());
        removeArms(device);
        remove(device.interaction());
        device.setDisplay(null);
        device.setTopDisplay(null);
        device.setInteraction(null);
    }

    /** Makes a hologram above a pump of the given height, with the given lines, when holograms are on. */
    Optional<Hologram> hologram(Location block, int height, List<String> lines) {
        return holograms.create(HologramService.spotAbove(block, height), lines);
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

    /** The height of a device's interaction: both blocks of a tall pump, one block otherwise. */
    static float interactionHeight(WaterDevice device) {
        return device.part() == WaterPart.PUMP && PumpLevels.height(device.level()) > 1 ? TALL_INTERACTION : 1.0f;
    }

    private Optional<ItemStack> modelItem(WaterDevice device, String modelKey) {
        ComponentDefinition definition = core.componentDefinitions().get(device.part().componentId());
        if (definition == null) {
            return Optional.empty();
        }
        ItemStack item = core.items().machine(definition);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setItemModel(new NamespacedKey(ItemFactory.NAMESPACE, modelKey));
            item.setItemMeta(meta);
        }
        return Optional.of(item);
    }

    private static void removeArms(WaterDevice device) {
        for (UUID arm : device.arms().values()) {
            remove(arm);
        }
        device.arms().clear();
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

    private static UUID spawnInteraction(Location block, String deviceId, float height) {
        Interaction interaction = block.getWorld().spawn(block.clone().add(0.5, 0.0, 0.5), Interaction.class, entity -> {
            entity.setInteractionWidth(1.0f);
            entity.setInteractionHeight(height);
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
}
