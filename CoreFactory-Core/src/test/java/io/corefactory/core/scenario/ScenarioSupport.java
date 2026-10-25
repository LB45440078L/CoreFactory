package io.corefactory.core.scenario;

import io.corefactory.core.config.TestConfig;
import io.corefactory.core.drill.DrillStorage;
import io.corefactory.core.energy.GeneratorFuel;
import io.corefactory.core.energy.GeneratorKind;
import io.corefactory.core.energy.GeneratorState;
import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.Port;
import io.corefactory.core.logistics.PortItems;
import io.corefactory.core.logistics.StorageRules;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.ProcessCatalog;
import io.corefactory.core.process.Stack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
 * The parts of the production line that are not the Core's own models: the stacks of its containers, the ports that stand
 * for them, and the reader of the bundled YAML. Each port gives and takes items the way the plugin's port of the same kind
 * does, so the simulation moves items the way the plugin would.
 */
final class ScenarioSupport {

    /**
     * The largest stack of every item in the scenario. The plugin reads each item's own stack limit, which the Core does not
     * hold, so 64 is an assumption made here for every item.
     */
    static final int STACK_LIMIT = 64;

    private ScenarioSupport() {
    }

    /**
     * A file of the plugin's resources, read as plain data. The tests run from the CoreFactory-Core module, so the resources
     * are one folder up.
     *
     * @param name the file name, such as {@code processes.yml}
     */
    static Map<String, Object> bundled(String name) {
        Path path = Path.of("..", "CoreFactory-Paper", "src", "main", "resources", name);
        try {
            return TestConfig.yaml(Files.readString(path));
        } catch (IOException e) {
            throw new IllegalStateException("cannot read the bundled file " + path.toAbsolutePath(), e);
        }
    }

