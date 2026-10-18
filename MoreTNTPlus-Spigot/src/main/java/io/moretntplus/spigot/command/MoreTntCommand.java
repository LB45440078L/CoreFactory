package io.moretntplus.spigot.command;

import io.moretntplus.api.PlacedTnt;
import io.moretntplus.api.TntDefinition;
import io.moretntplus.core.MoreTntPlusCore;
import io.moretntplus.core.config.Messages;
import io.moretntplus.spigot.MoreTntPlusPlugin;
import io.moretntplus.spigot.gui.CatalogGui;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@code /mtnt}: give TNT and components, open the catalogue, inspect and configure placed TNT, reload.
 */
public final class MoreTntCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "help", "give", "list", "gui", "info", "aim", "target", "unlink", "count", "reload");

    private final MoreTntPlusPlugin plugin;
    private final MoreTntPlusCore core;

    public MoreTntCommand(MoreTntPlusPlugin plugin, MoreTntPlusCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "give" -> give(sender, args);
            case "list" -> list(sender);
            case "gui" -> gui(sender);
            case "info" -> info(sender);
            case "aim" -> aim(sender, args);
            case "target" -> target(sender, args);
            case "unlink" -> unlink(sender);
            case "count" -> sender.sendMessage(core.messages().get("placed-count", "count", core.placedTnts().size()));
            case "reload" -> reload(sender);
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender sender) {
        sender.sendMessage("§6MoreTNT+ commands:");
        sender.sendMessage("§7/mtnt give <id> [player] [amount] §8- give a TNT or component");
        sender.sendMessage("§7/mtnt list §8- every id");
        sender.sendMessage("§7/mtnt gui §8- open the catalogue");
        sender.sendMessage("§7/mtnt info §8- inspect the placed TNT you look at");
        sender.sendMessage("§7/mtnt aim [north|east|south|west|up|down] §8- set its direction (default: your facing)");
        sender.sendMessage("§7/mtnt target [x y z] §8- set a teleport TNT's destination (default: the block you stand on)");
        sender.sendMessage("§7/mtnt unlink §8- remove the fuse links of the TNT you look at");
        sender.sendMessage("§7/mtnt count | reload");
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("moretntplus.give")) {
            sender.sendMessage(core.messages().get("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(core.messages().get("unknown-id", "id", "?"));
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        Player target = args.length >= 3 ? plugin.getServer().getPlayerExact(args[2]) : asPlayer(sender);
        if (target == null) {
            sender.sendMessage(core.messages().get("player-offline", "player", args.length >= 3 ? args[2] : "?"));
            return;
        }
        int requested = 1;
        if (args.length >= 4) {
            try {
                requested = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
            } catch (NumberFormatException e) {
                requested = 1;
            }
        }
        final int amount = requested;
        Optional<ItemStack> item = core.createTntItem(id, amount)
                .or(() -> core.createComponentItem(id, amount));
        if (item.isEmpty()) {
            sender.sendMessage(core.messages().get("unknown-id", "id", id));
            return;
        }
        target.getInventory().addItem(item.get());
        String name = item.get().getItemMeta() != null && item.get().getItemMeta().hasDisplayName()
                ? item.get().getItemMeta().getDisplayName() : id;
        sender.sendMessage(core.messages().get("given", "amount", amount, "name", name, "player", target.getName()));
    }

    private void list(CommandSender sender) {
        sender.sendMessage("§6TNT: §f" + core.definitions().stream()
                .map(TntDefinition::id).collect(Collectors.joining(", ")));
        sender.sendMessage("§6Components: §f" + String.join(", ", core.componentDefinitions().keySet()));
    }

    private void gui(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            sender.sendMessage(core.messages().get("player-only"));
            return;
        }
        if (!player.hasPermission("moretntplus.give")) {
            player.sendMessage(core.messages().get("no-permission"));
            return;
        }
        CatalogGui.create(core).open(player);
    }

    private void info(CommandSender sender) {
        Player player = asPlayer(sender);
        Optional<PlacedTnt> placed = lookedAt(player);
        if (placed.isEmpty()) {
            sender.sendMessage(core.messages().get("not-placed"));
            return;
        }
        PlacedTnt tnt = placed.get();
        Optional<TntDefinition> definition = core.definition(tnt.tntId());
        String name = definition.map(TntDefinition::displayName).orElse(tnt.tntId());
        sender.sendMessage(core.messages().get("info-header", "name", name));
        definition.ifPresent(def -> {
            sender.sendMessage(core.messages().get("info-line", "label", "Mechanic",
                    "value", def.mechanic()));
            sender.sendMessage(core.messages().get("info-line", "label", "Radius", "value", def.radius()));
            sender.sendMessage(core.messages().get("info-line", "label", "Fuse (ticks)", "value", def.fuseTicks()));
            core.mechanicOf(def).ifPresent(mechanic ->
                    sender.sendMessage(core.messages().get("info-line", "label", "Summary",
                            "value", mechanic.summary())));
        });
        sender.sendMessage(core.messages().get("info-line", "label", "Direction", "value",
                tnt.direction() == null ? "none" : describe(tnt.direction())));
        sender.sendMessage(core.messages().get("info-line", "label", "Target", "value",
                tnt.target() == null ? "none" : format(tnt.target())));
        sender.sendMessage(core.messages().get("info-line", "label", "Fuse links", "value", tnt.links().size()));
    }

    private void aim(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        Optional<PlacedTnt> placed = lookedAt(player);
        if (placed.isEmpty()) {
            sender.sendMessage(core.messages().get("not-placed"));
            return;
        }
        if (!player.hasPermission("moretntplus.configure")) {
            player.sendMessage(core.messages().get("no-permission"));
            return;
        }
        Vector direction = args.length >= 2 ? named(args[1]) : player.getLocation().getDirection().setY(0);
        if (direction == null) {
            direction = player.getLocation().getDirection().setY(0);
        }
        core.aim(placed.get(), direction);
        sender.sendMessage(core.messages().get("aimed", "direction", describe(direction)));
    }

    /**
     * Sets the destination of the placed TNT you look at. Without coordinates the destination is the block you
     * stand on; {@code /mtnt target <x> <y> <z>} names another block in your world.
     */
    private void target(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player != null && !player.hasPermission("moretntplus.configure")) {
            player.sendMessage(core.messages().get("no-permission"));
            return;
        }
        Optional<PlacedTnt> placed = lookedAt(player);
        if (placed.isEmpty()) {
            sender.sendMessage(core.messages().get("not-placed"));
            return;
        }
        Location at = player.getLocation().getBlock().getLocation();
        if (args.length >= 4) {
            try {
                at = new Location(player.getWorld(), Integer.parseInt(args[1]), Integer.parseInt(args[2]),
                        Integer.parseInt(args[3]));
            } catch (NumberFormatException e) {
                sender.sendMessage(Messages.color("&cUsage: /mtnt target [x y z]"));
                return;
            }
        }
        placed.get().setTarget(at);
        sender.sendMessage(core.messages().get("target-set", "x", at.getBlockX(), "y", at.getBlockY(), "z", at.getBlockZ()));
    }

    private void unlink(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player != null && !player.hasPermission("moretntplus.configure")) {
            player.sendMessage(core.messages().get("no-permission"));
            return;
        }
        Optional<PlacedTnt> placed = lookedAt(player);
        if (placed.isEmpty()) {
            sender.sendMessage(core.messages().get("not-placed"));
            return;
        }
        core.unlinkAll(placed.get());
        sender.sendMessage(core.messages().get("link-cleared"));
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("moretntplus.reload")) {
            sender.sendMessage(core.messages().get("no-permission"));
            return;
        }
        // Placed TNT stays in memory: reloading configuration never re-reads the placed-TNT file.
        plugin.reloadConfiguration();
        sender.sendMessage(core.messages().get("reloaded", "tnts", core.definitions().size(),
                "components", core.componentDefinitions().size()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && sub.equals("give")) {
            List<String> ids = new ArrayList<>();
            core.definitions().forEach(def -> ids.add(def.id()));
            ids.addAll(core.componentDefinitions().keySet());
            return filter(ids, args[1]);
        }
        if (args.length == 2 && sub.equals("aim")) {
            return filter(List.of("north", "east", "south", "west", "up", "down"), args[1]);
        }
        if (args.length == 3 && sub.equals("give")) {
            return null;
        }
        return new ArrayList<>();
    }

    private Player asPlayer(CommandSender sender) {
        return sender instanceof Player player ? player : null;
    }

    private Optional<PlacedTnt> lookedAt(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        Block block = player.getTargetBlockExact(6);
        if (block == null || block.getType() != Material.TNT) {
            return Optional.empty();
        }
        return core.placedAt(block.getLocation());
    }

    private static Vector named(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "north" -> new Vector(0, 0, -1);
            case "south" -> new Vector(0, 0, 1);
            case "east" -> new Vector(1, 0, 0);
            case "west" -> new Vector(-1, 0, 0);
            case "up" -> new Vector(0, 1, 0);
            case "down" -> new Vector(0, -1, 0);
            default -> null;
        };
    }

    private static String describe(Vector v) {
        return String.format(Locale.ROOT, "%.2f, %.2f, %.2f", v.getX(), v.getY(), v.getZ());
    }

    private static String format(Location l) {
        return l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(lower)).sorted().collect(Collectors.toList());
    }
}
