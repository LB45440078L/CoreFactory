package io.corefactory.core.logistics;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

/** A port held in memory. A source has no acceptance; a sink accepts what its filter allows while it is open. */
final class MemoryPort implements Port {

    private final Deque<Item> stock = new ArrayDeque<>();
    private final Predicate<Item> filter;
    private boolean open;
    private int accepted;
    private int taken;

    private MemoryPort(boolean open, Predicate<Item> filter) {
        this.open = open;
        this.filter = filter;
    }

    static MemoryPort source(Item... items) {
        MemoryPort port = new MemoryPort(false, item -> true);
        port.add(items);
        return port;
    }

    static MemoryPort sink() {
        return new MemoryPort(true, item -> true);
    }

    static MemoryPort sink(Predicate<Item> filter) {
        return new MemoryPort(true, filter);
    }

    void setOpen(boolean open) {
        this.open = open;
    }

    void add(Item... items) {
        stock.addAll(Arrays.asList(items));
    }

    List<Item> contents() {
        return new ArrayList<>(stock);
    }

    int accepted() {
        return accepted;
    }

    int taken() {
        return taken;
    }

    @Override
    public boolean accepts(Item item) {
        return open && filter.test(item);
    }

    @Override
    public void accept(Item item) {
        stock.addLast(item);
        accepted++;
    }

    @Override
    public Item peekOutput() {
        return stock.peekFirst();
    }

    @Override
    public Item takeOutput() {
        Item item = stock.pollFirst();
        if (item != null) {
            taken++;
        }
        return item;
    }

    @Override
    public int storedCount() {
        return stock.size();
    }
}
