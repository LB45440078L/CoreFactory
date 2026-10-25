package io.corefactory.paper.logistics;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.energy.Pos;
import io.corefactory.paper.energy.DeviceRegistry;
import io.corefactory.paper.energy.EnergyDevice;
import io.corefactory.paper.machine.InteractionRoute;
import io.corefactory.paper.machine.ItemRefs;
import io.corefactory.paper.machine.MachineStore;
import io.corefactory.paper.machine.PlacedMachine;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
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
import org.bukkit.util.RayTraceResult;

import java.util.List;
import java.util.Optional;

/**
 * Routes the player's clicks on the logistics devices, on the machines and generators they feed, and the chunk events of the
 * devices.
 *
 * <p>A right-click with a belt, picker or splitter in the main hand places it against the face the player aims at. A picker or a
 * splitter placed on a machine or a generator is a sneaking right-click on its interaction (a concealed machine is clicked
 * through its interaction, not its block); the host is the aimed block, and the device goes on the face the server's ray hits it
 * through. A right-click on a splitter with nothing to place opens its filter panel, as before.
 *
 * <p>A left-click breaks the device through a block break event, so protection plugins see it, and the device's items and its own
 * item drop. A left-click with a belt, picker or splitter in hand does nothing to the device, so a placement cannot break it by
 * accident; with any other item or an empty hand it breaks the device as before. Clicks with any other item on a belt or a picker
 * do nothing.
 */
public final class LogisticsListener implements Listener {

    /** The reach of the server's ray, in blocks: the block reach of a creative player. */
    private static final double REACH = 6.0;

    private final CoreFactoryCore core;
    private final LogisticsService service;
    private final MachineStore machines;
    private final DeviceRegistry energyDevices;
    private final LogisticsPlacement placement;
    private final ItemRefs refs;

