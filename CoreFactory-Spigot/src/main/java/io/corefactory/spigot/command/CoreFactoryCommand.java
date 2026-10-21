package io.corefactory.spigot.command;

import io.corefactory.api.PackVersion;
import io.corefactory.api.PlacedTnt;
import io.corefactory.api.TntDefinition;
import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.research.ResearchTree;
import io.corefactory.spigot.CoreFactoryPlugin;
import io.corefactory.spigot.achievement.AchievementTracker;
import io.corefactory.spigot.energy.ChartCommand;
import io.corefactory.spigot.energy.EnergyEffects;
import io.corefactory.spigot.energy.EnergyService;
import io.corefactory.spigot.friends.FriendsCommand;
import io.corefactory.spigot.geology.GeologyCommand;
import io.corefactory.spigot.gui.AchievementGui;
import io.corefactory.spigot.research.ResearchCommand;
import io.corefactory.spigot.pack.BundledPack;
import io.corefactory.spigot.pack.PackInstaller.Result;
import io.corefactory.spigot.gui.CatalogGui;
import io.corefactory.spigot.gui.RecipeGui;
import io.corefactory.spigot.guide.GuideBook;
import io.corefactory.spigot.turret.TurretCommand;
import io.corefactory.spigot.turret.TurretIds;
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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * {@code /cf}: give TNT and components, open the catalogue, inspect and configure placed TNT, reload.
 */
public final class CoreFactoryCommand implements CommandExecutor, TabCompleter {

    private static final double PYLON_VIEW = 96.0;

    private static final String PERMISSION_GIVE = "corefactory.give";
    private static final String PERMISSION_RECIPES = "corefactory.recipes";
    private static final String PERMISSION_CONFIGURE = "corefactory.configure";
    private static final String PERMISSION_RELOAD = "corefactory.reload";
    private static final String PERMISSION_PYLONS = "corefactory.pylons";
    private static final String PERMISSION_GRID = "corefactory.grid";
    private static final String PERMISSION_GUIDE = "corefactory.guide";
    private static final String PERMISSION_ACHIEVEMENTS = "corefactory.achievements";

    /** The subcommands tab completion offers, each with the permission it needs; the rest need none. */
    private static final List<Completion.Candidate> SUBCOMMANDS = List.of(
            new Completion.Candidate("help", null), new Completion.Candidate("give", PERMISSION_GIVE),
            new Completion.Candidate("list", null), new Completion.Candidate("gui", PERMISSION_GUIDE),
            new Completion.Candidate("recipe", PERMISSION_RECIPES), new Completion.Candidate("book", PERMISSION_GUIDE),
            new Completion.Candidate("achievements", PERMISSION_ACHIEVEMENTS),
            new Completion.Candidate("pack", PERMISSION_RELOAD), new Completion.Candidate("info", null),
            new Completion.Candidate("aim", PERMISSION_CONFIGURE), new Completion.Candidate("target", PERMISSION_CONFIGURE),
            new Completion.Candidate("unlink", PERMISSION_CONFIGURE), new Completion.Candidate("count", null),
            new Completion.Candidate("reload", PERMISSION_RELOAD),
            new Completion.Candidate("geology", GeologyCommand.PERMISSION),
            new Completion.Candidate("research", ResearchCommand.PERMISSION),
            new Completion.Candidate("pylons", PERMISSION_PYLONS), new Completion.Candidate("grid", null),
            new Completion.Candidate("friends", FriendsCommand.PERMISSION),
            new Completion.Candidate("turret", TurretIds.PERMISSION));

    private final CoreFactoryPlugin plugin;
    private final CoreFactoryCore core;
    private final AchievementTracker achievements;
    private final GeologyCommand geology;
    private final ResearchCommand research;
    private final GuideBook guide;
    private final EnergyService energy;
    private final EnergyEffects effects;
    private final ChartCommand charts;
    private final FriendsCommand friends;
    private final TurretCommand turret;

