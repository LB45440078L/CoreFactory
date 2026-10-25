package io.corefactory.paper.machine;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * The barrel that holds a lab tool's items, read through its block state. The live inventory is kept only while the chunk
 * stays loaded: a block state is valid only while its chunk is, so the chunk's unload clears it and the next use reads the
 * barrel again. A slot number past the end of the live inventory is absent: it reads as empty and is never written. A
 * container smaller than the machine needs is reported once, and the slots it lacks stay absent.
 */
final class Barrel {

    private final Location site;
    private final int required;
    private Inventory live;
    private boolean reported;

    /**
     * @param site     the host block's position
     * @param required the barrel slots the machine uses
     */
    Barrel(Location site, int required) {
        this.site = site;
        this.required = required;
    }

    /** The host block's position, with whole block coordinates. */
    Location site() {
        return site;
    }

    /** The barrel's live inventory, or empty when its chunk is not loaded or its block is not a container. */
    Optional<Inventory> inventory() {
        World world = site.getWorld();
        if (world == null || !world.isChunkLoaded(site.getBlockX() >> 4, site.getBlockZ() >> 4)) {
            live = null;
            return Optional.empty();
        }
        if (live == null) {
            BlockState state = site.getBlock().getState();
            if (state instanceof Container container) {
                live = container.getInventory();
                reportIfTooSmall();
            }
        }
        return Optional.ofNullable(live);
    }

    /** Forgets the inventory, so the next use reads the barrel again. Called when the barrel's chunk unloads. */
    void forget() {
        live = null;
    }

    /** Drops a stack at the barrel's block, for items that no slot can keep. */
    void drop(ItemStack item) {
        World world = site.getWorld();
        if (world != null && item != null && !item.getType().isAir()) {
            world.dropItemNaturally(site.clone().add(0.5, 0.5, 0.5), item);
        }
    }

    /** True when the inventory has this slot. */
    static boolean has(Inventory inventory, int index) {
        return index >= 0 && index < inventory.getSize();
    }

    /** The item in a slot, or null when the slot is empty or the inventory has no such slot. */
    static ItemStack itemAt(Inventory inventory, int index) {
        return has(inventory, index) ? inventory.getItem(index) : null;
    }

    /** Sets a slot's item. Does nothing when the inventory has no such slot. */
    static void setItemAt(Inventory inventory, int index, ItemStack item) {
        if (has(inventory, index)) {
            inventory.setItem(index, item);
        }
    }

    private void reportIfTooSmall() {
        if (reported || live.getSize() >= required) {
            return;
        }
        reported = true;
        Bukkit.getLogger().warning("CoreFactory: host too small at " + site.getBlockX() + " " + site.getBlockY() + " "
                + site.getBlockZ() + ": a container of " + live.getSize() + " slots, but its machine needs "
                + required + ". The slots it lacks are unused.");
    }
}
