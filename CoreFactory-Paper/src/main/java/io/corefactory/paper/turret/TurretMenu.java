package io.corefactory.paper.turret;

import io.corefactory.core.text.Text;
import io.corefactory.core.railgun.RailgunLevel;
import io.corefactory.core.railgun.RailgunSettings;
import io.corefactory.core.turret.Magazine;
import io.corefactory.core.turret.TurretAccess;
import io.corefactory.core.turret.TargetCatalog;
import io.corefactory.core.turret.TargetKind;
import io.corefactory.core.turret.TargetPolicy;
import io.corefactory.core.turret.TurretLevel;
import io.corefactory.core.turret.TurretLevels;
import io.corefactory.core.turret.TurretSettings;
import io.corefactory.paper.friends.FriendAccess;
import io.corefactory.paper.gui.MenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The menu of one turret, three rows. Row one holds the turret's info, the load and eject buttons, the friendly-fire switch, the team switch,
 * and the four target categories. A machine gun shows its rounds, their damage and its status; it has the load and eject buttons. A laser turret
 * holds no rounds, so it shows its power, range and damage in their place, and has no load or eject buttons. Each info item shows the level, the
 * range, the damage, the cannons and the fire interval the level gives. A rail gun is the same, with its charge and reload in its info, and its power switch in the slot of the load button. The two rows below hold one wool toggle per listed entity type: green
 * when the turret fires at that type, red when it spares it. A toggle set here applies to this turret only, and only its owner may change it.
 * Single players are set with the turret command, since a menu cannot pick a name. The slots are decoration that cannot be moved, so the rounds
 * are only ever in the turret's magazine.
 */
public final class TurretMenu implements MenuHolder {

    /** The size of the menu: three rows. */
    public static final int SIZE = 27;

    /** The slot of the load button. */
    public static final int LOAD_SLOT = 1;

    /** The slot of the eject button. */
    public static final int EJECT_SLOT = 2;

    /** The slot of a rail gun's power switch, which is the load button's slot: an electric turret holds no rounds to load. */
    public static final int POWER_SLOT = LOAD_SLOT;

    /** The slot of the friendly-fire switch. */
    public static final int FRIENDLY_FIRE_SLOT = 3;

    /** The slot of the team switch: spare, target or ignore the owner's scoreboard team. */
    public static final int TEAM_SLOT = 4;

    private static final int INFO_SLOT = 0;
    private static final int FIRST_CATEGORY_SLOT = 5;
    private static final int FIRST_ENTITY_SLOT = 9;

    private final TurretService service;
    private final TurretDevice turret;
    private final Inventory inventory;

