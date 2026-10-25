package io.corefactory.core.logistics;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.ToIntFunction;

/**
 * The nine-slot grid of a crafter as a logistics port: a picker takes from it and a belt or picker puts into it. All the slot rules
 * of a crafter live here, so they are tested without a server.
 *
 * <p>A picker takes from the first enabled slot that holds a plain item, in slot order, one item at a time. A slot a crafter setting
 * disables is never offered, and an item put into a disabled slot is left where it is. An item goes into the first enabled slot that
 * holds the same item with room, else the first enabled empty slot, by the rules of {@link StorageRules}. A crafter takes any item a
 * slot holds, so the grid accepts whatever its enabled slots take: the crafting itself is the crafter's business.
 *
 * <p>The grid counts every slot in {@link #storedCount()}, disabled ones included, since their items stay in the crafter.
 */
public final class CrafterGrid implements Port {

    /** The slots of a crafter's grid: three rows of three. */
    public static final int SLOTS = 9;

    private final List<Stack> slots;
    private final boolean[] disabled;
    private final ToIntFunction<ItemRef> limit;

    /**
     * @param slots    the nine grid slots, in slot order
     * @param disabled for each slot in slot order, whether the crafter has it disabled
     * @param limit    the largest stack of an item, at least one
     * @throws IllegalArgumentException when there are not nine slots and nine flags
     */
    public CrafterGrid(List<Stack> slots, boolean[] disabled, ToIntFunction<ItemRef> limit) {
        Objects.requireNonNull(slots, "slots");
        Objects.requireNonNull(disabled, "disabled");
        Objects.requireNonNull(limit, "limit");
        if (slots.size() != SLOTS || disabled.length != SLOTS) {
            throw new IllegalArgumentException("a crafter has " + SLOTS + " slots, not " + slots.size() + " with " + disabled.length
                    + " flags");
        }
        this.slots = List.copyOf(slots);
        this.disabled = disabled.clone();
        this.limit = limit;
    }

    @Override
    public boolean accepts(Item item) {
        return slotFor(PortItems.refOf(item)).isPresent();
    }

    @Override
    public void accept(Item item) {
        ItemRef ref = PortItems.refOf(item);
        OptionalInt index = slotFor(ref);
        if (index.isEmpty() || !StorageRules.insertOne(slots.get(index.getAsInt()), ref, limit.applyAsInt(ref))) {
            throw new IllegalStateException("the crafter does not take " + item.id());
        }
    }

    @Override
    public Item peekOutput() {
        OptionalInt index = takeableSlot();
        return index.isEmpty() ? null : PortItems.itemOf(slots.get(index.getAsInt()).ref());
    }

    @Override
    public Item takeOutput() {
        OptionalInt index = takeableSlot();
        if (index.isEmpty()) {
            return null;
        }
        Stack slot = slots.get(index.getAsInt());
        ItemRef ref = slot.ref();
        if (!StorageRules.takeOne(slot, ref)) {
            throw new IllegalStateException("the crafter did not give " + ref.name());
        }
        return PortItems.itemOf(ref);
    }

    @Override
    public int storedCount() {
        int count = 0;
        for (Stack slot : slots) {
            count += slot.count();
        }
        return count;
    }

    /** The first enabled slot a picker may take from: one holding a plain item, which a named stack is not. */
    private OptionalInt takeableSlot() {
        for (int index = 0; index < SLOTS; index++) {
            Stack slot = slots.get(index);
            if (!disabled[index] && slot.count() > 0 && slot.ref() != null) {
                return OptionalInt.of(index);
            }
        }
        return OptionalInt.empty();
    }

    /** The enabled slot one more of the item goes into, by {@link StorageRules#slotFor}, or empty when no enabled slot takes it. */
    private OptionalInt slotFor(ItemRef ref) {
        List<Stack> open = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        for (int index = 0; index < SLOTS; index++) {
            if (!disabled[index]) {
                open.add(slots.get(index));
                positions.add(index);
            }
        }
        OptionalInt position = StorageRules.slotFor(limit.applyAsInt(ref), open, ref);
        return position.isEmpty() ? OptionalInt.empty() : OptionalInt.of(positions.get(position.getAsInt()));
    }
}
