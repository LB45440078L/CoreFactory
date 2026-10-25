package io.corefactory.paper.command;

import io.corefactory.api.PackVersion;
import io.corefactory.api.PlacedTnt;
import io.corefactory.api.TntDefinition;
import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Product;
import io.corefactory.paper.CoreFactoryPlugin;
import io.corefactory.paper.achievement.AchievementTracker;
import io.corefactory.paper.energy.ChartCommand;
import io.corefactory.paper.energy.EnergyEffects;
import io.corefactory.paper.energy.EnergyService;
import io.corefactory.paper.scanner.DepositScanner;
import io.corefactory.paper.gui.AchievementGui;
import io.corefactory.paper.pack.BundledPack;
import io.corefactory.paper.pack.PackInstaller.Result;
import io.corefactory.paper.gui.CatalogGui;
import io.corefactory.paper.gui.RecipeGui;
import io.corefactory.paper.guide.GuideBook;
import io.corefactory.paper.hologram.HologramService;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code /cf}: give TNT and components, open the catalogue, inspect and configure placed TNT, reload. The command tree in
 * {@link CfCommand} parses the arguments, checks the permissions and offers the completions; these handlers take the typed values
 * and hold the messages and the effects. Where an argument is optional, a handler takes {@code null} for the part the player did not type.
 */
public final class CoreFactoryCommand {

    /** The seconds {@code /cf pylons show} lasts when no number is typed. */
    static final int PYLON_SECONDS = 15;

    private static final double PYLON_VIEW = 96.0;

    private final CoreFactoryPlugin plugin;
    private final CoreFactoryCore core;
    private final AchievementTracker achievements;
    private final GuideBook guide;
    private final EnergyService energy;
    private final EnergyEffects effects;
    private final DepositScanner scanner;
    private final HologramService holograms;

    public CoreFactoryCommand(CoreFactoryPlugin plugin, CoreFactoryCore core, AchievementTracker achievements, GuideBook guide,
                              EnergyService energy, EnergyEffects effects, DepositScanner scanner, HologramService holograms) {
        this.plugin = plugin;
        this.core = core;
        this.achievements = achievements;
        this.guide = guide;
        this.energy = energy;
        this.effects = effects;
        this.scanner = scanner;
        this.holograms = holograms;
    }

    /** {@code /cf} and {@code /cf help}: the usage of every subcommand. */
    void help(CommandSender sender) {
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
        sender.sendMessage(Style.COMMAND.of("/cf count").append(Style.HINT.of(" | ")).append(Style.COMMAND.of("/cf reload")));
        sender.sendMessage(Style.usage("/cf geology deposits", "[radius]",
                "the deposits within radius blocks of you, nearest first"));
        sender.sendMessage(Style.usage("/cf deposits", "", "the deposit map: the nearest deposits, click one for a waypoint"));
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
        sender.sendMessage(Style.usage("/cf holograms", "[on|off]",
                "show or hide the holograms of devices for everyone (until restart)"));
        for (Component line : ChartCommand.USAGE) {
            sender.sendMessage(line);
        }
    }

    /** {@code /cf holograms}: says whether the holograms of devices are shown to everyone. */
    void hologramsStatus(CommandSender sender) {
        sender.sendMessage(Style.DESCRIPTION.of("Device holograms are ").append(hologramState(holograms.devicesVisible())));
    }

    /** {@code /cf holograms on|off}: shows or hides the holograms of devices for everyone, until the server restarts. */
    void setHolograms(CommandSender sender, boolean visible) {
        holograms.setDevicesVisible(visible);
        sender.sendMessage(Style.DESCRIPTION.of("Device holograms are now ").append(hologramState(visible)));
    }

    private static Component hologramState(boolean visible) {
        return visible ? Style.SUCCESS.of("on") : Style.HINT.of("off");
    }

    /** {@code /cf count}: the number of placed TNT. */
    void count(CommandSender sender) {
        sender.sendMessage(core.messages().get("placed-count", "count", core.placedTnts().size()));
    }

