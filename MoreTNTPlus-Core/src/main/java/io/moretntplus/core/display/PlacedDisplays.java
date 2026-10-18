package io.moretntplus.core.display;

import io.moretntplus.core.item.ItemFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.UUID;

/**
 * Item displays that show a placed TNT's custom model. The TNT block stays underneath as the hitbox and the
 * ignition target; the display sits a hair outside it so the vanilla texture is hidden.
 */
public final class PlacedDisplays {

    private static final NamespacedKey DISPLAY_MARK = new NamespacedKey(ItemFactory.NAMESPACE, "display");

    private float scale;

    public PlacedDisplays(float scale) {
        this.scale = scale;
    }

    public void reconfigure(float scale) {
        this.scale = scale;
    }

    /** Shows {@code item} on the block at {@code blockLocation} and returns the display's id. */
    public UUID spawn(Location blockLocation, ItemStack item) {
        World world = blockLocation.getWorld();
        // An item display is centred on its origin, so spawn at the block centre.
        ItemDisplay display = world.spawn(blockLocation.clone().add(0.5, 0.5, 0.5), ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            entity.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new Quaternionf(),
                    new Vector3f(scale, scale, scale),
                    new Quaternionf()));
            entity.setPersistent(true);
            entity.getPersistentDataContainer().set(DISPLAY_MARK, PersistentDataType.BYTE, (byte) 1);
        });
        return display.getUniqueId();
    }

    public void remove(UUID displayId) {
        if (displayId == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(displayId);
        if (entity != null) {
            entity.remove();
        }
    }

    /** Removes displays left behind by a crash: marked displays whose id no placed TNT owns. */
    public int sweepOrphans(Collection<UUID> owned) {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (ItemDisplay display : world.getEntitiesByClass(ItemDisplay.class)) {
                boolean marked = display.getPersistentDataContainer().has(DISPLAY_MARK, PersistentDataType.BYTE);
                if (marked && !owned.contains(display.getUniqueId())) {
                    display.remove();
                    removed++;
                }
            }
        }
        return removed;
    }
}
