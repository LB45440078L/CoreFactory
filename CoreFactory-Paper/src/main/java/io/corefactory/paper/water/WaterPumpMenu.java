package io.corefactory.paper.water;

import io.corefactory.core.text.Text;
import io.corefactory.core.water.PumpFuel;
import io.corefactory.core.water.PumpLevels;
import io.corefactory.core.water.WaterText;
import io.corefactory.paper.gui.SlotMenu;
import io.corefactory.paper.machine.ItemRefs;
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
import java.util.Set;

/**
 * The menu of one water pump: its status, and a fuel slot for level I. The status items show the pump's level, the water it gives now
 * against what it can give, its source, its fuel or power, and whether it runs. The fuel slot takes only coal or charcoal, from the cursor
 * or a shift-click; a fuel item can be taken back out. A level II pump has no slot: it draws its diesel from the oil network, and its
 * status item says so. Every other slot is decoration that cannot be moved. The fuel in the slot is added to the pump when the menu
 * closes, and any that does not fit goes back to the player, so nothing is lost.
 */
public final class WaterPumpMenu implements SlotMenu {

    /** The size of the menu: three rows. */
    public static final int SIZE = 27;

    /** The slot that takes fuel. It is the only slot a player can put items into. */
    public static final int FUEL_SLOT = 11;

    private static final int LEVEL_SLOT = 13;
    private static final int FLOW_SLOT = 14;
    private static final int SOURCE_SLOT = 15;
    private static final int FUEL_INFO_SLOT = 16;
    private static final int STATUS_SLOT = 22;

    private static final Set<InventoryAction> TAKE = Set.of(InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_ONE, InventoryAction.PICKUP_SOME, InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.DROP_ONE_SLOT, InventoryAction.DROP_ALL_SLOT);
    private static final Set<InventoryAction> PUT = Set.of(InventoryAction.PLACE_ALL, InventoryAction.PLACE_SOME,
            InventoryAction.PLACE_ONE, InventoryAction.SWAP_WITH_CURSOR);

    private final WaterService service;
    private final WaterDevice pump;
    private final ItemRefs refs;
    private final Inventory inventory;

    WaterPumpMenu(WaterService service, WaterDevice pump, ItemRefs refs) {
        this.service = service;
        this.pump = pump;
        this.refs = refs;
        this.inventory = Bukkit.createInventory(this, SIZE, Text.parse(WaterText.title(pump.level())));
        refresh();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** True while a player has this menu open. */
    public boolean isOpen() {
        return !inventory.getViewers().isEmpty();
    }

    /** Shows the menu to a player. */
    void open(Player player) {
        refresh();
        player.openInventory(inventory);
    }

    /** Closes the menu for everyone who has it open. Their close events give the fuel back. */
    void closeViewers() {
        for (HumanEntity viewer : List.copyOf(inventory.getViewers())) {
            viewer.closeInventory();
        }
    }

    /** The pump whose menu this is. */
    WaterDevice pump() {
        return pump;
    }

    /** True when the pump takes fuel by hand at its level, which is level I only. Level II draws its diesel from the oil network. */
    boolean takesFuel() {
        return !PumpLevels.powered(pump.level()) && !PumpFuel.drawsFluid(pump.level());
    }

    /**
     * Adds the fuel in the slot to the pump, as far as it fits, and returns the rest to the player. Called when the menu closes.
     *
     * @param player the player who had the menu open
     */
    void absorb(Player player) {
        ItemStack slot = inventory.getItem(FUEL_SLOT);
        inventory.setItem(FUEL_SLOT, null);
        if (slot == null || slot.getAmount() <= 0) {
            return;
        }
        int accepted = PumpFuel.accept(pump.burn().held(), slot.getAmount());
        if (accepted > 0) {
            service.addFuel(pump, accepted);
        }
        int rest = slot.getAmount() - accepted;
        if (rest > 0) {
            ItemStack back = slot.clone();
            back.setAmount(rest);
            player.getInventory().addItem(back).values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }
    }

    /** Redraws the status items. Called every few ticks while the menu is open, and when it opens. */
    void refresh() {
        WaterService.PumpReading reading = service.reading(pump);
        inventory.setItem(LEVEL_SLOT, item(Material.NETHER_STAR, WaterText.title(pump.level()),
                List.of("&7Level " + WaterText.roman(pump.level()) + " of " + PumpLevels.MAX_LEVEL + ".",
                        pump.level() < PumpLevels.MAX_LEVEL ? "&7The next kit raises it one level." : "&7The top level.")));
        inventory.setItem(FLOW_SLOT, item(Material.WATER_BUCKET, WaterText.flow(reading.delivered(), reading.capacity()),
                List.of("&7The water it gives its pipes now.")));
        inventory.setItem(SOURCE_SLOT, item(Material.PRISMARINE_SHARD, WaterText.source(reading.source(), reading.minimum()),
                List.of("&7The water connected to it through a pipe.", "&7A source needs " + reading.minimum() + " blocks.")));
        if (PumpFuel.drawsFluid(pump.level())) {
            inventory.setItem(FUEL_INFO_SLOT, item(Material.BLACK_DYE, WaterText.fuel(pump.level(), 0, reading.running()),
                    List.of("&7It draws diesel from the oil network.", "&7One litre a minute, while it has a source.")));
        } else if (takesFuel()) {
            inventory.setItem(FUEL_INFO_SLOT, item(Material.COAL, WaterText.fuel(pump.level(), pump.burn().held(), reading.running()),
                    List.of("&7Put " + fuelName() + " in the slot to the left.", "&7One item burns for 60 seconds.")));
        } else {
            inventory.setItem(FUEL_INFO_SLOT, item(Material.REDSTONE, reading.power(),
                    List.of("&7The share of its power it gets from the grid.")));
        }
        String status = reading.running() ? "&aRunning" : "&cStopped";
        inventory.setItem(STATUS_SLOT, item(reading.running() ? Material.LIME_DYE : Material.GRAY_DYE, "&fStatus: " + status,
                List.of("&7Running when it has a source and fuel or power.")));
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
            }
            return;
        }
        boolean fuelAction = TAKE.contains(action) || PUT.contains(action);
        if (raw != FUEL_SLOT || !takesFuel() || !fuelAction) {
            event.setCancelled(true);
            return;
        }
        if (PUT.contains(action) && !acceptsCursor(event)) {
            event.setCancelled(true);
        }
    }

    private boolean acceptsCursor(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        if (cursor == null || cursor.getType().isAir()) {
            return true;
        }
        return refs.of(cursor).map(ref -> PumpFuel.accepts(pump.level(), ref)).orElse(false);
    }

    /** The name of the fuel the pump's level takes by hand, as the slot's hint says it. */
    private String fuelName() {
        return "coal or charcoal";
    }

    private static ItemStack item(Material material, String title, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(title));
            meta.lore(Text.lore(lore));
            item.setItemMeta(meta);
        }
        return item;
    }
}
