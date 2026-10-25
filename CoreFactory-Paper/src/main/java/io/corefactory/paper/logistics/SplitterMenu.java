package io.corefactory.paper.logistics;

import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.PortItems;
import io.corefactory.core.text.Text;
import io.corefactory.paper.gui.MoveDecision;
import io.corefactory.paper.gui.SlotMenu;
import io.corefactory.paper.machine.ItemRefs;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Optional;

/**
 * The filter panel of a splitter: two lists of up to {@link SplitterFilter#LIMIT} item types, a left one and a right one. A click on a
 * list slot with an item on the cursor adds the item's type to that list, and moves it off the other list if it is there. A click on
 * an entry with an empty cursor takes it off the list. A drag of an item over list slots adds its type to the list of each slot the
 * drag covers, and a shift-click on an item in the player's inventory adds its type to the first list with room. The panel never
 * moves real items: the entries are ghost copies of one item, and the cursor is left as it was. Every change is saved at once,
 * through the service, which rebuilds the network.
 */
public final class SplitterMenu implements SlotMenu {

    /** The size of the panel: six rows. */
    public static final int SIZE = 54;

    private static final int LEFT_LABEL = 9;
    private static final int RIGHT_LABEL = 27;
    private static final int INFO = 4;

    private final LogisticsDevice device;
    private final LogisticsService service;
    private final ItemRefs refs;
    private final Inventory inventory;

