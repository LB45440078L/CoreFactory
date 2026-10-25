package io.corefactory.paper.artillery;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.text.Text;
import io.corefactory.paper.energy.DeviceRegistry;
import io.corefactory.paper.energy.PlacementRules;
import io.corefactory.paper.friends.FriendAccess;
import io.corefactory.paper.machine.MachineStore;
import io.corefactory.paper.research.ResearchHints;
import io.corefactory.paper.turret.TurretRegistry;
import io.corefactory.core.research.ResearchService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Places, opens and breaks the Command Centers, as {@link ArtilleryListener} does for the pieces. A right-click on a block with the command
 * center's item in the main hand places it on the air beside the clicked face. A right-click on its screen by its owner or a friend of the
 * owner opens the main dialog; anyone else is told it is not theirs. A left-click on the screen by one of them breaks it, after a block break
 * event for its block, and gives its item back.
 */
public final class CommandCenterListener implements Listener {

    private final CoreFactoryCore core;
    private final CommandCenterService service;
    private final DeviceRegistry devices;
    private final MachineStore machines;
    private final TurretRegistry turrets;
    private final ResearchService research;
    private final CommandCenterDialogs dialogs;
    private final ListeningMode targeting;
    private final FriendAccess friends;

    /**
     * @param core      the core, for the items and the disabled worlds
     * @param service   the Command Centers' runtime
     * @param devices   the energy devices, whose blocks a center may not take
     * @param machines  the placed machines, which keep their own clicks
     * @param turrets   the turrets, whose blocks a center may not take
     * @param research  the research progress, which gates the item
     * @param dialogs   the dialogs a click on the screen opens
     * @param targeting the Set Target mode, whose player's clicks on the screen are the map's, not the dialog's
     * @param friends   the friend rule, which lets a friend of the owner use a center
     */
    public CommandCenterListener(CoreFactoryCore core, CommandCenterService service, DeviceRegistry devices, MachineStore machines,
                                 TurretRegistry turrets, ResearchService research, CommandCenterDialogs dialogs, ListeningMode targeting,
                                 FriendAccess friends) {
        this.core = core;
        this.service = service;
        this.devices = devices;
        this.machines = machines;
        this.turrets = turrets;
        this.research = research;
        this.dialogs = dialogs;
        this.targeting = targeting;
        this.friends = friends;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        service.centers().sweep(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        service.centers().unloaded(event.getChunk());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack held = event.getItem();
        Block clicked = event.getClickedBlock();
        boolean isCenter = held != null && core.items().componentId(held).filter(ArtilleryIds::isCommandCenter).isPresent();
        if (clicked == null || !isCenter || machines.at(clicked.getLocation()).isPresent()) {
            return;
        }
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        Player player = event.getPlayer();
        Block target = clicked.getRelative(event.getBlockFace() == null ? BlockFace.UP : event.getBlockFace());
        Location spot = target.getLocation();
        String itemId = ArtilleryIds.COMMAND_CENTER;
        Optional<PlacementRules.Refusal> refusal = PlacementRules.before(core.isDisabledWorld(target.getWorld()),
                target.getType().isAir(), occupied(spot), research.isUnlocked(player.getUniqueId(), itemId));
        if (refusal.isPresent()) {
            refuse(player, itemId, refusal.get());
            return;
        }
        ItemStack one = held.clone();
        one.setAmount(1);
        BlockState replaced = target.getState();
        BlockPlaceEvent check = new BlockPlaceEvent(target, replaced, clicked, one, player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(check);
        if (check.isCancelled()) {
            refuse(player, itemId, PlacementRules.Refusal.PROTECTED);
            return;
        }
        service.place(spot, player.getUniqueId());
        if (player.getGameMode() != GameMode.CREATIVE) {
            take(player);
        }
        player.swingMainHand();
    }

    /** A block placed on a command center or on its map wall is refused: the center stands there. Any other placement is left alone. */
    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Location at = event.getBlockPlaced().getLocation();
        if (service.centers().at(at).isEmpty() && !service.centers().wallCovers(at)) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(Text.parse("&cThat block belongs to the artillery command center."));
    }

    /** A block of a map wall is not broken: its barriers hold the frames up. */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (service.centers().wallCovers(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    /** A frame of a map wall does not break, so the map stays on the wall. */
    @EventHandler
    public void onHangingBreak(HangingBreakEvent event) {
        if (service.centers().isWallEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    /** A right-click on the screen opens the main dialog for the owner. A frame of the map wall is not turned by a click. */
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (targeting.isTargeting(event.getPlayer().getUniqueId())) {
            return;
        }
        if (service.centers().isWallEntity(event.getRightClicked())) {
            event.setCancelled(true);
            return;
        }
        Optional<CommandCenter> found = service.centers().byInteraction(event.getRightClicked().getUniqueId());
        if (found.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        CommandCenter center = found.get();
        if (!friends.mayUse(center.owner(), player.getUniqueId())) {
            player.sendMessage(Text.parse("&cOnly the owner or a friend of the owner can use this command center."));
            return;
        }
        dialogs.main(player, center);
    }

    /** A left-click on the screen breaks the center for its owner. A left-click on a frame of the map wall does nothing. */
    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (service.centers().isWallEntity(event.getEntity())) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Interaction)) {
            return;
        }
        Optional<CommandCenter> found = service.centers().byInteraction(event.getEntity().getUniqueId());
        if (found.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        breakCenter(found.get(), player);
    }

    /**
     * Breaks a center for its owner or a friend of the owner: a break event for its block first, then the center goes and its item is dropped.
     */
    private void breakCenter(CommandCenter center, Player player) {
        if (!friends.mayUse(center.owner(), player.getUniqueId())) {
            player.sendMessage(Text.parse("&cOnly the owner or a friend of the owner can break this command center."));
            return;
        }
        Location block = service.centers().blockOf(center);
        if (block == null) {
            return;
        }
        BlockBreakEvent event = new BlockBreakEvent(block.getBlock(), player);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || service.centers().byId(center.id()).isEmpty()) {
            return;
        }
        List<ItemStack> drops = service.remove(center);
        if (player.getGameMode() != GameMode.CREATIVE) {
            for (ItemStack item : drops) {
                block.getWorld().dropItemNaturally(block.clone().add(0.5, 0.5, 0.5), item);
            }
        }
    }

    /** True when a device, machine, turret, piece or center already stands on the block. */
    private boolean occupied(Location at) {
        return service.centers().at(at).isPresent() || service.artillery().registry().at(at).isPresent()
                || devices.at(at).isPresent() || machines.at(at).isPresent() || turrets.at(at).isPresent();
    }

    private void refuse(Player player, String itemId, PlacementRules.Refusal refusal) {
        switch (refusal) {
            case DISABLED_WORLD -> player.sendMessage(core.messages().get("disabled-world"));
            case NOT_AIR -> player.sendMessage(Text.parse("&cThat spot is taken: the block beside the face you aim at must be air."));
            case OCCUPIED -> player.sendMessage(Text.parse("&cA device already stands there."));
            case LOCKED -> research.nodeFor(player.getUniqueId(), itemId)
                    .ifPresent(node -> player.sendMessage(ResearchHints.requires(node)));
            case PROTECTED -> player.sendMessage(Text.parse("&cThis spot is protected."));
        }
    }

    private static void take(Player player) {
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            inHand.setAmount(inHand.getAmount() - 1);
        }
    }
}
