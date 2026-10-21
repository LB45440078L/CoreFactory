package io.corefactory.spigot.machine;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import io.corefactory.core.device.Footprint;
import io.corefactory.spigot.device.DevicePart;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Routes the clicks on a machine's interaction entity, which stands over its host block. A concealed client sees air
 * there and sends its clicks to the entity. A right-click opens the machine's menu through {@link
 * MachineListener#openMenu}, as a right-click on the host does, unless the player sneaks with a block in hand, which is
 * then placed against the host. A left-click breaks the machine: a block break event is fired for each block the machine
 * fills, the host and a tall machine's upper block, so the machine's break handler and any protection plugin see them, and
 * the machine goes when no one cancels one of them. Clicks on other entities are not touched.
 */
public final class MachineInteractions implements Listener {

    private final MachineStore store;
    private final MachineListener machines;
    private final HandPlacement hands;

    public MachineInteractions(CoreFactoryCore core, MachineStore store, MachineListener machines) {
        this.store = store;
        this.machines = machines;
        this.hands = new HandPlacement(core);
    }

    /**
     * A right-click on the entity opens the menu, or, for a sneaking player with a block in hand, places the block against the
     * host. The decision is {@link ClickDecision}; the placement is {@link HandPlacement}.
     */
    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        InteractionRoute route = InteractionRoute.of(InteractionRoute.Click.RIGHT, event.getHand() == EquipmentSlot.HAND);
        if (route != InteractionRoute.OPEN) {
            return;
        }
        Optional<PlacedMachine> placed = machineOf(event.getRightClicked());
        if (placed.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        Block host = placed.get().block().getBlock();
        ClickDecision.Held kind = hands.heldKind(held);
        Optional<BlockFace> face = player.isSneaking() && kind == ClickDecision.Held.BLOCK
                ? hands.aimedFace(player, host) : Optional.empty();
        boolean free = face.isPresent() && HandPlacement.free(host.getRelative(face.get()));
        switch (ClickDecision.of(player.isSneaking(), kind, face.isPresent(), free)) {
            case OPEN_MENU -> {
                if (machines.openMenu(placed.get(), player, held)) {
                    event.setCancelled(true);
                }
            }
            case PLACE_BY_HAND -> {
                hands.place(player, host, face.get(), held);
                event.setCancelled(true);
            }
            case PASS_THROUGH -> {
                if (kind == ClickDecision.Held.BLOCK) {
                    player.sendMessage(Messages.color("&cThere is no room to place that block here."));
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        Optional<PlacedMachine> placed = machineOf(event.getEntity());
        if (placed.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        breakMachine(placed.get(), player);
    }

    /**
     * Breaks a machine once no break event for any of its blocks is cancelled: the host block and, for a tall machine, the block above
     * it. The break events are fired with the break handler held back, so the machine stays until the check has passed; then it is
     * removed as the break handler would remove it, and the host block becomes air.
     */
    private void breakMachine(PlacedMachine placed, Player player) {
        if (!checkBreak(DevicePart.blocksOf(placed.block(), Footprint.heightOf(placed.machine().id())), player)) {
            return;
        }
        store.remove(placed, player.getGameMode() != GameMode.CREATIVE);
        Block block = placed.block().getBlock();
        if (store.at(placed.block()).isEmpty()) {
            block.setType(Material.AIR);
        }
    }

    /** Fires a break event for each block of a machine. True when no event is cancelled. */
    private boolean checkBreak(List<Location> blocks, Player player) {
        machines.holdBreaks(true);
        try {
            for (Location part : blocks) {
                BlockBreakEvent event = new BlockBreakEvent(part.getBlock(), player);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return false;
                }
            }
            return true;
        } finally {
            machines.holdBreaks(false);
        }
    }

    /** The machine whose interaction entity this is, or empty for any other entity or a leftover one. */
    private Optional<PlacedMachine> machineOf(Entity entity) {
        if (!(entity instanceof Interaction)) {
            return Optional.empty();
        }
        return store.at(entity.getLocation()).filter(placed -> entity.getUniqueId().equals(placed.interaction()));
    }
}