    public CoreFactoryCommand(CoreFactoryPlugin plugin, CoreFactoryCore core, AchievementTracker achievements,
                          GeologyCommand geology, ResearchCommand research, GuideBook guide, EnergyService energy,
                          EnergyEffects effects, ChartCommand charts, FriendsCommand friends, TurretCommand turret) {
        this.friends = friends;
        this.turret = turret;
        this.plugin = plugin;
        this.core = core;
        this.achievements = achievements;
        this.geology = geology;
        this.research = research;
        this.guide = guide;
        this.energy = energy;
        this.effects = effects;
        this.charts = charts;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "give" -> give(sender, args);
            case "list" -> list(sender);
            case "gui" -> gui(sender);
            case "recipe" -> recipe(sender, args);
            case "book" -> book(sender, args);
            case "achievements" -> achievements(sender, args);
            case "pack" -> pack(sender, args);
            case "info" -> info(sender);
            case "aim" -> aim(sender, args);
            case "target" -> target(sender, args);
            case "unlink" -> unlink(sender);
            case "count" -> sender.sendMessage(core.messages().get("placed-count", "count", core.placedTnts().size()));
            case "reload" -> reload(sender);
            case "geology" -> geology.run(sender, args);
            case "research" -> research.run(sender, args);
            case "pylons" -> pylons(sender, args);
            case "grid" -> charts.run(sender, args);
            case "friends" -> friends.run(sender, args);
            case "turret" -> turret.run(sender, args);
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender sender) {
        sender.sendMessage(Style.TITLE.of("CoreFactory commands:"));
        sender.sendMessage(Style.usage("/cf give", "<id> [player] [amount]", "give a TNT, component or ore block"));
        sender.sendMessage(Style.usage("/cf list", "", "every id"));
        sender.sendMessage(Style.usage("/cf gui", "", "open the catalogue (right-click an entry for its recipe)"));
        sender.sendMessage(Style.usage("/cf recipe", "<id>", "show a recipe in the crafting grid"));
        sender.sendMessage(Style.usage("/cf book", "[player]", "give the tutorial book"));
        sender.sendMessage(Style.usage("/cf achievements", "[player]", "open the achievements"));
        sender.sendMessage(Style.usage("/cf info", "", "inspect the placed TNT you look at"));
        sender.sendMessage(Style.usage("/cf aim", "[north|east|south|west|up|down]",
                "set its direction (default: your facing)"));
        sender.sendMessage(Style.usage("/cf target", "[x y z]",
                "set a teleport TNT's destination (default: the block you stand on)"));
        sender.sendMessage(Style.usage("/cf unlink", "", "remove the fuse links of the TNT you look at"));
        sender.sendMessage(Style.usage("/cf pack", "[update]",
                "show the resource pack version, or install a newer one now"));
        sender.sendMessage(Style.COMMAND.of("/cf count") + Style.HINT.of(" | ") + Style.COMMAND.of("/cf reload"));
        sender.sendMessage(Style.usage("/cf geology status", "", "marker state of the loaded chunks"));
        sender.sendMessage(Style.usage("/cf geology retrofit", "<radius> [force]",
                "lay ores into existing chunks"));
        sender.sendMessage(Style.usage("/cf research max", "[player]", "finish every node (admins)"));
        sender.sendMessage(Style.usage("/cf friends", "add|remove <player> | list",
                "your friends: a turret never fires at them while friendly fire is off"));
        sender.sendMessage(Style.usage("/cf turret info", "", "level, range, targets and stances of the turret you look at (owner only)"));
        sender.sendMessage(Style.usage("/cf turret target", "<mob|category> <on|off>",
                "fire at a mob or a category (monsters, phantoms, slimes, ghasts), or spare it"));
        sender.sendMessage(Style.usage("/cf turret player", "<name> <target|spare|clear>",
                "target one player even with friendly fire off, spare them, or clear the stance"));
        sender.sendMessage(Style.usage("/cf turret team", "<spare|target|ignore>",
                "your scoreboard team: spared as friends, targeted, or not counted"));
        sender.sendMessage(Style.usage("/cf turret friendlyfire", "<on|off>",
                "friendly fire of the turret you look at (owner only)"));
        sender.sendMessage(Style.usage("/cf research grant", "<node> [player]",
                "finish a node and the nodes it needs (admins)"));
        sender.sendMessage(Style.usage("/cf research nodes", "", "list the node ids (admins)"));
        sender.sendMessage(Style.usage("/cf pylons show", "[seconds]",
                "draw the outline of the grids within 96 blocks (default 15 s)"));
        for (String line : ChartCommand.USAGE) {
            sender.sendMessage(line);
        }
    }

