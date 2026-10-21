package io.corefactory.core.process;

import java.util.ArrayList;
import java.util.List;

/** A slot held in memory, for tests of the process engine. */
final class MemoryStack implements Stack {

    private ItemRef ref;
    private int count;

    static List<Stack> slots(int size) {
        List<Stack> slots = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            slots.add(new MemoryStack());
        }
        return slots;
    }

    static void fill(List<Stack> slots, int index, ItemRef ref, int count) {
        slots.get(index).set(ref, count);
    }

    @Override
    public ItemRef ref() {
        return count > 0 ? ref : null;
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public void set(ItemRef ref, int count) {
        if (count <= 0) {
            this.ref = null;
            this.count = 0;
        } else {
            this.ref = ref;
            this.count = count;
        }
    }
}
