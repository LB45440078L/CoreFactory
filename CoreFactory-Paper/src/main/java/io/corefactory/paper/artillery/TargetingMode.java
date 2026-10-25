package io.corefactory.paper.artillery;

import org.bukkit.entity.Player;

/**
 * Sets the target of a Command Center: the player's view on the map, while the center listens. The Set Target button of the main dialog
 * starts it. Its implementation is the map's listening mode, which replaces the placeholder that is wired in for now.
 */
public interface TargetingMode {

    /**
     * Starts setting the target of a center for a player.
     *
     * @param player the owner, who is online
     * @param center the center whose target is set
     */
    void begin(Player player, CommandCenter center);
}
