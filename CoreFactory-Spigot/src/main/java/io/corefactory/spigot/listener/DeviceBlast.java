package io.corefactory.spigot.listener;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.device.Footprint;
import io.corefactory.core.energy.Pos;
import io.corefactory.core.engine.BlastReach;
import io.corefactory.core.engine.DetonationObserver;
import io.corefactory.spigot.device.DevicePart;
import io.corefactory.spigot.energy.DeviceRegistry;
import io.corefactory.spigot.energy.EnergyDevice;
import io.corefactory.spigot.energy.EnergyService;
import io.corefactory.spigot.logistics.LogisticsDevice;
import io.corefactory.spigot.logistics.LogisticsService;
import io.corefactory.spigot.machine.MachineStore;
import io.corefactory.spigot.machine.PlacedMachine;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Destroys the custom devices a blast reaches: lab tools and drills, energy devices (pylons, accumulators, generators) and
 * logistics devices (belts, pickers, splitters). A custom detonation reaches them through {@link DetonationObserver}, and a
 * vanilla explosion through its event.
 *
 * <p>A device is destroyed when the 3D distance from the blast centre to the centre of one of its blocks is at most the radius, and
 * no protection spares any of its blocks (the same check the blast uses). A tall device (see {@link Footprint}) has two blocks, so
 * either block can bring it down, and either block's protection spares it. A device in an unloaded chunk is left as it is. Each device
 * goes as its player break takes it, without the break event: a lab tool or drill drops its barrel contents and its machine
 * item and its block is cleared; an energy or logistics device drops its items, its wires or belt items go, and its network
 * is rebuilt. Its entities and holograms are removed with it.
 *
 * <p>A vanilla explosion that a barrel host lies in has the host removed from the event's block list, so the barrel is
 * handled here and not twice. Explosions the core runs itself (custom TNT primed by vanilla, or a placed custom TNT that
 * vanilla primed) are skipped: the detonation observer covers them with the custom radius.
 */
public final class DeviceBlast implements DetonationObserver, Listener {

    /** The power of a bed or respawn anchor explosion, in blocks. */
    private static final double BLOCK_EXPLOSION_RADIUS = 5.0;

    private final CoreFactoryCore core;
    private final MachineStore machines;
    private final DeviceRegistry energyDevices;
    private final EnergyService energy;
    private final LogisticsService logistics;

    public DeviceBlast(CoreFactoryCore core, MachineStore machines, DeviceRegistry energyDevices, EnergyService energy,
                       LogisticsService logistics) {
        this.core = core;
        this.machines = machines;
        this.energyDevices = energyDevices;
        this.energy = energy;
        this.logistics = logistics;
    }

    @Override
    public void onDetonation(World world, Location origin, double radius, Entity source) {
        destroyWithin(world, origin.getX(), origin.getY(), origin.getZ(), radius, null);
    }

    /** A vanilla entity explosion: the devices within its vanilla power are destroyed, and the barrels among them leave the list. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (core.manager().isCustomExplosion(event.getEntity())) {
            return;
        }
        Location at = event.getLocation();
        destroyWithin(at.getWorld(), at.getX(), at.getY(), at.getZ(), vanillaRadius(event.getEntity()), event.blockList());
    }

    /** A vanilla block explosion, such as a bed or a respawn anchor. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Location at = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        destroyWithin(at.getWorld(), at.getX(), at.getY(), at.getZ(), BLOCK_EXPLOSION_RADIUS, event.blockList());
    }

    /**
     * The power of a vanilla entity explosion, in blocks. Spigot's explosion events carry no radius, so this follows the
     * vanilla power of each source; TNT, and every source not listed, uses the TNT power.
     */
    static double vanillaRadius(Entity entity) {
        if (entity instanceof Creeper creeper) {
            return creeper.isPowered() ? 6.0 : 3.0;
        }
        if (entity instanceof EnderCrystal) {
            return 6.0;
        }
        if (entity instanceof Fireball || entity instanceof WitherSkull) {
            return 1.0;
        }
        return 4.0;
    }

    private void destroyWithin(World world, double x, double y, double z, double radius, List<Block> blockList) {
        if (world == null || radius < 0) {
            return;
        }
        for (PlacedMachine placed : machines.all()) {
            Location at = placed.block();
            int height = Footprint.heightOf(placed.machine().id());
            if (!placed.isLoaded() || !reaches(world, at, height, x, y, z, radius)) {
                continue;
            }
            Block host = at.getBlock();
            if (!BlastReach.destroyed(true, protectedAny(at, height))) {
                continue;
            }
            machines.remove(placed, true);
            if (blockList != null) {
                blockList.remove(host);
            }
            host.setType(Material.AIR, false);
        }
        for (EnergyDevice device : energyDevices.all()) {
            Location at = device.block();
            int height = device.height();
            if (!device.isLoaded() || !reaches(world, at, height, x, y, z, radius)) {
                continue;
            }
            if (!BlastReach.destroyed(true, protectedAny(at, height))) {
                continue;
            }
            if (energy.removeDevice(device)) {
                drop(at, energyDevices.drops(device));
            }
        }
        for (LogisticsDevice device : logistics.all()) {
            if (!device.world().equals(world.getUID())) {
                continue;
            }
            Pos pos = device.pos();
            Location at = new Location(world, pos.x(), pos.y(), pos.z());
            if (!logistics.isLoaded(device) || !reaches(world, at, Footprint.SHORT, x, y, z, radius)) {
                continue;
            }
            if (!BlastReach.destroyed(true, core.protection().isProtected(at.getBlock()))) {
                continue;
            }
            drop(at, logistics.remove(device));
        }
    }

    /** True when the centre of a block of the device at {@code at} lies within the blast sphere, in the blast's world. */
    private static boolean reaches(World world, Location at, int height, double x, double y, double z, double radius) {
        return DevicePart.reaches(world, at, height, x, y, z, radius);
    }

    /** True when any block of a device is protected: a device is spared when one of its blocks is. */
    private boolean protectedAny(Location anchor, int height) {
        for (Location block : DevicePart.blocksOf(anchor, height)) {
            if (core.protection().isProtected(block.getBlock())) {
                return true;
            }
        }
        return false;
    }

    private static void drop(Location block, List<ItemStack> items) {
        World world = block.getWorld();
        if (world == null) {
            return;
        }
        Location spot = block.clone().add(0.5, 0.5, 0.5);
        for (ItemStack item : items) {
            world.dropItemNaturally(spot, item);
        }
    }
}
