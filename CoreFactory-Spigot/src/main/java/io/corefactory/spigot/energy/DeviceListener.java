package io.corefactory.spigot.energy;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import io.corefactory.core.energy.EnergyTable;
import io.corefactory.core.energy.WireCheck;
import io.corefactory.spigot.device.DevicePart;
import io.corefactory.spigot.logistics.LogisticsIds;
import io.corefactory.spigot.machine.InteractionRoute;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Routes the clicks on a device's interaction entity and the chunk events of the devices. A right-click with the energy wire in the
 * main hand runs the wire tool; a sneaking right-click cancels a wire start. A right-click with anything else on a generator opens its
 * menu, and closing the menu gives its fuel back into the count. A sneaking right-click with a picker or a splitter in hand is left
 * to the logistics listener, which places it against the device. A left-click breaks the device: a block break event is fired for each of
 * its blocks, so protection plugins see it, and the device goes with its wires and drops its item, and a generator its fuel, when no one
 * cancels it.
 */
public final class DeviceListener implements Listener {

    private final CoreFactoryCore core;
    private final DeviceRegistry devices;
    private final EnergyService energy;

    public DeviceListener(CoreFactoryCore core, DeviceRegistry devices, EnergyService energy) {
        this.core = core;
        this.devices = devices;
        this.energy = energy;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        devices.sweep(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        devices.unloaded(event.getChunk());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        energy.forgetPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Optional<EnergyDevice> device = devices.byInteraction(event.getRightClicked().getUniqueId());
        if (device.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (player.isSneaking() && event.getHand() == EquipmentSlot.HAND
                && LogisticsIds.takesSideAim(core.items().componentId(held).orElse(null))) {
            return;
        }
        event.setCancelled(true);
        if (InteractionRoute.of(InteractionRoute.Click.RIGHT, event.getHand() == EquipmentSlot.HAND) != InteractionRoute.OPEN) {
            return;
        }
        if (EnergyService.WIRE_ITEM.equals(core.items().componentId(held).orElse(null))) {
            WireSelection.Result result = energy.clickWire(player.getUniqueId(), device.get(), player.isSneaking());
            tell(player, result, device.get());
        } else if (device.get() instanceof GeneratorDevice generator) {
            openGenerator(player, generator);
        }
    }

    /** Shows a generator's menu to a player, unless someone else has it open. */
    private static void openGenerator(Player player, GeneratorDevice generator) {
        GeneratorMenu menu = generator.menu();
        if (menu.isOpen()) {
            player.sendMessage(Messages.color("&cSomeone is already using this generator."));
            return;
        }
        menu.open(player);
    }

    /** A generator's menu closed: its fuel slot goes back into the count the generator keeps. */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GeneratorMenu menu) {
            menu.absorb();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        Optional<EnergyDevice> device = deviceOf(event.getEntity());
        if (device.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        breakDevice(device.get(), player);
    }

    /**
     * Breaks a device through a break event fired for each of its blocks, and refuses it when any block of the device is protected. The
     * upper block of a tall device is not a vanilla block, so the event is fired for it directly, as the blast protection reads it.
     */
    private void breakDevice(EnergyDevice device, Player player) {
        Location block = device.block();
        for (Location part : DevicePart.blocksOf(block, device.height())) {
            BlockBreakEvent event = new BlockBreakEvent(part.getBlock(), player);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
        }
        if (anyProtected(device) || !energy.removeDevice(device)) {
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE && block.getWorld() != null) {
            for (ItemStack item : devices.drops(device)) {
                block.getWorld().dropItemNaturally(block.clone().add(0.5, 0.5, 0.5), item);
            }
        }
    }

    private boolean anyProtected(EnergyDevice device) {
        for (Location block : DevicePart.blocksOf(device.block(), device.height())) {
            if (core.protection().isProtected(block.getBlock())) {
                return true;
            }
        }
        return false;
    }

    private Optional<EnergyDevice> deviceOf(Entity entity) {
        if (!(entity instanceof Interaction)) {
            return Optional.empty();
        }
        return devices.byInteraction(entity.getUniqueId());
    }

    private void tell(Player player, WireSelection.Result result, EnergyDevice device) {
        switch (result.verdict()) {
            case SELECTED -> player.sendMessage(Messages.color("&7Wire start: &f" + energy.label(device)));
            case CANCELLED -> player.sendMessage(Messages.color("&7Wire selection cancelled."));
            case MADE -> player.sendMessage(Messages.color("&aWire made."));
            case REFUSED -> player.sendMessage(Messages.color(refusal(result.check())));
            case OTHER_WORLD -> player.sendMessage(Messages.color("&cA wire cannot cross worlds."));
            case DUPLICATE -> player.sendMessage(Messages.color("&cThose two are already wired."));
            case IDLE -> {
            }
        }
    }

    private static String refusal(WireCheck check) {
        return switch (check) {
            case OK -> "&7Wire made.";
            case SAME_DEVICE -> "&cA wire cannot join a device to itself.";
            case NOT_WIRABLE -> "&cThis device takes no wire. Wires join a generator to a pylon, or two pylons.";
            case SAME_KIND -> "&cTwo generators cannot be wired together.";
            case TOO_LONG -> "&cThat is too far for a wire: at most " + EnergyTable.WIRE_MAX_LENGTH + " blocks.";
            case DUPLICATE -> "&cThose two are already wired.";
        };
    }
}
