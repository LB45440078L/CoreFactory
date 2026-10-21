package io.corefactory.spigot.achievement;

import io.corefactory.api.TntDefinition;
import io.corefactory.api.event.TntDetonateEvent;
import io.corefactory.api.event.TntIgniteEvent;
import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.config.Messages;
import io.corefactory.spigot.achievement.Achievement.Trigger;
import io.corefactory.spigot.machine.ItemRefs;
import io.corefactory.spigot.machine.MachineLayout;
import io.corefactory.spigot.machine.MachineMenu;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Turns game events into achievement progress. Every event is credited to the player who caused it: a crafted
 * recipe, a player's ignition, a detonation they started, a dropped item they threw. Blasts from redstone or other
 * plugins are not credited to anyone.
 */
public final class AchievementTracker implements Listener {

    /**
     * The triggers that a game event feeds. Every trigger in a catalogue must be in this set, or no event would ever
     * advance it. {@link Trigger#GROUP} is not listed: it completes from its members.
     */
    public static final Set<Trigger> HOOKED = EnumSet.of(Trigger.CRAFT, Trigger.USE, Trigger.IGNITE, Trigger.DETONATE,
            Trigger.NUKE, Trigger.VILLAGE, Trigger.SURVIVE, Trigger.LINK, Trigger.THROW, Trigger.CHAIN, Trigger.MINE,
            Trigger.PRODUCE, Trigger.MAKE, Trigger.RESEARCH, Trigger.PLACE, Trigger.PLACE_EACH);

    /** How long after a nuclear blast the village and survival checks run. */
    private static final long CHECK_DELAY_TICKS = 200L;

    /** Villagers that must die within the blast for it to count as a village nuke. */
    private static final int VILLAGERS_NEEDED = 3;

    private final Plugin plugin;
    private final CoreFactoryCore core;
    private final AchievementStore store;
    private AchievementCatalog catalog;

    public AchievementTracker(Plugin plugin, CoreFactoryCore core, AchievementStore store,
                              AchievementCatalog catalog) {
        this.plugin = plugin;
        this.core = core;
        this.store = store;
        this.catalog = catalog;
    }

    public AchievementStore store() {
        return store;
    }

    public AchievementCatalog catalog() {
        return catalog;
    }

    /** Replaces the catalogue after a reload, when TNT types or components may have changed. */
    public void setCatalog(AchievementCatalog catalog) {
        this.catalog = catalog;
    }

    /** A fuse linker connected two placed TNT. */
    public void linked(Player player) {
        record(player, Trigger.LINK, "link");
    }

    /** A TNT item was dropped on purpose. */
    public void thrown(Player player, String tntId) {
        record(player, Trigger.THROW, tntId);
    }

    /** A player mined a block of an ore. */
    public void mined(Player player, String oreId) {
        record(player, Trigger.MINE, oreId);
    }

    /** A player placed a machine. The first of each kind also counts toward the every-kind feat. */
    public void placed(Player player, String machineId) {
        record(player, Trigger.PLACE, AchievementCatalog.ANY);
        UUID id = player.getUniqueId();
        String seen = "machine:" + machineId;
        if (store.progress(id, seen) == 0) {
            store.add(id, seen, 1);
            record(player, Trigger.PLACE_EACH, AchievementCatalog.ANY);
        }
    }