    /** Empty slots, one per position. */
    static List<Stack> slots(int size) {
        List<Stack> slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            slots.add(new Slot());
        }
        return slots;
    }

    /** How many of one item the slots hold. */
    static int countOf(List<Stack> slots, ItemRef ref) {
        int total = 0;
        for (Stack slot : slots) {
            if (ref.equals(slot.ref())) {
                total += slot.count();
            }
        }
        return total;
    }

    /** The first slot that holds an item, or null when every slot is empty. */
    private static Stack firstStored(List<Stack> slots) {
        for (Stack slot : slots) {
            if (slot.count() > 0) {
                return slot;
            }
        }
        return null;
    }

    /** Puts one item into the slots the storage rules choose for it. */
    private static void store(List<Stack> slots, Item item) {
        ItemRef ref = PortItems.refOf(item);
        OptionalInt index = StorageRules.slotFor(STACK_LIMIT, slots, ref);
        if (index.isEmpty()) {
            throw new IllegalStateException("no slot takes " + item.id());
        }
        Stack slot = slots.get(index.getAsInt());
        slot.set(ref, slot.count() + 1);
    }

    /** A slot of a container, held in memory. */
    static final class Slot implements Stack {

        private ItemRef ref;
        private int count;

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

    /**
     * A chest, such as the double chest the charcoal is fed from or the double copper chest the charcoal of the ore line
     * goes into. It takes an item its slots have room for, and gives out the item of its first stored slot.
     */
    static final class ChestPort implements Port {

        private final List<Stack> slots;

        ChestPort(List<Stack> slots) {
            this.slots = slots;
        }

        @Override
        public boolean accepts(Item item) {
            return StorageRules.accepts(STACK_LIMIT, slots, PortItems.refOf(item));
        }

        @Override
        public void accept(Item item) {
            store(slots, item);
        }

        @Override
        public Item peekOutput() {
            Stack slot = firstStored(slots);
            return slot == null ? null : PortItems.itemOf(slot.ref());
        }

        @Override
        public Item takeOutput() {
            Stack slot = firstStored(slots);
            if (slot == null) {
                return null;
            }
            ItemRef ref = slot.ref();
            slot.set(ref, slot.count() - 1);
            return PortItems.itemOf(ref);
        }

        @Override
        public int storedCount() {
            return DrillStorage.stored(slots);
        }

        /** How many of one item the chest holds. */
        int count(ItemRef ref) {
            return countOf(slots, ref);
        }
    }

    /**
     * The input slots of a lab machine. It takes an item only when a process of the machine takes it and its slot has room,
     * as the plugin's lab port does. It gives nothing.
     */
    static final class MachineInputPort implements Port {

        private final ProcessCatalog catalog;
        private final String machineId;
        private final List<Stack> inputs;

        MachineInputPort(ProcessCatalog catalog, String machineId, List<Stack> inputs) {
            this.catalog = catalog;
            this.machineId = machineId;
            this.inputs = inputs;
        }

        @Override
        public boolean accepts(Item item) {
            ItemRef ref = PortItems.refOf(item);
            return catalog.acceptsInput(machineId, ref) && StorageRules.accepts(STACK_LIMIT, inputs, ref);
        }

        @Override
        public void accept(Item item) {
            store(inputs, item);
        }

        @Override
        public Item peekOutput() {
            return null;
        }

        @Override
        public Item takeOutput() {
            return null;
        }

        @Override
        public int storedCount() {
            return DrillStorage.stored(inputs);
        }
    }

    /**
     * The barrel of an ore drill, read by the picker that takes its output. The drill puts its items into the barrel itself; a
     * port takes nothing in.
     */
    static final class BarrelPort implements Port {

        private final List<Stack> outputs;

        BarrelPort(List<Stack> outputs) {
            this.outputs = outputs;
        }

        @Override
        public boolean accepts(Item item) {
            return false;
        }

        @Override
        public void accept(Item item) {
            throw new IllegalStateException("a drill takes nothing through a port: " + item.id());
        }

        @Override
        public Item peekOutput() {
            Stack slot = firstStored(outputs);
            return slot == null ? null : PortItems.itemOf(slot.ref());
        }

        @Override
        public Item takeOutput() {
            Stack slot = firstStored(outputs);
            if (slot == null) {
                return null;
            }
            ItemRef ref = slot.ref();
            slot.set(ref, slot.count() - 1);
            return PortItems.itemOf(ref);
        }

        @Override
        public int storedCount() {
            return DrillStorage.stored(outputs);
        }
    }

    /**
     * The fuel input of a generator. It takes one charcoal while the generator keeps fewer than {@link GeneratorFuel#CAP}
     * items, and the generator's state carries the count. The caller reads and writes the state around each tick.
     */
    static final class FuelPort implements Port {

        private final GeneratorKind kind;
        private GeneratorState state;
        private int accepted;

        FuelPort(GeneratorKind kind, GeneratorState state) {
            this.kind = kind;
            this.state = state;
        }

        /** The generator's fuel state as the port holds it now. */
        GeneratorState state() {
            return state;
        }

        /** Replaces the state after a tick of the generator. */
        void state(GeneratorState next) {
            this.state = next;
        }

        /** How many items the port has taken in, in all. */
        int accepted() {
            return accepted;
        }

        @Override
        public boolean accepts(Item item) {
            GeneratorFuel.Source source = Item.COMPONENT.equals(item.kind())
                    ? GeneratorFuel.Source.COMPONENT : GeneratorFuel.Source.MATERIAL;
            return state.fuelItems() < GeneratorFuel.CAP && GeneratorFuel.accepts(kind, source, item.id());
        }

        @Override
        public void accept(Item item) {
            if (!accepts(item)) {
                throw new IllegalStateException("the generator does not take " + item.id());
            }
            state = state.withFuel(1);
            accepted++;
        }

        @Override
        public Item peekOutput() {
            return null;
        }

        @Override
        public Item takeOutput() {
            return null;
        }

        @Override
        public int storedCount() {
            return state.fuelItems();
        }
    }
}
