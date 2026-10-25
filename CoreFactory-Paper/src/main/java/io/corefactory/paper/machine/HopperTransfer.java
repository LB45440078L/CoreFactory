package io.corefactory.paper.machine;

import io.corefactory.core.logistics.StorageRules;
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

    /** Adds exactly one item to a slot, by {@link StorageRules#insertOne}. */
    static boolean insertOne(Stack slot, ItemRef item, int limit) {
        return StorageRules.insertOne(slot, item, limit);
    }

    /** Removes exactly one item from a slot, by {@link StorageRules#takeOne}. */
    static boolean takeOne(Stack slot, ItemRef item) {
        return StorageRules.takeOne(slot, item);
    }
}
