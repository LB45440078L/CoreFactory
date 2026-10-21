package io.corefactory.spigot.friends;

import io.corefactory.core.config.Messages;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code /cf friends add|remove <player>} and {@code /cf friends list}: the owner's friend list. A friend is never a target of the
 * owner's turrets while their friendly-fire switch is off, and a turret of a friend's claim trusts them the same way. Needs
 * {@link #PERMISSION}.
 */
public final class FriendsCommand {

    /** The permission to keep a friend list. */
    public static final String PERMISSION = "corefactory.friends";

    private final Friends friends;

    /** @param friends the internal friend lists */
    public FriendsCommand(Friends friends) {
        this.friends = friends;
    }

    /**
     * Runs {@code /cf friends ...}. The arguments start with {@code friends}.
     *
     * @param sender the command's sender
     * @param args   the arguments, the first of which is {@code friends}
     */
    public void run(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.color("&cOnly a player can keep a friend list."));
            return;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Messages.color("&cYou may not keep a friend list."));
            return;
        }
        String sub = args.length < 2 ? "list" : args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add" -> add(player, args);
            case "remove" -> remove(player, args);
            case "list" -> list(player);
            default -> usage(player);
        }
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

    private void add(Player player, String[] args) {
        if (args.length < 3) {
            usage(player);
            return;
        }
        OfflinePlayer target = known(args[2]);
        if (target == null) {
            player.sendMessage(Messages.color("&cNo player named &f" + args[2] + "&c has joined this server."));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(Messages.color("&cYou cannot make yourself a friend."));
            return;
        }
        String name = nameOf(target, args[2]);
        if (friends.add(player.getUniqueId(), target.getUniqueId(), name)) {
            player.sendMessage(Messages.color("&aAdded &f" + name + " &ato your friends."));
        } else {
            player.sendMessage(Messages.color("&7" + name + " is already your friend."));
        }
    }

    private void remove(Player player, String[] args) {
        if (args.length < 3) {
            usage(player);
            return;
        }
        UUID owner = player.getUniqueId();
        Optional<Friends.Friend> listed = friends.of(owner).stream()
                .filter(friend -> friend.name().equalsIgnoreCase(args[2])).findFirst();
        UUID id = listed.map(Friends.Friend::id).orElseGet(() -> {
            OfflinePlayer cached = known(args[2]);
            return cached == null ? null : cached.getUniqueId();
        });
        if (id == null || !friends.remove(owner, id)) {
            player.sendMessage(Messages.color("&c" + args[2] + " is not in your friends."));
            return;
        }
        player.sendMessage(Messages.color("&aRemoved &f" + args[2] + " &afrom your friends."));
    }

    private void list(Player player) {
        List<Friends.Friend> of = friends.of(player.getUniqueId());
        if (of.isEmpty()) {
            player.sendMessage(Messages.color("&7You have no friends yet. Add one with &f/cf friends add <player>&7."));
            return;
        }
        player.sendMessage(Messages.color("&6Your friends (" + of.size() + "):"));
        for (Friends.Friend friend : of) {
            player.sendMessage(Messages.color("&7- &f" + (friend.name().isEmpty() ? friend.id().toString() : friend.name())));
        }
    }

    private static void usage(Player player) {
        player.sendMessage(Messages.color("&7Usage: &f/cf friends add <player>&7, &f/cf friends remove <player>&7, "
                + "&f/cf friends list"));
    }

    private static String nameOf(OfflinePlayer target, String typed) {
        String name = target.getName();
        return name == null || name.isEmpty() ? typed : name;
    }
}
