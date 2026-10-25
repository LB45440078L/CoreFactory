package io.corefactory.paper.machine;

import io.corefactory.core.CoreFactoryCore;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Machine;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.ProcessEngine;
import io.corefactory.core.process.Stack;
import io.corefactory.core.text.Text;
import io.corefactory.paper.gui.MoveDecision;
import io.corefactory.paper.gui.SlotMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.DragType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The menu of one placed machine. The menu is a view of the machine's barrel, which holds the items: the engine, the
 * hoppers and the menu all read and write the barrel. Each item slot of the menu keeps what the barrel held when it was last
 * copied. Copying the barrel into the menu skips a slot the player has changed; writing the menu back (push) writes those
 * slots into the barrel. Input slots take only what some process of the machine takes; fuel slots take fuel; output slots
 * only give items up. Nothing else in the menu can be moved.
 */
public class MachineMenu implements SlotMenu {

    private static final Set<InventoryAction> TAKE = Set.of(InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_ONE, InventoryAction.PICKUP_SOME, InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.DROP_ONE_SLOT, InventoryAction.DROP_ALL_SLOT);
    private static final Set<InventoryAction> PUT = Set.of(InventoryAction.PLACE_ALL, InventoryAction.PLACE_SOME,
            InventoryAction.PLACE_ONE, InventoryAction.SWAP_WITH_CURSOR);

    private final ProcessCatalog catalog;
    private final ItemRefs refs;
    private final Machine machine;
    private final MachineLayout layout;
    private final Barrel barrel;
    private final Set<ItemRef> fuels;
    private final MachineSlots slots;
    private final Inventory inventory;
    private final int[] viewSlot = new int[MachineSlots.BARREL_SIZE];
    private final ItemStack[] synced = new ItemStack[MachineSlots.BARREL_SIZE];
    /** The barrel inventory the last push read, or null when that push found none. Its edits are written on unload. */
    private Inventory lastBarrel;
    private final List<Stack> inputViews;
    private final List<Stack> outputViews;
    private final List<Stack> fuelViews;
    private int shownFilled = -1;
    private String shownStatus = "";
    private ItemStack shownInfo;
    private Boolean shownLit;
    private Consumer<Player> button = player -> { };

    /**
     * @param core    the core, which creates the component items
     * @param catalog the processes, which say what each input may be
     * @param machine the machine kind
     * @param layout  the inventory layout
     * @param site    the host block, which is the machine's barrel
     */
    public MachineMenu(CoreFactoryCore core, ProcessCatalog catalog, Machine machine, MachineLayout layout, Location site) {
        this(core, catalog, machine, layout, site, new MachineSlots(layout.inputSlots().size(),
                layout.outputSlots().size(), layout.fuelSlots().size()), ProcessEngine.FUEL);
    }

    /**
     * @param slots the barrel order of the machine, which must match the layout's slot counts
     * @param fuels the items the fuel slots take
     */
    public MachineMenu(CoreFactoryCore core, ProcessCatalog catalog, Machine machine, MachineLayout layout, Location site,
                       MachineSlots slots, Set<ItemRef> fuels) {
        this.catalog = catalog;
        this.refs = new ItemRefs(core);
        this.machine = machine;
        this.layout = layout;
        this.barrel = new Barrel(site, slots.used());
        this.fuels = Set.copyOf(fuels);
        this.slots = slots;
        this.inventory = Bukkit.createInventory(this, layout.size(), layout.title());
        layout.decorate(inventory);
        Arrays.fill(viewSlot, -1);
        for (MachineSlots.Role role : MachineSlots.Role.values()) {
            List<Integer> layoutSlots = layoutSlots(role);
            for (int i = 0; i < layoutSlots.size(); i++) {
                viewSlot[slots.barrelIndex(role, i)] = layoutSlots.get(i);
            }
        }
        this.inputViews = stacks(MachineSlots.Role.INPUT);
        this.outputViews = stacks(MachineSlots.Role.OUTPUT);
        this.fuelViews = stacks(MachineSlots.Role.FUEL);
        resetView();
        refresh(0, 0, false);
        showFirebox(false);
    }

    private List<Integer> layoutSlots(MachineSlots.Role role) {
        return switch (role) {
            case INPUT -> layout.inputSlots();
            case OUTPUT -> layout.outputSlots();
            case FUEL -> layout.fuelSlots();
        };
    }

