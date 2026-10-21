package io.corefactory.api;

import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Effects that exist only on one player's client. Implemented with PacketEvents in the Spigot module.
 */
public interface ClientEffects {

    /**
     * Holds the player's clock at {@code timeOfDay} (0-24000) regardless of the world's real time, until
     * {@link #resetClientTime} is called. The hold survives the server's own periodic time updates.
     */
    void setClientTime(Player player, long timeOfDay);

    /** Hands the player's clock back to the server. */
    void resetClientTime(Player player);

    /** Shows rain and thunder to one player, whatever the world is doing. */
    void setClientWeather(Player player, boolean raining, boolean thundering);

    /** Shows the given blocks to one player only. The server's world is not modified. */
    void showBlocks(Player player, World world, BlockChangeSet changes);

    /** Re-sends the real server state of the given positions to one player, undoing {@link #showBlocks}. */
    void restoreBlocks(Player player, World world, BlockChangeSet changes);
}
