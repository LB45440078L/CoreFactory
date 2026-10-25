package io.corefactory.paper.machine;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;

import java.util.List;

/**
 * The rules of two moves into a machine's input and fuel slots: a number-key swap and a shift-click. Pure: the menu reads the
 * server (the held item, the stack limit) and passes the answers in, and the targets are the menu's own slot views.
 */
final class SlotMoves {

    /** How many hotbar keys there are; key 1 is button 0 and key 9 is button 8. */
    private static final int HOTBAR_KEYS = 9;

    private SlotMoves() {
    }

    /** True when a hotbar number key is one of the nine keys, and not the offhand swap or a key outside the hotbar. */
    static boolean isHotbarKey(int button) {
        return button >= 0 && button < HOTBAR_KEYS;
    }

    /**
     * True when a number-key swap over a slot may go through. Only input and fuel slots take items, and only when the key is a
     * hotbar key and the item in that hotbar slot is one the slot accepts.
     *
     * @param kind         the kind of the slot the key is pressed over
     * @param hotbarButton the key the player pressed
     * @param accepted     true when the item in the hotbar slot is one the slot takes, within its stack limit
     */
    static boolean hotbarSwapAllowed(MachineLayout.Kind kind, int hotbarButton, boolean accepted) {
        boolean takesItems = kind == MachineLayout.Kind.INPUT || kind == MachineLayout.Kind.FUEL;
        return takesItems && isHotbarKey(hotbarButton) && accepted;
    }

    /**
     * Puts up to {@code amount} items of a reference into the stacks: first into the stacks that already hold the reference,
     * then into the empty ones, each stack up to {@code limit}. Each stack is read before and after its write, so the count
     * returned is what the stacks really took. A stack that refuses the write takes nothing.
     *
     * @param targets the slot views, in the order they are tried
     * @param ref     the item to put in
     * @param amount  how many the player holds
     * @param limit   the largest stack of the item
     * @return how many items went in, never more than {@code amount}; the player keeps {@code amount} minus this
     */
    static int insert(List<Stack> targets, ItemRef ref, int amount, int limit) {
        int added = 0;
        for (Stack slot : targets) {
            if (added < amount && ref.equals(slot.ref()) && slot.count() < limit) {
                added += fill(slot, ref, Math.min(limit - slot.count(), amount - added));
            }
        }
        for (Stack slot : targets) {
            if (added < amount && slot.ref() == null) {
                added += fill(slot, ref, Math.min(limit, amount - added));
            }
        }
        return added;
    }

    private static int fill(Stack slot, ItemRef ref, int room) {
        int before = slot.count();
        slot.set(ref, before + room);
        return slot.count() - before;
    }
}
