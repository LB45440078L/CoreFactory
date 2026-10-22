package io.corefactory.spigot.scanner;

import io.corefactory.core.config.Messages;
import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DepositText;
import io.corefactory.core.deposit.RichnessTier;
import io.corefactory.core.drill.DrillText;
import io.corefactory.spigot.gui.MenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * The deposit map, opened by {@code /cf deposits}: the nearest deposits of the scanner's filter, eighteen to a page, each as the
 * icon of its ore with its direction, distance, tier and share left. A click on a deposit sets the player's waypoint to it. The
 * bottom row holds the page arrows, the filter button (which moves the scanner's filter on), a summary and the waypoint clear.
 *
 * <p>The list is taken when the map opens. Read-only: {@code GuiListener} cancels every click.
 */
public final class DepositMap implements MenuHolder {

    /** The deposits on one page. */
    static final int PER_PAGE = 18;

    private static final int SIZE = 27;
    private static final int PREVIOUS = 18;
    private static final int FILTER = 20;
    private static final int SUMMARY = 22;
    private static final int CLEAR = 24;
    private static final int NEXT = 26;
    private static final int EMPTY = 13;

    private final DepositScanner scanner;
    private final int originX;
    private final int originZ;
    private final List<Deposit> shown;
    private final int page;
    private final int pages;
    private final int total;
    private final Inventory inventory;

    private DepositMap(DepositScanner scanner, Player viewer, int requested) {
        this.scanner = scanner;
        Location at = viewer.getLocation();
        this.originX = at.getBlockX();
        this.originZ = at.getBlockZ();
        String filter = scanner.filterOf(viewer.getUniqueId());
        List<Deposit> all = scanner.nearby(viewer, filter, DepositScanner.MAP_LIMIT);
        this.total = all.size();
        this.pages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
        this.page = Math.max(0, Math.min(requested, pages - 1));
        int from = Math.min(page * PER_PAGE, all.size());
        this.shown = all.subList(from, Math.min(from + PER_PAGE, all.size()));
        this.inventory = Bukkit.createInventory(this, SIZE, Messages.color("&8Deposits " + (page + 1) + "/" + pages));
        for (int i = 0; i < shown.size(); i++) {
            inventory.setItem(i, entry(shown.get(i)));
        }
        if (all.isEmpty()) {
            inventory.setItem(EMPTY, labelled(Material.PAPER, "&7No deposits within " + scanner.radius() + " blocks"));
        }
        if (page > 0) {
            inventory.setItem(PREVIOUS, labelled(Material.ARROW, "&7Previous page"));
        }
        if (page < pages - 1) {
            inventory.setItem(NEXT, labelled(Material.ARROW, "&7Next page"));
        }
        inventory.setItem(FILTER, labelled(Material.COMPASS, "&7Filter: &f" + scanner.filterLabel(filter),
                "&8Click to move to the next ore"));
        inventory.setItem(SUMMARY, labelled(Material.BOOK, "&6" + total + " &7deposits within &f" + scanner.radius() + " &7blocks",
                "&7Each page lists " + PER_PAGE + ", nearest first."));
        if (scanner.waypoints().get(viewer.getUniqueId()).isPresent()) {
            inventory.setItem(CLEAR, labelled(Material.BARRIER, "&cClear waypoint"));
        } else {
            inventory.setItem(CLEAR, labelled(Material.GRAY_STAINED_GLASS_PANE, "&7No waypoint set"));
        }
    }

    /**
     * Opens a page of the deposit map for a player.
     *
     * @param scanner the scanner that holds the filter and the waypoints
     * @param viewer  the player who opens it
     * @param page    the page, from 0; clamped to the pages there are
     */
    static void open(DepositScanner scanner, Player viewer, int page) {
        viewer.openInventory(new DepositMap(scanner, viewer, page).inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void click(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        if (slot >= 0 && slot < PER_PAGE) {
            if (slot < shown.size()) {
                scanner.setWaypoint(player, shown.get(slot));
                player.closeInventory();
            }
        } else if (slot == PREVIOUS && page > 0) {
            open(scanner, player, page - 1);
        } else if (slot == NEXT && page < pages - 1) {
            open(scanner, player, page + 1);
        } else if (slot == FILTER) {
            scanner.cycleFilter(player);
            open(scanner, player, 0);
        } else if (slot == CLEAR && scanner.waypoints().get(player.getUniqueId()).isPresent()) {
            scanner.clearWaypoint(player);
            open(scanner, player, page);
        }
    }

    private ItemStack entry(Deposit deposit) {
        ItemStack item = scanner.core().createOreItem(deposit.ore(), 1).orElseGet(() -> new ItemStack(Material.STONE));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(scanner.oreName(deposit.ore())));
            meta.setLore(List.of(
                    Messages.color("&7Where &f" + DepositText.place(deposit, originX, originZ)),
                    Messages.color("&7Tier " + RichnessTier.of(deposit.remaining()).coloured()),
                    Messages.color("&7Left &f" + DrillText.amount(deposit.remaining()) + " &7of &f"
                            + DrillText.amount(deposit.initialAmount()) + " &8(" + DrillText.share(deposit) + ")"),
                    "",
                    Messages.color("&eClick for a waypoint to this deposit")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack labelled(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(name));
            if (lore.length > 0) {
                meta.setLore(List.of(lore).stream().map(Messages::color).toList());
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
