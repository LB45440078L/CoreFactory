package io.corefactory.paper.friends;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The internal friend lists of the players: each owner keeps a set of friends, and a turret its owner placed never targets one of them
 * while its friendly-fire switch is off. Changes are saved at once through the {@link FriendStore}. Used from the main thread only.
 */
public final class Friends {

    /**
     * One friend of an owner.
     *
     * @param id   the friend's id
     * @param name the friend's name when it was added
     */
    public record Friend(UUID id, String name) {
    }

    private final FriendStore store;
    private final Map<UUID, Map<UUID, String>> byOwner;

    /** @param store the file the friends are kept in; its saved friends are read now */
    public Friends(FriendStore store) {
        this.store = store;
        this.byOwner = new HashMap<>(store.read());
    }

    /** True when the other player is a friend of the owner. */
    public boolean isFriend(UUID owner, UUID other) {
        Map<UUID, String> friends = byOwner.get(owner);
        return friends != null && friends.containsKey(other);
    }

    /** The friends of an owner, by name, then by id. Empty when the owner has none. */
    public List<Friend> of(UUID owner) {
        Map<UUID, String> friends = byOwner.getOrDefault(owner, Map.of());
        List<Friend> list = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : friends.entrySet()) {
            list.add(new Friend(entry.getKey(), entry.getValue()));
        }
        list.sort(Comparator.comparing(Friend::name, String.CASE_INSENSITIVE_ORDER).thenComparing(friend -> friend.id().toString()));
        return list;
    }

    /**
     * Adds a friend to an owner, and saves.
     *
     * @return false when the other player was already a friend, and nothing changed
     */
    public boolean add(UUID owner, UUID friend, String name) {
        Map<UUID, String> friends = byOwner.computeIfAbsent(owner, key -> new HashMap<>());
        if (friends.containsKey(friend)) {
            return false;
        }
        friends.put(friend, name == null ? "" : name);
        save();
        return true;
    }

    /**
     * Removes a friend from an owner, and saves.
     *
     * @return false when the other player was not a friend, and nothing changed
     */
    public boolean remove(UUID owner, UUID friend) {
        Map<UUID, String> friends = byOwner.get(owner);
        if (friends == null || friends.remove(friend) == null) {
            return false;
        }
        if (friends.isEmpty()) {
            byOwner.remove(owner);
        }
        save();
        return true;
    }

    private void save() {
        store.write(byOwner);
    }
}