    private List<Stack> stacks(MachineSlots.Role role) {
        List<Stack> views = new ArrayList<>();
        for (int index : slots.indexes(role)) {
            views.add(new BarrelSlot(barrel::inventory, refs, index));
        }
        return List.copyOf(views);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** The host block this menu belongs to. */
    protected Location site() {
        return barrel.site();
    }

    public Machine machine() {
        return machine;
    }

    public MachineLayout layout() {
        return layout;
    }

    /** The input slots as process stacks, in input order. */
    public List<Stack> inputs() {
        return inputViews;
    }

    public List<Stack> outputs() {
        return outputViews;
    }

    public List<Stack> fuel() {
        return fuelViews;
    }

    protected ItemRefs refs() {
        return refs;
    }

    Barrel barrel() {
        return barrel;
    }

    MachineSlots slots() {
        return slots;
    }

    /** The barrel slot of a layout slot that is an input, output or fuel slot, or -1 for any other slot. */
    int barrelIndexOf(int layoutSlot) {
        MachineLayout.Kind kind = layout.kindAt(layoutSlot);
        MachineSlots.Role role;
        if (kind == MachineLayout.Kind.INPUT) {
            role = MachineSlots.Role.INPUT;
        } else if (kind == MachineLayout.Kind.OUTPUT) {
            role = MachineSlots.Role.OUTPUT;
        } else if (kind == MachineLayout.Kind.FUEL) {
            role = MachineSlots.Role.FUEL;
        } else {
            return -1;
        }
        int index = layout.indexAt(layoutSlot);
        return index < 0 || index >= slots.count(role) ? -1 : slots.barrelIndex(role, index);
    }

    /** Sets what a click on the layout's resources button does. Nothing is set for a layout without one. */
    public void onButton(Consumer<Player> handler) {
        this.button = handler;
    }

    /** Opens the menu for a player, after the menu shows the barrel. */
    public void open(Player player) {
        sync();
        player.openInventory(inventory);
    }

    /** True while a player has this menu open. */
    public boolean isOpen() {
        return !inventory.getViewers().isEmpty();
    }

    /** Closes the menu for everyone who has it open. */
    public void closeViewers() {
        for (HumanEntity viewer : List.copyOf(inventory.getViewers())) {
            viewer.closeInventory();
        }
    }

    /** True when the player changed an item slot that has not been written to the barrel yet. */
    public boolean hasPending() {
        Optional<Inventory> held = barrel.inventory();
        for (int b = 0; b < slots.used(); b++) {
            if (held.isPresent() && !Barrel.has(held.get(), b)) {
                continue;
            }
            if (!Objects.equals(shown(viewSlot[b]), synced[b])) {
                return true;
            }
        }
        return false;
    }

    /** True when an item of this reference may go into a slot of the given kind. */
    public boolean accepts(ItemRef ref, MachineLayout.Kind kind) {
        return switch (kind) {
            case INPUT -> catalog.acceptsInput(machine.id(), ref);
            case FUEL -> !fuelViews.isEmpty() && fuels.contains(ref);
            default -> false;
        };
    }

    /** Writes the player's changes into the barrel, then copies the barrel into the menu. */
    public void sync() {
        push();
        pull();
    }

    /** Copies the barrel into the menu, except for the slots the player has changed and not yet written back. */
    public void pull() {
        Optional<Inventory> held = barrel.inventory();
        if (held.isEmpty()) {
            return;
        }
        for (int b = 0; b < slots.used(); b++) {
            int slot = viewSlot[b];
            if (!Barrel.has(held.get(), b) || !Objects.equals(shown(slot), synced[b])) {
                continue;
            }
            ItemStack actual = normalized(Barrel.itemAt(held.get(), b));
            if (Objects.equals(actual, synced[b])) {
                continue;
            }
            synced[b] = actual;
            inventory.setItem(slot, actual == null ? hint(b) : actual.clone());
        }
    }

    /** Writes the slots the player changed into the barrel. */
    public void push() {
        Optional<Inventory> held = barrel.inventory();
        lastBarrel = held.orElse(null);
        held.ifPresent(this::writeEdits);
    }

    /** Writes the slots the player changed into the given barrel inventory. */
    private void writeEdits(Inventory held) {
        for (int b = 0; b < slots.used(); b++) {
            int slot = viewSlot[b];
            ItemStack shown = shown(slot);
            if (!Barrel.has(held, b) || Objects.equals(shown, synced[b])) {
                continue;
            }
            ItemStack current = normalized(Barrel.itemAt(held, b));
            ItemStack target = Objects.equals(current, synced[b]) ? shown : reconcile(synced[b], shown, current);
            Barrel.setItemAt(held, b, target);
            synced[b] = target;
            inventory.setItem(slot, target == null ? hint(b) : target.clone());
        }
    }

    /**
     * What a barrel slot holds once the player's edit is written, when the barrel changed under the edit. The edit is applied
     * to what the barrel holds now, so no item is made or lost. A different item in the barrel is dropped at the block.
     */
    private ItemStack reconcile(ItemStack base, ItemStack shown, ItemStack current) {
        ItemStack edit = shown != null ? shown : base;
        ItemRef editRef = refs.of(edit).orElse(null);
        if (current != null && editRef != null && !editRef.equals(refs.of(current).orElse(null))) {
            barrel.drop(current);
            return shown;
        }
        ItemStack template = current != null ? current : edit;
        if (template == null) {
            return null;
        }
        int total = count(current) + count(shown) - count(base);
        int limit = refs.of(template).map(refs::limit).orElse(1);
        if (total > limit) {
            ItemStack overflow = template.clone();
            overflow.setAmount(total - limit);
            barrel.drop(overflow);
            total = limit;
        }
        if (total <= 0) {
            return null;
        }
        ItemStack result = template.clone();
        result.setAmount(total);
        return result;
    }

    /** Empties the barrel's items into the menu's hints, for when the menu is first built. */
    private void resetView() {
        for (int b = 0; b < slots.used(); b++) {
            inventory.setItem(viewSlot[b], hint(b));
            synced[b] = null;
        }
    }

    /**
     * Draws the hints of empty slots, the progress cells and the status item. Called every tick by the machine ticker.
     *
     * @param progress the ticks the current pass has run
     * @param total    the ticks one pass takes, zero when idle
     * @param blocked  true when a finished pass waits for room in the outputs
     */
    public void refresh(int progress, int total, boolean blocked) {
        List<Integer> cells = layout.progressSlots();
        int filled = total <= 0 ? 0 : Math.min(cells.size(), progress * cells.size() / total);
        if (filled != shownFilled) {
            for (int i = 0; i < cells.size(); i++) {
                inventory.setItem(cells.get(i), layout.progressCell(i < filled));
            }
            shownFilled = filled;
        }
        if (layout.statusSlot() >= 0) {
            int percent = total <= 0 ? 0 : progress * 100 / total;
            String key = blocked ? "blocked" : total > 0 ? "run" + percent : "idle";
            if (!key.equals(shownStatus)) {
                inventory.setItem(layout.statusSlot(), status(blocked, total > 0, percent));
                shownStatus = key;
            }
        }
    }

    /** Shows an item in the layout's info slot, when the layout has one. Only a changed item is written. */
    public void showInfo(ItemStack item) {
        int slot = layout.infoSlot();
        if (slot < 0 || item.equals(shownInfo)) {
            return;
        }
        inventory.setItem(slot, item);
        shownInfo = item.clone();
    }

    /** Shows the firebox indicator lit or unlit, when the layout has one. Only a change is written. */
    public void showFirebox(boolean lit) {
        int slot = layout.fireboxSlot();
        if (slot < 0 || Objects.equals(shownLit, lit)) {
            return;
        }
        inventory.setItem(slot, layout.firebox(lit));
        shownLit = lit;
    }

    /** The items a broken machine gives back: everything in its barrel. */
    public List<ItemStack> drops() {
        List<ItemStack> items = new ArrayList<>();
        Optional<Inventory> held = barrel.inventory();
        if (held.isEmpty()) {
            return items;
        }
        for (int b = 0; b < slots.used(); b++) {
            ItemStack item = normalized(Barrel.itemAt(held.get(), b));
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    /** Empties the barrel's machine slots. Called once the items have been dropped, so the block gives nothing more. */
    public void clearBarrel() {
        Optional<Inventory> held = barrel.inventory();
        if (held.isEmpty()) {
            return;
        }
        for (int b = 0; b < slots.used(); b++) {
            Barrel.setItemAt(held.get(), b, null);
            synced[b] = null;
            inventory.setItem(viewSlot[b], hint(b));
        }
    }

    /**
     * Writes the player's pending edits into the barrel that the menu last read, then forgets the barrel, when its chunk unloads.
     * The edits go first: once the barrel is forgotten, its items are read again only after the chunk loads, so an edit left
     * here would be lost.
     */
    void forgetBarrel() {
        if (lastBarrel != null) {
            writeEdits(lastBarrel);
        }
        lastBarrel = null;
        barrel.forget();
    }

    @Override
    public void slotClick(InventoryClickEvent event, Player player) {
        int raw = event.getRawSlot();
        if (raw < 0) {
            return;
        }
        sync();
        InventoryAction action = event.getAction();
        if (action == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        if (raw >= inventory.getSize()) {
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
                shiftIn(event, player);
                pull();
            }
            return;
        }
        MachineLayout.Kind kind = layout.kindAt(raw);
        if (kind == MachineLayout.Kind.BUTTON) {
            event.setCancelled(true);
            button.accept(player);
            return;
        }
        if (kind != MachineLayout.Kind.INPUT && kind != MachineLayout.Kind.OUTPUT && kind != MachineLayout.Kind.FUEL) {
            event.setCancelled(true);
            return;
        }
        if (!Objects.equals(normalized(event.getCurrentItem()), shown(raw))) {
            event.setCancelled(true);
            return;
        }
        if (kind == MachineLayout.Kind.OUTPUT) {
            takeClick(event, raw);
        } else {
            putClick(event, player, raw);
        }
    }

    /**
     * A drag over the menu's slots. A drag that covers an output, a button or any other slot is refused whole. A drag over input
     * and fuel slots that take the item places it into each covered slot, sharing the cursor's items among them, and the cursor
     * keeps the rest. A drag that covers only the player's inventory is left to the server.
     */
    @Override
    public void slotDrag(InventoryDragEvent event, Player player) {
        int size = inventory.getSize();
        List<Integer> covered = event.getRawSlots().stream().filter(raw -> raw < size).sorted().toList();
        if (covered.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        ItemStack cursor = event.getOldCursor();
        ItemRef ref = refs.of(cursor).orElse(null);
        for (int raw : covered) {
            MachineLayout.Kind kind = layout.kindAt(raw);
            boolean accepted = ref != null && (kind == MachineLayout.Kind.INPUT || kind == MachineLayout.Kind.FUEL)
                    && accepts(ref, kind);
            if (!allowed(moveSlot(kind), accepted)) {
                return;
            }
        }
        sync();
        int share = event.getType() == DragType.SINGLE ? 1 : cursor.getAmount() / covered.size();
        int left = cursor.getAmount();
        for (int raw : covered) {
            ItemStack existing = inventory.getItem(raw);
            ItemRef inSlot = refs.of(existing).orElse(null);
            if (inSlot != null && !inSlot.equals(ref)) {
                continue;
            }
            int have = inSlot == null ? 0 : existing.getAmount();
            int add = Math.min(Math.min(share, left), refs.limit(ref) - have);
            if (add <= 0) {
                continue;
            }
            ItemStack placed = cursor.clone();
            placed.setAmount(have + add);
            inventory.setItem(raw, placed);
            left -= add;
        }
        if (left > 0) {
            ItemStack rest = cursor.clone();
            rest.setAmount(left);
            player.setItemOnCursor(rest);
        } else {
            player.setItemOnCursor(null);
        }
    }

    /** A click on an input or fuel slot. The kind comes from the slot. */
    private void putClick(InventoryClickEvent event, Player player, int slot) {
        MachineLayout.Kind kind = layout.kindAt(slot);
        boolean hint = ItemRefs.isHint(inventory.getItem(slot));
        InventoryAction action = event.getAction();
        if (action == InventoryAction.HOTBAR_SWAP) {
            hotbarSwap(event, player, slot, kind);
            return;
        }
        if (TAKE.contains(action)) {
            if (hint) {
                event.setCancelled(true);
            }
            return;
        }
        if (!PUT.contains(action)) {
            event.setCancelled(true);
            return;
        }
        ItemRef cursor = refs.of(event.getCursor()).orElse(null);
        if (cursor == null || !accepts(cursor, kind)) {
            event.setCancelled(true);
        } else if (hint) {
            event.setCancelled(true);
            placeOnHint(event, player, slot, cursor, action);
        }
    }

    /** A click on an output slot: only taking is allowed. */
    private void takeClick(InventoryClickEvent event, int slot) {
        if (ItemRefs.isHint(inventory.getItem(slot)) || !TAKE.contains(event.getAction())) {
            event.setCancelled(true);
        }
    }

    /** Puts the cursor's items into a slot that shows only a hint, which the cursor replaces. */
    private void placeOnHint(InventoryClickEvent event, Player player, int slot, ItemRef ref, InventoryAction action) {
        ItemStack cursor = event.getCursor();
        int amount = action == InventoryAction.PLACE_ONE ? 1 : Math.min(cursor.getAmount(), refs.limit(ref));
        ItemStack placed = cursor.clone();
        placed.setAmount(amount);
        inventory.setItem(slot, placed);
        int left = cursor.getAmount() - amount;
        if (left > 0) {
            ItemStack rest = cursor.clone();
            rest.setAmount(left);
            player.setItemOnCursor(rest);
        } else {
            player.setItemOnCursor(null);
        }
    }

    /**
     * A number-key swap over an input or fuel slot. The held hotbar item goes into the slot and the slot's item goes to that
     * hotbar key, so nothing is made or lost. Any other swap, and one the slot does not take, is refused.
     */
    private void hotbarSwap(InventoryClickEvent event, Player player, int slot, MachineLayout.Kind kind) {
        int button = event.getHotbarButton();
        ItemStack held = SlotMoves.isHotbarKey(button) ? player.getInventory().getItem(button) : null;
        ItemRef ref = refs.of(held).orElse(null);
        boolean accepted = ref != null && accepts(ref, kind) && held.getAmount() <= refs.limit(ref);
        event.setCancelled(true);
        if (!SlotMoves.hotbarSwapAllowed(kind, button, accepted)) {
            return;
        }
        ItemStack old = normalized(inventory.getItem(slot));
        inventory.setItem(slot, held.clone());
        player.getInventory().setItem(button, old == null ? null : old.clone());
    }

    /**
     * Shift-click from the player's inventory: the stack goes into the first input or fuel slots that take it, and the player
     * keeps what did not fit. The insertion works on the menu's slot views and the player's stack is then set to what remains,
     * so the amount taken from the player is the amount the machine took.
     */
    private void shiftIn(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        ItemStack moving = player.getInventory().getItem(slot);
        ItemRef ref = refs.of(moving).orElse(null);
        if (ref == null) {
            return;
        }
        int limit = refs.limit(ref);
        int added;
        if (allowed(MoveDecision.Slot.INPUT, accepts(ref, MachineLayout.Kind.INPUT))) {
            added = SlotMoves.insert(inputViews, ref, moving.getAmount(), limit);
        } else if (allowed(MoveDecision.Slot.FUEL, accepts(ref, MachineLayout.Kind.FUEL))) {
            added = SlotMoves.insert(fuelViews, ref, moving.getAmount(), limit);
        } else {
            return;
        }
        if (added == 0) {
            return;
        }
        int left = moving.getAmount() - added;
        if (left == 0) {
            player.getInventory().setItem(slot, null);
        } else {
            ItemStack rest = moving.clone();
            rest.setAmount(left);
            player.getInventory().setItem(slot, rest);
        }
    }

    /** The item a menu slot shows, with a hint or an empty slot read as nothing. */
    private ItemStack shown(int slot) {
        return normalized(inventory.getItem(slot));
    }

    private ItemStack hint(int b) {
        MachineSlots.Role role = slots.roleAt(b).orElseThrow();
        return layout.hint(kindOf(role), slots.indexAt(b));
    }

    private static MachineLayout.Kind kindOf(MachineSlots.Role role) {
        return switch (role) {
            case INPUT -> MachineLayout.Kind.INPUT;
            case OUTPUT -> MachineLayout.Kind.OUTPUT;
            case FUEL -> MachineLayout.Kind.FUEL;
        };
    }

    /** True when the menu may put an item into a slot of this kind, by the rule of {@link MoveDecision}. */
    private static boolean allowed(MoveDecision.Slot slot, boolean accepted) {
        return MoveDecision.decide(MoveDecision.Holder.MACHINE, slot, accepted) == MoveDecision.Verdict.ALLOW;
    }

    private static MoveDecision.Slot moveSlot(MachineLayout.Kind kind) {
        return switch (kind) {
            case INPUT -> MoveDecision.Slot.INPUT;
            case FUEL -> MoveDecision.Slot.FUEL;
            case OUTPUT -> MoveDecision.Slot.OUTPUT;
            default -> MoveDecision.Slot.OTHER;
        };
    }

    private static ItemStack normalized(ItemStack item) {
        return ItemRefs.isEmpty(item) ? null : item.clone();
    }

    private static int count(ItemStack item) {
        return item == null ? 0 : item.getAmount();
    }

    private ItemStack status(boolean blocked, boolean running, int percent) {
        Material material;
        String name;
        String lore;
        if (blocked) {
            material = Material.RED_DYE;
            name = "&cOutput full";
            lore = "&7Take the products out to let the machine go on.";
        } else if (running) {
            material = Material.CLOCK;
            name = "&aRunning " + percent + "%";
            lore = "&7The machine works while you watch it.";
        } else {
            material = Material.GRAY_DYE;
            name = "&7Idle";
            lore = "&7Fill the inputs with a recipe to start.";
        }
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
