package io.corefactory.paper.gui;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.geology.OreDefinition;
import io.corefactory.core.process.Machine;
import io.corefactory.core.research.ResearchService;
import io.corefactory.core.research.ResearchTree;
import io.corefactory.core.text.Text;
import io.corefactory.paper.research.ResearchHints;
import io.corefactory.paper.gui.catalog.CatalogCategory;
import io.corefactory.paper.gui.catalog.CatalogEntry;
import io.corefactory.paper.gui.catalog.CatalogEntry.Kind;
import io.corefactory.paper.gui.catalog.CatalogModel;
import io.corefactory.paper.gui.catalog.MechanicFamily;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The catalogue: a paged, categorised browser of every TNT type, component, machine, ore block and research pack.
 *
 * <p>Row one holds a tab per category, and the tab of the open category is enchanted. When there are more categories
 * than fit in the row, its outer slots are arrows that scroll the tabs, seven at a time. Rows one to four hold 36
 * entries a page. The bottom row holds Close, the page arrows and a page indicator. A left click gives one copy, when
 * the player may give items (the {@code corefactory.give} permission, which operators have) and research has unlocked the
 * item; a right click opens its recipe. A click that does something plays a short sound to the clicking player, see
 * {@link GuiSounds}. Read-only: {@link GuiListener} cancels every click.
 */
public final class CatalogGui implements MenuHolder {

    public static final Component TITLE = Text.parse("&8CoreFactory Catalogue");

    private static final int SIZE = 54;
    private static final String PERMISSION_GIVE = "corefactory.give";
    private static final int TABS = 9;
    private static final int TAB_PREVIOUS = 0;
    private static final int TAB_NEXT = 8;
    private static final int FIRST_ENTRY = 9;
    private static final int PER_PAGE = 36;
    private static final int CLOSE = 45;
    private static final int PREVIOUS = 47;
    private static final int INDICATOR = 49;
    private static final int NEXT = 51;

    private final CoreFactoryCore core;
    private final List<CatalogCategory> categories;
    private final Inventory inventory;
    private int category;
    private int page;
    private int firstTab;

    private CatalogGui(CoreFactoryCore core) {
        this.core = core;
        List<String> machines = core.processes().machines().stream().map(Machine::id).toList();
        List<String> ores = core.ores().stream().map(OreDefinition::id).toList();
        this.categories = CatalogModel.build(core.definitions(), core.componentDefinitions().keySet(), machines, ores,
                core.itemComponentIds());
        this.inventory = Bukkit.createInventory(this, SIZE, TITLE);
    }

    /** A new catalogue, on its first category and page. */
    public static CatalogGui create(CoreFactoryCore core) {
        return new CatalogGui(core);
    }

