package io.corefactory.spigot.machine;

import io.corefactory.core.config.Messages;
import io.corefactory.core.drill.DrillBox;
import io.corefactory.core.drill.DrillDefinition;
import io.corefactory.core.drill.DrillReport;
import io.corefactory.core.drill.DrillRules;
import io.corefactory.core.drill.PetroleumVolume;
import io.corefactory.core.geology.GeologyService;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.ledger.MiningLedger;
import io.corefactory.spigot.geology.GeologyWorker;
import io.corefactory.spigot.gui.MenuHolder;
import io.corefactory.spigot.machine.DrillResourceRows.Line;
import io.corefactory.spigot.machine.DrillResourceRows.OreLabel;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.logging.Logger;

/**
 * The resources menu of one drill: a row per ore that is in range or was mined by this drill. Each row has the ore's icon
 * (its name, the counts in its lore) and a toggle, a green dye for mining or a grey dye for ignoring. A click on the toggle
 * flips the ore and saves the drill. Five rows show a page; the bottom row holds the page arrows and the button back to the
 * drill's menu. Nothing in this menu can be moved: {@link io.corefactory.spigot.gui.GuiListener} cancels every click.
 */
public final class DrillResourcesMenu implements MenuHolder {

    static final int SIZE = 54;
    static final int ROWS_PER_PAGE = 5;
    static final int ROW_WIDTH = 9;
    static final int TOGGLE_COLUMN = 2;
    static final int PREVIOUS = 45;
    static final int BACK = 49;
    static final int NEXT = 53;

    private final Inventory inventory;
    private final PlacedMachine placed;
    private final GeologyWorker geology;
    private final MiningLedger ledger;
    private final Runnable save;
    private final Logger log;
    private final int depositIndex;
    private List<Line> lines = List.of();
    private int page;

    /**
     * @param placed the drill whose resources are shown
     * @param save   writes the machines file, after a toggle
     */
    DrillResourcesMenu(PlacedMachine placed, GeologyWorker geology, MiningLedger ledger, Runnable save, Logger log) {
        this.placed = placed;
        this.geology = geology;
        this.ledger = ledger;
        this.save = save;
        this.log = log;
        this.depositIndex = geology.service().indexOf(DrillRules.DEPOSIT);
        this.inventory = Bukkit.createInventory(this, SIZE, Messages.color("&8Drill resources"));
        render();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Draws the current page from the drill's box, its ledger and its selection. Called on open, on a toggle and periodically. */
    void render() {
        lines = lines();
        int pages = pageCount();
        page = Math.min(page, pages - 1);
        ItemStack blank = MachineLayout.decorItem(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot = 0; slot < PREVIOUS; slot++) {
            inventory.setItem(slot, blank);
        }
        if (lines.isEmpty()) {
            inventory.setItem(0, named(Material.PAPER, "&7No ores in range",
                    "&7Nothing this drill may mine lies in its box, and it has mined none."));
        }
        for (int row = 0; row < ROWS_PER_PAGE; row++) {
            int index = page * ROWS_PER_PAGE + row;
            if (index >= lines.size()) {
                continue;
            }
            Line line = lines.get(index);
            inventory.setItem(row * ROW_WIDTH, icon(line));
            inventory.setItem(row * ROW_WIDTH + TOGGLE_COLUMN, toggle(line));
        }
        ItemStack bottom = MachineLayout.decorItem(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = PREVIOUS; slot <= NEXT; slot++) {
            inventory.setItem(slot, bottom);
        }
        if (page > 0) {
            inventory.setItem(PREVIOUS, named(Material.ARROW, "&7Previous page", "&7Show the ores before these."));
        }
        inventory.setItem(BACK, named(Material.OAK_DOOR, "&eBack to the drill", "&7Return to the drill's menu."));
        if (page < pages - 1) {
            inventory.setItem(NEXT, named(Material.ARROW, "&7Next page", "&7Show the ores after these."));
        }
    }

    @Override
    public void click(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        if (slot == BACK) {
            placed.menu().open(player);
            return;
        }
        if (slot == PREVIOUS && page > 0) {
            page--;
            render();
            return;
        }
        if (slot == NEXT && page < pageCount() - 1) {
            page++;
            render();
            return;
        }
        if (slot < 0 || slot >= PREVIOUS || slot % ROW_WIDTH != TOGGLE_COLUMN) {
            return;
        }
        int index = page * ROWS_PER_PAGE + slot / ROW_WIDTH;
        if (index >= lines.size()) {
            return;
        }
        placed.selection().toggle(lines.get(index).oreId());
        save.run();
        render();
    }

    private int pageCount() {
        return Math.max(1, (lines.size() + ROWS_PER_PAGE - 1) / ROWS_PER_PAGE);
    }

    /** The lines of this drill: its report, with a label for every ore of the server. */
    private List<Line> lines() {
        Location site = placed.block();
        World world = site.getWorld();
        if (world == null) {
            return List.of();
        }
        DrillDefinition definition = placed.drill().definition();
        DrillBox box = DrillBox.around(site.getBlockX(), site.getBlockY(), site.getBlockZ(), definition.spec());
        GeologyService service = geology.service();
        IntPredicate targets = DrillRules.targets(definition.kind(), depositIndex);
        int depositFull = depositIndex < 0 ? PetroleumVolume.DEFAULT_VOLUME
                : PetroleumVolume.volumeOf(service.ore(depositIndex).volume());
        Map<String, Long> inRange = DrillSite.inRange(world, box, service, targets, DrillRules.DEPOSIT, depositFull);
        List<String> order = new ArrayList<>();
        Map<String, OreLabel> labels = new HashMap<>();
        for (int index = 0; index < service.ores().size(); index++) {
            OreDefinition ore = service.ore(index);
            labels.put(ore.id(), new OreLabel(ore.name(), ore.host(), ore.id().equals(DrillRules.DEPOSIT)));
            if (targets.test(index)) {
                order.add(ore.id());
            }
        }
        return DrillResourceRows.of(DrillReport.build(order, inRange, minedBy(site), placed.selection()), labels);
    }

    private Map<String, Long> minedBy(Location site) {
        try {
            return ledger.minedBy(DrillSite.keyOf(site));
        } catch (RuntimeException e) {
            log.warning("Could not read what the drill at " + site.getBlockX() + " " + site.getBlockY() + " "
                    + site.getBlockZ() + " has mined: " + e.getMessage());
            return Map.of();
        }
    }

    private static ItemStack icon(Line line) {
        Material material = Material.matchMaterial(line.icon());
        if (material == null || !material.isBlock()) {
            material = Material.STONE;
        }
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(line.name()));
            meta.setLore(line.lore().stream().map(Messages::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack toggle(Line line) {
        boolean mining = line.toggle() == DrillResourceRows.Toggle.MINING;
        return mining
                ? named(Material.GREEN_DYE, "&aMining", "&7Click to ignore this ore.")
                : named(Material.GRAY_DYE, "&7Ignoring", "&7Click to mine this ore.");
    }

    private static ItemStack named(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(name));
            meta.setLore(List.of(Messages.color(lore)));
            item.setItemMeta(meta);
        }
        return item;
    }
}
