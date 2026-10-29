package io.corefactory.paper.vertical;

import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.PickRule;
import io.corefactory.core.logistics.Port;
import io.corefactory.core.vertical.VerticalCarry;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * One cell of a vertical tower as a logistics port. A regular belt or a picker puts an item in at the cell's entry, when the tower has
 * room there. A picker beside the cell takes the item on the tower nearest its output end among the items in the cell, the same order a
 * picker takes from the side of a straight belt. The item waiting at the output end is not in any cell, so the port never offers it,
 * and the output cell takes it as before.
 *
 * <p>A take is checked by the claim rule through {@code takePermitted}, asked on each take, so a protected cell gives nothing to a
 * picker whose owner may not take from it. The port holds the tower's own carry and items, so it is only valid for the tick it was
 * made in.
 */
final class VerticalBeltPort implements Port {

    private final VerticalCarry carry;
    private final List<Item> items;
    private final int block;
    private final int capacity;
    private final BooleanSupplier takePermitted;

    /**
     * @param carry         the tower's carry
     * @param items         the tower's items, in the carry's order
     * @param block         the cell's block index from the tower's input end
     * @param capacity      the items per block at the tower's level
     * @param takePermitted true when the claim check lets the picker's owner take from the cell
     */
    VerticalBeltPort(VerticalCarry carry, List<Item> items, int block, int capacity, BooleanSupplier takePermitted) {
        this.carry = carry;
        this.items = items;
        this.block = block;
        this.capacity = capacity;
        this.takePermitted = takePermitted;
    }

    /** Whether the tower has room at the cell's entry for this item. */
    @Override
    public boolean accepts(Item item) {
        return carry.slotFor(block, capacity) >= 0;
    }

    /** Puts the item at the cell's entry. The caller has checked {@link #accepts(Item)} first. */
    @Override
    public void accept(Item item) {
        int slot = carry.place(block, capacity);
        if (slot < 0) {
            throw new IllegalStateException("the tower has no room at this cell for " + item.id());
        }
        items.add(slot, item);
    }

    /** The item nearest the output end in this cell, when the claim check lets the picker take it; null otherwise. */
    @Override
    public Item peekOutput() {
        int index = carry.indexIn(block);
        if (index < 0) {
            return null;
        }
        Item item = items.get(index);
        return PickRule.decide(item, takePermitted.getAsBoolean()) == PickRule.Verdict.TAKE ? item : null;
    }

    /** Removes the item {@link #peekOutput()} shows from the tower, and returns it. */
    @Override
    public Item takeOutput() {
        int index = carry.indexIn(block);
        if (index < 0) {
            return null;
        }
        Item item = items.get(index);
        if (PickRule.decide(item, takePermitted.getAsBoolean()) != PickRule.Verdict.TAKE) {
            throw new IllegalStateException("a claim protects the vertical belt of " + item.id());
        }
        carry.remove(index);
        return items.remove(index);
    }

    /** The items in this cell, waiting at the output end excluded. */
    @Override
    public int storedCount() {
        return carry.countIn(block);
    }
}
