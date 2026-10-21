package io.corefactory.spigot.machine;

import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.Port;
import io.corefactory.core.logistics.PortItems;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * A lab tool's barrel as a logistics port. It takes an item exactly when a hopper would: into an input slot when some process
 * of the lab tool takes the item as an input, else into the fuel slot when the lab tool burns fuel and the item is fuel. The
 * slot is chosen by the hopper's rule, the first slot already holding the item with room, else the first empty one. It gives
 * items from its output slots, first slot first. The rules are the hopper rules of {@link HopperRules}; the slots are the
 * process stacks of the menu, so the port is tested with plain stacks.
 */
public final class LabPort implements Port {

    private final List<Stack> inputs;
    private final List<Stack> fuel;
    private final List<Stack> outputs;
    private final Predicate<ItemRef> takesInput;
    private final Predicate<ItemRef> takesFuel;
    private final ToIntFunction<ItemRef> limit;

    /**
     * A lab tool with no fuel slot, so a belt feeds its inputs only.
     *
     * @param inputs     the input slots, in input order
     * @param outputs    the output slots, in output order
     * @param takesInput true for an item some process of the lab tool takes as an input
     * @param limit      the largest stack of a reference
     */
    public LabPort(List<Stack> inputs, List<Stack> outputs, Predicate<ItemRef> takesInput, ToIntFunction<ItemRef> limit) {
        this(inputs, List.of(), outputs, takesInput, ref -> false, limit);
    }

    /**
     * @param inputs     the input slots, in input order
     * @param fuel       the fuel slots, in fuel order; empty for a lab tool that burns no fuel
     * @param outputs    the output slots, in output order
     * @param takesInput true for an item some process of the lab tool takes as an input
     * @param takesFuel  true for an item the fuel slot takes
     * @param limit      the largest stack of a reference
     */
    public LabPort(List<Stack> inputs, List<Stack> fuel, List<Stack> outputs, Predicate<ItemRef> takesInput,
                   Predicate<ItemRef> takesFuel, ToIntFunction<ItemRef> limit) {
        this.inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
        this.fuel = List.copyOf(Objects.requireNonNull(fuel, "fuel"));
        this.outputs = List.copyOf(Objects.requireNonNull(outputs, "outputs"));
        this.takesInput = Objects.requireNonNull(takesInput, "takesInput");
        this.takesFuel = Objects.requireNonNull(takesFuel, "takesFuel");
        this.limit = Objects.requireNonNull(limit, "limit");
    }

    /**
     * The port of a lab tool, or empty when the placed machine is a drill, its barrel is not loaded, or its menu has a player's
     * change that is not written to the barrel yet.
     */
    public static Optional<LabPort> of(PlacedMachine placed, ItemRefs refs) {
        if (placed.drill() != null) {
            return Optional.empty();
        }
        MachineMenu menu = placed.menu();
        if (menu.barrel().inventory().isEmpty() || menu.hasPending()) {
            return Optional.empty();
        }
        return Optional.of(new LabPort(menu.inputs(), menu.fuel(), menu.outputs(),
                ref -> menu.accepts(ref, MachineLayout.Kind.INPUT), ref -> menu.accepts(ref, MachineLayout.Kind.FUEL),
                refs::limit));
    }

    @Override
    public boolean accepts(Item item) {
        return stackFor(PortItems.refOf(item)).isPresent();
    }

    @Override
    public void accept(Item item) {
        ItemRef ref = PortItems.refOf(item);
        Optional<Stack> stack = stackFor(ref);
        if (stack.isEmpty() || !HopperTransfer.placeOne(1, stack.get(), ref, limit.applyAsInt(ref))) {
            throw new IllegalStateException("the lab tool does not take " + item.id());
        }
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
        for (Stack stack : inputs) {
            count += stack.count();
        }
        for (Stack stack : fuel) {
            count += stack.count();
        }
        for (Stack stack : outputs) {
            count += stack.count();
        }
        return count;
    }

    /**
     * The stack an item goes into: an input slot when a process takes the item, else the fuel slot when the lab tool burns it. Empty
     * when neither takes it or the slots of its role have no room.
     */
    private Optional<Stack> stackFor(ItemRef ref) {
        Optional<MachineSlots.Role> role = HopperRules.roleOf(takesInput, takesFuel, ref);
        if (role.isEmpty()) {
            return Optional.empty();
        }
        List<Stack> stacks = role.get() == MachineSlots.Role.FUEL ? fuel : inputs;
        OptionalInt position = positionIn(stacks, ref);
        return position.isPresent() ? Optional.of(stacks.get(position.getAsInt())) : Optional.empty();
    }

    /** The position in a list of slots that an item goes into, by the hopper's slot rule. */
    private OptionalInt positionIn(List<Stack> stacks, ItemRef ref) {
        List<ItemRef> held = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Stack stack : stacks) {
            held.add(stack.ref());
            counts.add(stack.count());
        }
        return HopperRules.insertionSlot(held, counts, ref, limit.applyAsInt(ref));
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
