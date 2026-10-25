package io.corefactory.paper.logistics;

import io.corefactory.core.logistics.Item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The two lists of a splitter's filter: the item types that go left and the ones that go right. Each list holds at most
 * {@link #LIMIT} types, and a type is on one list only: adding a type that the other list holds moves it. Pure.
 */
public final class SplitterFilter {

    /** The most types one list holds. */
    public static final int LIMIT = 9;

    /** The two lists. */
    public enum Side {
        /** The list of items sent to the left output. */
        LEFT,
        /** The list of items sent to the right output. */
        RIGHT
    }

    /** What adding a type did. */
    public enum Outcome {
        /** The type went onto the list. */
        ADDED,
        /** The type came off the other list and went onto this one. */
        MOVED,
        /** The type was already on this list; nothing changed. */
        ALREADY_HERE,
        /** This list has no room; nothing changed. */
        FULL
    }

    private static final int[] LEFT_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20};
    private static final int[] RIGHT_SLOTS = {28, 29, 30, 31, 32, 33, 34, 37, 38};

    private final List<Item> left = new ArrayList<>();
    private final List<Item> right = new ArrayList<>();

    private SplitterFilter() {
    }

    /** An empty filter. */
    public static SplitterFilter empty() {
        return new SplitterFilter();
    }

    /**
     * A filter with the given lists, keeping their order.
     *
     * @throws IllegalArgumentException when a list is longer than {@link #LIMIT} or a type is on both lists
     */
    public static SplitterFilter of(Collection<Item> left, Collection<Item> right) {
        SplitterFilter filter = new SplitterFilter();
        for (Item item : Objects.requireNonNull(left, "left list")) {
            if (filter.add(Side.LEFT, item) != Outcome.ADDED) {
                throw new IllegalArgumentException("left list holds a type twice or is full: " + item.id());
            }
        }
        for (Item item : Objects.requireNonNull(right, "right list")) {
            if (filter.add(Side.RIGHT, item) != Outcome.ADDED) {
                throw new IllegalArgumentException("right list holds a type twice, is full or shares a type: " + item.id());
            }
        }
        return filter;
    }

    /**
     * Adds a type to a list. A type on the other list moves over, when this list has room.
     *
     * @return what happened; nothing changes unless it is {@link Outcome#ADDED} or {@link Outcome#MOVED}
     */
    public Outcome add(Side side, Item item) {
        Objects.requireNonNull(item, "item");
        List<Item> here = listOf(side);
        List<Item> there = listOf(side == Side.LEFT ? Side.RIGHT : Side.LEFT);
        if (here.contains(item)) {
            return Outcome.ALREADY_HERE;
        }
        if (here.size() >= LIMIT) {
            return Outcome.FULL;
        }
        if (there.remove(item)) {
            here.add(item);
            return Outcome.MOVED;
        }
        here.add(item);
        return Outcome.ADDED;
    }

    /**
     * Takes the type at a position off a list.
     *
     * @return the type removed, or empty when the position holds nothing
     */
    public Optional<Item> remove(Side side, int index) {
        List<Item> list = listOf(side);
        if (index < 0 || index >= list.size()) {
            return Optional.empty();
        }
        return Optional.of(list.remove(index));
    }

    /** The types on a list, in the order they were added. */
    public List<Item> items(Side side) {
        return List.copyOf(listOf(side));
    }

    /** The types on the left list, as a set for the splitter. */
    public Set<Item> leftSet() {
        return Set.copyOf(left);
    }

    /** The types on the right list, as a set for the splitter. */
    public Set<Item> rightSet() {
        return Set.copyOf(right);
    }

    /** The GUI slots of a list, in the order of its entries. The list holds {@link #LIMIT} slots. */
    public static int[] slots(Side side) {
        return side == Side.LEFT ? LEFT_SLOTS.clone() : RIGHT_SLOTS.clone();
    }

    /** The position in a list that a GUI slot shows, or -1 when the slot is not one of the list's slots. */
    public static int indexOf(Side side, int slot) {
        int[] slots = slots(side);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    private List<Item> listOf(Side side) {
        return side == Side.LEFT ? left : right;
    }
}
