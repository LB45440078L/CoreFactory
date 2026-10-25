package io.corefactory.paper.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.corefactory.paper.energy.ChartCommand;
import io.corefactory.paper.friends.FriendsCommand;
import io.corefactory.paper.geology.GeologyCommand;
import io.corefactory.paper.research.ResearchCommand;
import io.corefactory.paper.turret.TurretCommand;
import io.corefactory.paper.turret.TurretIds;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The tree of {@code /cf}, with every subcommand, its typed arguments, the permission it needs and the completion of its arguments.
 * The tree only reads the typed values and checks the permissions; each line then runs a handler, which holds the messages and the
 * effects. A subcommand the sender lacks the permission for is left out of the tree, so it is neither run nor completed.
 */
public final class CfCommand {

    private static final String PERMISSION_GIVE = "corefactory.give";
    private static final String PERMISSION_RECIPES = "corefactory.recipes";
    private static final String PERMISSION_CONFIGURE = "corefactory.configure";
    private static final String PERMISSION_RELOAD = "corefactory.reload";
    private static final String PERMISSION_PYLONS = "corefactory.pylons";
    private static final String PERMISSION_GRID = "corefactory.grid";
    private static final String PERMISSION_GUIDE = "corefactory.guide";
    private static final String PERMISSION_ACHIEVEMENTS = "corefactory.achievements";

    private static final List<String> DIRECTIONS = List.of("north", "east", "south", "west", "up", "down");
    private static final List<String> CHART_MODES = List.of("satisfaction", "production", "storage");
    private static final List<String> ON_OFF = List.of("on", "off");
    private static final List<String> STANCES = List.of("target", "spare", "clear");
    private static final List<String> TEAM_MODES = List.of("spare", "target", "ignore");

    private final CoreFactoryCommand handlers;
    private final GeologyCommand geologyCommand;
    private final ResearchCommand researchCommand;
    private final ChartCommand chartCommand;
    private final FriendsCommand friendsCommand;
    private final TurretCommand turretCommand;

    /**
     * @param handlers        the handlers of the core subcommands
     * @param geologyCommand  the geology subcommands
     * @param researchCommand the research subcommands
     * @param chartCommand    the grid subcommands
     * @param friendsCommand  the friend-list subcommands
     * @param turretCommand   the turret subcommands
     */
    public CfCommand(CoreFactoryCommand handlers, GeologyCommand geologyCommand, ResearchCommand researchCommand,
                     ChartCommand chartCommand, FriendsCommand friendsCommand, TurretCommand turretCommand) {
        this.handlers = handlers;
        this.geologyCommand = geologyCommand;
        this.researchCommand = researchCommand;
        this.chartCommand = chartCommand;
        this.friendsCommand = friendsCommand;
        this.turretCommand = turretCommand;
    }

    /**
     * The tree of {@code /cf}, to register under the name {@code cf}. Without a subcommand it shows the help.
     *
     * @return the root of the tree
     */
    public LiteralCommandNode<CommandSourceStack> node() {
        return Commands.literal("cf")
                .executes(run(context -> handlers.help(sender(context))))
                .then(help())
                .then(give())
                .then(Commands.literal("list").executes(run(context -> handlers.list(sender(context)))))
                .then(gui())
                .then(recipe())
                .then(book())
                .then(achievements())
                .then(pack())
                .then(Commands.literal("info").executes(run(context -> handlers.info(sender(context)))))
                .then(aim())
                .then(target())
                .then(Commands.literal("unlink").requires(permission(PERMISSION_CONFIGURE))
                        .executes(run(context -> handlers.unlink(sender(context)))))
                .then(Commands.literal("count").executes(run(context -> handlers.count(sender(context)))))
                .then(Commands.literal("reload").requires(permission(PERMISSION_RELOAD))
                        .executes(run(context -> handlers.reload(sender(context)))))
                .then(geology())
                .then(Commands.literal("deposits").requires(permission(GeologyCommand.PERMISSION))
                        .executes(run(context -> handlers.deposits(sender(context)))))
                .then(research())
                .then(pylons())
                .then(grid())
                .then(friends())
                .then(turret())
                .then(holograms())
                .build();
    }

    /** {@code /cf help}: the usage of every subcommand. */
    private LiteralArgumentBuilder<CommandSourceStack> help() {
        return Commands.literal("help").executes(run(context -> handlers.help(sender(context))));
    }

