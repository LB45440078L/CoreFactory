package io.corefactory.paper.machine;

import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Portion;
import io.corefactory.core.process.ProcessDefinition;
import io.corefactory.core.process.ProcessEngine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The decisions a hopper's move meets at a lab tool's barrel. An item goes into an input slot when some process takes it,
 * or into a fuel slot when the machine burns fuel and the item is fuel. Only output slots give items up. Pure over item
 * references, slot contents and flags, so it is tested without a server.
 */
public final class HopperRules {

    /** What the move does. */
    public enum Action {
        /** One item goes into an input slot of the lab tool. */
        PUT_INPUT,
        /** One item goes into the fuel slot of the lab tool. */
        PUT_FUEL,
        /** One item leaves an output slot of the lab tool. */
        TAKE_OUTPUT,
        /** The move is cancelled and nothing changes. */
        REFUSE
    }

    private HopperRules() {
    }

    /**
     * The slot role an item may enter at a lab tool. Inputs win over fuel when an item is both.
     *
     * @param takesInput   true when the lab tool takes the item as an input
     * @param hasFuelSlots true when the lab tool burns fuel
     * @param item         the item
     * @return the role of the slot it enters, or empty when the lab tool takes it in no slot
     */
    public static Optional<MachineSlots.Role> roleOf(Predicate<ItemRef> takesInput, boolean hasFuelSlots, ItemRef item) {
        return roleOf(takesInput, ref -> hasFuelSlots && ProcessEngine.FUEL.contains(ref), item);
    }

    /**
     * The slot role an item may enter at a machine, with the fuel the machine takes given as a test. Inputs win over fuel when
     * an item is both. A drill has no inputs and no fuel slot, so no item is taken into it.
     *
     * @param takesInput true when the machine takes the item as an input
     * @param takesFuel  true when the machine's fuel slot takes the item
     * @param item       the item
     * @return the role of the slot it enters, or empty when the machine takes it in no slot
     */
    public static Optional<MachineSlots.Role> roleOf(Predicate<ItemRef> takesInput, Predicate<ItemRef> takesFuel,
                                                     ItemRef item) {
        if (takesInput.test(item)) {
            return Optional.of(MachineSlots.Role.INPUT);
        }
        if (takesFuel.test(item)) {
            return Optional.of(MachineSlots.Role.FUEL);
        }
        return Optional.empty();
    }

