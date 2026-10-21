package io.corefactory.core.logistics;

/**
 * A storage at one block that is not a logistics node: a machine barrel, a device inventory or a chest-like sink. The
 * caller supplies ports to each tick, keyed by block position. A picker takes from a port at its input cell and gives to
 * a port at its output cell; a belt or splitter gives to a port at its output cell.
 */
public interface Port {

    /** Whether the port takes this item now. */
    boolean accepts(Item item);

    /** Stores the item. The caller has checked {@link #accepts(Item)} first. */
    void accept(Item item);

    /** The item the port can give next, without removing it; null when it has nothing to give. */
    Item peekOutput();

    /** Removes and returns the item {@link #peekOutput()} shows; null when it has nothing to give. */
    Item takeOutput();

    /** How many items the port holds, for conservation checks. */
    int storedCount();
}
