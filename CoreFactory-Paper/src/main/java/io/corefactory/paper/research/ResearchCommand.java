package io.corefactory.paper.research;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.research.ResearchService;
import io.corefactory.core.research.ResearchTree;
import io.corefactory.paper.command.Style;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * {@code /cf research}: for administrators. {@code max [player]} finishes every node for a player, and
 * {@code grant <node> [player]} finishes one node together with the nodes it requires. The player may be offline: the
 * research is written to the store and is there when they next join. The command tree checks {@link #PERMISSION}.
 */
public final class ResearchCommand {

    public static final String PERMISSION = "corefactory.research.admin";

    private final CoreFactoryCore core;
    private final ResearchService research;

    public ResearchCommand(CoreFactoryCore core, ResearchService research) {
        this.core = core;
        this.research = research;
    }

    /** {@code /cf research} with no subcommand, or with an incomplete one: the usage lines. */
    public void usage(CommandSender sender) {
        sender.sendMessage(Style.TITLE.of("Research commands:"));
        sender.sendMessage(Style.usage("/cf research max", "[player]", "finish every node"));
        sender.sendMessage(Style.usage("/cf research grant", "<node> [player]",
                "finish a node and the nodes it needs"));
        sender.sendMessage(Style.usage("/cf research nodes", "", "list the node ids"));
    }

    /**
     * {@code /cf research max [player]}: finishes every node for the named player, or for the sender when no name is given.
     *
     * @param sender the sender
     * @param target the player's name, or {@code null} for the sender
     */
    public void max(CommandSender sender, String target) {
        Optional<OfflinePlayer> player = resolve(sender, target);
        if (player.isEmpty()) {
            return;
        }
        UUID id = player.get().getUniqueId();
        for (ResearchTree.Node node : research.tree().nodes()) {
            research.grant(id, node.id());
        }
        sender.sendMessage(Style.SUCCESS.of("Finished all " + research.tree().nodes().size() + " research nodes for ")
                .append(Style.ID.of(name(player.get()))).append(Style.SUCCESS.of(".")));
    }

    /**
     * {@code /cf research grant <node> [player]}: finishes the node and the nodes it requires, for the named player or the sender.
     *
     * @param sender the sender
     * @param node   the node id typed
     * @param target the player's name, or {@code null} for the sender
     */
    public void grant(CommandSender sender, String node, String target) {
        if (research.tree().node(node).isEmpty()) {
            sender.sendMessage(Style.ERROR.of("Unknown research node: ").append(Style.ID.of(node))
                    .append(Style.ERROR.of(". See ")).append(Style.COMMAND.of("/cf research nodes")).append(Style.ERROR.of(".")));
            return;
        }
        Optional<OfflinePlayer> player = resolve(sender, target);
        if (player.isEmpty()) {
            return;
        }
        UUID id = player.get().getUniqueId();
        Set<String> chain = chainOf(node);
        for (String required : chain) {
            research.grant(id, required);
        }
        sender.sendMessage(Style.SUCCESS.of("Finished ").append(Style.ID.of(node)).append(Style.SUCCESS.of(" and "
                + (chain.size() - 1) + " earlier node(s) for ")).append(Style.ID.of(name(player.get())))
                .append(Style.SUCCESS.of(".")));
    }

    /** {@code /cf research nodes}: the node ids, in the order of the tree. */
    public void nodes(CommandSender sender) {
        StringBuilder line = new StringBuilder();
        for (ResearchTree.Node node : research.tree().nodes()) {
            line.append(node.id()).append(' ');
        }
        sender.sendMessage(Style.DESCRIPTION.of("Nodes: ").append(Style.ID.of(line.toString().trim())));
    }

    /** The node ids that {@code /cf research grant} takes, for completion. */
    public List<String> nodeIds() {
        return research.tree().nodes().stream().map(ResearchTree.Node::id).toList();
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
            sender.sendMessage(Style.ERROR.of("Name a player: ").append(Style.COMMAND.of("/cf research max"))
                    .append(Component.text(" ")).append(Style.ARGUMENT.of("<player>")));
            return Optional.empty();
        }
        Player online = Bukkit.getPlayerExact(name);
        OfflinePlayer found = online != null ? online : Bukkit.getOfflinePlayer(name);
        if (!found.isOnline() && !found.hasPlayedBefore()) {
            sender.sendMessage(Style.ERROR.of("No player named ").append(Style.ID.of(name))
                    .append(Style.ERROR.of(" has joined this server.")));
            return Optional.empty();
        }
        return Optional.of(found);
    }

    private static String name(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }
}
