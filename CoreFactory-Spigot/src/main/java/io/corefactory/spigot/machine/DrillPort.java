package io.corefactory.spigot.machine;

import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.Port;
import io.corefactory.core.logistics.PortItems;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A drill's output slots as a logistics port. A picker takes a product out of the drill's barrel, first output slot first, one
 * item at a time. Nothing goes into a drill through a port: its fuel comes from a hopper or by hand, so {@link #accepts(Item)}
 * is false and {@link #accept(Item)} is refused. The port keeps no items of its own; its stacks are the drill's barrel slots, so
 * the barrel is the truth and an item cannot be counted twice.
 */
public final class DrillPort implements Port {

    private final List<Stack> outputs;

    /**
     * @param outputs the output slots, in output order; the fuel slot is not among them
     */
    public DrillPort(List<Stack> outputs) {
        this.outputs = List.copyOf(Objects.requireNonNull(outputs, "outputs"));
    }

    /**
     * The port of a drill, or empty when the placed machine is not a drill, its barrel is not loaded, or its menu has a
     * player's change that is not written to the barrel yet.
     */
    public static Optional<DrillPort> of(PlacedMachine placed) {
        if (placed.drill() == null) {
            return Optional.empty();
        }
        MachineMenu menu = placed.menu();
        if (menu.barrel().inventory().isEmpty() || menu.hasPending()) {
            return Optional.empty();
        }
        return Optional.of(new DrillPort(menu.outputs()));
    }

    @Override
    public boolean accepts(Item item) {
        return false;
    }

    @Override
    public void accept(Item item) {
        throw new IllegalStateException("a drill takes no items through a port; its fuel is fed by hoppers and by hand");
    }

    @Override
    public Item peekOutput() {
        Stack stack = firstOutput();
        return stack == null ? null : PortItems.itemOf(stack.ref());
    }

    @Override
    public Item takeOutput() {
        Stack stack = firstOutput();
        if (stack == null) {
            return null;
        }
        ItemRef ref = stack.ref();
        stack.set(ref, stack.count() - 1);
        return PortItems.itemOf(ref);
    }

    @Override
    public int storedCount() {
        int count = 0;
        for (Stack stack : outputs) {
            count += stack.count();
        }
        return count;
    }

    private Stack firstOutput() {
        for (Stack stack : outputs) {
            if (stack.count() > 0 && stack.ref() != null) {
                return stack;
            }
        }
        return null;
    }
}
