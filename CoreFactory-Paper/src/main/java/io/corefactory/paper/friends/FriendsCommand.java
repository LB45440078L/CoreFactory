package io.corefactory.paper.friends;

import io.corefactory.core.text.Palette;
import io.corefactory.core.text.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code /cf friends add|remove <player>} and {@code /cf friends list}: the owner's friend list. A friend is never a target of the
 * owner's turrets while their friendly-fire switch is off, and a turret of a friend's claim trusts them the same way. The command
 * tree checks {@link #PERMISSION}.
 */
public final class FriendsCommand {

    /** The permission to keep a friend list. */
    public static final String PERMISSION = "corefactory.friends";

    private final Friends friends;

    /** @param friends the internal friend lists */
    public FriendsCommand(Friends friends) {
        this.friends = friends;
    }

    /** {@code /cf friends} and {@code /cf friends list}: the player's friends. */
    public void list(CommandSender sender) {
        Player player = playerOf(sender);
        if (player == null) {
            return;
        }
        List<Friends.Friend> of = friends.of(player.getUniqueId());
        if (of.isEmpty()) {
            player.sendMessage(Text.parse("&7You have no friends yet. Add one with &f/cf friends add <player>&7."));
            return;
        }
        player.sendMessage(Text.parse("&6Your friends (" + of.size() + "):"));
        for (Friends.Friend friend : of) {
            player.sendMessage(friendLine(friend));
        }
    }

    /**
     * {@code /cf friends add <player>}: adds a player who has joined this server to the friends.
     *
     * @param sender the sender, who must be a player
     * @param name   the player's name, or {@code null} when none is typed, which shows the usage
     */
    public void add(CommandSender sender, String name) {
        Player player = playerOf(sender);
        if (player == null) {
            return;
        }
        if (name == null) {
            usage(player);
            return;
        }
        OfflinePlayer target = known(name);
        if (target == null) {
            player.sendMessage(Text.parse("&cNo player named &f" + Text.escape(name) + "&c has joined this server."));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Text.parse("&cYou cannot make yourself a friend."));
            return;
        }
        String friendName = nameOf(target, name);
        if (friends.add(player.getUniqueId(), target.getUniqueId(), friendName)) {
            player.sendMessage(Text.parse("&aAdded &f" + Text.escape(friendName) + " &ato your friends."));
        } else {
            player.sendMessage(Text.parse("&7" + Text.escape(friendName) + " is already your friend."));
        }
    }

    /**
     * {@code /cf friends remove <player>}: removes a friend.
     *
     * @param sender the sender, who must be a player
     * @param name   the friend's name, or {@code null} when none is typed, which shows the usage
     */
    public void remove(CommandSender sender, String name) {
        Player player = playerOf(sender);
        if (player == null) {
            return;
        }
        if (name == null) {
            usage(player);
            return;
        }
        UUID owner = player.getUniqueId();
        Optional<Friends.Friend> listed = friends.of(owner).stream()
                .filter(friend -> friend.name().equalsIgnoreCase(name)).findFirst();
        UUID id = listed.map(Friends.Friend::id).orElseGet(() -> {
            OfflinePlayer cached = known(name);
            return cached == null ? null : cached.getUniqueId();
        });
        if (id == null || !friends.remove(owner, id)) {
            player.sendMessage(Text.parse("&c" + Text.escape(name) + " is not in your friends."));
            return;
        }
        player.sendMessage(Text.parse("&aRemoved &f" + Text.escape(name) + " &afrom your friends."));
    }

    /** The sender as a player, or {@code null} after telling a non-player that only a player can keep a friend list. */
    private static Player playerOf(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(Text.parse("&cOnly a player can keep a friend list."));
        return null;
    }

    /** A player who is online, or who is already known to the server by name. Never waits for a profile lookup. */
    private static OfflinePlayer known(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (name.equalsIgnoreCase(offline.getName())) {
                return offline;
            }
        }
        return null;
    }

    /**
     * A friend in the list. A friend with a name is clickable: the click puts the removal command into the chat line, for the player to
     * send or change.
     */
    private static Component friendLine(Friends.Friend friend) {
        if (friend.name().isEmpty()) {
            return Text.parse("&7- &f" + friend.id());
        }
        Component name = Text.hover(Component.text(friend.name(), Palette.VALUE), Text.parse("&7Click to type the command that removes them"));
        return Component.text("- ", Palette.MUTED).append(Text.suggest(name, "/cf friends remove " + friend.name()));
    }

    private static void usage(Player player) {
        player.sendMessage(Text.parse("&7Usage: &f/cf friends add <player>&7, &f/cf friends remove <player>&7, "
                + "&f/cf friends list"));
    }

    private static String nameOf(OfflinePlayer target, String typed) {
        String name = target.getName();
        return name == null || name.isEmpty() ? typed : name;
    }
}