    /**
     * @param device  the splitter whose filter the panel shows
     * @param service the service that saves the filter
     * @param refs    converts items to stacks and back
     */
    public SplitterMenu(LogisticsDevice device, LogisticsService service, ItemRefs refs) {
        this.device = device;
        this.service = service;
        this.refs = refs;
        this.inventory = Bukkit.createInventory(this, SIZE, Text.parse("&6" + LogisticsText.name(LogisticsIds.Kind.SPLITTER,
                device.level()) + " filter"));
        refresh();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Shows the panel to a player. */
    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public void slotClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        int raw = event.getRawSlot();
        if (raw >= inventory.getSize()) {
            if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                shiftAdd(event.getCurrentItem(), event.isRightClick(), player);
            }
            return;
        }
        for (SplitterFilter.Side side : SplitterFilter.Side.values()) {
            int index = SplitterFilter.indexOf(side, raw);
            if (index >= 0) {
                listClick(side, index, event.getCursor(), player);
                return;
            }
        }
    }

    /**
     * A drag of one or more item stacks over list slots adds the dragged item's type to the list of each slot the drag covers,
     * once per list. The drag is cancelled: no item moves, and the cursor keeps what it held.
     */
    @Override
    public void slotDrag(InventoryDragEvent event, Player player) {
        event.setCancelled(true);
        Optional<Item> item = itemOf(event.getOldCursor());
        if (item.isEmpty()) {
            return;
        }
        for (SplitterFilter.Side side : SplitterFilter.Side.values()) {
            if (event.getRawSlots().stream().anyMatch(slot -> covers(side, slot))) {
                add(side, item.get(), player);
                break;
            }
        }
        refresh();
    }

    private void listClick(SplitterFilter.Side side, int index, ItemStack cursor, Player player) {
        SplitterFilter filter = device.filter();
        if (cursor != null && !cursor.getType().isAir()) {
            Optional<Item> item = itemOf(cursor);
            if (item.isEmpty()) {
                return;
            }
            add(side, item.get(), player);
        } else {
            Optional<Item> removed = filter.remove(side, index);
            if (removed.isPresent()) {
                service.setFilter(device, filter);
                player.sendMessage(Text.parse("&7Removed from the " + name(side) + " list: &f" + describe(removed.get())));
            }
        }
        refresh();
    }

    /**
     * A shift-click on an item in the player's inventory adds its type to the first list with room: the left list first for a
     * left-click, the right list first for a right-click. A type already on the first list is reported and not added again.
     */
    private void shiftAdd(ItemStack stack, boolean right, Player player) {
        Optional<Item> item = itemOf(stack);
        if (MoveDecision.decide(MoveDecision.Holder.SPLITTER_PANEL, MoveDecision.Slot.SPLITTER_LIST, item.isPresent())
                == MoveDecision.Verdict.DENY) {
            return;
        }
        List<SplitterFilter.Side> order = right
                ? List.of(SplitterFilter.Side.RIGHT, SplitterFilter.Side.LEFT)
                : List.of(SplitterFilter.Side.LEFT, SplitterFilter.Side.RIGHT);
        for (SplitterFilter.Side side : order) {
            if (add(side, item.get(), player) != SplitterFilter.Outcome.FULL) {
                break;
            }
        }
        refresh();
    }

    /**
     * Adds a type to a list, saves the filter when it changed, and tells the player what happened.
     *
     * @return what the filter did
     */
    private SplitterFilter.Outcome add(SplitterFilter.Side side, Item item, Player player) {
        SplitterFilter filter = device.filter();
        SplitterFilter.Outcome outcome = filter.add(side, item);
        switch (outcome) {
            case ADDED, MOVED -> {
                service.setFilter(device, filter);
                player.sendMessage(Text.parse(outcome == SplitterFilter.Outcome.MOVED
                        ? "&7Moved to the " + name(side) + " list: &f" + describe(item)
                        : "&7Added to the " + name(side) + " list: &f" + describe(item)));
            }
            case ALREADY_HERE -> player.sendMessage(Text.parse("&7That type is already on the " + name(side) + " list."));
            case FULL -> player.sendMessage(Text.parse("&cThe " + name(side) + " list is full: at most "
                    + SplitterFilter.LIMIT + " types."));
        }
        return outcome;
    }

    /** True when a slot of the drag is a slot of this list, which takes the dragged item. */
    private static boolean covers(SplitterFilter.Side side, int slot) {
        MoveDecision.Slot kind = SplitterFilter.indexOf(side, slot) >= 0 ? MoveDecision.Slot.SPLITTER_LIST
                : MoveDecision.Slot.SPLITTER_OTHER;
        return MoveDecision.decide(MoveDecision.Holder.SPLITTER_PANEL, kind, true) == MoveDecision.Verdict.ALLOW;
    }

    /** Redraws the panel: the two lists as ghost copies, and the labels. */
    void refresh() {
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, pane);
        }
        inventory.setItem(LEFT_LABEL, item(Material.LIME_STAINED_GLASS_PANE, "&aLeft list",
                List.of("&7Items on this list go left.", "&7Click a slot with an item on the cursor to add its type.")));
        inventory.setItem(RIGHT_LABEL, item(Material.ORANGE_STAINED_GLASS_PANE, "&6Right list",
                List.of("&7Items on this list go right.", "&7Items on neither list go straight.")));
        inventory.setItem(INFO, item(Material.HOPPER, "&6" + LogisticsText.name(LogisticsIds.Kind.SPLITTER, device.level()),
                List.of("&7Click an entry with an empty cursor to take it off its list.")));
        showList(SplitterFilter.Side.LEFT);
        showList(SplitterFilter.Side.RIGHT);
    }

    private void showList(SplitterFilter.Side side) {
        int[] slots = SplitterFilter.slots(side);
        List<Item> items = device.filter().items(side);
        for (int i = 0; i < slots.length; i++) {
            if (i < items.size()) {
                ItemStack ghost = refs.create(PortItems.refOf(items.get(i)), 1);
                inventory.setItem(slots[i], ghost == null ? item(Material.BARRIER, "&c" + describe(items.get(i)), List.of())
                        : ghost);
            } else {
                inventory.setItem(slots[i], null);
            }
        }
    }

    private Optional<Item> itemOf(ItemStack stack) {
        return refs.of(stack).map(PortItems::itemOf);
    }

    private static String name(SplitterFilter.Side side) {
        return side == SplitterFilter.Side.LEFT ? "left" : "right";
    }

    private static String describe(Item item) {
        return item.id();
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(name));
            meta.lore(Text.lore(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
