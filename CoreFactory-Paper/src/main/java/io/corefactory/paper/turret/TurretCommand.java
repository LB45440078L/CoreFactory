package io.corefactory.paper.turret;

import io.corefactory.core.text.Text;
import io.corefactory.core.turret.TargetCatalog;
import io.corefactory.core.turret.TargetKind;
import io.corefactory.core.turret.TargetPolicy;
import io.corefactory.core.turret.TurretLevels;
import io.corefactory.core.turret.TurretSettings;
import io.corefactory.paper.command.Style;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

/**
 * {@code /cf turret ...}, for the turret the player looks at within six blocks, or else the nearest turret the player owns within eight
 * blocks. Only the turret's owner may use it, and the player needs {@link TurretIds#PERMISSION}, which the command tree checks:
 * <ul>
 * <li>{@code info}: the level, range, damage, cannons, fire, power, targets, friendly fire, team mode and the player stances;</li>
 * <li>{@code target <mob|category> <on|off>}: a per-turret mob override, by an entity name such as ZOMBIE or a category such as monsters;</li>
 * <li>{@code player <name> <target|spare|clear>}: a stance for one player, whatever the friend rules say; the owner is never a target;</li>
 * <li>{@code team <spare|target|ignore>}: how the owner's scoreboard team is treated;</li>
 * <li>{@code friendlyfire <on|off>}: the friendly-fire switch.</li>
 * </ul>
 */
public final class TurretCommand {

    /** The subcommand of the friendly-fire switch, after {@code turret}. */
    public static final String FRIENDLY_FIRE = "friendlyfire";

    private static final double LOOK_REACH = 6.0;
    private static final double NEAR_REACH = 8.0;
    private static final String USAGE_INFO = "/cf turret info";
    private static final String USAGE_TARGET = "/cf turret target";
    private static final String USAGE_PLAYER = "/cf turret player";
    private static final String USAGE_TEAM = "/cf turret team";
    private static final String USAGE_FRIENDLY_FIRE = "/cf turret friendlyfire";

    private final TurretService service;
    private final TurretRegistry registry;

    /**
     * @param service  the turrets' runtime, which saves the choices
     * @param registry the placed turrets, to find the one the player means
     */
    public TurretCommand(TurretService service, TurretRegistry registry) {
        this.service = service;
        this.registry = registry;
    }

    /** {@code /cf turret} with no subcommand: the usage lines. */
    public void usage(CommandSender sender) {
        sender.sendMessage(Style.usage(USAGE_INFO, "", "the level, range, targets and stances of the turret you look at"));
        sender.sendMessage(Style.usage(USAGE_TARGET, "<mob|category> <on|off>", "fire at a mob or a category, or spare it"));
        sender.sendMessage(Style.usage(USAGE_PLAYER, "<name> <target|spare|clear>", "a stance for one player"));
        sender.sendMessage(Style.usage(USAGE_TEAM, "<spare|target|ignore>", "the owner's scoreboard team"));
        sender.sendMessage(Style.usage(USAGE_FRIENDLY_FIRE, "<on|off>", "fire at players who are not your friends"));
    }

    /** {@code /cf turret info}: the level, range, targets and stances of the turret the player means. */
    public void info(CommandSender sender) {
        Optional<TurretDevice> found = ownedTurret(sender, USAGE_INFO);
        if (found.isEmpty()) {
            return;
        }
        TurretDevice turret = found.get();
        TargetPolicy policy = turret.policy();
        TurretFacts facts = new TurretFacts(turret, service);
        sender.sendMessage(Text.parse("&6" + facts.name() + "&7, level &f" + turret.level() + "&7 of &f" + TurretLevels.MAX_LEVEL
                + (turret.height() > 1 ? " &7(two blocks tall)" : "")));
        sender.sendMessage(Text.parse("&7Range: &f" + format(facts.view().range()) + " &7blocks, damage &f"
                + format(facts.damage()) + " &7per hit, cannons &f" + facts.cannons()));
        sender.sendMessage(Text.parse("&7Fire: every &f" + facts.view().firingIntervalTicks() + " &7ticks at full power, power &f"
                + facts.view().powerDemandW() + " &7W"));
        long now = service.currentTick();
        sender.sendMessage(Text.parse(damageLine(turret.damageDealt(), turret.damageInLastSecond(now),
                turret.hitsInLastSecond(now))));
        sender.sendMessage(Text.parse("&7Friendly fire: " + (policy.friendlyFire() ? "&con" : "&aoff")
                + "&7, team: &f" + teamWord(policy.teamMode())));
        sender.sendMessage(Text.parse("&7Monsters: " + onOff(policy.allows(null, TargetKind.MONSTER, service.settings()))
                + "&7, phantoms: " + onOff(policy.allows(null, TargetKind.PHANTOM, service.settings()))
                + "&7, slimes: " + onOff(policy.allows(null, TargetKind.SLIME, service.settings()))
                + "&7, ghasts: " + onOff(policy.allows(null, TargetKind.GHAST, service.settings()))));
        sender.sendMessage(Text.parse("&7Mobs set here: " + overridesText(policy)));
        sender.sendMessage(Text.parse("&7Players: " + stancesText(policy)));
    }

