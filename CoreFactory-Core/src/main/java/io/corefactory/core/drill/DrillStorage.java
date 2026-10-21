package io.corefactory.core.drill;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;

import java.util.List;

/**
 * The output slots of a drill, as process stacks. An item goes into the slots that already hold it first, then into an
 * empty slot. Pure: the drill's barrel is read and written through {@link Stack}.
 */
public final class DrillStorage {

    private DrillStorage() {
    }

    /**
     * How many items of a reference fit in the slots. A null reference counts only empty slots, for an item the drill
     * does not yet know.
     *
     * @param slots the output slots
     * @param ref   the item, or null for any new item
     * @param limit the largest stack of the item
     */
    public static int room(List<Stack> slots, ItemRef ref, int limit) {
        int stackLimit = Math.max(1, limit);
        int room = 0;
        for (Stack slot : slots) {
            ItemRef held = slot.count() > 0 ? slot.ref() : null;
            if (held == null) {
                room += stackLimit;
            } else if (ref != null && ref.equals(held)) {
                room += Math.max(0, stackLimit - slot.count());
            }
        }
        return room;
    }

    /**
     * Puts items into the slots, all or nothing. Changes nothing when they do not all fit.
     *
     * @return true when every item was put in
     */
    public static boolean add(List<Stack> slots, ItemRef ref, int amount, int limit) {
        if (amount < 1 || room(slots, ref, limit) < amount) {
            return false;
        }
        int stackLimit = Math.max(1, limit);
        int left = amount;
        for (Stack slot : slots) {
            ItemRef held = slot.count() > 0 ? slot.ref() : null;
            if (left > 0 && ref.equals(held) && slot.count() < stackLimit) {
                int added = Math.min(stackLimit - slot.count(), left);
                slot.set(ref, slot.count() + added);
                left -= added;
            }
        }
        for (Stack slot : slots) {
            if (left > 0 && slot.count() == 0) {
                int added = Math.min(stackLimit, left);
                slot.set(ref, added);
                left -= added;
            }
        }
        return true;
    }

    /** The total number of items in the slots. */
    public static int stored(List<Stack> slots) {
        int total = 0;
        for (Stack slot : slots) {
            total += Math.max(0, slot.count());
        }
        return total;
    }
}
