package io.corefactory.core.logistics;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;

/** A slot held in memory: a count of zero or less empties it. */
final class MemorySlot implements Stack {

    private ItemRef ref;
    private int count;

    MemorySlot(ItemRef ref, int count) {
        this.ref = ref;
        this.count = count;
    }

    static MemorySlot empty() {
        return new MemorySlot(null, 0);
    }

    static MemorySlot of(ItemRef ref, int count) {
        return new MemorySlot(ref, count);
    }

    @Override
    public ItemRef ref() {
        return ref;
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public void set(ItemRef ref, int count) {
        this.ref = count <= 0 ? null : ref;
        this.count = Math.max(0, count);
    }
}
