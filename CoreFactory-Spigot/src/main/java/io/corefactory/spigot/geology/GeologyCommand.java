package io.corefactory.spigot.geology;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DepositSettings;
import io.corefactory.core.deposit.DepositStore;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.spigot.command.Style;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * {@code /cf geology deposits [radius]} lists the deposits whose circle reaches within the radius of the player, each with
 * its ore, the amount left of the amount it started with, its centre and its radius. Players only.
 */
public final class GeologyCommand {

    public static final String PERMISSION = "corefactory.geology";

    /** The radius the list uses when none is given, in blocks. */
    public static final int DEFAULT_RADIUS = 64;

    /** The largest radius the list accepts, in blocks. */
    public static final int MAX_RADIUS = 512;

    private final CoreFactoryCore core;
    private final DepositStore store;
    private final DepositSettings settings;

    public GeologyCommand(CoreFactoryCore core, DepositStore store, DepositSettings settings) {
        this.core = core;
        this.store = store;
        this.settings = settings;
    }

    /** Runs a subcommand. {@code args[0]} is {@code geology}. */
    public void run(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(core.messages().get("no-permission"));
            return;
        }
        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "deposits" -> deposits(sender, args);
            default -> usage(sender);
        }
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(Style.TITLE.of("Geology commands:"));
        sender.sendMessage(Style.usage("/cf geology deposits", "[radius]", "the deposits within radius blocks of you (default "
                + DEFAULT_RADIUS + ", up to " + MAX_RADIUS + ")"));
    }

    private void deposits(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(core.messages().get("player-only"));
            return;
        }
        int radius = args.length >= 3 ? parseRadius(args[2]) : DEFAULT_RADIUS;
        if (radius < 1 || radius > MAX_RADIUS) {
            usage(sender);
            return;
        }
        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();
        String world = player.getWorld().getName();
        // The store needs a reach of at least the largest radius it holds, so it is asked with that, and the list is
        // then cut down to the radius the player asked for.
        int reach = Math.max(radius, settings.maxRadius());
        List<Deposit> found = store.overlapping(world, x, z, reach).stream()
                .filter(deposit -> DepositStore.reaches(deposit, x, z, radius))
                .sorted(Comparator.comparingLong((Deposit deposit) -> squared(deposit.centreX() - x, deposit.centreZ() - z)))
                .toList();
        sender.sendMessage(Messages.color("&6Deposits within &f" + radius + " &6blocks of you in &f" + world + "&6: &f"
                + found.size()));
        if (found.isEmpty()) {
            sender.sendMessage(Messages.color("&7None. Try a larger radius."));
            return;
        }
        for (Deposit deposit : found) {
            sender.sendMessage(Messages.color("&8 - " + nameOf(deposit.ore()) + "&7: &f" + deposit.remaining()
                    + " &7of &f" + deposit.initialAmount() + " &7left, centre &f" + deposit.centreX() + ", "
                    + deposit.centreZ() + "&7, radius &f" + deposit.radius()));
        }
    }

    /** The radius typed, or -1 when it is not a number. */
    private static int parseRadius(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String nameOf(String id) {
        for (OreDefinition ore : core.ores()) {
            if (ore.id().equals(id)) {
                return ore.name();
            }
        }
        return id;
    }

    private static long squared(long dx, long dz) {
        return dx * dx + dz * dz;
    }
}
