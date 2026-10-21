package io.corefactory.spigot.geology;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import io.corefactory.core.geology.GeologyService;
import io.corefactory.spigot.command.Style;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * {@code /cf geology}: {@code status} reports the ore state of the loaded chunks, and {@code retrofit <radius>
 * [force]} queues the chunks around the player for ore placement. Retrofit skips chunks already laid, and never
 * re-places ores into a chunk that has an ore budget, even when forced.
 */
public final class GeologyCommand {

    public static final String PERMISSION = "corefactory.geology";

    /** The largest radius retrofit accepts, in chunks. */
    public static final int MAX_RADIUS = 32;

    private final CoreFactoryCore core;
    private final GeologyWorker worker;

    public GeologyCommand(CoreFactoryCore core, GeologyWorker worker) {
        this.core = core;
        this.worker = worker;
    }

    /** Runs a subcommand. {@code args[0]} is {@code geology}. */
    public void run(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(core.messages().get("no-permission"));
            return;
        }
        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "status" -> status(sender);
            case "retrofit" -> retrofit(sender, args);
            default -> usage(sender);
        }
    }

    private void usage(CommandSender sender) {
        sender.sendMessage(Style.TITLE.of("Geology commands:"));
        sender.sendMessage(Style.usage("/cf geology status", "", "marker state of the loaded chunks"));
        sender.sendMessage(Style.usage("/cf geology retrofit", "<radius> [force]", "lay ores into the chunks "
                + "around you (radius in chunks, up to " + MAX_RADIUS + ")"));
    }

    private void status(CommandSender sender) {
        List<World> worlds = sender instanceof Player player ? List.of(player.getWorld()) : Bukkit.getWorlds();
        int current = 0;
        int outdated = 0;
        int unmarked = 0;
        int positions = 0;
        for (World world : worlds) {
            for (Chunk chunk : world.getLoadedChunks()) {
                int marker = OreChunks.marker(chunk);
                if (marker >= GeologyService.CURRENT_VERSION) {
                    current++;
                } else if (marker == 0) {
                    unmarked++;
                } else {
                    outdated++;
                }
                int[] registry = OreChunks.registry(chunk);
                positions += registry == null ? 0 : registry.length;
            }
        }
        sender.sendMessage(Messages.color("&6Geology: &f" + worker.service().ores().size() + " &7ores, marker version &f"
                + GeologyService.CURRENT_VERSION));
        sender.sendMessage(Messages.color("&7Loaded chunks: &f" + current + " &7current, &f" + outdated
                + " &7outdated, &f" + unmarked + " &7unmarked"));
        sender.sendMessage(Messages.color("&7Ore positions: &f" + positions + "&7. Queued chunks: &f"
                + worker.queue().size() + "&7 (" + worker.queue().perTick() + " per tick)."));
    }

    private void retrofit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(core.messages().get("player-only"));
            return;
        }
        int radius;
        try {
            radius = args.length >= 3 ? Integer.parseInt(args[2]) : -1;
        } catch (NumberFormatException e) {
            radius = -1;
        }
        if (radius < 0 || radius > MAX_RADIUS) {
            usage(sender);
            return;
        }
        if (worker.service().ores().isEmpty()) {
            sender.sendMessage(Messages.color("&cNo ores are loaded, so there is nothing to lay."));
            return;
        }
        boolean force = args.length >= 4 && args[3].equalsIgnoreCase("force");
        World world = player.getWorld();
        Chunk centre = player.getLocation().getChunk();
        int queued = 0;
        int skipped = 0;
        int kept = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = centre.getX() + dx;
                int z = centre.getZ() + dz;
                if (!world.isChunkLoaded(x, z) && !world.isChunkGenerated(x, z)) {
                    skipped++;
                    continue;
                }
                if (worker.budgets().isBudgeted(world.getName(), x, z)
                        || (force && world.isChunkLoaded(x, z) && OreChunks.marker(world.getChunkAt(x, z)) > 0)) {
                    kept++;
                    continue;
                }
                if (!force && world.isChunkLoaded(x, z)
                        && OreChunks.marker(world.getChunkAt(x, z)) >= GeologyService.CURRENT_VERSION) {
                    skipped++;
                    continue;
                }
                if (worker.enqueue(world, x, z, force)) {
                    queued++;
                } else {
                    skipped++;
                }
            }
        }
        if (kept > 0) {
            String message = "Retrofit kept " + kept + " budgeted chunk(s) in " + world.getName()
                    + " as they are: their ores are not placed again.";
            worker.log().info(message);
            sender.sendMessage(Messages.color("&e" + message));
        }
        sender.sendMessage(Messages.color("&7Queued &f" + queued + " &7chunks in &f" + world.getName()
                + "&7 (" + skipped + " skipped" + (force ? ", forced" : "") + "). " + worker.queue().size()
                + " waiting, " + worker.queue().perTick() + " per tick."));
    }
}
