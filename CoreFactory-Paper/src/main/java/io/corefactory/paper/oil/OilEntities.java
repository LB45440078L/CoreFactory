package io.corefactory.paper.oil;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.oil.OilModels;
import io.corefactory.core.oil.OilPart;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.WaterOrientation;
import io.corefactory.paper.water.WaterEntities;
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

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The entities of the oil tubes: the item display that shows a tube, the arm displays of a junction, and the interaction that takes its
 * clicks. Every entity carries a PDC marker with its device id, so the entities a previous run left behind are found and removed. The
 * entities are not persistent: they go when their chunk unloads, and the service makes new ones when the chunk loads.
 *
 * <p>Every display is drawn from an oil model id (see {@link OilModels}) and takes the water displays' transform, a half turn about the
 * vertical axis about the block centre, so an oil tube lies on the faces its model names, as a water pipe does.
 */
final class OilEntities {

    /** The marker of a tube's entities, which holds the device id. */
    static final NamespacedKey DEVICE_MARK = new NamespacedKey(ItemFactory.NAMESPACE, "oil_device");

    private final CoreFactoryCore core;

    OilEntities(CoreFactoryCore core) {
        this.core = core;
    }

    /** The item a part gives back when it is removed: its component item. */
    Optional<ItemStack> itemOf(OilPart part) {
        ComponentDefinition definition = core.componentDefinitions().get(part.componentId());
        if (definition == null) {
            return Optional.empty();
        }
        return Optional.of(core.items().component(definition, 1));
    }

    /**
     * The id of the model a tube is drawn with: a pipe's straight run, bend or junction hub from the faces it joins, and a splitter's model
     * from its input and output.
     */
    static String modelId(OilDevice device) {
        return switch (device.part()) {
            case PIPE -> OilModels.pipe(device.shown());
            case TEE -> OilModels.tee(device.face(), device.output());
            case CROSS -> OilModels.cross(device.face(), device.output());
        };
    }

    /** The item a tube's display shows: its machine item, with the model of its shape. */
    Optional<ItemStack> displayItem(OilDevice device) {
        return modelItem(device, ItemFactory.displayKey(modelId(device)));
    }

    /** Spawns the display and the interaction of a tube at its block, and its arms when it is a junction. */
    void spawn(OilDevice device, Location block) {
        displayItem(device).ifPresent(item -> device.setDisplay(spawnDisplay(block, item, device.id())));
        syncArms(device, block);
        device.setInteraction(spawnInteraction(block, device.id()));
    }

    /**
     * Makes a junction's arms match its connections: one arm display to each connected face. A pipe that is not a junction has none. The
     * arms are made again only when the set of connected faces changes.
     */
    void syncArms(OilDevice device, Location block) {
        boolean junction = device.part() == OilPart.PIPE
                && WaterOrientation.pipe(device.shown()).shape() == WaterOrientation.PipeShape.JUNCTION;
        Set<Face> wanted = junction ? device.shown() : Set.of();
        if (device.arms().keySet().equals(wanted)) {
            return;
        }
        removeArms(device);
        for (Face face : wanted) {
            modelItem(device, ItemFactory.displayKey(OilModels.arm(face)))
                    .ifPresent(item -> device.arms().put(face, spawnDisplay(block, item, device.id())));
        }
    }

    /** Shows a tube's current model and turn on its displays, when they exist. A junction's arms follow its connections. */
    void refresh(OilDevice device, Location block) {
        syncArms(device, block);
        Entity entity = device.display() == null ? null : Bukkit.getEntity(device.display());
        if (entity instanceof ItemDisplay shown) {
            displayItem(device).ifPresent(shown::setItemStack);
            shown.setTransformation(turn());
        }
    }

    /** Removes a tube's displays and interaction, and forgets their ids. */
    void release(OilDevice device) {
        remove(device.display());
        removeArms(device);
        remove(device.interaction());
        device.setDisplay(null);
        device.setInteraction(null);
    }

    /** Removes the marked entities of a chunk, so the entities of a previous run do not stay next to the new ones. */
    static void sweep(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (entity.getPersistentDataContainer().has(DEVICE_MARK, PersistentDataType.STRING)) {
                entity.remove();
            }
        }
    }

    /** The device id an entity belongs to, when it is an oil tube's entity. */
    static Optional<String> deviceOf(Entity entity) {
        return Optional.ofNullable(entity.getPersistentDataContainer().get(DEVICE_MARK, PersistentDataType.STRING));
    }

    /** True when an entity with this id exists and is valid. */
    static boolean alive(UUID entity) {
        Entity found = entity == null ? null : Bukkit.getEntity(entity);
        return found != null && found.isValid();
    }

    /** The transform of every oil display: the water displays' half turn. */
    private static Transformation turn() {
        return WaterEntities.halfTurn();
    }

    private Optional<ItemStack> modelItem(OilDevice device, String modelKey) {
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

    private static void removeArms(OilDevice device) {
        for (UUID arm : device.arms().values()) {
            remove(arm);
        }
        device.arms().clear();
    }

    private static UUID spawnDisplay(Location block, ItemStack item, String deviceId) {
        ItemDisplay display = block.getWorld().spawn(block.clone().add(0.5, 0.5, 0.5), ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setTransformation(turn());
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
}