    /** A research node was finished by feeding it at a console. The player may have gone offline in the meantime. */
    public void researched(UUID player, String nodeId) {
        Player online = Bukkit.getPlayer(player);
        if (online != null) {
            record(online, Trigger.RESEARCH, nodeId);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        Optional<String> crafted = core.tntIdOf(result).or(() -> core.items().componentId(result));
        crafted.ifPresent(id -> record(player, Trigger.CRAFT, id));
        Set<String> used = new LinkedHashSet<>();
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            core.items().componentId(ingredient).ifPresent(used::add);
        }
        for (String id : used) {
            record(player, Trigger.USE, id);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(TntIgniteEvent event) {
        if (event.getSource() instanceof Player player) {
            record(player, Trigger.IGNITE, event.getDefinition().id());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDetonate(TntDetonateEvent event) {
        if (!(event.getSource() instanceof Player player)) {
            return;
        }
        TntDefinition definition = event.getDefinition();
        record(player, Trigger.DETONATE, definition.id());
        if ("nuclear".equals(definition.mechanic())) {
            record(player, Trigger.NUKE, definition.id());
            watchNuclearBlast(player, event);
        }
        checkChain(player, event.getDepth());
    }

    /**
     * A player took something out of a machine's output slots. The machine is credited as producing it, and the item as
     * made. Only a take that was not cancelled counts, so a click that the menu refused does not.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMachineOutput(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MachineMenu menu)) {
            return;
        }
        int raw = event.getRawSlot();
        if (raw < 0 || raw >= top.getSize() || menu.layout().kindAt(raw) != MachineLayout.Kind.OUTPUT) {
            return;
        }
        ItemStack taken = event.getCurrentItem();
        if (ItemRefs.isEmpty(taken) || ItemRefs.isHint(taken)) {
            return;
        }
        record(player, Trigger.PRODUCE, menu.machine().id());
        core.items().componentId(taken).ifPresent(id -> record(player, Trigger.MAKE, id));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        store.save();
    }

    /**
     * Notes who was near a nuclear blast and which villagers were, and checks them again after a while. A village
     * counts when at least {@link #VILLAGERS_NEEDED} of those villagers are dead by then. The blast's own survivors
     * are checked the same way, so a player who lives through it is credited.
     */
    private void watchNuclearBlast(Player player, TntDetonateEvent event) {
        Location origin = event.getOrigin();
        World world = origin.getWorld();
        if (world == null) {
            return;
        }
        double radius = event.getDefinition().radius();
        List<UUID> villagers = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(origin, radius, radius, radius)) {
            if (entity instanceof Villager) {
                villagers.add(entity.getUniqueId());
            }
        }
        List<UUID> witnesses = new ArrayList<>();
        for (Player near : world.getPlayers()) {
            boolean counts = near.getGameMode() != GameMode.CREATIVE && near.getGameMode() != GameMode.SPECTATOR;
            if (counts && near.getLocation().distance(origin) <= radius * 1.5) {
                witnesses.add(near.getUniqueId());
            }
        }
        UUID source = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (villagers.size() >= VILLAGERS_NEEDED && deadCount(villagers) >= VILLAGERS_NEEDED) {
                Player attacker = Bukkit.getPlayer(source);
                if (attacker != null && attacker.isOnline()) {
                    record(attacker, Trigger.VILLAGE, AchievementCatalog.ANY);
                }
            }
            for (UUID id : witnesses) {
                Player survivor = Bukkit.getPlayer(id);
                if (survivor != null && survivor.isOnline() && !survivor.isDead()) {
                    record(survivor, Trigger.SURVIVE, AchievementCatalog.ANY);
                }
            }
        }, CHECK_DELAY_TICKS);
    }

    private static long deadCount(List<UUID> villagers) {
        return villagers.stream().filter(id -> {
            Entity entity = Bukkit.getEntity(id);
            return entity == null || entity.isDead();
        }).count();
    }

    private void checkChain(Player player, int depth) {
        for (Achievement achievement : catalog.withTrigger(Trigger.CHAIN)) {
            if (depth >= Integer.parseInt(achievement.param())) {
                grant(player, achievement);
            }
        }
    }

    /** Adds one to every achievement watching this trigger and id, and unlocks any that reach their goal. */
    private void record(Player player, Trigger trigger, String param) {
        UUID id = player.getUniqueId();
        for (Achievement achievement : catalog.withTrigger(trigger)) {
            if (!AchievementCatalog.ANY.equals(achievement.param()) && !achievement.param().equals(param)) {
                continue;
            }
            if (store.isUnlocked(id, achievement.id())) {
                continue;
            }
            int total = store.add(id, achievement.id(), 1);
            mirror(player, achievement, total);
            if (total >= achievement.goal()) {
                grant(player, achievement);
            }
        }
    }

    /** Unlocks an achievement, tells the player, and unlocks any group that this completed. */
    private void grant(Player player, Achievement achievement) {
        UUID id = player.getUniqueId();
        if (store.isUnlocked(id, achievement.id())) {
            return;
        }
        store.unlock(id, achievement.id(), System.currentTimeMillis());
        mirror(player, achievement, achievement.goal());
        memberUnlocked(player, achievement);
        if (!inAdvancements(achievement)) {
            announce(player, achievement);
        }
        for (Achievement group : catalog.withTrigger(Trigger.GROUP)) {
            if (!store.isUnlocked(id, group.id()) && groupComplete(id, group.param())) {
                grant(player, group);
            }
        }
    }

    private boolean groupComplete(UUID id, String group) {
        List<Achievement> members = catalog.members(group);
        return !members.isEmpty() && members.stream().allMatch(member -> store.isUnlocked(id, member.id()));
    }

    /** Whether the achievement exists in the server's advancements, so the server itself announces it. */
    private static boolean inAdvancements(Achievement achievement) {
        return Bukkit.getAdvancement(AchievementDatapack.key(achievement)) != null;
    }

    /**
     * Gives the player the vanilla criteria for {@code total} progress. Awarding the last one completes the
     * advancement, and the server then shows its toast and chat message to everyone. Groups are completed by their
     * members, see {@link #memberUnlocked}.
     */
    private void mirror(Player player, Achievement achievement, int total) {
        if (achievement.trigger() == Trigger.GROUP) {
            return;
        }
        Advancement advancement = Bukkit.getAdvancement(AchievementDatapack.key(achievement));
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        int steps = AchievementDatapack.steps(achievement);
        int done = total >= achievement.goal() ? steps : (int) ((long) total * steps / achievement.goal());
        for (int i = 1; i <= done; i++) {
            progress.awardCriteria(AchievementDatapack.step(i));
        }
    }

    /** Gives each group that counts this achievement its criterion for it. */
    private void memberUnlocked(Player player, Achievement member) {
        for (Achievement group : catalog.withTrigger(Trigger.GROUP)) {
            boolean counts = catalog.members(group.param()).stream().anyMatch(m -> m.id().equals(member.id()));
            if (!counts) {
                continue;
            }
            Advancement advancement = Bukkit.getAdvancement(AchievementDatapack.key(group));
            if (advancement != null) {
                player.getAdvancementProgress(advancement).awardCriteria(member.id());
            }
        }
    }

    /**
     * Fallback for when the datapack is not loaded: the same sound and a chat line. With the datapack loaded the
     * server's own toast and announcement are used instead.
     */
    private void announce(Player player, Achievement achievement) {
        String title = Messages.color(achievement.title());
        player.sendMessage(Messages.color("&6Achievement unlocked: ") + title
                + Messages.color(" &7- " + achievement.description()));
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
    }
}
