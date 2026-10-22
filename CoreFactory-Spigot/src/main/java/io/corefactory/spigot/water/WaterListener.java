package io.corefactory.spigot.water;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.research.ResearchService;
import io.corefactory.core.water.Face;
import io.corefactory.core.water.PumpFuel;
import io.corefactory.core.water.PumpLevels;
import io.corefactory.core.water.WaterOrientation;
import io.corefactory.core.water.WaterPart;
import io.corefactory.spigot.energy.DeviceRegistry;
import io.corefactory.spigot.machine.ItemRefs;
import io.corefactory.spigot.machine.MachineStore;
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
import java.util.OptionalInt;

/**
 * Routes the player's clicks on the water blocks, and their chunk events.
 *
 * <p>A right-click with a pipe, splitter or pump in the main hand places it: a pipe or splitter on the face the player aimed at, a pump on
 * the water the player aims at. A right-click on a placed block with a water part places the next block against it, through the face the
 * player is on. A right-click on a pump with a kit upgrades it, for its owner; with coal, charcoal or diesel it takes fuel; with anything
 * else it shows the pump's status. A left-click breaks the block, unless a water part is in hand, so a placement cannot break one.
 */
public final class WaterListener implements Listener {

    /** The reach of the server's ray, in blocks: the block reach of a creative player. */
    private static final double REACH = 6.0;

    private final CoreFactoryCore core;
    private final WaterService service;
    private final MachineStore machines;
    private final WaterPlacement placement;
    private final ItemRefs refs;

    /**
     * @param core      the core, which reads the identity of an item
     * @param service   the water runtime
     * @param machines  the lab machines: a right-click on one is left to its own listener
     * @param devices   the energy devices: a block of one is not a place for a water part
     * @param research  the research, which unlocks the items
     */
    public WaterListener(CoreFactoryCore core, WaterService service, MachineStore machines, DeviceRegistry devices,
                         ResearchService research) {
        this.core = core;
        this.service = service;
        this.machines = machines;
        this.placement = new WaterPlacement(core, service, machines, devices, research);
        this.refs = new ItemRefs(core);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (event.getHand() != EquipmentSlot.HAND
                || (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR)) {
            return;
        }
        ItemStack held = event.getItem();
        Optional<WaterPart> part = partOf(held);
        if (part.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        if (part.get() == WaterPart.PUMP) {
            event.setCancelled(true);
            RayTraceResult hit = player.rayTraceBlocks(REACH, FluidCollisionMode.ALWAYS);
            if (hit == null || hit.getHitBlock() == null) {
                WaterPlacement.say(player, "&cAim at a water block to place a pump.");
                return;
            }
            Location water = hit.getHitBlock().getLocation();
            placement.place(player, water, water, null, held, WaterPart.PUMP);
            return;
        }
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
        Optional<WaterDevice> device = deviceOf(event.getRightClicked());
        if (device.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        WaterDevice target = device.get();
        Optional<WaterPart> part = partOf(held);
        if (part.isPresent()) {
            Location block = service.location(target);
            Face aim = aimFace(player, block);
            Pos next = aim.step(target.pos());
            Location spot = new Location(block.getWorld(), next.x(), next.y(), next.z());
            placement.place(player, spot, block, aim, held, part.get());
            return;
        }
        OptionalInt kit = PumpLevels.kitLevel(core.items().componentId(held).orElse(null));
        if (kit.isPresent()) {
            upgrade(player, target, held, kit.getAsInt());
            return;
        }
        if (target.part() == WaterPart.PUMP && !fuel(player, target, held)) {
            for (String line : service.status(target)) {
                WaterPlacement.say(player, "&7" + line);
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        Optional<WaterDevice> device = deviceOf(event.getEntity());
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

    private void upgrade(Player player, WaterDevice pump, ItemStack held, int kit) {
        if (pump.part() != WaterPart.PUMP) {
            WaterPlacement.say(player, "&7Upgrade kits go on a water pump.");
            return;
        }
        if (!player.getUniqueId().equals(pump.owner())) {
            WaterPlacement.say(player, "&cOnly the owner of this pump can upgrade it.");
            return;
        }
        OptionalInt next = PumpLevels.upgrade(pump.level(), kit);
        if (next.isEmpty()) {
            WaterPlacement.say(player, "&c" + PumpLevels.refusal(pump.level(), kit));
            return;
        }
        service.upgrade(pump, next.getAsInt());
        WaterPlacement.take(player, 1);
        WaterPlacement.say(player, "&aPump upgraded to level " + next.getAsInt() + ".");
    }

    /** Takes fuel into a pump when the held item is fuel for its level. False when the item is not fuel for it. */
    private boolean fuel(Player player, WaterDevice pump, ItemStack held) {
        if (PumpLevels.powered(pump.level())) {
            return false;
        }
        ItemRef ref = refs.of(held).orElse(null);
        if (!PumpFuel.accepts(pump.level(), ref)) {
            return false;
        }
        int accepted = PumpFuel.accept(pump.burn().items(), held.getAmount());
        if (accepted == 0) {
            WaterPlacement.say(player, "&7The pump's fuel is full.");
            return true;
        }
        service.addFuel(pump, accepted);
        WaterPlacement.take(player, accepted);
        WaterPlacement.say(player, "&7Added " + accepted + " fuel. The pump holds " + pump.burn().items() + ".");
        return true;
    }

    private void breakDevice(WaterDevice device, Player player) {
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

    private Optional<WaterPart> partOf(ItemStack item) {
        if (item == null) {
            return Optional.empty();
        }
        return core.items().componentId(item).flatMap(WaterPart::ofComponent);
    }

    /** The water block an interaction entity belongs to. Only an interaction counts: a display takes no clicks. */
    private Optional<WaterDevice> deviceOf(Entity entity) {
        if (!(entity instanceof Interaction)) {
            return Optional.empty();
        }
        return WaterEntities.deviceOf(entity).flatMap(service::byId);
    }
}
