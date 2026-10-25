package io.corefactory.paper.energy;

import io.corefactory.core.energy.ChartMode;
import io.corefactory.core.text.Text;
import io.corefactory.paper.command.Style;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * {@code /cf grid ...}: the grid charts. {@code create}, {@code remove} and {@code mode} need {@code corefactory.grid}; {@code list}
 * and {@code info} need {@code corefactory.pylons}. {@code create} and {@code info} need a player, since they use the player's block.
 * The command tree checks the permissions before these methods run.
 */
public final class ChartCommand {

    /** The usage lines of {@code /cf grid}. */
    public static final List<Component> USAGE = List.of(
            Style.usage("/cf grid create", "<name>", "a chart of your grid, one block above your head"),
            Style.usage("/cf grid remove", "<name>", "remove a chart"),
            Style.usage("/cf grid list", "", "the charts, their grid, mode and viewers"),
            Style.usage("/cf grid mode", "<name> <satisfaction|production|storage>", "switch a chart's mode"),
            Style.usage("/cf grid info", "", "the grid you stand in"));

    private final ChartService charts;

    /**
     * @param charts the chart service that makes and changes the charts
     */
    public ChartCommand(ChartService charts) {
        this.charts = charts;
    }

    /** {@code /cf grid} with no subcommand: the usage lines. */
    public void usage(CommandSender sender) {
        for (Component line : USAGE) {
            sender.sendMessage(line);
        }
    }

    /**
     * {@code /cf grid create <name>}: makes a chart of the player's grid.
     *
     * @param sender the sender, who must be a player
     * @param name   the chart's name, or {@code null} when none is typed, which shows the usage
     */
    public void create(CommandSender sender, String name) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Style.ERROR.of("Only a player can make a chart."));
            return;
        }
        if (name == null) {
            sender.sendMessage(Style.usageError("/cf grid create", "<name>"));
            return;
        }
        sender.sendMessage(Text.parse(charts.create(player, name)));
    }

    /**
     * {@code /cf grid remove <name>}: removes a chart.
     *
     * @param sender the sender
     * @param name   the chart's name, or {@code null} when none is typed, which shows the usage
     */
    public void remove(CommandSender sender, String name) {
        if (name == null) {
            sender.sendMessage(Style.usageError("/cf grid remove", "<name>"));
            return;
        }
        sender.sendMessage(Text.parse(charts.remove(name)));
    }

    /**
     * {@code /cf grid mode <name> <satisfaction|production|storage>}: switches a chart's mode.
     *
     * @param sender the sender
     * @param name   the chart's name, or {@code null} when none is typed
     * @param mode   the mode typed, or {@code null} when none is typed; either missing shows the usage
     */
    public void mode(CommandSender sender, String name, String mode) {
        Optional<ChartMode> parsed = name != null && mode != null ? ChartRules.parseMode(mode) : Optional.empty();
        if (parsed.isEmpty()) {
            sender.sendMessage(Style.usageError("/cf grid mode", "<name> <satisfaction|production|storage>"));
            return;
        }
        sender.sendMessage(Text.parse(charts.setMode(name, parsed.get())));
    }

    /** {@code /cf grid list}: the charts, their grid, mode and viewers. */
    public void list(CommandSender sender) {
        List<String> lines = charts.list();
        if (lines.isEmpty()) {
            sender.sendMessage(Style.DESCRIPTION.of("No charts. Make one with ").append(Style.COMMAND.of("/cf grid create"))
                    .append(Component.text(" ")).append(Style.ARGUMENT.of("<name>")).append(Style.DESCRIPTION.of(".")));
            return;
        }
        for (String line : lines) {
            sender.sendMessage(Text.parse(line));
        }
    }

    /** {@code /cf grid info}: the grid the player stands in. */
    public void info(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Style.ERROR.of("Only a player can ask for their grid."));
            return;
        }
        for (String line : charts.info(player)) {
            sender.sendMessage(Text.parse(line));
        }
    }
}
