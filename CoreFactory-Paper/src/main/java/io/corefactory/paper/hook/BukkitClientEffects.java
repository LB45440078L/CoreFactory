package io.corefactory.paper.hook;

import io.corefactory.api.BlockChangeSet;
import io.corefactory.api.ClientEffects;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;

/**
 * Client-only effects through the Bukkit player API: a per-player clock, per-player rain and per-player block views. Nothing sent here
 * changes the server's world, so the effects vanish for the player when the view is restored.
 *
 * <p>The server's own periodic clock updates do not reach a player whose clock is held, so the hold needs no rewriting of packets.
 * The API has no per-player thunder, so {@link #setClientWeather} shows rain only.
 */
public final class BukkitClientEffects implements ClientEffects {

    /** The length of a day in ticks. */
    private static final long DAY_LENGTH = 24_000L;

    @Override
    public void setClientTime(Player player, long timeOfDay) {
        player.setPlayerTime(Math.floorMod(timeOfDay, DAY_LENGTH), false);
    }

    @Override
    public void resetClientTime(Player player) {
        player.resetPlayerTime();
    }

    @Override
    public void setClientWeather(Player player, boolean raining, boolean thundering) {
        player.setPlayerWeather(raining ? WeatherType.DOWNFALL : WeatherType.CLEAR);
    }

    @Override
    public void showBlocks(Player player, World world, BlockChangeSet changes) {
        for (int i = 0; i < changes.size(); i++) {
            Location at = new Location(world, changes.x(i), changes.y(i), changes.z(i));
            player.sendBlockChange(at, changes.data(i));
        }
    }

    @Override
    public void restoreBlocks(Player player, World world, BlockChangeSet changes) {
        for (int i = 0; i < changes.size(); i++) {
            Location at = new Location(world, changes.x(i), changes.y(i), changes.z(i));
            player.sendBlockChange(at, world.getBlockData(at));
        }
    }
}
