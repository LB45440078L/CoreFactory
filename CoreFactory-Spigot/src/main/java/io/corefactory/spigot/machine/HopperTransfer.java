package io.corefactory.spigot.machine;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;

/**
 * The transfer of one item by a hopper move into a lab tool's barrel or a plain inventory, as a transaction over one slot.
 *
 * <p>The server takes the item out of its source before the move event fires, and it puts the item back into the source
 * when the event is cancelled. So a move the plugin completes is not cancelled: the plugin puts the one item into the
 * destination, and the event's item is then set to air, so the server finds nothing left to insert and its removal from
 * the source stands. A move the plugin cannot complete is cancelled, and the server restores the source. Pure: the
 * slots are views, so the rules are tested with plain objects.
 */
final class HopperTransfer {

    private HopperTransfer() {
    }

    /**
     * Moves one item into a destination slot. The move is refused, and the slot left as it was, unless exactly one item moves and
     * the slot takes it.
     *
     * @param moved how many items the server took out of the source for this move
     * @param slot  the destination slot
     * @param item  the item
     * @param limit the largest stack of the item
     * @return true when the one item is in the slot; false when nothing was inserted
     */
    static boolean placeOne(int moved, Stack slot, ItemRef item, int limit) {
        return moved == 1 && insertOne(slot, item, limit);
    }

    /**
     * Adds exactly one item to a slot. The slot must be empty or hold the same item with room. The count must rise by
     * exactly one; when it does not, the slot is set back to what it held and the insertion is refused.
     */
    static boolean insertOne(Stack slot, ItemRef item, int limit) {
        ItemRef before = slot.ref();
        int count = slot.count();
        if (count >= limit || count > 0 && !item.equals(before)) {
            return false;
        }
        slot.set(item, count + 1);
        if (slot.count() == count + 1 && item.equals(slot.ref())) {
            return true;
        }
        slot.set(before, count);
        return false;
    }

    /**
     * Removes exactly one item from a slot that holds the item. The count must fall by exactly one; when it does not, the slot is set
     * back to what it held and the removal is refused. A slot holding one item empties, and that counts as the removal.
     *
     * @param slot the slot
     * @param item the item the slot must hold
     * @return true when one item left the slot; false when nothing was removed
     */
    static boolean takeOne(Stack slot, ItemRef item) {
        ItemRef before = slot.ref();
        int count = slot.count();
        if (count <= 0 || !item.equals(before)) {
            return false;
        }
        slot.set(item, count - 1);
        if (slot.count() == count - 1 && (count == 1 || item.equals(slot.ref()))) {
            return true;
        }
        slot.set(before, count);
        return false;
    }
}