    /**
     * The position, within a list of the role's slots, of the slot an item goes into: the first slot already holding the same
     * item with room, else the first empty slot.
     *
     * @param held   what each slot of the role holds, null for an empty slot
     * @param counts how many each slot holds
     * @param item   the item
     * @param limit  the largest stack of the item
     * @return the position in the lists, or empty when no slot has room
     */
    /**
     * The position of the slot an item goes into in a lab tool's input slots. The first slot already holding the item with room
     * comes first. Then an empty slot: the slot the ingredient owns, by its place in the machine's ingredient order, or a slot no
     * ingredient owns. When the machine has more ingredients than slots, an empty slot takes the item only if it leaves each
     * other input of a recipe with the item a slot. An item never takes a slot another ingredient needs.
     *
     * @param held      what each slot holds, null for an empty slot
     * @param counts    how many each slot holds
     * @param item      the item
     * @param limit     the largest stack of the item
     * @param processes the processes of the machine
     * @return the position in the lists, or empty when the item has no slot
     */
    public static OptionalInt insertionSlot(List<ItemRef> held, List<Integer> counts, ItemRef item, int limit,
                                            Collection<ProcessDefinition> processes) {
        for (int i = 0; i < held.size(); i++) {
            if (item.equals(held.get(i)) && counts.get(i) < limit) {
                return OptionalInt.of(i);
            }
        }
        List<ItemRef> ingredients = ingredientsOf(processes);
        if (ingredients.size() <= held.size()) {
            // Each ingredient owns the slot at its position in the machine's ingredient order. The item takes its own slot first,
            // then a slot no ingredient owns, and never the slot of another ingredient.
            for (int i = 0; i < held.size(); i++) {
                if (held.get(i) == null && i < ingredients.size() && item.equals(ingredients.get(i))) {
                    return OptionalInt.of(i);
                }
            }
            for (int i = 0; i < held.size(); i++) {
                if (held.get(i) == null && i >= ingredients.size()) {
                    return OptionalInt.of(i);
                }
            }
            return OptionalInt.empty();
        }
        // More ingredients than slots across the machine's recipes, so no slot is owned. An empty slot takes the item only when
        // every other ingredient of a recipe that takes the item still has an empty slot left.
        if (!emptySlotKeptFor(held, item, processes)) {
            return OptionalInt.empty();
        }
        for (int i = 0; i < held.size(); i++) {
            if (held.get(i) == null) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    /** The distinct inputs of the processes, in the order the processes give them. */
    private static List<ItemRef> ingredientsOf(Collection<ProcessDefinition> processes) {
        Set<ItemRef> ingredients = new LinkedHashSet<>();
        for (ProcessDefinition process : processes) {
            for (Portion input : process.inputs()) {
                ingredients.add(input.ref());
            }
        }
        return new ArrayList<>(ingredients);
    }

    /**
     * True when an empty slot may take the item. Some recipe of two or more inputs that takes the item must still have an empty slot
     * for each of its other inputs that no slot holds yet. A recipe with a single input reserves nothing, and an item that no
     * two-input recipe takes may take an empty slot.
     */
    private static boolean emptySlotKeptFor(List<ItemRef> held, ItemRef item, Collection<ProcessDefinition> processes) {
        long empty = held.stream().filter(Objects::isNull).count();
        boolean inTwoInputRecipe = false;
        for (ProcessDefinition process : processes) {
            Set<ItemRef> inputs = process.inputs().stream().map(Portion::ref).collect(Collectors.toSet());
            if (!inputs.contains(item) || inputs.size() < 2) {
                continue;
            }
            inTwoInputRecipe = true;
            long missing = inputs.stream().filter(ref -> !item.equals(ref) && !held.contains(ref)).count();
            if (missing < empty) {
                return true;
            }
        }
        return !inTwoInputRecipe;
    }

    public static OptionalInt insertionSlot(List<ItemRef> held, List<Integer> counts, ItemRef item, int limit) {
        for (int i = 0; i < held.size(); i++) {
            if (item.equals(held.get(i)) && counts.get(i) < limit) {
                return OptionalInt.of(i);
            }
        }
        for (int i = 0; i < held.size(); i++) {
            if (held.get(i) == null) {
                return OptionalInt.of(i);
            }
        }
        return OptionalInt.empty();
    }

    /**
     * The decision for an item moving into a lab tool.
     *
     * @param role      the slot role from {@link #roleOf}, empty when the item is not valid for the lab tool
     * @param slotFound true when {@link #insertionSlot} found a slot of that role
     * @return the put action, or refuse
     */
    public static Action intake(Optional<MachineSlots.Role> role, boolean slotFound) {
        if (role.isEmpty() || !slotFound) {
            return Action.REFUSE;
        }
        return role.get() == MachineSlots.Role.FUEL ? Action.PUT_FUEL : Action.PUT_INPUT;
    }

    /**
     * True when an item is a product of one of the given processes. Only products leave a lab tool by a hopper.
     *
     * @param item      the item
     * @param processes the processes of the lab tool
     * @return true when some process of the lab tool makes the item
     */
    public static boolean isProduct(ItemRef item, Collection<ProcessDefinition> processes) {
        return processes.stream().flatMap(process -> process.outputs().stream())
                .anyMatch(product -> product.ref().equals(item));
    }

    /**
     * The decision for an item leaving a lab tool.
     *
     * @param outputHoldsItem true when the output slot taken from holds the item
     * @param destinationTakes true when the destination has room for one more of the item
     * @return take the output, or refuse
     */
    public static Action outflow(boolean outputHoldsItem, boolean destinationTakes) {
        return outputHoldsItem && destinationTakes ? Action.TAKE_OUTPUT : Action.REFUSE;
    }
}
