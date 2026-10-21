package io.corefactory.core.logistics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

/**
 * The items on one belt unit, front first: index 0 is the item nearest the exit. Positions run from 0 at the entry to 1 at
 * the exit. Items keep at least {@link #GAP} between them, and a unit holds at most {@link LogisticsTable#BELT_CAPACITY}.
 * Each item carries a serial from the network it is on, so the view of the items can follow one item from tick to tick.
 */
final class Lane {

    static final double GAP = 1.0 / LogisticsTable.BELT_CAPACITY;

    private static final double EPS = 1e-9;

    private final List<Carried> items = new ArrayList<>();
    private final LongSupplier serials;

    /** @param serials gives a new serial for every item that enters the lane */
    Lane(LongSupplier serials) {
        this.serials = serials;
    }

    /** Whether an item can enter at the entry now. */
    boolean hasRoom() {
        if (items.size() >= LogisticsTable.BELT_CAPACITY) {
            return false;
        }
        return items.isEmpty() || items.get(items.size() - 1).position >= GAP - EPS;
    }

    void insertAtEntry(Item item) {
        items.add(new Carried(item, 0.0, serials.getAsLong()));
    }

    /**
     * Puts an item on the lane at a position. Used to restore items that were on the belt.
     *
     * @return false, and nothing changes, when the position is outside the belt, the unit is full, or the item would be
     *         closer than {@link #GAP} to an item already on the lane
     */
    boolean insertAt(Item item, double position) {
        if (!(position >= 0.0 && position <= 1.0) || items.size() >= LogisticsTable.BELT_CAPACITY) {
            return false;
        }
        int index = 0;
        while (index < items.size() && items.get(index).position > position) {
            index++;
        }
        if (index > 0 && items.get(index - 1).position - position < GAP - EPS) {
            return false;
        }
        if (index < items.size() && position - items.get(index).position < GAP - EPS) {
            return false;
        }
        items.add(index, new Carried(item, position, serials.getAsLong()));
        return true;
    }

    /** Whether the front item has reached the exit and waits there. */
    boolean exitWaiting() {
        return !items.isEmpty() && items.get(0).position >= 1.0 - EPS;
    }

    /** The item waiting at the exit, or null. */
    Item exitItem() {
        return exitWaiting() ? items.get(0).item : null;
    }

    /** Removes the item waiting at the exit. */
    Item takeExit() {
        if (!exitWaiting()) {
            throw new IllegalStateException("no item waits at the exit");
        }
        return items.remove(0).item;
    }

    /** The item nearest the exit, wherever it sits on the unit, or null when the unit is empty. */
    Item frontItem() {
        return items.isEmpty() ? null : items.get(0).item;
    }

    /** Removes the item nearest the exit, wherever it sits on the unit. The other items keep their places. */
    Item takeFront() {
        if (items.isEmpty()) {
            throw new IllegalStateException("no item on the unit");
        }
        return items.remove(0).item;
    }

    int size() {
        return items.size();
    }

    List<BeltItem> view() {
        List<BeltItem> view = new ArrayList<>(items.size());
        for (Carried carried : items) {
            view.add(new BeltItem(carried.item, carried.position));
        }
        return view;
    }

    /** The serial of each item, in the same order as {@link #view()}. */
    List<Long> serialList() {
        List<Long> view = new ArrayList<>(items.size());
        for (Carried carried : items) {
            view.add(carried.serial);
        }
        return view;
    }

    /**
     * Moves the items forward by {@code distance}. Going from the front, each item moves as far as the item ahead of it
     * allows; an item that reaches the exit with nothing ahead is offered to {@code handover}. When the handover refuses,
     * the item stays at the exit and blocks the items behind it.
     *
     * @return how many items the handover accepted
     */
    int advance(double distance, Predicate<Item> handover) {
        int moved = 0;
        boolean free = true;
        double previous = 0.0;
        int index = 0;
        while (index < items.size()) {
            Carried carried = items.get(index);
            double cap = free ? 1.0 : previous - GAP;
            double position = Math.max(carried.position, Math.min(carried.position + distance, cap));
            if (free && position >= 1.0 - EPS) {
                position = 1.0;
                if (handover.test(carried.item)) {
                    items.remove(index);
                    moved++;
                    continue;
                }
            }
            carried.position = position;
            previous = position;
            free = false;
            index++;
        }
        return moved;
    }

    /** A mutable item on the lane. */
    private static final class Carried {

        private final Item item;
        private final long serial;
        private double position;

        Carried(Item item, double position, long serial) {
            this.item = item;
            this.position = position;
            this.serial = serial;
        }
    }
}
