package io.corefactory.paper.turret;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.text.Text;
import io.corefactory.core.turret.Magazine;
import io.corefactory.core.turret.RoundType;
import io.corefactory.core.turret.TurretType;
import io.corefactory.core.turret.TurretUpgrade;
import io.corefactory.paper.research.ResearchHints;
import io.corefactory.paper.device.DevicePart;
import io.corefactory.paper.energy.DeviceRegistry;
import io.corefactory.paper.energy.EnergyService;
import io.corefactory.paper.energy.PlacementRules;
import io.corefactory.paper.friends.FriendAccess;
import io.corefactory.paper.logistics.LogisticsIds;
import io.corefactory.paper.machine.MachineStore;
import io.corefactory.core.research.ResearchService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
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
import java.util.OptionalInt;

/**
 * Places, opens, loads, upgrades and breaks the turrets. A right-click on a block with a turret item in the main hand places that turret on
 * the air beside the clicked face, at the level the item carries; a tall level also needs the block above to be free. A right-click on a
 * turret the player owns, while holding an upgrade kit, moves it up one level. A right-click on a machine gun turret with rounds in hand loads
 * them; any other right-click, and a sneaking one, opens its menu, where its targeting is set. A laser turret holds no rounds, so every
 * right-click on it opens its menu. A left-click breaks a turret, after a block break event is fired for its block, so protection plugins see
 * it. A tall turret is broken from either of its blocks, and its upper block cannot be placed on while it stands. The turret's chunk events keep
 * its entities: the entities of a chunk are swept when it loads, and the turret's handles are forgotten when it unloads; the light of a laser
 * shot is set back before its chunk unloads.
 *
 * <p>Only the turret's owner or a friend of the owner, with the permission, may load, eject, upgrade or break it, as the menu also requires.
 * A break fires a block break event for each block the turret stands on.
 */
public final class TurretListener implements Listener {

    private final CoreFactoryCore core;
    private final TurretService service;
    private final DeviceRegistry devices;
    private final MachineStore machines;
    private final ResearchService research;
    private final FriendAccess friends;

