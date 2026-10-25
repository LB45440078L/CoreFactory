package io.corefactory.paper.vertical;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.item.ComponentDefinition;
import io.corefactory.core.item.ItemFactory;
import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.PortItems;
import io.corefactory.core.vertical.VerticalDir;
import io.corefactory.paper.machine.ItemRefs;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The entities of the vertical belts: the item display that shows a belt, the interaction that takes its clicks, and the item displays
 * that show the items carried on a tower. Every belt entity carries a PDC marker, so the entities a previous run or an unloaded chunk
 * left behind are found and removed. An item display is not persistent: it goes when its chunk unloads.
 */
final class VerticalEntities {

    /** The marker of a belt's display and interaction. */
    static final NamespacedKey BELT_MARK = new NamespacedKey(ItemFactory.NAMESPACE, "vertical_belt");

    /** The marker of an item display on a tower. */
    static final NamespacedKey ITEM_MARK = new NamespacedKey(ItemFactory.NAMESPACE, "vertical_item");

    /** The scale of an item on a tower. */
    static final float ITEM_SCALE = 0.3f;

    private final CoreFactoryCore core;
    private final ItemRefs refs;

    VerticalEntities(CoreFactoryCore core, ItemRefs refs) {
        this.core = core;
        this.refs = refs;
    }

    /** The item a belt's display shows: its machine model. Empty when the component is not defined. */
    Optional<ItemStack> beltItem(int level) {
        ComponentDefinition definition = core.componentDefinitions().get(VerticalIds.componentId(level));
        return definition == null ? Optional.empty() : Optional.of(core.items().machine(definition));
    }

    /** The item a belt gives back when it is broken: its component item. */
    Optional<ItemStack> deviceItem(int level) {
        ComponentDefinition definition = core.componentDefinitions().get(VerticalIds.componentId(level));
        return definition == null ? Optional.empty() : Optional.of(core.items().component(definition, 1));
    }

    /** A stack of one item for a logistics item, or empty when the item names no stack. */
    Optional<ItemStack> stackOf(Item item) {
        return Optional.ofNullable(refs.create(PortItems.refOf(item), 1));
    }

    /**
     * Spawns the display of a belt at its block. A belt that flows down is turned about its front-to-back axis, so its arrow strip
     * runs the other way.
     *
     * @return the display's id, or null when the belt's model is not defined
     */
    UUID spawnBelt(Location block, Optional<ItemStack> item, VerticalDir direction, String beltId) {
        if (item.isEmpty()) {
            return null;
        }
        float scale = core.settings().displayScale();
        Quaternionf turn = turn(direction, new Quaternionf());
        ItemDisplay display = block.getWorld().spawn(block.clone().add(0.5, 0.5, 0.5), ItemDisplay.class, entity -> {
            entity.setItemStack(item.get());
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setTransformation(new Transformation(new Vector3f(0f, 0f, 0f), turn, new Vector3f(scale, scale, scale),
                    new Quaternionf()));
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(BELT_MARK, PersistentDataType.STRING, beltId);
        });
        return display.getUniqueId();
    }

    /** Turns a belt's display for its direction, keeping its scale. */
    void turnBelt(UUID display, VerticalDir direction) {
        Entity entity = display == null ? null : Bukkit.getEntity(display);
        if (entity instanceof ItemDisplay shown) {
            Transformation old = shown.getTransformation();
            shown.setTransformation(new Transformation(old.getTranslation(), turn(direction, new Quaternionf()), old.getScale(),
                    old.getRightRotation()));
        }
    }

    /** Spawns the interaction of a belt at its block: a one-block box that takes its clicks. */
    UUID spawnInteraction(Location block, String beltId) {
        Interaction interaction = block.getWorld().spawn(block.clone().add(0.5, 0.0, 0.5), Interaction.class, entity -> {
            entity.setInteractionWidth(1.0f);
            entity.setInteractionHeight(1.0f);
            entity.setResponsive(true);
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(BELT_MARK, PersistentDataType.STRING, beltId);
        });
        return interaction.getUniqueId();
    }

    /**
     * Spawns the display of one item on a tower, at the centre of a block, with its height set. Teleports and transforms glide over
     * one tick. The display is not persistent and cannot be picked up.
     *
     * @return the display's id
     */
    UUID spawnItem(Location block, ItemStack item, double dy) {
        ItemDisplay display = block.getWorld().spawn(block.clone().add(0.5, 0.5, 0.5), ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setTransformation(itemTransform(dy));
            entity.setInterpolationDelay(0);
            entity.setInterpolationDuration(1);
            entity.setTeleportDuration(1);
            entity.setPersistent(false);
            entity.getPersistentDataContainer().set(ITEM_MARK, PersistentDataType.BYTE, (byte) 1);
        });
        return display.getUniqueId();
    }

    /** Sets the item an item display shows. */
    void setItem(UUID display, ItemStack item) {
        Entity entity = Bukkit.getEntity(display);
        if (entity instanceof ItemDisplay shown) {
            shown.setItemStack(item);
        }
    }

    /**
     * Sets the height of an item display, as an offset from the centre of the block it is anchored in.
     *
     * @return false when the display is gone
     */
    boolean moveItem(UUID display, double dy) {
        Entity entity = Bukkit.getEntity(display);
        if (!(entity instanceof ItemDisplay item)) {
            return false;
        }
        item.setTransformation(itemTransform(dy));
        return true;
    }

    /** Moves an item display's anchor to the centre of a block. */
    void anchor(UUID display, Location block) {
        Entity entity = Bukkit.getEntity(display);
        if (entity != null) {
            entity.teleport(block.clone().add(0.5, 0.5, 0.5));
        }
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

    /**
     * Removes the marked entities of a chunk that nothing owns: belt entities not held by a belt, and item displays not in
     * {@code shownItems}.
     */
    void sweep(Chunk chunk, Set<UUID> ownedBelts, Set<UUID> shownItems) {
        for (Entity entity : chunk.getEntities()) {
            if (entity.getPersistentDataContainer().has(BELT_MARK, PersistentDataType.STRING)) {
                if (!ownedBelts.contains(entity.getUniqueId())) {
                    entity.remove();
                }
            } else if (entity.getPersistentDataContainer().has(ITEM_MARK, PersistentDataType.BYTE)) {
                if (!shownItems.contains(entity.getUniqueId())) {
                    entity.remove();
                }
            }
        }
    }

    private static Transformation itemTransform(double dy) {
        return new Transformation(new Vector3f(0f, (float) dy, 0f), new Quaternionf(),
                new Vector3f(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE), new Quaternionf());
    }

    /** The turn of a belt's model: none for UP, a half turn about the front-to-back axis for DOWN. */
    private static Quaternionf turn(VerticalDir direction, Quaternionf into) {
        return direction == VerticalDir.UP ? into : into.rotationZ((float) Math.PI);
    }
}
