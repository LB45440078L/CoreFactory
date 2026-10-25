package io.corefactory.paper.artillery;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.artillery.ArtilleryLevel;
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
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Places, switches and breaks the artillery pieces. A right-click on a block with a piece's item in the main hand places the piece on the air
 * beside the clicked face; the air above it must be free for the height of its level. A right-click on a piece by its owner or a friend of the
 * owner switches it on or off. A left-click by one of them breaks it, after a block break event for its block, so protection plugins see it,
 * and gives its item back.
 * The piece's chunk events keep its entities, as the turrets' do.
 */
public final class ArtilleryListener implements Listener {

    private final CoreFactoryCore core;
    private final ArtilleryService service;
    private final DeviceRegistry devices;
    private final MachineStore machines;
    private final TurretRegistry turrets;
    private final ResearchService research;
    private final FriendAccess friends;

    /**
     * @param core     the core, for the items and the disabled worlds
     * @param service  the pieces' runtime
     * @param devices  the energy devices, whose blocks a piece may not take
     * @param machines the placed machines, which keep their own clicks
     * @param turrets  the turrets, whose blocks a piece may not take
     * @param research the research progress, which gates the item
     * @param friends  the friend rule, which lets a friend of the owner switch, break and load a piece
     */
    public ArtilleryListener(CoreFactoryCore core, ArtilleryService service, DeviceRegistry devices, MachineStore machines,
                             TurretRegistry turrets, ResearchService research, FriendAccess friends) {
        this.core = core;
        this.service = service;
        this.devices = devices;
        this.machines = machines;
        this.turrets = turrets;
        this.research = research;
        this.friends = friends;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        service.registry().sweep(event.getChunk());
        service.flights().sweep(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        service.registry().unloaded(event.getChunk());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack held = event.getItem();
        Block clicked = event.getClickedBlock();
        Optional<ArtilleryLevel> level = held == null ? Optional.empty()
                : core.items().componentId(held).flatMap(ArtilleryIds::levelOfItem);
        if (clicked == null || level.isEmpty() || machines.at(clicked.getLocation()).isPresent()) {
            return;
        }
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        Player player = event.getPlayer();
        Block target = clicked.getRelative(event.getBlockFace() == null ? BlockFace.UP : event.getBlockFace());
        Location spot = target.getLocation();
        String itemId = ArtilleryIds.itemOf(level.get());
        Optional<PlacementRules.Refusal> refusal = PlacementRules.before(core.isDisabledWorld(target.getWorld()),
                target.getType().isAir(), occupied(spot), research.isUnlocked(player.getUniqueId(), itemId));
        if (refusal.isPresent()) {
            refuse(player, itemId, refusal.get());
            return;
        }
        int above = (int) Math.ceil(service.settings().heightOf(level.get())) - 1;
        for (int i = 1; i <= above; i++) {
            Block block = target.getRelative(0, i, 0);
            if (!block.getType().isAir() || occupied(block.getLocation())) {
                player.sendMessage(Text.parse("&cThis piece is taller than one block: the air above the spot must be free."));
                return;
            }
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
        service.place(spot, player.getUniqueId(), level.get());
        if (player.getGameMode() != GameMode.CREATIVE) {
            take(player);
        }
        player.swingMainHand();
    }

    /** A block placed on a piece is refused: the piece stands there. Any other placement is left alone. */
    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (service.registry().at(event.getBlockPlaced().getLocation()).isEmpty()) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(Text.parse("&cThat block belongs to the artillery."));
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Optional<ArtilleryDevice> found = service.registry().byInteraction(event.getRightClicked().getUniqueId());
        if (found.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ArtilleryDevice piece = found.get();
        if (!friends.mayUse(piece.owner(), player.getUniqueId())) {
            player.sendMessage(Text.parse("&cOnly the owner or a friend of the owner can switch this piece."));
            return;
        }
        service.setOn(piece, !piece.on());
        player.sendMessage(Text.parse(piece.on() ? "&aThe piece is switched on." : "&7The piece is switched off."));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Interaction)) {
            return;
        }
        Optional<ArtilleryDevice> found = service.registry().byInteraction(event.getEntity().getUniqueId());
        if (found.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        breakPiece(found.get(), player);
    }

    /**
     * Breaks a piece for its owner or a friend of the owner: a break event for its block first, then the piece goes and its item is dropped.
     * The block event names the player who breaks, so a protection plugin decides for that player.
     */
    private void breakPiece(ArtilleryDevice piece, Player player) {
        if (!friends.mayUse(piece.owner(), player.getUniqueId())) {
            player.sendMessage(Text.parse("&cOnly the owner or a friend of the owner can break this piece."));
            return;
        }
        Location block = service.registry().blockOf(piece);
        if (block == null) {
            return;
        }
        BlockBreakEvent event = new BlockBreakEvent(block.getBlock(), player);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || service.registry().byId(piece.id()).isEmpty()) {
            return;
        }
        List<ItemStack> drops = service.remove(piece);
        if (player.getGameMode() != GameMode.CREATIVE) {
            for (ItemStack item : drops) {
                block.getWorld().dropItemNaturally(block.clone().add(0.5, 0.5, 0.5), item);
            }
        }
    }

    /** True when a device, machine, turret, piece or command center already stands on the block. */
    private boolean occupied(Location at) {
        return service.registry().at(at).isPresent() || service.centers().at(at).isPresent() || devices.at(at).isPresent()
                || machines.at(at).isPresent() || turrets.at(at).isPresent();
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
