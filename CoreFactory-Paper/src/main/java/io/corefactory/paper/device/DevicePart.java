package io.corefactory.paper.device;

import io.corefactory.core.device.Footprint;
import io.corefactory.core.engine.BlastReach;
import io.corefactory.core.item.ItemFactory;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/**
 * Maps any block of a placed device to the device. A device is keyed by its anchor, the lower block; a tall device also holds the
 * block above its anchor, so a click, break or blast on that upper block finds the same device. The rule itself is {@link Footprint};
 * this class walks the blocks below a clicked block and asks the lookup for each anchor.
 */
public final class DevicePart {

    /** The suffix of the display model of the upper block of a tall device, after the lower model's key. */
    public static final String TOP_SUFFIX = "_top";

    private DevicePart() {
    }

    /**
     * The device a block belongs to: the device anchored at the block, or the tall device anchored at a block below it that covers
     * this one. A short device covers only its own block.
     *
     * @param block    the block clicked, broken or blasted
     * @param at       the device anchored at a block, if any; given the anchor block, which is always in the block's own world
     * @param heightOf the footprint height of a device, from {@link Footprint#heightOf}
     * @param <T>      the device type
     * @return the device, or empty when no device covers the block
     */
    public static <T> Optional<T> covering(Location block, Function<Location, Optional<T>> at, ToIntFunction<T> heightOf) {
        for (int depth = 0; depth < Footprint.MAX_HEIGHT; depth++) {
            Optional<T> device = at.apply(below(block, depth));
            if (device.isPresent() && Footprint.covers(heightOf.applyAsInt(device.get()), depth)) {
                return device;
            }
        }
        return Optional.empty();
    }

    /**
     * The blocks a device of a height fills, from its anchor upward, in order: the anchor first.
     *
     * @param anchor the anchor block; its world may be null
     * @param height the footprint height, from {@link Footprint#heightOf}
     */
    public static List<Location> blocksOf(Location anchor, int height) {
        List<Location> blocks = new ArrayList<>(height);
        for (int dy = 0; dy < height; dy++) {
            blocks.add(new Location(anchor.getWorld(), anchor.getBlockX(), anchor.getBlockY() + dy, anchor.getBlockZ()));
        }
        return blocks;
    }

    /**
     * True when a blast centred at a point reaches any block of a device: the device is hit when the centre of one of its blocks
     * lies within the radius, in the blast's world.
     *
     * @param world  the blast's world
     * @param anchor the device's anchor block
     * @param height the footprint height of the device
     */
    public static boolean reaches(World world, Location anchor, int height, double x, double y, double z, double radius) {
        if (!world.equals(anchor.getWorld())) {
            return false;
        }
        for (Location block : blocksOf(anchor, height)) {
            if (BlastReach.within(x, y, z, block.getBlockX() + 0.5, block.getBlockY() + 0.5, block.getBlockZ() + 0.5, radius)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The display item of the upper block of a tall device: the lower item with its model switched to the upper half, which is the
     * lower model's key with {@link #TOP_SUFFIX} added.
     *
     * @param lower       the item the lower block shows
     * @param componentId the device's component id, such as {@code ore_drill_4}
     */
    public static ItemStack topItem(ItemStack lower, String componentId) {
        ItemStack top = lower.clone();
        ItemMeta meta = top.getItemMeta();
        if (meta != null) {
            meta.setItemModel(new NamespacedKey(ItemFactory.NAMESPACE, ItemFactory.displayKey(componentId) + TOP_SUFFIX));
            top.setItemMeta(meta);
        }
        return top;
    }

    private static Location below(Location block, int depth) {
        return new Location(block.getWorld(), block.getBlockX(), block.getBlockY() - depth, block.getBlockZ());
    }
}