    /** {@code /cf list}: every TNT, component and ore block id. */
    void list(CommandSender sender) {
        sender.sendMessage(Style.TITLE.of("TNT: ").append(Style.ID.of(core.definitions().stream()
                .map(TntDefinition::id).collect(Collectors.joining(", ")))));
        sender.sendMessage(Style.TITLE.of("Components: ")
                .append(Style.ID.of(String.join(", ", core.componentDefinitions().keySet()))));
        sender.sendMessage(Style.TITLE.of("Ore blocks: ").append(Style.ID.of(core.ores().stream()
                .map(OreDefinition::id).collect(Collectors.joining(", ")))));
    }

    /** {@code /cf gui}: opens the catalogue for the player who typed it. */
    void gui(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            sender.sendMessage(core.messages().get("player-only"));
            return;
        }
        CatalogGui.create(core).open(player);
    }

    /**
     * {@code /cf give <id> [player] [amount]}: gives a TNT, component or ore block to the named player, or to the sender. The amount is
     * clamped to 1 to 64. A missing id gives the unknown-id message for {@code ?}.
     *
     * @param sender     the sender
     * @param id         the id typed, or {@code null} when none is typed
     * @param playerName the player's name, or {@code null} for the sender
     * @param amount     the amount typed, clamped to 1 to 64
     */
    void give(CommandSender sender, String id, String playerName, int amount) {
        if (id == null) {
            sender.sendMessage(core.messages().get("unknown-id", "id", "?"));
            return;
        }
        String key = id.toLowerCase(Locale.ROOT);
        Player target = playerName != null ? plugin.getServer().getPlayerExact(playerName) : asPlayer(sender);
        if (target == null) {
            sender.sendMessage(core.messages().get("player-offline", "player", playerName != null ? playerName : "?"));
            return;
        }
        final int count = clampAmount(amount);
        Optional<ItemStack> item = core.createTntItem(key, count)
                .or(() -> core.createComponentItem(key, count))
                .or(() -> core.createOreItem(key, count));
        if (item.isEmpty()) {
            unknownId(sender, key, "/cf give", giveIds());
            return;
        }
        target.getInventory().addItem(item.get());
        Component name = item.get().getItemMeta() != null && item.get().getItemMeta().hasDisplayName()
                ? item.get().getItemMeta().displayName() : Component.text(key);
        sender.sendMessage(core.messages().get("given", "amount", count, "name", name, "player", target.getName()));
    }

    /** The amount of {@code /cf give}: a whole number from 1 to 64. Anything below one gives one, anything above a stack gives 64. */
    static int clampAmount(int amount) {
        return Math.max(1, Math.min(64, amount));
    }

    /**
     * {@code /cf recipe <id>}: shows a recipe in the crafting grid, for the player who typed it.
     *
     * @param sender the sender, who must be a player
     * @param id     the id typed, or {@code null} when none is typed, which shows the usage
     */
    void recipe(CommandSender sender, String id) {
        Player player = asPlayer(sender);
        if (player == null) {
            sender.sendMessage(core.messages().get("player-only"));
            return;
        }
        if (id == null) {
            player.sendMessage(Style.usageError("/cf recipe", "<id>"));
            return;
        }
        String key = id.toLowerCase(Locale.ROOT);
        if (!RecipeGui.open(core, player, key)) {
            unknownId(player, key, "/cf recipe", recipeIds());
        }
    }

    /**
     * The unknown-id message, and below it one line that names the close ids and says how to list every id: the Tab key after the
     * command. The close ids are left out when none is close.
     *
     * @param sender  who typed the command
     * @param typed   the id typed, in lower case
     * @param command the command and its literal words, such as {@code /cf recipe}
     * @param ids     the ids the command accepts
     */
    private void unknownId(CommandSender sender, String typed, String command, List<String> ids) {
        sender.sendMessage(core.messages().get("unknown-id", "id", typed));
        List<String> close = IdSuggestions.closest(typed, ids);
        Component line = Component.empty();
        if (!close.isEmpty()) {
            line = line.append(Style.DESCRIPTION.of("Did you mean "));
            for (int i = 0; i < close.size(); i++) {
                line = line.append(Style.ID.of(close.get(i)));
                line = line.append(Style.DESCRIPTION.of(i < close.size() - 1 ? ", " : "? "));
            }
        }
        line = line.append(Style.HINT.of("Press Tab after ")).append(Style.COMMAND.of(command))
                .append(Style.HINT.of(" to see every id."));
        sender.sendMessage(line);
    }

    /** {@code /cf book [player]}: gives the tutorial book to the named player, or to the sender. */
    void book(CommandSender sender, String playerName) {
        Player target = playerName != null ? plugin.getServer().getPlayerExact(playerName) : asPlayer(sender);
        if (target == null) {
            sender.sendMessage(core.messages().get("player-offline", "player", playerName != null ? playerName : "?"));
            return;
        }
        for (ItemStack book : guide.books()) {
            target.getInventory().addItem(book).values()
                    .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
        }
        sender.sendMessage(Style.SUCCESS.of("Gave the CoreFactory guide to ").append(Style.ID.of(target.getName()))
                .append(Style.SUCCESS.of(".")));
    }

    /**
     * {@code /cf achievements} opens your own; {@code /cf achievements <player>} opens the named player's, and needs the give permission,
     * which the command tree checks.
     *
     * @param sender     the sender, who must be a player
     * @param playerName the player's name, or {@code null} for the sender
     */
    void achievements(CommandSender sender, String playerName) {
        Player viewer = asPlayer(sender);
        if (viewer == null) {
            sender.sendMessage(core.messages().get("player-only"));
            return;
        }
        Player subject = viewer;
        if (playerName != null) {
            subject = plugin.getServer().getPlayerExact(playerName);
            if (subject == null) {
                viewer.sendMessage(core.messages().get("player-offline", "player", playerName));
                return;
            }
        }
        AchievementGui.open(achievements, viewer, subject.getUniqueId(), subject.getName(), 0);
    }

    /**
     * {@code /cf pack} reports the bundled and installed versions. {@code /cf pack update} copies the bundled
     * pack now, under the same rule as on start: only when it is newer.
     */
    void pack(CommandSender sender, boolean update) {
        Result result = update ? BundledPack.install(plugin) : BundledPack.inspect(plugin);
        sender.sendMessage(Style.TITLE.of("Resource pack: ").append(Style.ID.of(versionOf(result.bundled())))
                .append(Style.DESCRIPTION.of(" bundled, ")).append(Style.ID.of(versionOf(result.installed())))
                .append(Style.DESCRIPTION.of(" installed")));
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

    /** {@code /cf info}: the placed TNT the player looks at. */
    void info(CommandSender sender) {
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

    /**
     * {@code /cf aim [north|east|south|west|up|down]}: sets the direction of the placed TNT the player looks at. Without a direction, or
     * with one that is not a direction, it takes the player's facing.
     *
     * @param sender    the sender, who must be a player to look at a TNT
     * @param direction the direction typed, or {@code null} when none is typed
     */
    void aim(CommandSender sender, String direction) {
        Player player = asPlayer(sender);
        Optional<PlacedTnt> placed = lookedAt(player);
        if (placed.isEmpty()) {
            sender.sendMessage(core.messages().get("not-placed"));
            return;
        }
        Vector facing = player.getLocation().getDirection().setY(0);
        Vector typed = direction == null ? null : named(direction);
        Vector aimed = typed == null ? facing : typed;
        core.aim(placed.get(), aimed);
        sender.sendMessage(core.messages().get("aimed", "direction", describe(aimed)));
    }

    /**
     * {@code /cf target}: sets the destination of the placed TNT you look at to the block you stand on.
     */
    void target(CommandSender sender) {
        Player player = asPlayer(sender);
        Optional<PlacedTnt> placed = lookedAt(player);
        if (placed.isEmpty()) {
            sender.sendMessage(core.messages().get("not-placed"));
            return;
        }
        setTarget(sender, placed.get(), player.getLocation().getBlock().getLocation());
    }

    /**
     * {@code /cf target <x> <y> <z>}: sets the destination of the placed TNT you look at to the named block in your world.
     */
    void target(CommandSender sender, int x, int y, int z) {
        Player player = asPlayer(sender);
        Optional<PlacedTnt> placed = lookedAt(player);
        if (placed.isEmpty()) {
            sender.sendMessage(core.messages().get("not-placed"));
            return;
        }
        setTarget(sender, placed.get(), new Location(player.getWorld(), x, y, z));
    }

    private void setTarget(CommandSender sender, PlacedTnt placed, Location at) {
        placed.setTarget(at);
        sender.sendMessage(core.messages().get("target-set", "x", at.getBlockX(), "y", at.getBlockY(), "z", at.getBlockZ()));
    }

    /** {@code /cf unlink}: removes the fuse links of the placed TNT the player looks at. */
    void unlink(CommandSender sender) {
        Player player = asPlayer(sender);
        Optional<PlacedTnt> placed = lookedAt(player);
        if (placed.isEmpty()) {
            sender.sendMessage(core.messages().get("not-placed"));
            return;
        }
        core.unlinkAll(placed.get());
        sender.sendMessage(core.messages().get("link-cleared"));
    }

    /** {@code /cf reload}: reloads the configuration. */
    void reload(CommandSender sender) {
        // Placed TNT stays in memory: reloading configuration never re-reads the placed-TNT file.
        plugin.reloadConfiguration();
        sender.sendMessage(core.messages().get("reloaded", "tnts", core.definitions().size(),
                "components", core.componentDefinitions().size()));
    }

    /** {@code /cf deposits} opens the deposit map for the player who typed it. */
    void deposits(CommandSender sender) {
        Player player = asPlayer(sender);
        if (player == null) {
            sender.sendMessage(core.messages().get("player-only"));
            return;
        }
        scanner.openMap(player);
    }

    /**
     * {@code /cf pylons show [seconds]} draws the outlines of the grids near the player, in their colours, for the given seconds. The
     * seconds are clamped to 1 to 120.
     *
     * @param sender  the sender, who must be a player
     * @param seconds the seconds typed, or {@code null} when {@code show} is missing, which shows the usage
     */
    void pylons(CommandSender sender, Integer seconds) {
        Player player = asPlayer(sender);
        if (player == null) {
            sender.sendMessage(core.messages().get("player-only"));
            return;
        }
        if (seconds == null) {
            player.sendMessage(Style.usageError("/cf pylons show", "[seconds]"));
            return;
        }
        int shown = Math.max(1, Math.min(120, seconds));
        effects.showOutlines(player, shown, () -> energy.outlinesNear(player.getLocation(), PYLON_VIEW));
        player.sendMessage(Style.SUCCESS.of("Showing the grid outlines near you for ").append(Style.ID.of(shown + " s"))
                .append(Style.SUCCESS.of(".")));
    }

    /** The ids {@code /cf give} takes, for completion: every TNT type, component and ore block. */
    List<String> giveIds() {
        return Completion.giveIds(core.definitions().stream().map(TntDefinition::id).toList(),
                core.componentDefinitions().keySet(), core.ores().stream().map(OreDefinition::id).toList());
    }

    /** The ids {@code /cf recipe} can show, for completion: the crafted ones and the ones a process makes. */
    List<String> recipeIds() {
        Set<String> tntIds = core.definitions().stream().map(TntDefinition::id).collect(Collectors.toSet());
        List<ItemRef> outputs = core.processes().processes().stream()
                .flatMap(process -> process.outputs().stream())
                .map(Product::ref)
                .toList();
        return Completion.recipeIds(tntIds, core.tntRecipes(), core.componentDefinitions(), outputs);
    }

    /** The names of the players online, for completion of the player argument of {@code /cf give}. */
    List<String> onlinePlayerNames() {
        return plugin.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
    }

    /** The names of the grid charts, for completion of {@code /cf grid remove} and {@code /cf grid mode}. */
    List<String> chartNames() {
        return energy.charts().stream().map(chart -> chart.name()).toList();
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
