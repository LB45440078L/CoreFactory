package io.corefactory.core.access;

import java.util.UUID;

/**
 * The rule that decides whether a player may use a device another player placed: its menus, its kits and modules, its switches, its loading
 * and its breaking. The owner may, a friend of the owner may, and a device with no known owner (one saved before owners were kept) may be used
 * by anyone. The caller says whether the player is a friend, from the owner's friend list, so the rule itself needs no server. Pure.
 *
 * <p>The rule replaces only the owner check. A protection plugin still decides the block events that a use fires, so a friend in a claim or a
 * region that refuses them is refused as anyone else is.
 */
public final class DeviceAccess {

    private DeviceAccess() {
    }

    /**
     * True when the player may use a device.
     *
     * @param owner  the id of the player who placed the device, or null when none is known
     * @param player the id of the player who acts
     * @param friend true when the player is a friend of the owner
     */
    public static boolean mayUse(UUID owner, UUID player, boolean friend) {
        return owner == null || owner.equals(player) || friend;
    }
}
