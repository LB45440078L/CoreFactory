package io.corefactory.paper.friends;

import io.corefactory.core.access.DeviceAccess;

import java.util.UUID;

/**
 * The friend rule of the plugin: a player may use a device their friend placed. It asks the owner's friend list and gives the answer to the
 * core rule, {@link DeviceAccess#mayUse}, so every listener and service that lets a friend use a device goes through the same check. Used from
 * the main thread only.
 */
public final class FriendAccess {

    private final Friends friends;

    /** @param friends the friend lists, which answer whether a player is a friend of an owner */
    public FriendAccess(Friends friends) {
        this.friends = friends;
    }

    /**
     * True when the player may use a device the owner placed: the owner, a friend of the owner, or anyone when no owner is known.
     *
     * @param owner  the id of the player who placed the device, or null when none is known
     * @param player the id of the player who acts
     */
    public boolean mayUse(UUID owner, UUID player) {
        return DeviceAccess.mayUse(owner, player, owner != null && friends.isFriend(owner, player));
    }

    /** True when the other player is a friend of the owner. */
    public boolean isFriend(UUID owner, UUID other) {
        return friends.isFriend(owner, other);
    }
}
