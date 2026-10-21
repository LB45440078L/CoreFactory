package io.corefactory.core.process;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * Runs machines one tick at a time. A pass starts when a process has all its inputs and fuel. It runs for the process's
 * ticks. At the end, every product is rolled once against its chance. If the rolled products fit in the output slots, the
 * inputs and one fuel item are consumed and the products are deposited, and the machine goes idle. If they do not fit,
 * the pass waits with the same rolled products until there is room. Nothing is consumed while a pass waits.
 *
 * <p>Pure logic over {@link Stack} views, so it has no Bukkit types.
 */
public final class ProcessEngine {

    /**
     * The items a fuel slot takes: coal, vanilla charcoal, and the charcoal component. Coal ore drops the component when it is
     * broken by hand, and a drill gives vanilla charcoal, so a lab tool's fuel slot takes both forms of charcoal.
     */
    public static final Set<ItemRef> FUEL = Set.of(ItemRef.material("COAL"), ItemRef.material("CHARCOAL"),
            ItemRef.item("charcoal"));

    /** What one tick of a machine did. */
    public enum Step {
        /** No process can run. */
        IDLE,
        /** A pass is running. */
        RUNNING,
        /** A pass has finished but its products do not fit in the output slots yet. */
        BLOCKED,
        /** A pass finished this tick: inputs and fuel were consumed and products deposited. */
        COMPLETED
    }

    private record Placement(int index, ItemRef ref, int amount) {
    }

    private final ProcessCatalog catalog;
    private final Set<ItemRef> fuels;
    private final ToIntFunction<ItemRef> stackLimit;

    /**
     * @param catalog    the machines and processes
     * @param fuels      the items a fuel slot takes
     * @param stackLimit the largest stack of each item, which limits how much an output slot holds
     */
    public ProcessEngine(ProcessCatalog catalog, Set<ItemRef> fuels, ToIntFunction<ItemRef> stackLimit) {
        this.catalog = catalog;
        this.fuels = Set.copyOf(fuels);
        this.stackLimit = stackLimit;
    }

    /** The first process of the machine, in file order, whose inputs and fuel are all present. */
    public Optional<ProcessDefinition> select(MachineState state) {
        for (ProcessDefinition process : catalog.processesFor(state.machineId())) {
            if (supplied(process, state)) {
                return Optional.of(process);
            }
        }
        return Optional.empty();
    }

    /** True when the inputs of the process are all present in full, and its fuel is present when it burns fuel. */
    public boolean supplied(ProcessDefinition process, MachineState state) {
        for (Map.Entry<ItemRef, Integer> need : required(process).entrySet()) {
            if (count(state.inputs(), need.getKey()) < need.getValue()) {
                return false;
            }
        }
        return !process.fuel() || fuelCount(state.fuel()) >= 1;
    }

    /** Rolls every output of the process once, and returns the products that were made. */
    public List<Portion> roll(ProcessDefinition process, Random random) {
        List<Portion> made = new ArrayList<>();
        for (Product product : process.outputs()) {
            if (random.nextDouble() < product.chance()) {
                made.add(new Portion(product.ref(), product.amount()));
            }
        }
        return made;
    }

    /** True when the products can be deposited into the output slots as they are now. */
    public boolean fits(List<Stack> outputs, List<Portion> products) {
        return plan(outputs, products).isPresent();
    }

    /**
     * Advances the machine by one tick.
     *
     * @param state  the machine; its slots and process are updated
     * @param random the source of chance rolls
     * @return what the tick did
     */
    public Step tick(MachineState state, Random random) {
        if (state.processId() == null) {
            Optional<ProcessDefinition> next = select(state);
            if (next.isEmpty()) {
                return Step.IDLE;
            }
            state.start(next.get().id());
        }
        Optional<ProcessDefinition> current = catalog.process(state.processId());
        if (current.isEmpty() || !supplied(current.get(), state)) {
            state.stop();
            return Step.IDLE;
        }
        ProcessDefinition process = current.get();
        if (state.progress() < process.ticks()) {
            state.advance();
            if (state.progress() < process.ticks()) {
                return Step.RUNNING;
            }
        }
        if (state.pending() == null) {
            state.setPending(roll(process, random));
        }
        if (!fits(state.outputs(), state.pending())) {
            return Step.BLOCKED;
        }
        consume(process, state);
        deposit(state.outputs(), state.pending());
        state.stop();
        return Step.COMPLETED;
    }