    /**
     * {@code /cf turret target <mob|category> <on|off>}: a mob or a category the turret fires at, or spares.
     *
     * @param sender the sender
     * @param mob    the mob or category typed, or {@code null} when none is typed, which shows the usage
     * @param value  {@code on} or {@code off}, or {@code null} when none is typed, which shows the usage
     */
    public void target(CommandSender sender, String mob, String value) {
        if (mob == null || value == null) {
            sender.sendMessage(Style.usage(USAGE_TARGET, "<mob|category> <on|off>",
                    "a mob or a category this turret fires at, or spares"));
            return;
        }
        Optional<String> key = targetKeyOf(mob);
        if (key.isEmpty()) {
            sender.sendMessage(Text.parse("&cNo mob or category named &f" + Text.escape(mob) + "&c. Try &f"
                    + String.join(", ", targetNames()) + "&c."));
            return;
        }
        boolean on;
        if (value.equalsIgnoreCase("on")) {
            on = true;
        } else if (value.equalsIgnoreCase("off")) {
            on = false;
        } else {
            sender.sendMessage(Style.usageError(USAGE_TARGET, "<mob|category> <on|off>"));
            return;
        }
        Optional<TurretDevice> found = ownedTurret(sender, USAGE_TARGET);
        if (found.isEmpty()) {
            return;
        }
        TurretDevice turret = found.get();
        service.setPolicy(turret, turret.policy().withOverride(key.get(), on));
        sender.sendMessage(Text.parse((on ? "&aThis turret now fires at " : "&cThis turret now spares ")
                + "&f" + key.get().toLowerCase(Locale.ROOT) + "&7."));
    }

    /**
     * {@code /cf turret player <name> <target|spare|clear>}: a stance for one player.
     *
     * @param sender the sender
     * @param name   the player's name, or {@code null} when none is typed, which shows the usage
     * @param value  the stance, or {@code null} when none is typed, which shows the usage
     */
    public void player(CommandSender sender, String name, String value) {
        if (name == null || value == null) {
            sender.sendMessage(Style.usage(USAGE_PLAYER, "<name> <target|spare|clear>",
                    "a stance for one player: target it, spare it, or follow the friend rules"));
            return;
        }
        String stance = value.toLowerCase(Locale.ROOT);
        if (!stance.equals("target") && !stance.equals("spare") && !stance.equals("clear")) {
            sender.sendMessage(Style.usageError(USAGE_PLAYER, "<name> <target|spare|clear>"));
            return;
        }
        OfflinePlayer named = known(name);
        if (named == null) {
            sender.sendMessage(Text.parse("&cNo player named &f" + Text.escape(name) + "&c has joined this server."));
            return;
        }
        Optional<TurretDevice> found = ownedTurret(sender, USAGE_PLAYER);
        if (found.isEmpty()) {
            return;
        }
        TurretDevice turret = found.get();
        if (named.getUniqueId().equals(turret.owner()) && !stance.equals("clear")) {
            sender.sendMessage(Text.parse("&cYou own this turret, and an owner is never a target."));
            return;
        }
        TargetPolicy.PlayerStance playerStance = switch (stance) {
            case "target" -> TargetPolicy.PlayerStance.TARGET;
            case "spare" -> TargetPolicy.PlayerStance.SPARE;
            default -> null;
        };
        service.setPolicy(turret, turret.policy().withPlayer(named.getUniqueId(), playerStance));
        String shown = Text.escape(nameOf(named, name));
        sender.sendMessage(Text.parse(switch (stance) {
            case "target" -> "&cThis turret now targets &f" + shown + "&c, even with friendly fire off.";
            case "spare" -> "&aThis turret now spares &f" + shown + "&a.";
            default -> "&7This turret follows the friend rules for &f" + shown + "&7.";
        }));
    }

