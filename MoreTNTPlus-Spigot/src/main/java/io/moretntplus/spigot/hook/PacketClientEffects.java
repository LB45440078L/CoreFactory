package io.moretntplus.spigot.hook;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.clock.ClockNetworkState;
import com.github.retrooper.packetevents.protocol.world.clock.WorldClock;
import com.github.retrooper.packetevents.protocol.world.clock.WorldClocks;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChangeGameState;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTimeUpdate;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import io.moretntplus.api.BlockChangeSet;
import io.moretntplus.api.ClientEffects;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-only effects over PacketEvents: a per-player clock, per-player weather and per-player block views.
 * Nothing sent here changes the server's world, so the effects vanish for the player when the view is restored.
 *
 * <p>The server re-sends the real clock to every player once a second, which would undo a plain clock packet
 * straight away. While a player's clock is held, the outgoing clock packets are rewritten on their way to that
 * player, so the hold survives them.
 */
public final class PacketClientEffects implements ClientEffects {

    private static final long DAY_LENGTH = 24_000L;

    /** Time each held player's clock is pinned to. Written on the main thread, read on the netty threads. */
    private final Map<UUID, Long> held = new ConcurrentHashMap<>();

    public PacketClientEffects() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.NORMAL) {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (event.getPacketType() != PacketType.Play.Server.TIME_UPDATE) {
                    return;
                }
                Object recipient = event.getPlayer();
                if (!(recipient instanceof Player player)) {
                    return;
                }
                Long pinned = held.get(player.getUniqueId());
                if (pinned == null) {
                    return;
                }
                WrapperPlayServerTimeUpdate wrapper = new WrapperPlayServerTimeUpdate(event);
                Map<WorldClock, ClockNetworkState> clocks = new HashMap<>(wrapper.getClockUpdates());
                clocks.put(WorldClocks.OVERWORLD, new ClockNetworkState(pinned, 0f, 0f));
                wrapper.setClockUpdates(clocks);
                event.setLastUsedWrapper(wrapper);
                event.markForReEncode(true);
            }
        });
    }

    @Override
    public void setClientTime(Player player, long timeOfDay) {
        held.put(player.getUniqueId(), timeOfDay);
        send(player, clockPacket(player.getWorld(), Math.floorMod(timeOfDay, DAY_LENGTH), 0f));
    }

    /** Always clears the hold, even for a player who has logged off, so a later login is not pinned. */
    @Override
    public void resetClientTime(Player player) {
        held.remove(player.getUniqueId());
        if (!player.isOnline()) {
            return;
        }
        World world = player.getWorld();
        send(player, clockPacket(world, world.getTime(), 1f));
    }

    @Override
    public void setClientWeather(Player player, boolean raining, boolean thundering) {
        send(player, new WrapperPlayServerChangeGameState(
                raining ? WrapperPlayServerChangeGameState.Reason.BEGIN_RAINING
                        : WrapperPlayServerChangeGameState.Reason.END_RAINING, 0f));
        send(player, new WrapperPlayServerChangeGameState(
                WrapperPlayServerChangeGameState.Reason.RAIN_LEVEL_CHANGE, raining ? 1f : 0f));
        send(player, new WrapperPlayServerChangeGameState(
                WrapperPlayServerChangeGameState.Reason.THUNDER_LEVEL_CHANGE, thundering ? 1f : 0f));
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

    private static WrapperPlayServerTimeUpdate clockPacket(World world, long ticks, float rate) {
        return new WrapperPlayServerTimeUpdate(world.getFullTime(),
                Map.of(WorldClocks.OVERWORLD, new ClockNetworkState(ticks, 0f, rate)));
    }

    private static void send(Player player, PacketWrapper<?> packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
    }
}
