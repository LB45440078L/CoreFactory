package io.corefactory.spigot.gui;

import io.corefactory.core.config.Messages;
import io.corefactory.spigot.achievement.Achievement;
import io.corefactory.spigot.achievement.AchievementStore;
import io.corefactory.spigot.achievement.AchievementTracker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * One page of a player's achievements. Unlocked entries glow and show when they were earned; locked entries show
 * how far the player has got. The bottom row holds the page arrows and a summary.
 */
public final class AchievementGui implements MenuHolder {

    private static final int PER_PAGE = 45;
    private static final int PREVIOUS = 45;
    private static final int SUMMARY = 49;
    private static final int NEXT = 53;

    private final AchievementTracker tracker;
    private final UUID subject;
    private final String subjectName;
    private final int page;
    private final int pages;
    private final Inventory inventory;

    private AchievementGui(AchievementTracker tracker, UUID subject, String subjectName, int requested) {
        this.tracker = tracker;
        this.subject = subject;
        this.subjectName = subjectName;
        List<Achievement> all = tracker.catalog().all();
        this.pages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        this.page = Math.max(0, Math.min(requested, pages - 1));
        this.inventory = Bukkit.createInventory(this, 54,
                Messages.color("&8Achievements " + (page + 1) + "/" + pages + " - " + subjectName));
        int from = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && from + i < all.size(); i++) {
            inventory.setItem(i, entry(all.get(from + i)));
        }
        if (page > 0) {
            inventory.setItem(PREVIOUS, labelled(Material.ARROW, "&7Previous page"));
        }
        if (page < pages - 1) {
            inventory.setItem(NEXT, labelled(Material.ARROW, "&7Next page"));
        }
        inventory.setItem(SUMMARY, summary(all));
    }

    /** Opens a page of {@code subjectName}'s achievements for {@code viewer}. */
    public static void open(AchievementTracker tracker, Player viewer, UUID subject, String subjectName, int page) {
        AchievementGui gui = new AchievementGui(tracker, subject, subjectName, page);
        viewer.openInventory(gui.inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void click(InventoryClickEvent event, Player player) {
        if (event.getSlot() == PREVIOUS && page > 0) {
            open(tracker, player, subject, subjectName, page - 1);
        } else if (event.getSlot() == NEXT && page < pages - 1) {
            open(tracker, player, subject, subjectName, page + 1);
        }
    }

    private ItemStack entry(Achievement achievement) {
        AchievementStore store = tracker.store();
        boolean unlocked = store.isUnlocked(subject, achievement.id());
        ItemStack item = achievement.icon().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(Messages.color((unlocked ? "&a" : "&7") + (unlocked ? "✔ " : "") + achievement.title()));
        List<String> lore = new ArrayList<>();
        lore.add(Messages.color("&8" + achievement.category().label()));
        lore.add(Messages.color("&7" + achievement.description()));
        lore.add("");
        if (unlocked) {
            long when = store.unlocked(subject).getOrDefault(achievement.id(), 0L);
            lore.add(Messages.color("&aUnlocked " + new SimpleDateFormat("yyyy-MM-dd").format(new Date(when))));
        } else {
            lore.add(Messages.color("&7Progress: &f" + progressText(achievement)));
        }
        meta.setLore(lore);
        if (unlocked) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private String progressText(Achievement achievement) {
        if (achievement.trigger() == Achievement.Trigger.GROUP) {
            List<Achievement> members = tracker.catalog().members(achievement.param());
            long done = members.stream().filter(m -> tracker.store().isUnlocked(subject, m.id())).count();
            return done + "/" + members.size() + " in the group";
        }
        if (achievement.trigger() == Achievement.Trigger.CHAIN) {
            return "depth " + achievement.param();
        }
        int value = Math.min(tracker.store().progress(subject, achievement.id()), achievement.goal());
        return value + "/" + achievement.goal();
    }

    private ItemStack summary(List<Achievement> all) {
        long unlocked = all.stream().filter(a -> tracker.store().isUnlocked(subject, a.id())).count();
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color("&6" + subjectName + "&7: &f" + unlocked + "&7/&f" + all.size()));
            meta.setLore(List.of(Messages.color("&7achievements unlocked")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack labelled(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(name));
            item.setItemMeta(meta);
        }
        return item;
    }
}