    /** {@code /cf give <id> [player] [amount]}: gives a TNT, component or ore block. */
    private LiteralArgumentBuilder<CommandSourceStack> give() {
        return Commands.literal("give").requires(permission(PERMISSION_GIVE))
                .executes(run(context -> handlers.give(sender(context), null, null, 1)))
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(suggest(PERMISSION_GIVE, handlers::giveIds))
                        .executes(run(context -> handlers.give(sender(context), text(context, "id"), null, 1)))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(suggest(PERMISSION_GIVE, handlers::onlinePlayerNames))
                                .executes(run(context -> handlers.give(sender(context), text(context, "id"),
                                        text(context, "player"), 1)))
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(run(context -> handlers.give(sender(context), text(context, "id"),
                                                text(context, "player"), number(context, "amount")))))));
    }

    /** {@code /cf gui}: opens the catalogue. */
    private LiteralArgumentBuilder<CommandSourceStack> gui() {
        return Commands.literal("gui").requires(permission(PERMISSION_GUIDE))
                .executes(run(context -> handlers.gui(sender(context))));
    }

    /** {@code /cf recipe <id>}: shows a recipe in the crafting grid. */
    private LiteralArgumentBuilder<CommandSourceStack> recipe() {
        return Commands.literal("recipe").requires(permission(PERMISSION_RECIPES))
                .executes(run(context -> handlers.recipe(sender(context), null)))
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(suggest(PERMISSION_RECIPES, handlers::recipeIds))
                        .executes(run(context -> handlers.recipe(sender(context), text(context, "id")))));
    }

    /** {@code /cf book [player]}: gives the tutorial book. */
    private LiteralArgumentBuilder<CommandSourceStack> book() {
        return Commands.literal("book").requires(permission(PERMISSION_GUIDE))
                .executes(run(context -> handlers.book(sender(context), null)))
                .then(Commands.argument("player", StringArgumentType.word())
                        .executes(run(context -> handlers.book(sender(context), text(context, "player")))));
    }

    /** {@code /cf achievements [player]}: opens your own achievements; {@code /cf achievements <player>} also needs the give permission. */
    private LiteralArgumentBuilder<CommandSourceStack> achievements() {
        return Commands.literal("achievements").requires(permission(PERMISSION_ACHIEVEMENTS))
                .executes(run(context -> handlers.achievements(sender(context), null)))
                .then(Commands.argument("player", StringArgumentType.word()).requires(permission(PERMISSION_GIVE))
                        .executes(run(context -> handlers.achievements(sender(context), text(context, "player")))));
    }

    /** {@code /cf pack [update]}: shows the resource pack version, or installs a newer one now. */
    private LiteralArgumentBuilder<CommandSourceStack> pack() {
        return Commands.literal("pack").requires(permission(PERMISSION_RELOAD))
                .executes(run(context -> handlers.pack(sender(context), false)))
                .then(Commands.literal("update").executes(run(context -> handlers.pack(sender(context), true))));
    }

    /** {@code /cf aim [direction]}: sets the direction of the placed TNT you look at. */
    private LiteralArgumentBuilder<CommandSourceStack> aim() {
        return Commands.literal("aim").requires(permission(PERMISSION_CONFIGURE))
                .executes(run(context -> handlers.aim(sender(context), null)))
                .then(Commands.argument("direction", StringArgumentType.word())
                        .suggests(suggest(PERMISSION_CONFIGURE, () -> DIRECTIONS))
                        .executes(run(context -> handlers.aim(sender(context), text(context, "direction")))));
    }

    /** {@code /cf target [x y z]}: sets the destination of the teleport TNT you look at. */
    private LiteralArgumentBuilder<CommandSourceStack> target() {
        return Commands.literal("target").requires(permission(PERMISSION_CONFIGURE))
                .executes(run(context -> handlers.target(sender(context))))
                .then(Commands.argument("x", IntegerArgumentType.integer())
                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(run(context -> handlers.target(sender(context), number(context, "x"),
                                                number(context, "y"), number(context, "z")))))));
    }

    /** {@code /cf geology deposits [radius]}: the deposits within the radius, nearest first. */
    private LiteralArgumentBuilder<CommandSourceStack> geology() {
        return Commands.literal("geology").requires(permission(GeologyCommand.PERMISSION))
                .executes(run(context -> geologyCommand.usage(sender(context))))
                .then(Commands.literal("deposits")
                        .executes(run(context -> geologyCommand.deposits(sender(context), GeologyCommand.DEFAULT_RADIUS)))
                        .then(Commands.argument("radius", IntegerArgumentType.integer())
                                .executes(run(context -> geologyCommand.deposits(sender(context), number(context, "radius"))))));
    }

    /** {@code /cf research max|grant|nodes}: for administrators. */
    private LiteralArgumentBuilder<CommandSourceStack> research() {
        return Commands.literal("research").requires(permission(ResearchCommand.PERMISSION))
                .executes(run(context -> researchCommand.usage(sender(context))))
                .then(Commands.literal("max")
                        .executes(run(context -> researchCommand.max(sender(context), null)))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(run(context -> researchCommand.max(sender(context), text(context, "player"))))))
                .then(Commands.literal("grant")
                        .executes(run(context -> researchCommand.usage(sender(context))))
                        .then(Commands.argument("node", StringArgumentType.word())
                                .suggests(suggest(ResearchCommand.PERMISSION, researchCommand::nodeIds))
                                .executes(run(context -> researchCommand.grant(sender(context), text(context, "node"), null)))
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(run(context -> researchCommand.grant(sender(context), text(context, "node"),
                                                text(context, "player")))))))
                .then(Commands.literal("nodes").executes(run(context -> researchCommand.nodes(sender(context)))));
    }

    /** {@code /cf pylons show [seconds]}: draws the outlines of the grids near you. */
    private LiteralArgumentBuilder<CommandSourceStack> pylons() {
        return Commands.literal("pylons").requires(permission(PERMISSION_PYLONS))
                .executes(run(context -> handlers.pylons(sender(context), null)))
                .then(Commands.literal("show")
                        .executes(run(context -> handlers.pylons(sender(context), CoreFactoryCommand.PYLON_SECONDS)))
                        .then(Commands.argument("seconds", IntegerArgumentType.integer())
                                .executes(run(context -> handlers.pylons(sender(context), number(context, "seconds"))))));
    }

    /** {@code /cf grid create|remove|list|mode|info}: the grid charts. */
    private LiteralArgumentBuilder<CommandSourceStack> grid() {
        return Commands.literal("grid")
                .executes(run(context -> chartCommand.usage(sender(context))))
                .then(Commands.literal("create").requires(permission(PERMISSION_GRID))
                        .executes(run(context -> chartCommand.create(sender(context), null)))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(run(context -> chartCommand.create(sender(context), text(context, "name"))))))
                .then(Commands.literal("remove").requires(permission(PERMISSION_GRID))
                        .executes(run(context -> chartCommand.remove(sender(context), null)))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(suggest(PERMISSION_GRID, handlers::chartNames))
                                .executes(run(context -> chartCommand.remove(sender(context), text(context, "name"))))))
                .then(Commands.literal("list").requires(permission(PERMISSION_PYLONS))
                        .executes(run(context -> chartCommand.list(sender(context)))))
                .then(Commands.literal("mode").requires(permission(PERMISSION_GRID))
                        .executes(run(context -> chartCommand.mode(sender(context), null, null)))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(suggest(PERMISSION_GRID, handlers::chartNames))
                                .executes(run(context -> chartCommand.mode(sender(context), text(context, "name"), null)))
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .suggests(suggest(PERMISSION_GRID, () -> CHART_MODES))
                                        .executes(run(context -> chartCommand.mode(sender(context), text(context, "name"),
                                                text(context, "mode")))))))
                .then(Commands.literal("info").requires(permission(PERMISSION_PYLONS))
                        .executes(run(context -> chartCommand.info(sender(context)))));
    }

    /** {@code /cf friends [add|remove <player>|list]}: your friend list. */
    private LiteralArgumentBuilder<CommandSourceStack> friends() {
        return Commands.literal("friends").requires(permission(FriendsCommand.PERMISSION))
                .executes(run(context -> friendsCommand.list(sender(context))))
                .then(Commands.literal("add")
                        .executes(run(context -> friendsCommand.add(sender(context), null)))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(run(context -> friendsCommand.add(sender(context), text(context, "player"))))))
                .then(Commands.literal("remove")
                        .executes(run(context -> friendsCommand.remove(sender(context), null)))
                        .then(Commands.argument("player", StringArgumentType.word())
                                .executes(run(context -> friendsCommand.remove(sender(context), text(context, "player"))))))
                .then(Commands.literal("list").executes(run(context -> friendsCommand.list(sender(context)))));
    }

    /** {@code /cf turret info|target|player|team|friendlyfire}: the turret you look at, or the nearest one you own. */
    private LiteralArgumentBuilder<CommandSourceStack> turret() {
        return Commands.literal("turret").requires(permission(TurretIds.PERMISSION))
                .executes(run(context -> turretCommand.usage(sender(context))))
                .then(Commands.literal("info").executes(run(context -> turretCommand.info(sender(context)))))
                .then(Commands.literal("target")
                        .executes(run(context -> turretCommand.target(sender(context), null, null)))
                        .then(Commands.argument("mob", StringArgumentType.word())
                                .suggests(suggest(TurretIds.PERMISSION, TurretCommand::targetNames))
                                .executes(run(context -> turretCommand.target(sender(context), text(context, "mob"), null)))
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .suggests(suggest(TurretIds.PERMISSION, () -> ON_OFF))
                                        .executes(run(context -> turretCommand.target(sender(context), text(context, "mob"),
                                                text(context, "value"))))))
                )
                .then(Commands.literal("player")
                        .executes(run(context -> turretCommand.player(sender(context), null, null)))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(suggest(TurretIds.PERMISSION, TurretCommand::onlineNames))
                                .executes(run(context -> turretCommand.player(sender(context), text(context, "name"), null)))
                                .then(Commands.argument("stance", StringArgumentType.word())
                                        .suggests(suggest(TurretIds.PERMISSION, () -> STANCES))
                                        .executes(run(context -> turretCommand.player(sender(context), text(context, "name"),
                                                text(context, "stance"))))))
                )
                .then(Commands.literal("team")
                        .executes(run(context -> turretCommand.team(sender(context), null)))
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests(suggest(TurretIds.PERMISSION, () -> TEAM_MODES))
                                .executes(run(context -> turretCommand.team(sender(context), text(context, "mode"))))))
                .then(Commands.literal(TurretCommand.FRIENDLY_FIRE)
                        .executes(run(context -> turretCommand.friendlyFire(sender(context), null)))
                        .then(Commands.argument("value", StringArgumentType.word())
                                .suggests(suggest(TurretIds.PERMISSION, () -> ON_OFF))
                                .executes(run(context -> turretCommand.friendlyFire(sender(context), text(context, "value"))))));
    }

    /** {@code /cf holograms [on|off]}: shows the state of the device holograms, or shows or hides them for everyone. */
    private LiteralArgumentBuilder<CommandSourceStack> holograms() {
        return Commands.literal("holograms").requires(permission(PERMISSION_CONFIGURE))
                .executes(run(context -> handlers.hologramsStatus(sender(context))))
                .then(Commands.literal("on").executes(run(context -> handlers.setHolograms(sender(context), true))))
                .then(Commands.literal("off").executes(run(context -> handlers.setHolograms(sender(context), false))));
    }

    /** The permission check of a literal or an argument: the sender must hold the node. */
    private static Predicate<CommandSourceStack> permission(String node) {
        return source -> source.getSender().hasPermission(node);
    }

    /**
     * The completion of an argument: the texts that start with what is typed, that the sender may use. The texts are read when the
     * player types the argument, on the server's thread, as the handlers read them.
     */
    private static SuggestionProvider<CommandSourceStack> suggest(String permission, Supplier<? extends Collection<String>> texts) {
        return (context, builder) -> {
            Predicate<String> has = context.getSource().getSender()::hasPermission;
            for (String text : Completion.complete(Completion.all(texts.get(), permission), builder.getRemaining(), has)) {
                builder.suggest(text);
            }
            return builder.buildFuture();
        };
    }

    /** A line of the tree: the action runs with the context, and the line counts as a success. */
    private static Command<CommandSourceStack> run(Consumer<CommandContext<CommandSourceStack>> action) {
        return context -> {
            action.accept(context);
            return Command.SINGLE_SUCCESS;
        };
    }

    private static CommandSender sender(CommandContext<CommandSourceStack> context) {
        return context.getSource().getSender();
    }

    private static String text(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, String.class);
    }

    private static int number(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, Integer.class);
    }
}