    private Map<ItemRef, Integer> required(ProcessDefinition process) {
        Map<ItemRef, Integer> need = new LinkedHashMap<>();
        for (Portion input : process.inputs()) {
            need.merge(input.ref(), input.amount(), Integer::sum);
        }
        return need;
    }

    private void consume(ProcessDefinition process, MachineState state) {
        for (Map.Entry<ItemRef, Integer> need : required(process).entrySet()) {
            take(state.inputs(), need.getKey(), need.getValue());
        }
        if (process.fuel()) {
            takeFuel(state.fuel());
        }
    }

    private static void take(List<Stack> slots, ItemRef ref, int amount) {
        int left = amount;
        for (Stack slot : slots) {
            if (left == 0) {
                return;
            }
            if (ref.equals(slot.ref()) && slot.count() > 0) {
                int taken = Math.min(left, slot.count());
                slot.set(ref, slot.count() - taken);
                left -= taken;
            }
        }
    }

    private void takeFuel(List<Stack> slots) {
        for (Stack slot : slots) {
            if (slot.ref() != null && slot.count() > 0 && fuels.contains(slot.ref())) {
                slot.set(slot.ref(), slot.count() - 1);
                return;
            }
        }
    }

    private static int count(List<Stack> slots, ItemRef ref) {
        int total = 0;
        for (Stack slot : slots) {
            if (ref.equals(slot.ref())) {
                total += slot.count();
            }
        }
        return total;
    }

    private int fuelCount(List<Stack> slots) {
        int total = 0;
        for (Stack slot : slots) {
            if (slot.ref() != null && fuels.contains(slot.ref())) {
                total += slot.count();
            }
        }
        return total;
    }

    private void deposit(List<Stack> outputs, List<Portion> products) {
        for (Placement placement : plan(outputs, products).orElseThrow()) {
            Stack slot = outputs.get(placement.index());
            slot.set(placement.ref(), slot.count() + placement.amount());
        }
    }

    /** Where each product would go, or empty when some product has no room. Changes nothing. */
    private Optional<List<Placement>> plan(List<Stack> outputs, List<Portion> products) {
        int size = outputs.size();
        ItemRef[] refs = new ItemRef[size];
        int[] counts = new int[size];
        for (int i = 0; i < size; i++) {
            Stack slot = outputs.get(i);
            if (slot.ref() != null && slot.count() > 0) {
                refs[i] = slot.ref();
                counts[i] = slot.count();
            }
        }
        List<Placement> placements = new ArrayList<>();
        for (Portion product : products) {
            int limit = Math.max(1, stackLimit.applyAsInt(product.ref()));
            int left = product.amount();
            for (int i = 0; i < size && left > 0; i++) {
                if (product.ref().equals(refs[i]) && counts[i] < limit) {
                    int added = Math.min(limit - counts[i], left);
                    counts[i] += added;
                    left -= added;
                    placements.add(new Placement(i, product.ref(), added));
                }
            }
            for (int i = 0; i < size && left > 0; i++) {
                if (refs[i] == null) {
                    int added = Math.min(limit, left);
                    refs[i] = product.ref();
                    counts[i] = added;
                    left -= added;
                    placements.add(new Placement(i, product.ref(), added));
                }
            }
            if (left > 0) {
                return Optional.empty();
            }
        }
        return Optional.of(placements);
    }
}
