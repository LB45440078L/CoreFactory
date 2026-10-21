package io.corefactory.spigot.research;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.research.ResearchService;
import io.corefactory.core.research.ResearchTree;
import io.corefactory.spigot.command.Style;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * {@code /cf research}: for administrators. {@code max [player]} finishes every node for a player, and
 * {@code grant <node> [player]} finishes one node together with the nodes it requires. The player may be offline: the
 * research is written to the store and is there when they next join.
 */
public final class ResearchCommand {

    public static final String PERMISSION = "corefactory.research.admin";

    private final CoreFactoryCore core;
    private final ResearchService research;

    public ResearchCommand(CoreFactoryCore core, ResearchService research) {
        this.core = core;
        this.research = research;
    }

    /** Runs a subcommand. {@code args[0]} is {@code research}. */
    public void run(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(core.messages().get("no-permission"));
            return;
        }
        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "max" -> max(sender, args.length >= 3 ? args[2] : null);
            case "grant" -> grant(sender, args);
            case "nodes" -> nodes(sender);
            default -> usage(sender);
        }
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(Style.TITLE.of("Research commands:"));
        sender.sendMessage(Style.usage("/cf research max", "[player]", "finish every node"));
        sender.sendMessage(Style.usage("/cf research grant", "<node> [player]",
                "finish a node and the nodes it needs"));
        sender.sendMessage(Style.usage("/cf research nodes", "", "list the node ids"));
    }

    private void max(CommandSender sender, String target) {
        Optional<OfflinePlayer> player = resolve(sender, target);
        if (player.isEmpty()) {
            return;
        }
        UUID id = player.get().getUniqueId();
        for (ResearchTree.Node node : research.tree().nodes()) {
            research.grant(id, node.id());
        }
        sender.sendMessage(Style.SUCCESS.of("Finished all " + research.tree().nodes().size() + " research nodes for ")
                + Style.ID.of(name(player.get())) + Style.SUCCESS.of("."));
    }

    private void grant(CommandSender sender, String[] args) {
        if (args.length < 3) {
            usage(sender);
            return;
        }
        String node = args[2];
        if (research.tree().node(node).isEmpty()) {
            sender.sendMessage(Style.ERROR.of("Unknown research node: ") + Style.ID.of(node)
                    + Style.ERROR.of(". See ") + Style.COMMAND.of("/cf research nodes") + Style.ERROR.of("."));
            return;
        }
        Optional<OfflinePlayer> player = resolve(sender, args.length >= 4 ? args[3] : null);
        if (player.isEmpty()) {
            return;
        }
        UUID id = player.get().getUniqueId();
        Set<String> chain = chainOf(node);
        for (String required : chain) {
            research.grant(id, required);
        }
        sender.sendMessage(Style.SUCCESS.of("Finished ") + Style.ID.of(node) + Style.SUCCESS.of(" and "
                + (chain.size() - 1) + " earlier node(s) for ") + Style.ID.of(name(player.get()))
                + Style.SUCCESS.of("."));
    }

    private void nodes(CommandSender sender) {
        StringBuilder line = new StringBuilder();
        for (ResearchTree.Node node : research.tree().nodes()) {
            line.append(node.id()).append(' ');
        }
        sender.sendMessage(Style.DESCRIPTION.of("Nodes: ") + Style.ID.of(line.toString().trim()));
    }

    /** The node and every node it requires, earliest first, so the research stays a line. */
    private Set<String> chainOf(String node) {
        Set<String> chain = new LinkedHashSet<>();
        visit(node, chain);
        return chain;
    }

    private void visit(String node, Set<String> chain) {
        research.tree().node(node).ifPresent(definition -> {
            for (String required : definition.requires()) {
                visit(required, chain);
            }
        });
        chain.add(node);
    }

    /** The named player, or the sender when no name is given and the sender is a player. */
    private Optional<OfflinePlayer> resolve(CommandSender sender, String name) {
        if (name == null) {
            if (sender instanceof Player player) {
                return Optional.of(player);
            }
            sender.sendMessage(Style.ERROR.of("Name a player: ") + Style.COMMAND.of("/cf research max") + " "
                    + Style.ARGUMENT.of("<player>"));
            return Optional.empty();
        }
        Player online = Bukkit.getPlayerExact(name);
        OfflinePlayer found = online != null ? online : Bukkit.getOfflinePlayer(name);
        if (!found.isOnline() && !found.hasPlayedBefore()) {
            sender.sendMessage(Style.ERROR.of("No player named ") + Style.ID.of(name)
                    + Style.ERROR.of(" has joined this server."));
            return Optional.empty();
        }
        return Optional.of(found);
    }

    private static String name(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }
}