    /**
     * {@code /cf turret team <spare|target|ignore>}: how the owner's scoreboard team is treated.
     *
     * @param sender the sender
     * @param value  the mode typed, or {@code null} when none is typed, which shows the usage
     */
    public void team(CommandSender sender, String value) {
        if (value == null) {
            sender.sendMessage(Style.usage(USAGE_TEAM, "<spare|target|ignore>",
                    "how the owner's scoreboard team is treated: friends, strangers, or not counted"));
            return;
        }
        TargetPolicy.TeamMode mode = switch (value.toLowerCase(Locale.ROOT)) {
            case "spare" -> TargetPolicy.TeamMode.SPARE;
            case "target" -> TargetPolicy.TeamMode.TARGET;
            case "ignore" -> TargetPolicy.TeamMode.IGNORE;
            default -> null;
        };
        if (mode == null) {
            sender.sendMessage(Style.usageError(USAGE_TEAM, "<spare|target|ignore>"));
            return;
        }
        Optional<TurretDevice> found = ownedTurret(sender, USAGE_TEAM);
        if (found.isEmpty()) {
            return;
        }
        TurretDevice turret = found.get();
        service.setPolicy(turret, turret.policy().withTeamMode(mode));
        sender.sendMessage(Text.parse("&7Team: &f" + teamWord(mode) + "&7."));
    }

    /**
     * {@code /cf turret friendlyfire <on|off>}: the friendly-fire switch of the turret the player means.
     *
     * @param sender the sender
     * @param value  {@code on} or {@code off}, or {@code null} when none is typed, which shows the usage
     */
    public void friendlyFire(CommandSender sender, String value) {
        String word = value == null ? "" : value.toLowerCase(Locale.ROOT);
        if (!word.equals("on") && !word.equals("off")) {
            sender.sendMessage(Style.usageError(USAGE_FRIENDLY_FIRE, "<on|off>"));
            return;
        }
        Optional<TurretDevice> found = ownedTurret(sender, USAGE_FRIENDLY_FIRE);
        if (found.isEmpty()) {
            return;
        }
        TurretDevice turret = found.get();
        boolean on = word.equals("on");
        service.setPolicy(turret, turret.policy().withFriendlyFire(on));
        sender.sendMessage(Text.parse(on
                ? "&cFriendly fire is on: this turret fires at players who are not your friends."
                : "&aFriendly fire is off: this turret never fires at players."));
    }