    /**
     * @param core          the core, which reads the identity of an item
     * @param service       the service that owns the devices
     * @param machines      the lab tools and drills, whose hosts take the placements of pickers and splitters
     * @param energyDevices the generators and other energy devices, whose blocks take the placements of pickers and splitters
     * @param placement     the placement flow
     * @param refs          converts items to stacks, for the filter panel
     */
    public LogisticsListener(CoreFactoryCore core, LogisticsService service, MachineStore machines, DeviceRegistry energyDevices,
                             LogisticsPlacement placement, ItemRefs refs) {
        this.core = core;
        this.service = service;
        this.machines = machines;
        this.energyDevices = energyDevices;
        this.placement = placement;
        this.refs = refs;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack held = event.getItem();
        Block clicked = event.getClickedBlock();
        if (held == null || clicked == null) {
            return;
        }
        Optional<LogisticsIds.Device> device = logisticsOf(held);
        if (device.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        boolean side = LogisticsIds.takesSide(device.get().kind());
        boolean sneaking = player.isSneaking();
        if (machines.at(clicked.getLocation()).isPresent() && !(side && sneaking)) {
            return;
        }
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        if (side && sneaking) {
            Optional<RayTraceResult> host = aimedHost(player);
            if (host.isPresent()) {
                Block block = host.get().getHitBlock();
                placement.place(player, block.getLocation(), faceOf(host.get()), held, device.get());
                return;
            }
        }
        BlockFace face = event.getBlockFace() == null ? BlockFace.UP : event.getBlockFace();
        placement.place(player, clicked.getLocation(), new Pos(face.getModX(), face.getModY(), face.getModZ()), held,
                device.get());
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Optional<LogisticsDevice> device = service.byInteraction(event.getRightClicked().getUniqueId());
        if (device.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (InteractionRoute.of(InteractionRoute.Click.RIGHT, event.getHand() == EquipmentSlot.HAND) != InteractionRoute.OPEN) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        Optional<LogisticsIds.Device> heldDevice = logisticsOf(held);
        if (heldDevice.isPresent()) {
            LogisticsDevice target = device.get();
            Location block = new Location(Bukkit.getWorld(target.world()), target.pos().x(), target.pos().y(), target.pos().z());
            placement.place(player, block, faceOf(player, target.pos()), held, heldDevice.get());
            return;
        }
        if (device.get().kind() == LogisticsIds.Kind.SPLITTER) {
            new SplitterMenu(device.get(), service, refs).open(player);
        }
    }

    /**
     * A sneaking right-click with a picker or a splitter in hand, on the interaction of a machine or a generator, places the device
     * against that block. Other clicks on these entities are left to their own listeners.
     */
    @EventHandler(ignoreCancelled = true)
    public void onInteractPort(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (event.getHand() != EquipmentSlot.HAND || !player.isSneaking()) {
            return;
        }
        Optional<Location> host = hostOf(event.getRightClicked());
        if (host.isEmpty()) {
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        Optional<LogisticsIds.Device> device = logisticsOf(held).filter(found -> LogisticsIds.takesSide(found.kind()));
        if (device.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        Location block = host.get();
        placement.place(player, block, faceOf(player, new Pos(block.getBlockX(), block.getBlockY(), block.getBlockZ())), held,
                device.get());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Interaction)) {
            return;
        }
        Optional<LogisticsDevice> device = service.byInteraction(event.getEntity().getUniqueId());
        if (device.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (SidePlacement.breaksOnLeftClick(logisticsOf(player.getInventory().getItemInMainHand()).isPresent())) {
            breakDevice(device.get(), player);
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

    private void breakDevice(LogisticsDevice device, Player player) {
        Location block = new Location(Bukkit.getWorld(device.world()), device.pos().x(), device.pos().y(), device.pos().z());
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

    /** The block a machine or a generator's interaction stands over, or empty for any other entity. */
    private Optional<Location> hostOf(Entity entity) {
        if (!(entity instanceof Interaction)) {
            return Optional.empty();
        }
        Optional<Location> machine = machines.at(entity.getLocation())
                .filter(placed -> entity.getUniqueId().equals(placed.interaction()))
                .map(PlacedMachine::block);
        if (machine.isPresent()) {
            return machine;
        }
        return energyDevices.byInteraction(entity.getUniqueId()).map(EnergyDevice::block);
    }

    /** The machine host the server's aim ray hits within reach, when the player aims at one. */
    private Optional<RayTraceResult> aimedHost(Player player) {
        RayTraceResult hit = player.rayTraceBlocks(REACH, FluidCollisionMode.NEVER);
        if (hit == null || hit.getHitBlock() == null || hit.getHitBlockFace() == null) {
            return Optional.empty();
        }
        return machines.at(hit.getHitBlock().getLocation()).isPresent() ? Optional.of(hit) : Optional.empty();
    }

    private static Pos faceOf(RayTraceResult hit) {
        BlockFace face = hit.getHitBlockFace();
        return new Pos(face.getModX(), face.getModY(), face.getModZ());
    }

    /**
     * The face of a cell the player aims through. The server knows the real block of a machine, so its ray gives the face it hits
     * when the ray hits that cell; otherwise the face is the one the aim enters the cell through.
     */
    private static Pos faceOf(Player player, Pos cell) {
        RayTraceResult hit = player.rayTraceBlocks(REACH, FluidCollisionMode.NEVER);
        if (hit != null && hit.getHitBlock() != null && hit.getHitBlockFace() != null
                && hit.getHitBlock().getX() == cell.x() && hit.getHitBlock().getY() == cell.y()
                && hit.getHitBlock().getZ() == cell.z()) {
            return faceOf(hit);
        }
        Location eye = player.getEyeLocation();
        return PlacementAim.entryFace(new double[] {eye.getX(), eye.getY(), eye.getZ()},
                new double[] {eye.getDirection().getX(), eye.getDirection().getY(), eye.getDirection().getZ()}, cell);
    }

    private Optional<LogisticsIds.Device> logisticsOf(ItemStack item) {
        return LogisticsIds.of(core.items().componentId(item).orElse(null));
    }
}
