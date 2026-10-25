package io.corefactory.paper.vertical;

import io.corefactory.core.text.Text;
import io.corefactory.paper.friends.FriendAccess;
import io.corefactory.core.vertical.VerticalDir;
import io.corefactory.paper.gui.MenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Optional;

/**
 * The menu of a vertical belt: an UP button, a DOWN button, with the tower's current direction glowing, and an info item with the
 * tower's size, level and the items it carries. A click on a button sets the direction of the whole tower, through the service, when
 * the player may change it. The menu is read-only: {@link org.bukkit.event.inventory.InventoryClickEvent}s are cancelled by the GUI
 * listener, and only the buttons act.
 */
public final class VerticalMenu implements MenuHolder {

    private final VerticalBelt belt;
    private final VerticalService service;
    private final FriendAccess friends;
    private final Inventory inventory;

    /**
     * @param belt    the belt whose tower the menu sets
     * @param service the service that sets the direction and reads the tower
     * @param friends the friend rule, which decides who may change the tower
     */
    public VerticalMenu(VerticalBelt belt, VerticalService service, FriendAccess friends) {
        this.belt = belt;
        this.service = service;
        this.friends = friends;
        this.inventory = Bukkit.createInventory(this, VerticalRules.SIZE,
                Text.parse("&6Vertical belt " + VerticalIds.roman(belt.level())));
        refresh();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Shows the menu to a player. */
    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public void click(InventoryClickEvent event, Player player) {
        Optional<VerticalDir> requested = VerticalRules.buttonAt(event.getRawSlot());
        if (requested.isEmpty()) {
            return;
        }
        boolean allowed = friends.mayUse(belt.owner(), player.getUniqueId());
        switch (VerticalRules.decide(requested.get(), belt.direction(), allowed)) {
            case SET -> {
                if (service.setDirection(belt, requested.get())) {
                    player.sendMessage(Text.parse("&7The tower now carries " + name(requested.get()) + "."));
                }
            }
            case SAME -> {
            }
            case DENIED -> player.sendMessage(Text.parse("&cOnly the owner or a friend of the owner can change this vertical belt's direction."));
        }
        refresh();
    }

    private void refresh() {
        inventory.clear();
        ItemStack filler = named(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(), false);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
        VerticalDir current = belt.direction();
        inventory.setItem(VerticalRules.UP_SLOT, named(Material.ARROW, "&aUp",
                List.of("&7The items move toward the top.", current == VerticalDir.UP ? "&eCurrent direction" : "&7Click to set."),
                current == VerticalDir.UP));
        inventory.setItem(VerticalRules.DOWN_SLOT, named(Material.ARROW, "&aDown",
                List.of("&7The items move toward the bottom.", current == VerticalDir.DOWN ? "&eCurrent direction" : "&7Click to set."),
                current == VerticalDir.DOWN));
        inventory.setItem(VerticalRules.INFO_SLOT, info());
    }

    private ItemStack info() {
        Optional<VerticalService.Info> info = service.infoOf(belt);
        List<String> lore = info.map(shown -> List.of(
                "&7Tower size: &f" + shown.size() + (shown.size() == 1 ? " belt" : " belts"),
                "&7Level: &f" + VerticalIds.roman(shown.level()),
                "&7Items carried: &f" + shown.carried(),
                "&7Direction: &f" + name(shown.direction())))
                .orElse(List.of("&cThis belt is not part of a tower yet."));
        return named(Material.PAPER, "&6Tower", lore, false);
    }

    private static String name(VerticalDir direction) {
        return direction == VerticalDir.UP ? "up" : "down";
    }

    private static ItemStack named(Material material, String name, List<String> lore, boolean glowing) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(Text.item(name));
        meta.lore(Text.lore(lore));
        meta.setEnchantmentGlintOverride(glowing);
        item.setItemMeta(meta);
        return item;
    }
}