    TurretMenu(TurretService service, TurretDevice turret) {
        this.service = service;
        this.turret = turret;
        this.inventory = Bukkit.createInventory(this, SIZE, Text.parse(TurretText.nameOf(turret.kind())));
        refresh();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** True while a player has this menu open. */
    public boolean isOpen() {
        return !inventory.getViewers().isEmpty();
    }

    /** Closes the menu for everyone who has it open. */
    void closeViewers() {
        for (HumanEntity viewer : List.copyOf(inventory.getViewers())) {
            viewer.closeInventory();
        }
    }

    /** Redraws every item from the turret's state. */
    void refresh() {
        TurretSettings settings = service.settings();
        TargetPolicy policy = turret.policy();
        if (turret.kind().isElectric()) {
            boolean railgun = turret.kind() == TurretKind.RAILGUN;
            inventory.setItem(INFO_SLOT, railgun ? railgunInfo() : laserInfo());
            inventory.setItem(LOAD_SLOT, railgun ? powerItem() : null);
            inventory.setItem(EJECT_SLOT, null);
        } else {
            inventory.setItem(INFO_SLOT, machineGunInfo());
            inventory.setItem(LOAD_SLOT, item(Material.LIME_DYE, "&aLoad rounds",
                    List.of("&7Takes up to one stack of one round type", "&7from your inventory.")));
            inventory.setItem(EJECT_SLOT, item(Material.HOPPER, "&cEject rounds",
                    List.of("&7Gives every round back to you.")));
        }
        boolean friendlyFire = policy.friendlyFire();
        inventory.setItem(FRIENDLY_FIRE_SLOT, item(friendlyFire ? Material.RED_DYE : Material.LIME_DYE,
                "&fFriendly fire: " + (friendlyFire ? "&con" : "&aoff"),
                friendlyFire
                        ? List.of("&7Fires at players who are not your friends.", "&7Your friends, your claim's trusted players",
                                "&7and you are never targeted.", "&7Click to switch off.")
                        : List.of("&7No player is targeted.", "&7Friends, trusted players and you stay safe.",
                                "&7Click to switch on.")));
        inventory.setItem(TEAM_SLOT, teamItem(policy.teamMode()));
        for (int index = 0; index < TargetCatalog.CATEGORIES.size(); index++) {
            TargetKind kind = TargetCatalog.CATEGORIES.get(index);
            String name = categoryName(kind);
            boolean on = policy.allows(null, kind, settings);
            boolean own = policy.overrideOf(kind.configName()).isPresent();
            inventory.setItem(FIRST_CATEGORY_SLOT + index,
                    toggle(on, name, own, "all " + name.toLowerCase(Locale.ROOT) + " not set below"));
        }
        for (int index = 0; index < TargetCatalog.ENTITIES.size(); index++) {
            TargetCatalog.Entry entry = TargetCatalog.ENTITIES.get(index);
            boolean on = policy.allows(entry.entity(), entry.kind(), settings);
            boolean own = policy.overrideOf(entry.entity()).isPresent();
            inventory.setItem(FIRST_ENTITY_SLOT + index, toggle(on, entityName(entry.entity()), own, null));
        }
    }

    @Override
    public void click(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();
        boolean machineGun = turret.kind() == TurretKind.MACHINE_GUN;
        if (turret.kind() == TurretKind.RAILGUN && slot == POWER_SLOT) {
            if (mayUse(player, turret, service.access())) {
                service.setPower(turret, !turret.on());
            }
        } else if (machineGun && slot == LOAD_SLOT) {
            if (mayUse(player, turret, service.access())) {
                int taken = service.loadFromInventory(turret, player);
                player.sendMessage(Text.parse(taken > 0 ? "&aLoaded &f" + taken + " &around."
                        : "&cNo round in your inventory fits this turret."));
            }
        } else if (machineGun && slot == EJECT_SLOT) {
            if (mayUse(player, turret, service.access())) {
                int count = service.eject(turret, player);
                player.sendMessage(Text.parse(count > 0 ? "&7Ejected &f" + count + " &7rounds." : "&7No rounds to eject."));
            }
        } else if (slot == FRIENDLY_FIRE_SLOT) {
            if (mayChange(player, turret)) {
                service.setPolicy(turret, turret.policy().withFriendlyFire(!turret.policy().friendlyFire()));
            }
        } else if (slot == TEAM_SLOT) {
            if (mayChange(player, turret)) {
                service.setPolicy(turret, turret.policy().withTeamMode(nextTeamMode(turret.policy().teamMode())));
            }
        } else if (slot >= FIRST_CATEGORY_SLOT && slot < FIRST_CATEGORY_SLOT + TargetCatalog.CATEGORIES.size()) {
            TargetKind kind = TargetCatalog.CATEGORIES.get(slot - FIRST_CATEGORY_SLOT);
            if (mayChange(player, turret)) {
                boolean on = turret.policy().allows(null, kind, service.settings());
                service.setPolicy(turret, turret.policy().withOverride(kind.configName(), !on));
            }
        } else if (slot >= FIRST_ENTITY_SLOT && slot < FIRST_ENTITY_SLOT + TargetCatalog.ENTITIES.size()) {
            TargetCatalog.Entry entry = TargetCatalog.ENTITIES.get(slot - FIRST_ENTITY_SLOT);
            if (mayChange(player, turret)) {
                boolean on = turret.policy().allows(entry.entity(), entry.kind(), service.settings());
                service.setPolicy(turret, turret.policy().withOverride(entry.entity(), !on));
            }
        }
        refresh();
    }

    /**
     * True when the player may change the turret's targeting: its friendly-fire switch, its team mode and its stances. Only the owner may,
     * with the permission, since these decide whom the turret fires at. A line says why not otherwise.
     */
    static boolean mayChange(Player player, TurretDevice turret) {
        TurretAccess.Verdict verdict = TurretAccess.decide(player.getUniqueId().equals(turret.owner()),
                player.hasPermission(TurretIds.PERMISSION));
        if (verdict == TurretAccess.Verdict.ALLOW) {
            return true;
        }
        player.sendMessage(Text.parse(verdict == TurretAccess.Verdict.NOT_ALLOWED
                ? "&cOnly the owner of this turret can change its targeting."
                : "&cYou may not change turrets."));
        return false;
    }

    /**
     * True when the player may use the turret: its owner or a friend of the owner, with the permission. That covers the power of a rail gun,
     * loading and ejecting rounds, upgrading and breaking it. A line says why not otherwise.
     */
    static boolean mayUse(Player player, TurretDevice turret, FriendAccess friends) {
        TurretAccess.Verdict verdict = TurretAccess.decide(friends.mayUse(turret.owner(), player.getUniqueId()),
                player.hasPermission(TurretIds.PERMISSION));
        if (verdict == TurretAccess.Verdict.ALLOW) {
            return true;
        }
        player.sendMessage(Text.parse(verdict == TurretAccess.Verdict.NOT_ALLOWED
                ? "&cOnly the owner or a friend of the owner can change this turret."
                : "&cYou may not change turrets."));
        return false;
    }

    /** The next team mode in the order spare, target, ignore, and back to spare. */
    static TargetPolicy.TeamMode nextTeamMode(TargetPolicy.TeamMode mode) {
        return switch (mode) {
            case SPARE -> TargetPolicy.TeamMode.TARGET;
            case TARGET -> TargetPolicy.TeamMode.IGNORE;
            case IGNORE -> TargetPolicy.TeamMode.SPARE;
        };
    }

    /** The info item of a machine gun: its rounds, their bypass and fire, its level and status, and its power. */
    private ItemStack machineGunInfo() {
        Magazine magazine = turret.magazine();
        List<String> level = levelLines();
        if (magazine.isEmpty() || magazine.type() == null) {
            List<String> lore = new ArrayList<>(level);
            lore.add("&7Load rounds with a round in hand,");
            lore.add("&7or with the button beside this one.");
            lore.add("&7Capacity: &f" + magazine.capacity());
            lore.add("&7Status: &f" + turret.status().text());
            return item(Material.ARROW, "&7No rounds loaded", lore);
        }
        TurretSettings.Round round = service.settings().round(magazine.type());
        List<String> lore = new ArrayList<>(level);
        lore.add("&7Armour bypass: &f" + percent(round.armourBypass()));
        lore.add("&7Fire: &f" + fireText(round.fireTicks()));
        lore.add("&7Status: &f" + turret.status().text());
        return item(Material.ARROW, "&6" + TurretText.roundName(magazine.type()) + " rounds: &f" + magazine.count()
                + "&7/&f" + magazine.capacity(), lore);
    }

    /** The info item of a laser turret: its level, its range, its damage and its power. It holds no rounds. */
    private ItemStack laserInfo() {
        List<String> lore = new ArrayList<>(levelLines());
        lore.add("&7Armour bypass: &f" + percent(service.stats().laser().armourBypass()));
        lore.add("&7Status: &f" + turret.status().text());
        return item(Material.REDSTONE_LAMP, "&cLaser: no rounds needed", lore);
    }

    /**
     * The info item of a rail gun: its level, its range, its damage and its power, with its charge and reload. It holds no rounds, and its
     * charge is the wind-up of each shot.
     */
    private ItemStack railgunInfo() {
        RailgunSettings.LevelStats stats = service.stats().railgun().level(RailgunLevel.ofNumber(turret.level()));
        List<String> lore = new ArrayList<>(levelLines());
        lore.add("&7Charge: &f" + format(stats.chargeSeconds()) + " &7s, then reload &f" + format(stats.reloadSeconds()) + " &7s");
        lore.add("&7Status: &f" + turret.status().text());
        return item(Material.LIGHTNING_ROD, "&4Rail gun: charges before each shot", lore);
    }

    /** The power switch of a rail gun: green while it is on, red while it is off. */
    private ItemStack powerItem() {
        return turret.on()
                ? item(Material.LIME_DYE, "&aPower: on", List.of("&7The rail gun charges and fires.", "&7Click to switch it off."))
                : item(Material.RED_DYE, "&cPower: off", List.of("&7The rail gun stays idle.", "&7Click to switch it on."));
    }

    /**
     * The lines of a turret's level: the level, the range, the damage per hit, the cannons, the fire interval, the power, and the height when
     * it is tall.
     */
    private List<String> levelLines() {
        TurretLevel level = service.stats().level(turret.kind(), turret.level());
        TurretSettings view = service.stats().view(turret.kind(), turret.level());
        List<String> lines = new ArrayList<>();
        lines.add("&7Level &f" + level.level() + "&7 of &f" + TurretLevels.MAX_LEVEL
                + (turret.height() > 1 ? " &8(two blocks tall)" : ""));
        lines.add("&7Range: &f" + format(view.range()) + " &7blocks");
        lines.add("&7Damage: &f" + format(level.effectiveDamage()) + " &7per hit");
        lines.add("&7Cannons: &f" + level.cannons());
        if (turret.kind() != TurretKind.RAILGUN) {
            lines.add("&7Fire: every &f" + view.firingIntervalTicks() + " &7ticks at full power");
        }
        lines.add("&7Power: &f" + percent(turret.satisfaction()) + " &7of &f" + view.powerDemandW() + " W");
        return lines;
    }

    /** The team switch: its mode, what it does, and the turret command for single players. */
    private static ItemStack teamItem(TargetPolicy.TeamMode mode) {
        return switch (mode) {
            case SPARE -> item(Material.LIME_DYE, "&aTeam: spared", teamLore("&7Teammates are treated as friends."));
            case TARGET -> item(Material.RED_DYE, "&cTeam: targeted", teamLore("&7Teammates are targets like strangers,"
                    + " with friendly fire on."));
            case IGNORE -> item(Material.GRAY_DYE, "&7Team: ignored", teamLore("&7The team does not count."));
        };
    }

    private static List<String> teamLore(String first) {
        return List.of(first, "&7Your scoreboard team, as the owner.", "&7Click to change it.",
                "&7Single players: &f/cf turret player <name> <target|spare|clear>");
    }

    /** A wool toggle: green when the turret targets it, red when it spares it. */
    private static ItemStack toggle(boolean on, String name, boolean own, String scope) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Targeted: " + (on ? "&ayes" : "&cno"));
        if (scope != null) {
            lore.add("&7Covers " + scope + ".");
        }
        lore.add(own ? "&7Set for this turret." : "&7Follows the settings of the config.");
        lore.add("&7Click to " + (on ? "spare" : "target") + " it.");
        return item(on ? Material.GREEN_WOOL : Material.RED_WOOL, (on ? "&a" : "&c") + name, lore);
    }

    private static ItemStack item(Material material, String title, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(title));
            meta.lore(Text.lore(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static String categoryName(TargetKind kind) {
        return switch (kind) {
            case MONSTER -> "Monsters";
            case PHANTOM -> "Phantoms";
            case SLIME -> "Slimes";
            case GHAST -> "Ghasts";
            case OTHER -> "Other";
        };
    }

    /** A title for an entity type's name: ZOMBIE becomes Zombie, WITHER_SKELETON becomes Wither Skeleton. */
    private static String entityName(String entity) {
        StringBuilder title = new StringBuilder();
        for (String word : entity.split("_")) {
            if (!title.isEmpty()) {
                title.append(' ');
            }
            title.append(word.charAt(0)).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return title.toString();
    }

    private static String percent(double share) {
        return Math.round(Math.max(0.0, Math.min(1.0, share)) * 100.0) + "%";
    }

    private static String format(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static String fireText(int ticks) {
        return ticks > 0 ? "sets targets alight for " + format(ticks / 20.0) + " s" : "none";
    }
}
