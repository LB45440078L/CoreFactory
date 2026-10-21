package io.corefactory.spigot.machine;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.item.PlaceableItems;
import io.corefactory.spigot.machine.ClickDecision.Held;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.SoundGroup;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Hopper;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.Optional;

/**
 * Places a block held by a sneaking player against a machine. The client aims at a machine's interaction entity, not at
 * its host, when the host is concealed, so vanilla never places the block. The aim is taken on the server, which still
 * has the real host: the ray must hit the host, and the face it hits is where the block goes. The block is placed as
 * vanilla places one: its data is set, the placement event is fired so protection plugins can cancel it, and it is
 * reverted when cancelled. A hopper faces into the block it is placed against, or down when it is placed above or
 * below, as vanilla sets it.
 */
final class HandPlacement {

    /** The reach of the aim, in blocks, the same as the reach of a block interaction. */
    static final double REACH = 6.0;

    private final CoreFactoryCore core;

    HandPlacement(CoreFactoryCore core) {
        this.core = core;
    }

    /** What the held item is, for the click decision: a block that may be placed by hand, nothing, or any other item. */
    Held heldKind(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return Held.EMPTY;
        }
        Optional<PlaceableItems.Identity> identity = PlaceableItems.identityOf(core.items(), item);
        PlaceableItems.Kind kind = identity.map(PlaceableItems.Identity::kind).orElse(null);
        boolean placeable = PlaceableItems.placedByHand(item.getType().isBlock(), kind)
                && !multiPart(item.getType().createBlockData());
        return placeable ? Held.BLOCK : Held.OTHER;
    }

    /** The face of the host the player aims at, or empty when the aim is not on the host within reach. */
    Optional<BlockFace> aimedFace(Player player, Block host) {
        RayTraceResult hit = player.rayTraceBlocks(REACH, FluidCollisionMode.NEVER);
        if (hit == null || hit.getHitBlock() == null || hit.getHitBlockFace() == null) {
            return Optional.empty();
        }
        Block block = hit.getHitBlock();
        boolean onHost = block.getWorld().equals(host.getWorld())
                && block.getX() == host.getX() && block.getY() == host.getY() && block.getZ() == host.getZ();
        return onHost ? Optional.of(hit.getHitBlockFace()) : Optional.empty();
    }

    /** True when the block a placement would fill is air or liquid, and no living entity stands in it. */
    static boolean free(Block target) {
        if (!target.isEmpty() && !target.isLiquid()) {
            return false;
        }
        return target.getWorld().getNearbyEntities(target.getBoundingBox(), entity -> entity instanceof LivingEntity).isEmpty();
    }

    /**
     * Places one of the player's held blocks on the face of the host and takes it from the hand, unless the placement event is
     * cancelled. The block is set first, as vanilla does, so the event sees it; a cancelled placement is reverted.
     *
     * @return true when the block was placed
     */
    boolean place(Player player, Block host, BlockFace face, ItemStack held) {
        Block target = host.getRelative(face);
        BlockData data = dataFor(held.getType(), face);
        BlockState replaced = target.getState();
        target.setBlockData(data, true);
        ItemStack one = held.clone();
        one.setAmount(1);
        BlockPlaceEvent event = new BlockPlaceEvent(target, replaced, host, one, player, true, EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            replaced.update(true, false);
            return false;
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            take(player);
        }
        player.swingMainHand();
        SoundGroup sound = data.getSoundGroup();
        target.getWorld().playSound(target.getLocation().add(0.5, 0.5, 0.5), sound.getPlaceSound(), SoundCategory.BLOCKS,
                (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f);
        return true;
    }

    /** The block data a placed block gets: its default, with a hopper facing as vanilla places it. */
    static BlockData dataFor(Material material, BlockFace face) {
        BlockData data = material.createBlockData();
        if (data instanceof Hopper hopper) {
            hopper.setFacing(face.getModY() != 0 ? BlockFace.DOWN : face.getOppositeFace());
        }
        return data;
    }

    /** A block that spans more than one block space, such as a door or a bed, is not placed by hand. */
    static boolean multiPart(BlockData data) {
        return data instanceof Bed || data instanceof Bisected && !(data instanceof Slab) && !(data instanceof Stairs);
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