    /**
     * @param core     the core, for the items and the disabled worlds
     * @param service  the turrets' runtime
     * @param devices  the energy devices, whose blocks a turret may not take
     * @param machines the placed machines, which keep their own clicks
     * @param research the research progress, which gates the item
     * @param friends  the friend rule, which lets a friend of the owner use a turret
     */
    public TurretListener(CoreFactoryCore core, TurretService service, DeviceRegistry devices, MachineStore machines,
                          ResearchService research, FriendAccess friends) {
        this.core = core;
        this.service = service;
        this.devices = devices;
        this.machines = machines;
        this.research = research;
        this.friends = friends;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        service.registry().sweep(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        service.releaseLightsIn(event.getChunk());
        service.registry().unloaded(event.getChunk());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack held = event.getItem();
        Block clicked = event.getClickedBlock();
        Optional<TurretKind> kind = held == null ? Optional.empty() : kindOf(held);
        if (clicked == null || kind.isEmpty() || machines.at(clicked.getLocation()).isPresent()) {
            return;
        }
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        Player player = event.getPlayer();
        Block target = clicked.getRelative(event.getBlockFace() == null ? BlockFace.UP : event.getBlockFace());
        Location spot = target.getLocation();
        int level = TurretIds.levelOf(held);
        boolean tall = level >= TurretUpgrade.TALL_FROM;
        boolean occupied = service.registry().at(spot).isPresent() || devices.at(spot).isPresent() || machines.at(spot).isPresent();
        Optional<PlacementRules.Refusal> refusal = PlacementRules.before(core.isDisabledWorld(target.getWorld()),
                target.getType().isAir(), occupied, research.isUnlocked(player.getUniqueId(), kind.get().itemId()));
        if (refusal.isPresent()) {
            refuse(player, kind.get(), refusal.get());
            return;
        }
        Block upper = target.getRelative(BlockFace.UP);
        if (tall && !spaceAbove(upper)) {
            player.sendMessage(Text.parse("&cThis turret is two blocks tall at level " + level
                    + ": the block above the spot must be free."));
            return;
        }
        ItemStack one = held.clone();
        one.setAmount(1);
        BlockState replaced = target.getState();
        BlockPlaceEvent check = new BlockPlaceEvent(target, replaced, clicked, one, player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(check);
        if (check.isCancelled() || (tall && !protectedOk(upper, clicked, one, player))) {
            refuse(player, kind.get(), PlacementRules.Refusal.PROTECTED);
            return;
        }
        service.place(spot, player.getUniqueId(), player.getLocation().getYaw(), kind.get(), level);
        if (player.getGameMode() != GameMode.CREATIVE) {
            take(player);
        }
        player.swingMainHand();
    }

    /**
     * A block placed on a turret, or on the block above a tall one, is refused: the turret stands there. Any other placement is left alone.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (service.registry().at(event.getBlockPlaced().getLocation()).isEmpty()) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage(Text.parse("&cThat block belongs to a turret."));
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Optional<TurretDevice> found = service.registry().byInteraction(event.getRightClicked().getUniqueId());
        if (found.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (player.isSneaking() && event.getHand() == EquipmentSlot.HAND
                && LogisticsIds.takesSideAim(core.items().componentId(held).orElse(null))) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND || EnergyService.WIRE_ITEM.equals(core.items().componentId(held).orElse(null))) {
            return;
        }
        TurretDevice turret = found.get();
        OptionalInt kit = TurretIds.kitLevelOf(core.items().componentId(held).orElse(null));
        if (kit.isPresent()) {
            upgrade(player, turret, kit.getAsInt(), held);
            return;
        }
        Optional<RoundType> round = core.items().componentId(held).flatMap(RoundType::ofItem);
        if (round.isPresent() && !player.isSneaking() && turret.kind() == TurretKind.MACHINE_GUN) {
            if (TurretMenu.mayUse(player, turret, friends)) {
                loadHeld(player, turret, round.get());
            }
        } else {
            openMenu(player, turret);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        Optional<TurretDevice> turret = turretOf(event.getEntity());
        if (turret.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        breakTurret(turret.get(), player);
    }

    private Optional<TurretDevice> turretOf(Entity entity) {
        if (!(entity instanceof Interaction)) {
            return Optional.empty();
        }
        return service.registry().byInteraction(entity.getUniqueId());
    }

    /**
     * Moves a turret up one level with an upgrade kit, for a player who may use it: its owner or a friend of the owner. The kit must be
     * the one of the level just above it. A tall level needs the block above the turret free. On success one kit is taken from the hand.
     */
    private void upgrade(Player player, TurretDevice turret, int kitLevel, ItemStack held) {
        if (!TurretMenu.mayUse(player, turret, friends)) {
            return;
        }
        int current = turret.level();
        TurretType type = turret.kind().type();
        boolean levelOk = TurretUpgrade.canUpgrade(type, current, kitLevel) == TurretUpgrade.Reason.OK;
        // A turret that is already tall owns the block above it, so only a move to a tall level from a short one needs that block free.
        boolean spaceAbove = !levelOk || kitLevel < TurretUpgrade.TALL_FROM || turret.height() > 1
                || spaceAboveTurret(turret, held, player);
        switch (TurretUpgrade.canUpgrade(type, current, kitLevel, spaceAbove)) {
            case OK -> {
                service.upgrade(turret, kitLevel);
                if (player.getGameMode() != GameMode.CREATIVE) {
                    take(player);
                }
                player.sendMessage(Text.parse("&aThe turret is now level &f" + kitLevel + "&a"
                        + (turret.height() > 1 ? ", two blocks tall." : ".")));
            }
            case WRONG_LEVEL -> player.sendMessage(Text.parse("&cThis kit makes level " + kitLevel + ". The turret is at level "
                    + current + ", so it takes a level " + (current + 1) + " kit."));
            case MAX_LEVEL -> player.sendMessage(Text.parse("&cThis turret is already at its highest level, "
                    + type.maxLevel() + "."));
            case BLOCKED_ABOVE -> player.sendMessage(Text.parse("&cNothing can grow above this turret: the block above it "
                    + "must be free."));
        }
    }

    /** True when the block above a turret is free for its upper half, and the claims let the owner use it. */
    private boolean spaceAboveTurret(TurretDevice turret, ItemStack kit, Player player) {
        Block anchor = turret.block().getBlock();
        Block upper = anchor.getRelative(BlockFace.UP);
        if (!spaceAbove(upper)) {
            return false;
        }
        ItemStack turretItem = service.registry().itemOf(turret).orElseGet(() -> {
            ItemStack copy = kit.clone();
            copy.setAmount(1);
            return copy;
        });
        return protectedOk(upper, anchor, turretItem, player);
    }

    /** True when a block is air, and no device, machine or other turret stands in it. */
    private boolean spaceAbove(Block upper) {
        Location at = upper.getLocation();
        return upper.getType().isAir() && devices.at(at).isEmpty() && machines.at(at).isEmpty() && service.registry().at(at).isEmpty();
    }

    /** Fires the placement check of a block for a player, so a protection plugin may refuse it. True when nothing refuses it. */
    private static boolean protectedOk(Block block, Block against, ItemStack item, Player player) {
        BlockPlaceEvent check = new BlockPlaceEvent(block, block.getState(), against, item, player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(check);
        return !check.isCancelled();
    }

    private void loadHeld(Player player, TurretDevice turret, RoundType type) {
        Magazine magazine = turret.magazine();
        if (magazine.roomFor(type) < 1) {
            if (magazine.type() != null && magazine.type() != type) {
                player.sendMessage(Text.parse("&cThis turret holds " + TurretText.roundName(magazine.type())
                        + " rounds. Eject them first, or load the same type."));
            } else {
                player.sendMessage(Text.parse("&cThe turret is full: " + magazine.capacity() + " rounds."));
            }
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        int taken = service.load(turret, type, held.getAmount());
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (held.getAmount() > taken) {
                held.setAmount(held.getAmount() - taken);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }
        Magazine after = turret.magazine();
        player.sendMessage(Text.parse("&aLoaded &f" + taken + " &arounds. &7Now &f" + after.count() + "&7/&f"
                + after.capacity() + " " + TurretText.roundName(type) + "."));
    }

    /** Opens the turret's menu: its rounds, its status, and the targeting toggles. */
    private void openMenu(Player player, TurretDevice turret) {
        TurretMenu menu = turret.menu(service);
        menu.refresh();
        player.openInventory(menu.getInventory());
    }

    /**
     * Breaks a turret for a player who may use it: its owner or a friend of the owner, with the permission. A break event is fired for each block the turret
     * stands on, so a protection plugin sees and may refuse the upper block of a tall turret as well as its anchor.
     */
    private void breakTurret(TurretDevice turret, Player player) {
        if (!TurretMenu.mayUse(player, turret, friends)) {
            return;
        }
        Location block = turret.block();
        for (Location cell : DevicePart.blocksOf(block, turret.height())) {
            BlockBreakEvent event = new BlockBreakEvent(cell.getBlock(), player);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
        }
        if (service.registry().byId(turret.id()).isEmpty()) {
            return;
        }
        List<ItemStack> drops = service.remove(turret);
        if (player.getGameMode() != GameMode.CREATIVE && block.getWorld() != null) {
            for (ItemStack item : drops) {
                block.getWorld().dropItemNaturally(block.clone().add(0.5, 0.5, 0.5), item);
            }
        }
    }

    /** The kind of turret an item places, or empty when the item is no turret. */
    private Optional<TurretKind> kindOf(ItemStack item) {
        return core.items().componentId(item).flatMap(TurretKind::ofItem);
    }

    private void refuse(Player player, TurretKind kind, PlacementRules.Refusal refusal) {
        switch (refusal) {
            case DISABLED_WORLD -> player.sendMessage(core.messages().get("disabled-world"));
            case NOT_AIR -> player.sendMessage(Text.parse("&cThat spot is taken: the block beside the face you aim at must be air."));
            case OCCUPIED -> player.sendMessage(Text.parse("&cA device already stands there."));
            case LOCKED -> research.nodeFor(player.getUniqueId(), kind.itemId())
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
