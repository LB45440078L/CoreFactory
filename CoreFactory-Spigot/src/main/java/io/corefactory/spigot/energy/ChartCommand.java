package io.corefactory.spigot.energy;

import io.corefactory.core.config.Messages;
import io.corefactory.core.energy.ChartMode;
import io.corefactory.spigot.command.Style;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * {@code /cf grid ...}: the grid charts. {@code create} and {@code remove} need {@code corefactory.grid}, and so does {@code mode};
 * {@code list} and {@code info} need {@code corefactory.pylons}. {@code create} and {@code info} need a player, since they use the
 * player's block.
 */
public final class ChartCommand {

    /** The usage lines of {@code /cf grid}. */
    public static final List<String> USAGE = List.of(
            Style.usage("/cf grid create", "<name>", "a chart of your grid, one block above your head"),
            Style.usage("/cf grid remove", "<name>", "remove a chart"),
            Style.usage("/cf grid list", "", "the charts, their grid, mode and viewers"),
            Style.usage("/cf grid mode", "<name> <satisfaction|production|storage>", "switch a chart's mode"),
            Style.usage("/cf grid info", "", "the grid you stand in"));

    private static final String PERMISSION_GRID = "corefactory.grid";
    private static final String PERMISSION_PYLONS = "corefactory.pylons";

    private final ChartService charts;

    /**
     * @param charts the chart service that makes and changes the charts
     */
    public ChartCommand(ChartService charts) {
        this.charts = charts;
    }

    /**
     * Runs {@code /cf grid} with the arguments after {@code grid}.
     *
     * @param sender the sender
     * @param args   the arguments of {@code /cf}, the first being {@code grid}
     */
    public void run(CommandSender sender, String[] args) {
        String sub = args.length < 2 ? "" : args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> create(sender, args);
            case "remove" -> remove(sender, args);
            case "list" -> list(sender);
            case "mode" -> mode(sender, args);
            case "info" -> info(sender);
            default -> usage(sender);
        }
    }

    private void create(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_GRID)) {
            sender.sendMessage(Style.ERROR.of("You do not have permission."));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Style.ERROR.of("Only a player can make a chart."));
            return;
        }
        if (args.length != 3) {
            sender.sendMessage(Style.usageError("/cf grid create", "<name>"));
            return;
        }
        sender.sendMessage(Messages.color(charts.create(player, args[2])));
    }

    private void remove(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_GRID)) {
            sender.sendMessage(Style.ERROR.of("You do not have permission."));
            return;
        }
        if (args.length != 3) {
            sender.sendMessage(Style.usageError("/cf grid remove", "<name>"));
            return;
        }
        sender.sendMessage(Messages.color(charts.remove(args[2])));
    }

    private void mode(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_GRID)) {
            sender.sendMessage(Style.ERROR.of("You do not have permission."));
            return;
        }
        Optional<ChartMode> mode = args.length == 4 ? ChartRules.parseMode(args[3]) : Optional.empty();
        if (mode.isEmpty()) {
            sender.sendMessage(Style.usageError("/cf grid mode", "<name> <satisfaction|production|storage>"));
            return;
        }
        sender.sendMessage(Messages.color(charts.setMode(args[2], mode.get())));
    }

    private void list(CommandSender sender) {
        if (!sender.hasPermission(PERMISSION_PYLONS)) {
            sender.sendMessage(Style.ERROR.of("You do not have permission."));
            return;
        }
        List<String> lines = charts.list();
        if (lines.isEmpty()) {
            sender.sendMessage(Style.DESCRIPTION.of("No charts. Make one with ") + Style.COMMAND.of("/cf grid create")
                    + " " + Style.ARGUMENT.of("<name>") + Style.DESCRIPTION.of("."));
            return;
        }
        for (String line : lines) {
            sender.sendMessage(Messages.color(line));
        }
    }

    private void info(CommandSender sender) {
        if (!sender.hasPermission(PERMISSION_PYLONS)) {
            sender.sendMessage(Style.ERROR.of("You do not have permission."));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Style.ERROR.of("Only a player can ask for their grid."));
            return;
        }
        for (String line : charts.info(player)) {
            sender.sendMessage(Messages.color(line));
        }
    }

    private static void usage(CommandSender sender) {
        for (String line : USAGE) {
            sender.sendMessage(line);
        }
    }
}