    /** Opens the catalogue on its first category. */
    public void open(Player player) {
        render(player);
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void click(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        if (slot < 0) {
            return;
        }
        if (slot < TABS) {
            clickTab(slot, player);
            return;
        }
        if (slot == CLOSE) {
            GuiSounds.back(player);
            player.closeInventory();
        } else if (slot == PREVIOUS && page > 0) {
            show(category, page - 1, player);
            GuiSounds.page(player);
        } else if (slot == NEXT && page < pages() - 1) {
            show(category, page + 1, player);
            GuiSounds.page(player);
        } else if (slot >= FIRST_ENTRY && slot < FIRST_ENTRY + PER_PAGE) {
            int index = page * PER_PAGE + slot - FIRST_ENTRY;
            if (index < entries().size()) {
                entryClick(entries().get(index), event.getClick(), player);
            }
        }
    }

    private void clickTab(int slot, Player player) {
        if (tabsPaged() && slot == TAB_PREVIOUS && firstTab > 0) {
            firstTab--;
            render(player);
            GuiSounds.page(player);
        } else if (tabsPaged() && slot == TAB_NEXT && firstTab < lastFirstTab()) {
            firstTab++;
            render(player);
            GuiSounds.page(player);
        } else if (slot >= firstTabSlot() && slot - firstTabSlot() < tabsPerBar()
                && firstTab + slot - firstTabSlot() < categories.size()) {
            show(firstTab + slot - firstTabSlot(), 0, player);
            GuiSounds.click(player);
        }
    }

    private boolean tabsPaged() {
        return categories.size() > TABS;
    }

    private int tabsPerBar() {
        return tabsPaged() ? TABS - 2 : TABS;
    }

    private int firstTabSlot() {
        return tabsPaged() ? 1 : 0;
    }

    private int lastFirstTab() {
        return Math.max(0, categories.size() - tabsPerBar());
    }

    private void show(int nextCategory, int nextPage, Player player) {
        category = nextCategory;
        page = nextPage;
        render(player);
    }

    private void render(Player viewer) {
        inventory.clear();
        page = Math.min(page, pages() - 1);
        for (int i = 0; i < tabsPerBar() && firstTab + i < categories.size(); i++) {
            inventory.setItem(firstTabSlot() + i, tab(firstTab + i));
        }
        if (tabsPaged() && firstTab > 0) {
            inventory.setItem(TAB_PREVIOUS, labelled(Material.ARROW, "&7Previous categories"));
        }
        if (tabsPaged() && firstTab < lastFirstTab()) {
            inventory.setItem(TAB_NEXT, labelled(Material.ARROW, "&7Next categories"));
        }
        List<CatalogEntry> entries = entries();
        int from = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE && from + i < entries.size(); i++) {
            inventory.setItem(FIRST_ENTRY + i, item(entries.get(from + i), viewer));
        }
        ItemStack filler = filler();
        for (int slot = CLOSE; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        inventory.setItem(CLOSE, labelled(Material.BARRIER, "&cClose"));
        if (page > 0) {
            inventory.setItem(PREVIOUS, labelled(Material.ARROW, "&7Previous page"));
        }
        inventory.setItem(INDICATOR, indicator());
        if (page < pages() - 1) {
            inventory.setItem(NEXT, labelled(Material.ARROW, "&7Next page"));
        }
    }

    private List<CatalogEntry> entries() {
        return categories.get(category).entries();
    }

    private int pages() {
        return Math.max(1, (entries().size() + PER_PAGE - 1) / PER_PAGE);
    }

    private ItemStack tab(int index) {
        CatalogCategory tab = categories.get(index);
        boolean open = index == category;
        ItemStack item = tab.icon().flatMap(this::stackOf).orElseGet(() -> new ItemStack(Material.PAPER));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(Text.item((open ? "&a&l" : "&f") + tab.title()));
        meta.lore(Text.lore(List.of("&7" + tab.entries().size() + " items",
                open ? "&aShowing now" : "&eClick to open")));
        if (open) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack indicator() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item("&fPage " + (page + 1) + " of " + pages()));
            meta.lore(Text.lore(List.of("&7" + entries().size() + " items in "
                    + categories.get(category).title())));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack item(CatalogEntry entry, Player viewer) {
        Optional<ItemStack> stack = stackOf(entry);
        if (stack.isEmpty()) {
            return labelled(Material.BARRIER, "&c" + entry.id() + " is not defined");
        }
        ItemStack item = stack.get();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<Component> lore = new ArrayList<>(meta.lore() == null ? List.of() : meta.lore());
        lore.add(Text.item(""));
        lore.add(Text.item("&8Kind: &7" + kindLabel(entry.kind())));
        if (entry.kind() == Kind.TNT) {
            addTntLines(entry.id(), lore);
        }
        addResearchLine(entry.id(), viewer, lore);
        lore.add(clickLine(entry.kind(), viewer));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * The line that says what a click does. A player who may take items reads what a left click gives. A player who may not is told the
     * left click is for administrators, so the left click does not look like a broken button.
     */
    private static Component clickLine(Kind kind, Player viewer) {
        if (viewer.hasPermission(PERMISSION_GIVE)) {
            return Text.item(kind == Kind.ORE
                    ? "&eLeft click: &7get one."
                    : "&eLeft click: &7get one. &eRight click: &7recipe.");
        }
        return Text.item(kind == Kind.ORE
                ? "&8Left click is for administrators."
                : "&8Left click is for administrators. Right click shows the recipe.");
    }

    private void addTntLines(String id, List<Component> lore) {
        core.definition(id).ifPresent(definition -> {
            lore.add(Text.item("&8Family: &7" + MechanicFamily.of(definition.mechanic()).title()));
            core.mechanicOf(definition).ifPresent(mechanic ->
                    lore.add(Text.item("&8Mechanic: &7" + mechanic.summary())));
        });
    }

    private void addResearchLine(String id, Player viewer, List<Component> lore) {
        ResearchService research = core.research();
        Optional<ResearchTree.Node> locked = research.nodeFor(viewer.getUniqueId(), id);
        if (locked.isPresent()) {
            lore.add(Text.item("&cRequires research: &f" + locked.get().name()));
            return;
        }
        ResearchTree tree = research.tree();
        tree.gatesOf(id).stream()
                .flatMap(node -> tree.node(node).stream())
                .filter(node -> research.isFinished(viewer.getUniqueId(), node.id()))
                .findFirst()
                .ifPresent(node -> lore.add(Text.item("&aResearched: &f" + node.name())));
    }

    private void entryClick(CatalogEntry entry, ClickType click, Player player) {
        if (click == ClickType.RIGHT) {
            if (entry.kind() == Kind.ORE || !RecipeGui.open(core, player, entry.id())) {
                player.sendMessage(Text.parse("&7No recipe for &f" + entry.id() + "&7."));
            } else {
                GuiSounds.click(player);
            }
            return;
        }
        if (click != ClickType.LEFT) {
            return;
        }
        if (!player.hasPermission(PERMISSION_GIVE)) {
            player.sendMessage(Text.parse("&cOnly administrators can take items from the catalogue. "
                    + "&7Right-click an entry to see its recipe."));
            return;
        }
        Optional<ResearchTree.Node> locked = core.research().nodeFor(player.getUniqueId(), entry.id());
        if (locked.isPresent()) {
            player.sendMessage(ResearchHints.requires(locked.get()));
            return;
        }
        Optional<ItemStack> item = stackOf(entry);
        if (item.isEmpty()) {
            return;
        }
        player.getInventory().addItem(item.get()).values()
                .forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        GuiSounds.take(player);
    }

    private Optional<ItemStack> stackOf(CatalogEntry entry) {
        return switch (entry.kind()) {
            case TNT -> core.createTntItem(entry.id(), 1);
            case COMPONENT, MACHINE, RESEARCH_PACK -> core.createComponentItem(entry.id(), 1);
            case ORE -> core.createOreItem(entry.id(), 1);
        };
    }

    private static String kindLabel(Kind kind) {
        return switch (kind) {
            case TNT -> "TNT";
            case COMPONENT -> "Component";
            case MACHINE -> "Machine";
            case ORE -> "Ore";
            case RESEARCH_PACK -> "Research pack";
        };
    }

    private static ItemStack filler() {
        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(" "));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static ItemStack labelled(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(name));
            item.setItemMeta(meta);
        }
        return item;
    }
}
