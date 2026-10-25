package io.corefactory.paper.oil;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.oil.OilPart;
import io.corefactory.core.research.ResearchService;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.WaterOrientation;
import io.corefactory.paper.energy.DeviceRegistry;
import io.corefactory.paper.machine.MachineStore;
import io.corefactory.paper.water.WaterService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
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
 * Routes the player's clicks on the oil tubes, and their chunk events.
 *
 * <p>A right-click with an oil tube or splitter in the main hand places it on the face the player aimed at. A right-click on a placed tube
 * with an oil part places the next one against it, through the face the player is on. A left-click breaks the tube, unless an oil part is
 * in hand, so a placement cannot break one. A right-click with anything else does nothing.
 */
public final class OilListener implements Listener {

    private final CoreFactoryCore core;
    private final OilService service;
    private final MachineStore machines;
    private final OilPlacement placement;

    /**
     * @param core     the core, which reads the identity of an item
     * @param service  the oil runtime
     * @param water    the water runtime: a block of a water part is not a place for an oil tube
     * @param machines the lab machines: a right-click on one is left to its own listener
     * @param devices  the energy devices: a block of one is not a place for an oil tube
     * @param research the research, which unlocks the items
     */
    public OilListener(CoreFactoryCore core, OilService service, WaterService water, MachineStore machines, DeviceRegistry devices,
                       ResearchService research) {
        this.core = core;
        this.service = service;
        this.machines = machines;
        this.placement = new OilPlacement(core, service, water, machines, devices, research);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (event.getHand() != EquipmentSlot.HAND
                || (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR)) {
            return;
        }
        ItemStack held = event.getItem();
        Optional<OilPart> part = partOf(held);
        if (part.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        if (action != Action.RIGHT_CLICK_BLOCK || clicked == null || machines.at(clicked.getLocation()).isPresent()) {
            return;
        }
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        BlockFace blockFace = event.getBlockFace() == null ? BlockFace.UP : event.getBlockFace();
        Face face = WaterOrientation.dominantFace(blockFace.getModX(), blockFace.getModY(), blockFace.getModZ());
        Location target = clicked.getLocation().add(blockFace.getModX(), blockFace.getModY(), blockFace.getModZ());
        placement.place(player, target, clicked.getLocation(), face, held, part.get());
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Optional<OilDevice> device = deviceOf(event.getRightClicked());
        if (device.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        Optional<OilPart> part = partOf(held);
        if (part.isEmpty()) {
            return;
        }
        OilDevice target = device.get();
        Location block = service.location(target);
        Face aim = aimFace(player, block);
        Pos next = aim.step(target.pos());
        Location spot = new Location(block.getWorld(), next.x(), next.y(), next.z());
        placement.place(player, spot, block, aim, held, part.get());
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        Optional<OilDevice> device = deviceOf(event.getEntity());
        if (device.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (partOf(player.getInventory().getItemInMainHand()).isPresent()) {
            return;
        }
        breakDevice(device.get(), player);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        service.onChunkLoad(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        service.onChunkUnload(event.getChunk());
    }

    private void breakDevice(OilDevice device, Player player) {
        Location block = service.location(device);
        BlockBreakEvent event = new BlockBreakEvent(block.getBlock(), player);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        List<ItemStack> drops = service.remove(device);
        if (player.getGameMode() != GameMode.CREATIVE && block.getWorld() != null) {
            for (ItemStack item : drops) {
                block.getWorld().dropItemNaturally(block.clone().add(0.5, 0.5, 0.5), item);
            }
        }
    }

    /** The face of a block the player looks at, from the block's centre toward the player's eye. */
    private static Face aimFace(Player player, Location block) {
        Location eye = player.getEyeLocation();
        return WaterOrientation.dominantFace(eye.getX() - (block.getX() + 0.5), eye.getY() - (block.getY() + 0.5),
                eye.getZ() - (block.getZ() + 0.5));
    }

    private Optional<OilPart> partOf(ItemStack item) {
        if (item == null) {
            return Optional.empty();
        }
        return core.items().componentId(item).flatMap(OilPart::ofComponent);
    }

    /** The oil tube an interaction entity belongs to. Only an interaction counts: a display takes no clicks. */
    private Optional<OilDevice> deviceOf(Entity entity) {
        if (!(entity instanceof Interaction)) {
            return Optional.empty();
        }
        return OilEntities.deviceOf(entity).flatMap(service::byId);
    }
}