    /** {@code /cf pylons show [seconds]} draws the outlines of the grids near the player, in their colours, for the given seconds. */
    private void pylons(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            sender.sendMessage(core.messages().get("player-only"));
            return;
        }
        if (!player.hasPermission("corefactory.pylons")) {
            player.sendMessage(core.messages().get("no-permission"));
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("show")) {
            player.sendMessage(Style.usageError("/cf pylons show", "[seconds]"));
            return;
        }
        int seconds = args.length >= 3 ? secondsArgument(args[2]) : 15;
        effects.showOutlines(player, seconds, () -> energy.outlinesNear(player.getLocation(), PYLON_VIEW));
        player.sendMessage(Style.SUCCESS.of("Showing the grid outlines near you for ") + Style.ID.of(seconds + " s")
                + Style.SUCCESS.of("."));
    }

    /** The seconds of {@code /cf pylons show}: a whole number from 1 to 120. Anything else gives 15. */
    static int secondsArgument(String arg) {
        try {
            return Math.max(1, Math.min(120, Integer.parseInt(arg)));
        } catch (NumberFormatException e) {
            return 15;
        }
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("corefactory.give")) {
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
        final int amount = args.length >= 4 ? amountArgument(args[3]) : 1;
        Optional<ItemStack> item = core.createTntItem(id, amount)
                .or(() -> core.createComponentItem(id, amount))
                .or(() -> core.createOreItem(id, amount));
        if (item.isEmpty()) {
            sender.sendMessage(core.messages().get("unknown-id", "id", id));
            return;
        }
        target.getInventory().addItem(item.get());
        String name = item.get().getItemMeta() != null && item.get().getItemMeta().hasDisplayName()
                ? item.get().getItemMeta().getDisplayName() : id;
        sender.sendMessage(core.messages().get("given", "amount", amount, "name", name, "player", target.getName()));
    }

    /** The amount of {@code /cf give}: a whole number from 1 to 64. Anything else gives one. */
    static int amountArgument(String arg) {
        try {
            return Math.max(1, Math.min(64, Integer.parseInt(arg)));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void list(CommandSender sender) {
        sender.sendMessage(Style.TITLE.of("TNT: ") + Style.ID.of(core.definitions().stream()
                .map(TntDefinition::id).collect(Collectors.joining(", "))));
        sender.sendMessage(Style.TITLE.of("Components: ")
                + Style.ID.of(String.join(", ", core.componentDefinitions().keySet())));
        sender.sendMessage(Style.TITLE.of("Ore blocks: ") + Style.ID.of(core.ores().stream()
                .map(OreDefinition::id).collect(Collectors.joining(", "))));
    }

    private void gui(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            sender.sendMessage(core.messages().get("player-only"));
            return;
        }
        if (!player.hasPermission("corefactory.guide")) {
            player.sendMessage(core.messages().get("no-permission"));
            return;
        }
        CatalogGui.create(core).open(player);
    }

    private void recipe(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player == null) {
            sender.sendMessage(core.messages().get("player-only"));
            return;
        }
        if (!player.hasPermission("corefactory.recipes")) {
            player.sendMessage(core.messages().get("no-permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Style.usageError("/cf recipe", "<id>"));
            return;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (!RecipeGui.open(core, player, id)) {
            player.sendMessage(core.messages().get("unknown-id", "id", id));
        }
    }

    private void book(CommandSender sender, String[] args) {
        if (!sender.hasPermission("corefactory.guide")) {
            sender.sendMessage(core.messages().get("no-permission"));
            return;
        }
        Player target = args.length >= 2 ? plugin.getServer().getPlayerExact(args[1]) : asPlayer(sender);
        if (target == null) {
            sender.sendMessage(core.messages().get("player-offline", "player", args.length >= 2 ? args[1] : "?"));
            return;
        }
        for (ItemStack book : guide.books()) {
            target.getInventory().addItem(book).values()
                    .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
        }
        sender.sendMessage(Style.SUCCESS.of("Gave the CoreFactory guide to ") + Style.ID.of(target.getName())
                + Style.SUCCESS.of("."));
    }

    /** {@code /cf achievements} opens your own; {@code /cf achievements <player>} needs the give permission. */
    private void achievements(CommandSender sender, String[] args) {
        Player viewer = asPlayer(sender);
        if (viewer == null) {
            sender.sendMessage(core.messages().get("player-only"));
            return;
        }
        if (!viewer.hasPermission("corefactory.achievements")) {
            viewer.sendMessage(core.messages().get("no-permission"));
            return;
        }
        Player subject = viewer;
        if (args.length >= 2) {
            if (!viewer.hasPermission("corefactory.give")) {
                viewer.sendMessage(core.messages().get("no-permission"));
                return;
            }
            subject = plugin.getServer().getPlayerExact(args[1]);
            if (subject == null) {
                viewer.sendMessage(core.messages().get("player-offline", "player", args[1]));
                return;
            }
        }
        AchievementGui.open(achievements, viewer, subject.getUniqueId(), subject.getName(), 0);
    }

    /**
     * {@code /cf pack} reports the bundled and installed versions. {@code /cf pack update} copies the bundled
     * pack now, under the same rule as on start: only when it is newer.
     */
    private void pack(CommandSender sender, String[] args) {
        if (!sender.hasPermission("corefactory.reload")) {
            sender.sendMessage(core.messages().get("no-permission"));
            return;
        }
        boolean update = args.length >= 2 && args[1].equalsIgnoreCase("update");
        Result result = update ? BundledPack.install(plugin) : BundledPack.inspect(plugin);
        sender.sendMessage(Style.TITLE.of("Resource pack: ") + Style.ID.of(versionOf(result.bundled()))
                + Style.DESCRIPTION.of(" bundled, ") + Style.ID.of(versionOf(result.installed()))
                + Style.DESCRIPTION.of(" installed"));
        sender.sendMessage(Style.DESCRIPTION.of(update ? result.detail() : status(result)));
    }

    private static String versionOf(Optional<PackVersion> version) {
        return version.map(PackVersion::toString).orElse("none");
    }

    private static String status(Result result) {
        return switch (result.action()) {
            case INSTALLED -> "Not installed yet. It is copied on the next start, or run /cf pack update.";
            case UPDATED -> "Outdated. It is replaced on the next start, or run /cf pack update.";
            case UP_TO_DATE -> "Up to date.";
            case KEPT_NEWER -> "The installed pack is newer than this jar, so it is kept.";
            case NO_BUNDLE, FAILED -> result.detail();
        };
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
        if (!player.hasPermission("corefactory.configure")) {
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
     * stand on; {@code /cf target <x> <y> <z>} names another block in your world.
     */
    private void target(CommandSender sender, String[] args) {
        Player player = asPlayer(sender);
        if (player != null && !player.hasPermission("corefactory.configure")) {
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
                sender.sendMessage(Style.usageError("/cf target", "[x y z]"));
                return;
            }
        }
        placed.get().setTarget(at);
        sender.sendMessage(core.messages().get("target-set", "x", at.getBlockX(), "y", at.getBlockY(), "z", at.getBlockZ()));
    }

    private void unlink(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player != null && !player.hasPermission("corefactory.configure")) {
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
        if (!sender.hasPermission("corefactory.reload")) {
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
        Predicate<String> has = sender::hasPermission;
        if (args.length == 1) {
            return Completion.complete(SUBCOMMANDS, args[0], has);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("give") && args.length == 3) {
            return has.test(PERMISSION_GIVE) ? null : List.of();
        }
        return Completion.complete(optionsFor(sub, args), args[args.length - 1], has);
    }

    /**
     * The suggestions for the argument being typed, which is the last of {@code args} and follows the subcommand
     * {@code sub}. Every id offered is one the subcommand takes.
     */
    private List<Completion.Candidate> optionsFor(String sub, String[] args) {
        int index = args.length - 1;
        String first = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        return switch (sub) {
            case "give" -> index == 1 ? Completion.all(giveIds(), PERMISSION_GIVE) : List.of();
            case "recipe" -> index == 1 ? Completion.all(recipeIds(), PERMISSION_RECIPES) : List.of();
            case "aim" -> index == 1
                    ? Completion.all(List.of("north", "east", "south", "west", "up", "down"), PERMISSION_CONFIGURE)
                    : List.of();
            case "pack" -> index == 1 ? Completion.all(List.of("update"), PERMISSION_RELOAD) : List.of();
            case "pylons" -> index == 1 ? Completion.all(List.of("show"), PERMISSION_PYLONS) : List.of();
            case "turret" -> turret.options(args);
            case "geology" -> {
                if (index == 1) {
                    yield Completion.all(List.of("status", "retrofit"), GeologyCommand.PERMISSION);
                }
                yield index == 3 && first.equals("retrofit")
                        ? Completion.all(List.of("force"), GeologyCommand.PERMISSION) : List.of();
            }
            case "research" -> {
                if (index == 1) {
                    yield Completion.all(List.of("max", "grant", "nodes"), ResearchCommand.PERMISSION);
                }
                yield index == 2 && first.equals("grant")
                        ? Completion.all(nodeIds(), ResearchCommand.PERMISSION) : List.of();
            }
            case "grid" -> {
                if (index == 1) {
                    yield List.of(new Completion.Candidate("create", PERMISSION_GRID),
                            new Completion.Candidate("remove", PERMISSION_GRID),
                            new Completion.Candidate("list", PERMISSION_PYLONS),
                            new Completion.Candidate("mode", PERMISSION_GRID),
                            new Completion.Candidate("info", PERMISSION_PYLONS));
                }
                if (index == 2 && (first.equals("remove") || first.equals("mode"))) {
                    yield Completion.all(energy.charts().stream().map(chart -> chart.name()).toList(), PERMISSION_GRID);
                }
                yield index == 3 && first.equals("mode")
                        ? Completion.all(List.of("satisfaction", "production", "storage"), PERMISSION_GRID)
                        : List.of();
            }
            default -> List.of();
        };
    }

    private List<String> giveIds() {
        return Completion.giveIds(core.definitions().stream().map(TntDefinition::id).toList(),
                core.componentDefinitions().keySet(), core.ores().stream().map(OreDefinition::id).toList());
    }

    private List<String> recipeIds() {
        Set<String> tntIds = core.definitions().stream().map(TntDefinition::id).collect(Collectors.toSet());
        return Completion.recipeIds(tntIds, core.tntRecipes(), core.componentDefinitions());
    }

    private List<String> nodeIds() {
        return core.research().tree().nodes().stream().map(ResearchTree.Node::id).toList();
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

}
