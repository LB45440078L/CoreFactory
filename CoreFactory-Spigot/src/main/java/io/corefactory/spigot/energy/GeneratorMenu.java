package io.corefactory.spigot.energy;

import io.corefactory.core.config.Messages;
import io.corefactory.core.energy.GeneratorFuel;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.spigot.gui.SlotMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The menu of one generator: one fuel slot, and a status row beside it. The fuel slot takes only the items of the generator's kind
 * (see {@link GeneratorItems#accepts}), from the cursor or a shift-click; a fuel kind's items can be taken back out. Every other slot
 * is decoration that cannot be moved. Only one player at a time has the menu open, so the slot is the store while it is open and
 * the device's count is the store while it is closed: {@link #absorb} moves the slot into the count when the menu closes.
 */
public final class GeneratorMenu implements SlotMenu {

    /** The size of the menu: three rows. */
    public static final int SIZE = 27;

    /** The slot that holds fuel. It is the only slot a player can put items into. */
    public static final int FUEL_SLOT = 11;

    private static final int LABEL_SLOT = 10;
    private static final int OUTPUT_SLOT = 13;
    private static final int PRODUCED_SLOT = 14;
    private static final int FUEL_INFO_SLOT = 15;
    private static final int STATUS_SLOT = 16;

    private static final Set<InventoryAction> TAKE = Set.of(InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_ONE, InventoryAction.PICKUP_SOME, InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.DROP_ONE_SLOT, InventoryAction.DROP_ALL_SLOT);
    private static final Set<InventoryAction> PUT = Set.of(InventoryAction.PLACE_ALL, InventoryAction.PLACE_SOME,
            InventoryAction.PLACE_ONE, InventoryAction.SWAP_WITH_CURSOR);

    private final GeneratorDevice device;
    private final GeneratorItems items;
    private final Inventory inventory;

    GeneratorMenu(GeneratorDevice device, GeneratorItems items) {
        this.device = device;
        this.items = items;
        this.inventory = Bukkit.createInventory(this, SIZE, Messages.color(GeneratorText.name(device.generatorKind())));
        decorate();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** True while a player has this menu open. */
    public boolean isOpen() {
        return !inventory.getViewers().isEmpty();
    }

    /** The fuel items in the slot, capped; zero for a kind with no fuel slot. */
    public int fuelCount() {
        if (!hasFuelSlot()) {
            return 0;
        }
        ItemStack slot = inventory.getItem(FUEL_SLOT);
        return slot == null ? 0 : GeneratorFuel.countOf(slot.getAmount());
    }

    /** Shows the menu to a player, with the fuel the device keeps in its slot. */
    void open(Player player) {
        writeFuel(device.fuelItems());
        refresh();
        player.openInventory(inventory);
    }

    /** Closes the menu for everyone who has it open. Their close events give the fuel back. */
    void closeViewers() {
        for (HumanEntity viewer : List.copyOf(inventory.getViewers())) {
            viewer.closeInventory();
        }
    }

    /** Writes the fuel count into the slot, when the slot shows a different count. */
    void writeFuel(int count) {
        if (!hasFuelSlot() || fuelCount() == count) {
            return;
        }
        ItemStack fuel = count > 0 ? items.fuel(device.generatorKind(), count).orElse(null) : null;
        inventory.setItem(FUEL_SLOT, fuel);
    }

    /** Moves the slot's fuel into the device's count, and empties the slot. Called when the menu closes and when the device goes. */
    public void absorb() {
        if (!hasFuelSlot()) {
            return;
        }
        device.setFuelItems(fuelCount());
        inventory.setItem(FUEL_SLOT, null);
    }

    /** Redraws the status row: output, power made, fuel and status. Called every few ticks while the menu is open. */
    void refresh() {
        GeneratorKind kind = device.generatorKind();
        GeneratorStatus status = device.status();
        inventory.setItem(OUTPUT_SLOT, item(Material.REDSTONE, GeneratorText.outputTitle(device.outputW()),
                List.of("&7The power the generator gives now.")));
        inventory.setItem(PRODUCED_SLOT, item(Material.GOLD_NUGGET, GeneratorText.producedTitle(device.totalWh()),
                List.of("&7Made since it was placed.")));
        if (kind.burnsFuel()) {
            inventory.setItem(FUEL_INFO_SLOT, item(Material.COAL, GeneratorText.fuelTitle(kind, device.fuel()),
                    GeneratorText.fuelLore(kind, device.remainingSeconds())));
        } else {
            inventory.setItem(FUEL_INFO_SLOT, item(Material.LIGHT_GRAY_STAINED_GLASS_PANE,
                    GeneratorText.fuelTitle(kind, 0), GeneratorText.fuelLore(kind, 0)));
        }
        inventory.setItem(STATUS_SLOT, item(statusMaterial(status), GeneratorText.statusTitle(status),
                List.of("&7The state of the generator now.")));
    }

    @Override
    public void slotClick(InventoryClickEvent event, Player player) {
        int raw = event.getRawSlot();
        if (raw < 0) {
            return;
        }
        InventoryAction action = event.getAction();
        if (raw >= SIZE) {
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
                shiftIn(event);
            } else if (action == InventoryAction.COLLECT_TO_CURSOR) {
                event.setCancelled(true);
            }
            return;
        }
        boolean fuelAction = TAKE.contains(action) || PUT.contains(action);
        if (raw != FUEL_SLOT || !hasFuelSlot() || !fuelAction) {
            event.setCancelled(true);
            return;
        }
        if (PUT.contains(action) && !items.accepts(device.generatorKind(), event.getCursor())) {
            event.setCancelled(true);
        }
    }

    /** A shift-click from the player's inventory: as much of the stack as the slot takes goes in, the rest stays. */
    private void shiftIn(InventoryClickEvent event) {
        ItemStack moving = event.getCurrentItem();
        if (!hasFuelSlot() || !items.accepts(device.generatorKind(), moving)) {
            return;
        }
        ItemStack slot = inventory.getItem(FUEL_SLOT);
        int have = slot == null ? 0 : slot.getAmount();
        int limit = Math.min(GeneratorFuel.CAP, moving.getMaxStackSize());
        int added = Math.max(0, Math.min(limit - have, moving.getAmount()));
        if (added == 0) {
            return;
        }
        ItemStack next = moving.clone();
        next.setAmount(have + added);
        inventory.setItem(FUEL_SLOT, next);
        int left = moving.getAmount() - added;
        if (left == 0) {
            event.setCurrentItem(null);
        } else {
            ItemStack rest = moving.clone();
            rest.setAmount(left);
            event.setCurrentItem(rest);
        }
    }

    private boolean hasFuelSlot() {
        return device.generatorKind().burnsFuel();
    }

    private void decorate() {
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, pane);
        }
        GeneratorKind kind = device.generatorKind();
        if (hasFuelSlot()) {
            inventory.setItem(LABEL_SLOT, item(Material.PAPER, "&eFuel: " + GeneratorText.fuelName(kind),
                    List.of("&7Put " + GeneratorText.fuelName(kind).toLowerCase(Locale.ROOT) + " here.")));
            inventory.setItem(FUEL_SLOT, null);
        } else {
            inventory.setItem(FUEL_SLOT, item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "&7No fuel slot",
                    List.of("&7This generator takes no fuel.")));
        }
        refresh();
    }

    private static Material statusMaterial(GeneratorStatus status) {
        return switch (status) {
            case RUNNING -> Material.LIME_DYE;
            case NO_FUEL -> Material.RED_DYE;
            case NOT_UNDERWATER -> Material.ORANGE_DYE;
            case CLOSED, NIGHT -> Material.GRAY_DYE;
        };
    }

    private static ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(name));
            meta.setLore(lore.stream().map(Messages::color).toList());
            item.setItemMeta(meta);
        }
        return item;
    }
}