    /**
     * The owned turret the sender means, after the check that the sender is a player who owns it. A line says why not when a check fails.
     */
    private Optional<TurretDevice> ownedTurret(CommandSender sender, String usage) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.parse("&cOnly a player can change a turret."));
            return Optional.empty();
        }
        Optional<TurretDevice> turret = turretOf(player);
        if (turret.isEmpty()) {
            player.sendMessage(Text.parse("&cLook at a turret you own, within " + (int) LOOK_REACH + " blocks, or stand near one. "
                    + "&7(" + usage + ")"));
            return Optional.empty();
        }
        if (!player.getUniqueId().equals(turret.get().owner())) {
            player.sendMessage(Text.parse("&cOnly the owner of this turret can change it."));
            return Optional.empty();
        }
        return turret;
    }

    /** The turret the player looks at, or else the nearest turret the player owns. */
    private Optional<TurretDevice> turretOf(Player player) {
        Location eye = player.getEyeLocation();
        RayTraceResult hit = eye.getWorld() == null ? null : eye.getWorld().rayTraceEntities(eye, eye.getDirection(), LOOK_REACH,
                entity -> entity instanceof Interaction);
        Entity entity = hit == null ? null : hit.getHitEntity();
        if (entity != null) {
            Optional<TurretDevice> looked = registry.byInteraction(entity.getUniqueId());
            if (looked.isPresent()) {
                return looked;
            }
        }
        World world = player.getWorld();
        Location at = player.getLocation();
        return registry.all().stream()
                .filter(turret -> turret.loadedWorld() == world && player.getUniqueId().equals(turret.owner()))
                .filter(turret -> turret.barrel().distance(at) <= NEAR_REACH)
                .min(Comparator.comparingDouble(turret -> turret.barrel().distance(at)));
    }

    /**
     * The override key a typed word names: a category's config name, such as {@code monsters}, or a mob of the target menu's list, such as
     * {@code ZOMBIE}. Empty when the word names neither.
     */
    private static Optional<String> targetKeyOf(String word) {
        Optional<TargetKind> kind = TargetKind.ofConfigName(word);
        if (kind.isPresent()) {
            return Optional.of(kind.get().configName());
        }
        String entity = word.trim().toUpperCase(Locale.ROOT);
        return TargetCatalog.ENTITIES.stream().map(TargetCatalog.Entry::entity).filter(candidate -> candidate.equals(entity)).findFirst();
    }

    /** The names a target command takes: the four categories, then the mobs of the target menu, for completion. */
    public static List<String> targetNames() {
        List<String> names = new ArrayList<>();
        for (TargetKind kind : TargetCatalog.CATEGORIES) {
            names.add(kind.configName());
        }
        for (TargetCatalog.Entry entry : TargetCatalog.ENTITIES) {
            names.add(entry.entity());
        }
        return names;
    }

    /** The names of the players online, for the completion of {@code /cf turret player}. */
    public static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            names.add(online.getName());
        }
        return names;
    }

    private static String overridesText(TargetPolicy policy) {
        if (policy.overrides().isEmpty()) {
            return "none (the config decides)";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Boolean> override : new TreeMap<>(policy.overrides()).entrySet()) {
            parts.add(override.getKey() + " " + (override.getValue() ? "&aon" : "&coff") + "&f");
        }
        return String.join(", ", parts);
    }

    private static String stancesText(TargetPolicy policy) {
        if (policy.players().isEmpty()) {
            return "none set (the friend rules apply)";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<UUID, TargetPolicy.PlayerStance> stance : policy.players().entrySet()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(stance.getKey());
            String name = player.getName() == null ? stance.getKey().toString() : player.getName();
            parts.add(name + " " + (stance.getValue() == TargetPolicy.PlayerStance.TARGET ? "&ctarget" : "&aspare") + "&f");
        }
        return String.join(", ", parts);
    }

    private static String teamWord(TargetPolicy.TeamMode mode) {
        return switch (mode) {
            case SPARE -> "spared as friends";
            case TARGET -> "targeted like strangers";
            case IGNORE -> "not counted";
        };
    }

    private static String onOff(boolean on) {
        return on ? "&aon" : "&coff";
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(Math.round(value * 100.0) / 100.0);
    }

    /**
     * The damage line of the info, with its colour codes: the damage the turret dealt in total, the damage in the last second, and the hits
     * in the last second.
     */
    static String damageLine(double total, double lastSecond, long hitsInLastSecond) {
        return "&7Damage dealt: &f" + format(total) + " &7total, &f" + format(lastSecond) + " &7in the last second, hits in the last second: &f"
                + hitsInLastSecond;
    }

    /** A player who is online, or who is already known to the server by name. Never waits for a profile lookup. */
    private static OfflinePlayer known(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (name.equalsIgnoreCase(offline.getName())) {
                return offline;
            }
        }
        return null;
    }

    private static String nameOf(OfflinePlayer player, String typed) {
        String name = player.getName();
        return name == null || name.isEmpty() ? typed : name;
    }

    /** The name, the damage, the cannons and the settings of a turret at its level, read in one place for the info lines. */
    private record TurretFacts(TurretDevice turret, TurretService service) {

        String name() {
            return switch (turret.kind()) {
                case MACHINE_GUN -> "Machine Gun Turret";
                case LASER -> "Laser Turret";
                case RAILGUN -> "Rail Gun";
            };
        }

        TurretSettings view() {
            return service.stats().view(turret.kind(), turret.level());
        }

        double damage() {
            return service.stats().damage(turret.kind(), turret.level());
        }

        int cannons() {
            return service.stats().cannons(turret.kind(), turret.level());
        }
    }
}
