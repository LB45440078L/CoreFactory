package io.corefactory.core.vertical;

import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.Port;

import java.util.List;
import java.util.Objects;

/**
 * The intake of a tower from the block beyond its input end: a chest, a barrel or another storage that stands there gives one item
 * into the tower's input block, the same as a belt fed from that storage. Pure: the port is any {@link Port}, so the rule needs no
 * server.
 */
public final class VerticalIntake {

    private VerticalIntake() {
    }

    /**
     * Takes one item from the source into the input block of a tower, when the block has room and the source offers an item. The
     * item goes behind the items already on the tower, as {@link VerticalCarry#place} keeps them in order.
     *
     * @param carry            the tower's carry
     * @param items            the tower's items, in the carry's order; the taken item is added at the index the carry gives
     * @param capacityPerBlock the items per block at the tower's level
     * @param source           the storage beyond the input end
     * @return the item taken, or null when nothing was taken
     */
    public static Item pull(VerticalCarry carry, List<Item> items, int capacityPerBlock, Port source) {
        Objects.requireNonNull(source, "source");
        if (carry.slotFor(0.0, capacityPerBlock) < 0 || source.peekOutput() == null) {
            return null;
        }
        Item taken = source.takeOutput();
        if (taken == null) {
            return null;
        }
        items.add(carry.place(0.0, capacityPerBlock), taken);
        return taken;
    }
}
