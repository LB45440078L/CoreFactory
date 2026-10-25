package io.corefactory.paper.lock;

import io.corefactory.core.lock.PinEntry;
import io.corefactory.core.lock.PinHash;
import io.corefactory.core.text.Text;
import io.corefactory.paper.gui.MenuHolder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * The keypad of a nuclear TNT lock, 27 slots. The digits 1 to 9 and 0 sit in a 3 by 4 grid on the right: three rows of 1 2 3, 4 5 6,
 * 7 8 9, with BACKSPACE and CLEAR in the fourth column and 0 under 9. The display in the top middle shows the digits typed as
 * asterisks and the number still to type. OK in the bottom middle is enabled only with four digits. On the UNLOCK keypad a correct
 * PIN unlocks the TNT; on the SET PIN keypad the digits are asked for twice, and a mismatch starts again. A remove button sits bottom
 * left when it is offered.
 *
 * <p>The menu never moves items: {@link io.corefactory.paper.gui.GuiListener} cancels every click, and a click outside the keypad
 * does nothing. Closing the menu without OK drops the digits typed.
 */
public final class LockKeypad implements MenuHolder {

    /** What the keypad is for. */
    public enum Purpose {
        /** The owner sets a PIN on a TNT that is unlocked. Two entries. */
        SET_PIN,
        /** A PIN is entered to unlock a locked TNT. One entry. */
        UNLOCK
    }

    static final int SIZE = 27;
    static final int DISPLAY = 4;
    static final int REMOVE = 18;
    static final int BACKSPACE = 8;
    static final int CLEAR = 17;
    static final int OK = 22;

    /** The slot of each digit, indexed by the digit: 0 sits under 9. */
    private static final int[] DIGIT_SLOTS = {26, 5, 6, 7, 14, 15, 16, 23, 24, 25};

    private final TntLocks locks;
    private final Location at;
    private final Purpose purpose;
    private final boolean removeOffered;
    private final boolean manageAfter;
    private final PinEntry entry;
    private final Inventory inventory;

    /**
     * @param at            the TNT's block
     * @param removeOffered whether the remove button is shown
     * @param manageAfter   on a correct PIN of an UNLOCK keypad, open the owner's SET PIN keypad next
     */
    LockKeypad(TntLocks locks, Location at, Purpose purpose, boolean removeOffered, boolean manageAfter) {
        this.locks = locks;
        this.at = at.clone();
        this.purpose = purpose;
        this.removeOffered = removeOffered;
        this.manageAfter = manageAfter;
        this.entry = new PinEntry(purpose == Purpose.SET_PIN);
        String title = purpose == Purpose.SET_PIN ? "&8Set a PIN lock" : "&8Enter the PIN";
        this.inventory = Bukkit.createInventory(this, SIZE, Text.parse(title));
        render();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /** Opens this keypad for the player. */
    void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public void click(InventoryClickEvent event, Player player) {
        int slot = event.getSlot();
        if (slot == BACKSPACE) {
            entry.backspace();
            render();
        } else if (slot == CLEAR) {
            entry.clear();
            render();
        } else if (slot == OK) {
            submit(player);
        } else if (slot == REMOVE && removeOffered) {
            locks.remove(player, at);
            player.closeInventory();
        } else {
            int digit = digitAt(slot);
            if (digit >= 0 && entry.press(digit)) {
                render();
            }
        }
    }

    private void submit(Player player) {
        if (!entry.complete()) {
            return;
        }
        PinEntry.Submission submission = entry.submit();
        switch (submission.outcome()) {
            case FIRST_STORED -> {
                player.sendMessage(locks.message("lock-confirm"));
                render();
            }
            case MISMATCH -> {
                player.sendMessage(locks.message("lock-mismatch"));
                render();
            }
            case READY -> finish(player, submission.pin());
            default -> {
            }
        }
    }

    private void finish(Player player, String pin) {
        if (purpose == Purpose.SET_PIN) {
            locks.setPin(player, at, pin);
            player.closeInventory();
            return;
        }
        switch (locks.unlock(player, at, pin)) {
            case UNLOCKED -> {
                if (manageAfter) {
                    locks.openSetPin(player, at, true);
                } else {
                    player.closeInventory();
                }
            }
            case RETRY -> {
                entry.clear();
                render();
            }
            default -> player.closeInventory();
        }
    }

    /** Draws the keypad from the digits typed so far. */
    private void render() {
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < SIZE; slot++) {
            inventory.setItem(slot, filler);
        }
        for (int digit = 0; digit <= 9; digit++) {
            inventory.setItem(DIGIT_SLOTS[digit], item(Material.STONE_BUTTON, "&f" + digit));
        }
        inventory.setItem(BACKSPACE, item(Material.ARROW, "&eBackspace", "&7Remove the last digit."));
        inventory.setItem(CLEAR, item(Material.BARRIER, "&cClear", "&7Remove every digit."));
        inventory.setItem(DISPLAY, display());
        inventory.setItem(OK, entry.complete()
                ? item(Material.LIME_WOOL, "&aOK", "&7Use the digits entered.")
                : item(Material.GRAY_WOOL, "&8OK", "&7Enter four digits first."));
        if (removeOffered) {
            inventory.setItem(REMOVE, item(Material.RED_WOOL, "&cRemove lock",
                    "&7Removes the PIN entirely.", "&7The TNT is unlocked, with no PIN."));
        }
    }

    private ItemStack display() {
        int typed = entry.length();
        int left = PinHash.PIN_LENGTH - typed;
        String stars = "&e" + "*".repeat(typed) + "&8" + "_".repeat(left);
        if (purpose == Purpose.SET_PIN && entry.confirming()) {
            return item(Material.PAPER, stars, "&7Confirm: enter the same digits again.", "&7Remaining: &f" + left);
        }
        return item(Material.PAPER, stars, "&7Digits entered: &f" + typed + "&7 of " + PinHash.PIN_LENGTH,
                "&7Remaining: &f" + left);
    }

    private int digitAt(int slot) {
        for (int digit = 0; digit < DIGIT_SLOTS.length; digit++) {
            if (DIGIT_SLOTS[digit] == slot) {
                return digit;
            }
        }
        return -1;
    }

    private static ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.item(name));
            if (lore.length > 0) {
                meta.lore(Text.lore(Arrays.asList(lore)));
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
