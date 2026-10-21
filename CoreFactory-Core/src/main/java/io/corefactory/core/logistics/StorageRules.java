package io.corefactory.core.logistics;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * The decisions a storage's slots make for one item: a slot takes the item when it is empty, up to the item's stack limit,
 * or when it holds the same item with room. An item goes into the first slot that holds it with room, else the first empty
 * slot, so an amount over one stack spills into the next slot. A slot holding a different item takes nothing. Pure over
 * {@link Stack} views, so it is tested with plain stacks and no server.
 */
public final class StorageRules {

    private StorageRules() {
    }

    /**
     * How many of an item the slots can still take: the room left in each slot holding the item, plus a full stack in each
     * empty slot.
     *
     * @param itemLimit the largest stack of the item, at least one
     * @param stacks    the slots, in slot order
     * @param item      the item
     * @return the number of items that fit, zero or more
     */
    public static int room(int itemLimit, List<Stack> stacks, ItemRef item) {
        requireLimit(itemLimit);
        Objects.requireNonNull(stacks, "stacks");
        Objects.requireNonNull(item, "item");
        int room = 0;
        for (Stack stack : stacks) {
            room += slotRoom(itemLimit, Objects.requireNonNull(stack, "stack"), item);
        }
        return room;
    }

    /**
     * Whether the slots take one more of an item: some slot holds the item with room, or some slot is empty.
     *
     * @param itemLimit the largest stack of the item, at least one
     * @param stacks    the slots, in slot order
     * @param item      the item
     * @return true when {@link #slotFor} finds a slot
     */
    public static boolean accepts(int itemLimit, List<Stack> stacks, ItemRef item) {
        return slotFor(itemLimit, stacks, item).isPresent();
    }

    /**
     * The position, within the slots, of the slot one more of an item goes into: the first slot holding the item with room,
     * else the first empty slot.
     *
     * @param itemLimit the largest stack of the item, at least one
     * @param stacks    the slots, in slot order
     * @param item      the item
     * @return the position in the list, or empty when no slot takes the item
     */
    public static OptionalInt slotFor(int itemLimit, List<Stack> stacks, ItemRef item) {
        requireLimit(itemLimit);
        Objects.requireNonNull(stacks, "stacks");
        Objects.requireNonNull(item, "item");
        for (int i = 0; i < stacks.size(); i++) {
            Stack stack = Objects.requireNonNull(stacks.get(i), "stack");
            if (stack.count() > 0 && item.equals(stack.ref()) && stack.count() < itemLimit) {
                return OptionalInt.of(i);
            }
        }
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i).count() <= 0) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    private static int slotRoom(int itemLimit, Stack stack, ItemRef item) {
        if (stack.count() <= 0) {
            return itemLimit;
        }
        if (item.equals(stack.ref())) {
            return Math.max(0, itemLimit - stack.count());
        }
        return 0;
    }

    private static void requireLimit(int itemLimit) {
        if (itemLimit < 1) {
            throw new IllegalArgumentException("the stack limit must be at least 1: " + itemLimit);
        }
    }
}
