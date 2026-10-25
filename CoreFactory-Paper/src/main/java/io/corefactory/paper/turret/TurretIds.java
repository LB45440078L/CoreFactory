package io.corefactory.paper.turret;

import io.corefactory.core.item.ItemFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.OptionalInt;

/** The ids of the turrets: their items, their device types, the marker their entities carry, and the level an item carries. */
public final class TurretIds {

    /** The component id of the machine gun turret's item. */
    public static final String ITEM = "machine_gun_turret";

    /** The device type of the machine gun turret written in the turret store. */
    public static final String DEVICE_TYPE = "turret";

    /** The component id of the laser turret's item. */
    public static final String LASER_ITEM = "laser_turret";

    /** The device type of the laser turret written in the turret store. */
    public static final String LASER_DEVICE_TYPE = "laser_turret";

    /** The component id of the rail gun's item. */
    public static final String RAILGUN_ITEM = "railgun_turret";

    /** The device type of the rail gun written in the turret store. */
    public static final String RAILGUN_DEVICE_TYPE = "railgun";

    /** The prefix of an upgrade kit's component id; the level the kit makes follows it, such as {@code turret_upgrade_3}. */
    public static final String KIT_PREFIX = "turret_upgrade_";

    /** The permission an owner needs to change the targeting of the turrets they placed: the menu toggles and the turret command. */
    public static final String PERMISSION = "corefactory.turret";

    /** The marker on the entities of a turret, so entities left behind by an earlier run are found and removed. */
    static final NamespacedKey MARK = new NamespacedKey(ItemFactory.NAMESPACE, "turret");

    /** The level a turret item carries. Only a turret above level 1 writes it, so the items of level 1 stack as they always did. */
    static final NamespacedKey LEVEL = new NamespacedKey(ItemFactory.NAMESPACE, "turret_level");

    private TurretIds() {
    }

    /**
     * The level an upgrade kit makes, read from its component id.
     *
     * @param componentId the component id, such as {@code turret_upgrade_4}; null is no kit
     * @return the level, 2 to 5, or empty when the id is no kit
     */
    public static OptionalInt kitLevelOf(String componentId) {
        if (componentId == null || !componentId.startsWith(KIT_PREFIX)) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(componentId.substring(KIT_PREFIX.length())));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    /**
     * Writes the level onto a turret item. A level 1 item is left as it is.
     *
     * @param item  the turret item, which is changed
     * @param level the level it carries
     * @return the same item
     */
    static ItemStack withLevel(ItemStack item, int level) {
        if (level <= 1) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(LEVEL, PersistentDataType.INTEGER, level);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * The level a turret item carries: 1 when it carries none.
     *
     * @param item the turret item, or null
     */
    static int levelOf(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return 1;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 1;
        }
        Integer level = meta.getPersistentDataContainer().get(LEVEL, PersistentDataType.INTEGER);
        return level == null ? 1 : level;
    }
}
