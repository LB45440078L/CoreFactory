package io.corefactory.paper.machine;

import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.text.Text;
import io.corefactory.paper.gui.MenuHolder;
import io.corefactory.paper.machine.DrillResourceRows.Line;
import io.corefactory.paper.machine.DrillResourceRows.OreLabel;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The resources menu of one drill: the deposits in its square that it may mine, with what is left in each. Each deposit has its
 * ore's icon (its name, and the amounts in its lore) and beside it a toggle for its ore, a green dye for mining or a grey dye for
 * ignoring. A click on a toggle flips the ore for every deposit of it and saves the drill. Three deposits sit on each of two rows,
 * and the bottom row holds the page arrows and the button back to the drill's menu. Nothing in this menu can be moved:
 * {@link io.corefactory.paper.gui.GuiListener} cancels every click.
 */
public final class DrillResourcesMenu implements MenuHolder {

    static final int SIZE = 27;
    static final int ENTRIES_PER_ROW = 3;
    static final int ROWS_PER_PAGE = 2;
    static final int ENTRIES_PER_PAGE = ENTRIES_PER_ROW * ROWS_PER_PAGE;
    static final int ROW_WIDTH = 9;
    static final int ENTRY_WIDTH = 3;
    static final int PREVIOUS = 18;
    static final int BACK = 22;
    static final int NEXT = 26;

    private final Inventory inventory;
    private final PlacedMachine placed;
    private final Map<String, OreLabel> labels = new HashMap<>();
    private final Runnable save;
    private int page;

    /**
     * @param placed the drill whose resources are shown
     * @param ores   the ore definitions by id, which give the name and icon of each deposit
     * @param save   writes the machines file, after a toggle
     */
    DrillResourcesMenu(PlacedMachine placed, Map<String, OreDefinition> ores, Runnable save) {
        this.placed = placed;
        this.save = save;
        for (OreDefinition ore : ores.values()) {
            labels.put(ore.id(), new OreLabel(ore.name(), ore.host()));
        }
        this.inventory = Bukkit.createInventory(this, SIZE, Text.parse("&8Drill resources"));
        render();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Draws the current page from the drill's last search and its selection. Called on open, on a toggle and periodically. */
    void render() {
        List<Line> lines = lines();
        int pages = pageCount(lines);
        page = Math.min(page, pages - 1);
        ItemStack blank = MachineLayout.decorItem(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot = 0; slot < PREVIOUS; slot++) {
            inventory.setItem(slot, blank);
        }
        if (lines.isEmpty()) {
            inventory.setItem(0, named(Material.PAPER, "&7No deposits in range",
                    "&7No deposit this drill may mine touches its square."));
        }
        for (int entry = 0; entry < ENTRIES_PER_PAGE; entry++) {
            int index = page * ENTRIES_PER_PAGE + entry;
            if (index >= lines.size()) {
                continue;
            }
            Line line = lines.get(index);
            inventory.setItem(iconSlot(entry), icon(line));
            inventory.setItem(iconSlot(entry) + 1, toggle(line));
        }
        ItemStack bottom = MachineLayout.decorItem(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = PREVIOUS; slot <= NEXT; slot++) {
            inventory.setItem(slot, bottom);
        }
        if (page > 0) {
            inventory.setItem(PREVIOUS, named(Material.ARROW, "&7Previous page", "&7Show the deposits before these."));
        }
        inventory.setItem(BACK, named(Material.OAK_DOOR, "&eBack to the drill", "&7Return to the drill's menu."));
        if (page < pages - 1) {
            inventory.setItem(NEXT, named(Material.ARROW, "&7Next page", "&7Show the deposits after these."));
        }
    }

    @Override
    public void click(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        if (slot == BACK) {
            placed.menu().open(player);
            return;
        }
        List<Line> lines = lines();
        if (slot == PREVIOUS && page > 0) {
            page--;
            render();
            return;
        }
        if (slot == NEXT && page < pageCount(lines) - 1) {
            page++;
            render();
            return;
        }
        if (slot < 0 || slot >= PREVIOUS || !isToggleSlot(slot)) {
            return;
        }
        int index = page * ENTRIES_PER_PAGE + entryOf(slot);
        if (index >= lines.size()) {
            return;
        }
        placed.selection().toggle(lines.get(index).oreId());
        save.run();
        render();
    }

    private static int pageCount(List<Line> lines) {
        return Math.max(1, (lines.size() + ENTRIES_PER_PAGE - 1) / ENTRIES_PER_PAGE);
    }

    /** The slot of the icon of an entry on the page. The toggle is the slot after it. */
    static int iconSlot(int entry) {
        return (entry / ENTRIES_PER_ROW) * ROW_WIDTH + (entry % ENTRIES_PER_ROW) * ENTRY_WIDTH;
    }

    /** True when the slot is the toggle of an entry: the second slot of each entry's pair on the two content rows. */
    static boolean isToggleSlot(int slot) {
        return slot / ROW_WIDTH < ROWS_PER_PAGE && slot % ROW_WIDTH % ENTRY_WIDTH == 1;
    }

    /** The entry on the page of a toggle slot. */
    static int entryOf(int slot) {
        return (slot / ROW_WIDTH) * ENTRIES_PER_ROW + (slot % ROW_WIDTH) / ENTRY_WIDTH;
    }

    /** The lines of this drill: its deposits in reach, as the last search left them, with a label for every ore. */
    private List<Line> lines() {
        if (placed.mining() == null) {
            return List.of();
        }
        List<Deposit> deposits = placed.mining().listed();
        return DrillResourceRows.of(deposits, labels, placed.selection());
    }

    private static ItemStack icon(Line line) {
        Material material = Material.matchMaterial(line.icon());
        if (material == null || !material.isBlock()) {
            material = Material.STONE;
        }
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(line.name()));
            meta.lore(Text.lore(line.lore()));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack toggle(Line line) {
        boolean mining = line.toggle() == DrillResourceRows.Toggle.MINING;
        return mining
                ? named(Material.GREEN_DYE, "&aMining", "&7Click to ignore this ore in every deposit.")
                : named(Material.GRAY_DYE, "&7Ignoring", "&7Click to mine this ore again.");
    }

    private static ItemStack named(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(name));
            meta.lore(Text.lore(List.of(lore)));
            item.setItemMeta(meta);
        }
        return item;
    }
}
