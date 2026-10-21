package io.corefactory.spigot.machine;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import io.corefactory.core.device.Footprint;
import io.corefactory.core.drill.DrillCatalog;
import io.corefactory.core.drill.DrillRules;
import io.corefactory.core.process.Machine;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.spigot.achievement.AchievementTracker;
import io.corefactory.spigot.energy.DeviceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Routes placing, breaking and right-clicking machines. A machine's component item places its host block and registers
 * the machine. Breaking the host removes the machine and returns its items. Right-clicking the host opens its menu and
 * cancels the block's own interaction, so a furnace or dispenser does not open its vanilla inventory instead.
 */
public final class MachineListener implements Listener {

    private final CoreFactoryCore core;
    private final ProcessCatalog catalog;
    private final DrillCatalog drills;
    private final MachineStore store;
    private final DeviceRegistry devices;
    private final AchievementTracker achievements;
    private boolean holdingBreaks;

    public MachineListener(CoreFactoryCore core, ProcessCatalog catalog, DrillCatalog drills, MachineStore store,
                           DeviceRegistry devices, AchievementTracker achievements) {
        this.core = core;
        this.catalog = catalog;
        this.drills = drills;
        this.store = store;
        this.devices = devices;
        this.achievements = achievements;
    }

    /**
     * Refuses a placement that a machine may not take, before anything is changed. Runs after the research gate, so a locked machine is
     * already cancelled here. A tall machine's upper block is checked by a placement event of its own, as the energy devices' is. The
     * machine is registered by {@link #onPlaced}, once the placement stands.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (upperCell(event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Messages.color("&cThat block belongs to a two-block device."));
            return;
        }
        Optional<Machine> machine = machineItem(event.getItemInHand());
        if (machine.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();
        if (core.isDisabledWorld(block.getWorld())) {
            event.setCancelled(true);
            player.sendMessage(core.messages().get("disabled-world"));
            return;
        }
        if (drills.drill(machine.get().id()).isPresent()
                && !DrillRules.hardBase(block.getRelative(BlockFace.DOWN).getType().name())) {
            event.setCancelled(true);
            player.sendMessage(Messages.color("&cA drill needs hard stone under it: stone, granite, diorite, andesite, "
                    + "tuff or deepslate."));
            return;
        }
        if (Footprint.heightOf(machine.get().id()) > Footprint.SHORT) {
            Block upper = block.getRelative(BlockFace.UP);
            if (!free(upper) || devices.at(upper.getLocation()).isPresent() || store.at(upper.getLocation()).isPresent()) {
                event.setCancelled(true);
                player.sendMessage(Messages.color("&cThis drill is two blocks tall: the block above it must be free."));
                return;
            }
            if (!protectedCell(upper, block, Material.matchMaterial(machine.get().host()), player)) {
                event.setCancelled(true);
                player.sendMessage(Messages.color("&cThis spot is protected."));
            }
        }
    }

    /**
     * Registers a machine once its placement stands. It runs at MONITOR, after every other handler, and only when the event is not
     * cancelled: a protection plugin may still cancel the placement at a later priority, and then no machine is registered. A
     * tall machine's upper block is cleared here, with its host block set.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaced(BlockPlaceEvent event) {
        Optional<Machine> machine = machineItem(event.getItemInHand());
        if (machine.isEmpty()) {
            return;
        }
        Block block = event.getBlockPlaced();
        if (Footprint.heightOf(machine.get().id()) > Footprint.SHORT) {
            Block upper = block.getRelative(BlockFace.UP);
            if (!upper.getType().isAir()) {
                upper.setType(Material.AIR, false);
            }
        }
        Material host = Material.matchMaterial(machine.get().host());
        if (host != null && block.getType() != host) {
            block.setType(host, false);
        }
        store.place(block.getLocation(), machine.get());
        achievements.placed(event.getPlayer(), machine.get().id());
    }

    /** The machine an item places: a lab tool from processes.yml, or a drill, or empty for any other item. */
    private Optional<Machine> machineItem(ItemStack item) {
        return core.items().componentId(item).flatMap(id -> catalog.machine(id).or(() -> drills.machine(id)));
    }

    /**
     * Fires a placement event for a block a tall machine fills above its host, so a protection plugin may refuse it. The event carries
     * the host block's item, not the machine's, so the machine's own placement handlers do not run for it. True when nothing refuses it.
     */
    private static boolean protectedCell(Block cell, Block against, Material host, Player player) {
        ItemStack item = new ItemStack(host == null ? Material.STONE : host);
        BlockPlaceEvent check = new BlockPlaceEvent(cell, cell.getState(), against, item, player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(check);
        return !check.isCancelled();
    }

    /**
     * True when a spot is the upper block of a tall machine or energy device, which is not the device's own anchor. A vanilla block is
     * not placed there, so nothing joins the device from outside.
     */
    private boolean upperCell(Location spot) {
        return store.at(spot).filter(placed -> placed.block().getBlockY() != spot.getBlockY()).isPresent()
                || devices.at(spot).filter(device -> device.block().getBlockY() != spot.getBlockY()).isPresent();
    }

    /** A block a tall machine may fill above its host: air, or a replaceable block that is not liquid, which is then cleared. */
    private static boolean free(Block block) {
        Material type = block.getType();
        return type.isAir() || (Tag.REPLACEABLE.isTagged(type) && !block.isLiquid());
    }

    /**
     * Holds the break handler back, or lets it act again. It is held while a check fires break events for a machine's blocks, since each
     * event would otherwise remove the machine before the check has passed.
     */
    void holdBreaks(boolean hold) {
        holdingBreaks = hold;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (holdingBreaks) {
            return;
        }
        Optional<PlacedMachine> placed = store.at(event.getBlock().getLocation());
        if (placed.isEmpty()) {
            return;
        }
        event.setDropItems(false);
        store.remove(placed.get(), event.getPlayer().getGameMode() != GameMode.CREATIVE);
    }

    /** Explosions do not break machines: the blocks stay and the machine keeps running. */
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> store.at(block.getLocation()).isPresent());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> store.at(block.getLocation()).isPresent());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        Optional<PlacedMachine> placed = store.at(event.getClickedBlock().getLocation());
        if (placed.isEmpty() || !openMenu(placed.get(), event.getPlayer(), event.getItem())) {
            return;
        }
        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
    }

    /**
     * Opens a machine's menu for a right-click on it, on its host block or on its interaction entity. A player sneaking with an
     * item in hand places that item against the machine instead, so nothing opens.
     *
     * @return true when the menu was opened, so the caller takes the click as the machine's
     */
    public boolean openMenu(PlacedMachine placed, Player player, ItemStack held) {
        if (player.isSneaking() && held != null && !held.getType().isAir()) {
            return false;
        }
        placed.menu().open(player);
        return true;
    }

    /** Writes a closing menu's last changes into its barrel, so nothing a player moved is left in the menu. */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof MachineMenu menu) {
            menu.push();
        }
    }

    /** A barrel's live inventory is valid only while its chunk is loaded, so it is read again after the chunk loads. */
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        store.unloaded(event.getChunk());
    }
}
