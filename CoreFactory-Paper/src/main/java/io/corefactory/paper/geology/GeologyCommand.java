package io.corefactory.paper.geology;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DepositFilter;
import io.corefactory.core.deposit.DepositScan;
import io.corefactory.core.deposit.DepositSettings;
import io.corefactory.core.deposit.DepositStore;
import io.corefactory.core.deposit.DepositText;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.text.Text;
import io.corefactory.paper.command.Style;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * {@code /cf geology deposits [radius]} lists the deposits whose circle reaches within the radius of the player, nearest first,
 * in the same chat line as the scanner: the ore, the direction and distance, the tier and the share left. Players only. The
 * deposit map ({@code /cf deposits}) is the menu of the same list. The command tree checks {@link #PERMISSION}.
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

    /** {@code /cf geology} with no subcommand: the usage lines. */
    public void usage(CommandSender sender) {
        sender.sendMessage(Style.TITLE.of("Geology commands:"));
        sender.sendMessage(Style.usage("/cf geology deposits", "[radius]", "the deposits within radius blocks of you (default "
                + DEFAULT_RADIUS + ", up to " + MAX_RADIUS + ")"));
    }

    /**
     * {@code /cf geology deposits [radius]}: the deposits within the radius of the player, nearest first. A radius outside 1 to
     * {@link #MAX_RADIUS} shows the usage.
     *
     * @param sender the sender, who must be a player
     * @param radius the radius in blocks
     */
    public void deposits(CommandSender sender, int radius) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(core.messages().get("player-only"));
            return;
        }
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
        List<Deposit> found = DepositScan.nearest(store.overlapping(world, x, z, reach), x, z, radius, DepositFilter.ALL,
                Integer.MAX_VALUE);
        sender.sendMessage(Text.parse("&6Deposits within &f" + radius + " &6blocks of you in &f" + Text.escape(world) + "&6: &f"
                + found.size()));
        if (found.isEmpty()) {
            sender.sendMessage(Text.parse("&7None. Try a larger radius."));
            return;
        }
        for (Deposit deposit : found) {
            sender.sendMessage(Text.parse(DepositText.listLine(nameOf(deposit.ore()), deposit, x, z)));
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
}
