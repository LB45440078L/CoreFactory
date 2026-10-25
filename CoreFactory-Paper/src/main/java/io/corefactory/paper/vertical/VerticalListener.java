package io.corefactory.paper.vertical;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.research.ResearchService;
import io.corefactory.core.text.Text;
import io.corefactory.paper.research.ResearchHints;
import io.corefactory.paper.friends.FriendAccess;
import io.corefactory.paper.logistics.LogisticsService;
import io.corefactory.paper.logistics.PlacementAim;
import io.corefactory.paper.logistics.SidePlacement;
import io.corefactory.paper.machine.MachineStore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
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
import java.util.OptionalInt;

/**
 * Routes the clicks on the vertical belts and the chunk events of their entities.
 *
 * <p>A right-click on a block with a vertical belt in the main hand places the belt on the free air beside the face clicked. A
 * right-click on a belt's interaction with one in hand places it on the face the player aims through. A right-click on a belt with an
 * empty hand, not sneaking, opens its menu. A left-click breaks the belt, unless a vertical belt is in hand, so a placement cannot break
 * one by accident. The placement follows the same checks as a logistics device: the world is not disabled, the research is unlocked,
 * the spot is free, and a protection plugin does not cancel the placement.
 */
public final class VerticalListener implements Listener {

    private final CoreFactoryCore core;
    private final VerticalService service;
    private final LogisticsService logistics;
    private final MachineStore machines;
    private final ResearchService research;
    private final FriendAccess friends;

    /**
     * @param core      the core, which reads the identity of an item and the disabled worlds
     * @param service   the service that owns the belts
     * @param logistics the logistics service, which tells whether a spot is free
     * @param machines  the lab tools and drills, which keep their own clicks
     * @param research  the research, which unlocks the levels
     * @param friends   the friend rule, which lets a friend of the owner change a tower's direction
     */
    public VerticalListener(CoreFactoryCore core, VerticalService service, LogisticsService logistics, MachineStore machines,
                            ResearchService research, FriendAccess friends) {
        this.core = core;
        this.service = service;
        this.logistics = logistics;
        this.machines = machines;
        this.research = research;
        this.friends = friends;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack held = event.getItem();
        Block clicked = event.getClickedBlock();
        if (held == null || clicked == null || levelOf(held).isEmpty() || machines.at(clicked.getLocation()).isPresent()) {
            return;
        }
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        BlockFace face = event.getBlockFace() == null ? BlockFace.UP : event.getBlockFace();
        Pos clickedPos = new Pos(clicked.getX(), clicked.getY(), clicked.getZ());
        Pos target = new Pos(clickedPos.x() + face.getModX(), clickedPos.y() + face.getModY(), clickedPos.z() + face.getModZ());
        place(event.getPlayer(), clicked.getLocation(), clickedPos, target, held);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Optional<VerticalBelt> belt = service.byInteraction(event.getRightClicked().getUniqueId());
        if (belt.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (levelOf(held).isPresent()) {
            World world = Bukkit.getWorld(belt.get().world());
            if (world == null) {
                return;
            }
            Pos cell = belt.get().pos();
            Location eye = player.getEyeLocation();
            Pos face = PlacementAim.entryFace(new double[] {eye.getX(), eye.getY(), eye.getZ()},
                    new double[] {eye.getDirection().getX(), eye.getDirection().getY(), eye.getDirection().getZ()}, cell);
            Location block = new Location(world, cell.x(), cell.y(), cell.z());
            place(player, block, cell, new Pos(cell.x() + face.x(), cell.y() + face.y(), cell.z() + face.z()), held);
            return;
        }
        if (!player.isSneaking() && (held.getType().isAir())) {
            new VerticalMenu(belt.get(), service, friends).open(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Interaction)) {
            return;
        }
        Optional<VerticalBelt> belt = service.byInteraction(event.getEntity().getUniqueId());
        if (belt.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (levelOf(player.getInventory().getItemInMainHand()).isEmpty()) {
            breakBelt(belt.get(), player);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        service.sweep(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        service.unloaded(event.getChunk());
    }

    /**
     * Places a belt on the target block, when the spot is free and the level is unlocked, and takes the item from the hand. A belt placed
     * beside a tower joins it.
     */
    private void place(Player player, Location clicked, Pos clickedPos, Pos target, ItemStack held) {
        World world = clicked.getWorld();
        if (world == null) {
            return;
        }
        if (core.isDisabledWorld(world)) {
            player.sendMessage(core.messages().get("disabled-world"));
            return;
        }
        int level = levelOf(held).orElse(0);
        if (level == 0) {
            return;
        }
        String id = VerticalIds.componentId(level);
        Location spot = new Location(world, target.x(), target.y(), target.z());
        if (service.at(spot).isPresent()) {
            player.sendMessage(Text.parse("&cAnother vertical belt already stands there."));
            return;
        }
        SidePlacement.Space space = logistics.spaceAt(spot);
        if (space != SidePlacement.Space.FREE) {
            player.sendMessage(Text.parse(space == SidePlacement.Space.ENTITY
                    ? "&cSomething stands in the spot where it would go."
                    : "&cThat spot is blocked: the block beside the face you aim at must be air."));
            return;
        }
        if (!research.isUnlocked(player.getUniqueId(), id)) {
            player.sendMessage(research.nodeFor(player.getUniqueId(), id).map(ResearchHints::needed)
                    .orElseGet(() -> Text.parse("&cResearch is needed for this item.")));
            return;
        }
        Block targetBlock = spot.getBlock();
        BlockState replaced = targetBlock.getState();
        ItemStack one = held.clone();
        one.setAmount(1);
        BlockPlaceEvent check = new BlockPlaceEvent(targetBlock, replaced, clicked.getBlock(), one, player, true,
                EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(check);
        if (check.isCancelled()) {
            player.sendMessage(Text.parse("&cThis spot is protected."));
            return;
        }
        service.add(world.getUID(), target, level, player.getUniqueId(), clickedPos);
        if (player.getGameMode() != GameMode.CREATIVE) {
            take(player);
        }
        player.swingMainHand();
    }

    /** Breaks a belt through a block break event, so protection plugins see it, and drops its component item. */
    private void breakBelt(VerticalBelt belt, Player player) {
        World world = Bukkit.getWorld(belt.world());
        if (world == null) {
            return;
        }
        Location block = new Location(world, belt.pos().x(), belt.pos().y(), belt.pos().z());
        BlockBreakEvent event = new BlockBreakEvent(block.getBlock(), player);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        List<ItemStack> drops = service.remove(belt);
        if (player.getGameMode() != GameMode.CREATIVE) {
            for (ItemStack item : drops) {
                world.dropItemNaturally(block.clone().add(0.5, 0.5, 0.5), item);
            }
        }
    }

    /** The level of a vertical belt item in hand, or empty for any other item. */
    private Optional<Integer> levelOf(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Optional.empty();
        }
        OptionalInt level = VerticalIds.levelOf(core.items().componentId(item).orElse(null));
        return level.isPresent() ? Optional.of(level.getAsInt()) : Optional.empty();
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
