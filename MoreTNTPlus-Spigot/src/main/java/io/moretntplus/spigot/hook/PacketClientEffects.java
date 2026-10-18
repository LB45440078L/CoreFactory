package io.moretntplus.spigot.hook;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTimeUpdate;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.moretntplus.api.BlockChangeSet;
import io.moretntplus.api.ClientEffects;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

/**
 * Client-only effects over PacketEvents: a per-player clock and per-player block views. Nothing sent here
 * changes the server's world, so the effects vanish for the player when the view is restored.
 */
public final class PacketClientEffects implements ClientEffects {

    @Override
    public void setClientTime(Player player, long timeOfDay) {
        long worldAge = player.getWorld().getFullTime();
        send(player, new WrapperPlayServerTimeUpdate(worldAge, timeOfDay, false));
    }

    @Override
    public void resetClientTime(Player player) {
        World world = player.getWorld();
        send(player, new WrapperPlayServerTimeUpdate(world.getFullTime(), world.getTime(), true));
    }

    @Override
    public void showBlocks(Player player, World world, BlockChangeSet changes) {
        for (int i = 0; i < changes.size(); i++) {
            WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(changes.data(i));
            send(player, new WrapperPlayServerBlockChange(new Vector3i(changes.x(i), changes.y(i), changes.z(i)), state));
        }
    }

    @Override
    public void restoreBlocks(Player player, World world, BlockChangeSet changes) {
        for (int i = 0; i < changes.size(); i++) {
            BlockData real = world.getBlockData(changes.x(i), changes.y(i), changes.z(i));
            WrappedBlockState state = SpigotConversionUtil.fromBukkitBlockData(real);
            send(player, new WrapperPlayServerBlockChange(new Vector3i(changes.x(i), changes.y(i), changes.z(i)), state));
        }
    }

    private static void send(Player player, PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }
}
