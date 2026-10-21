package io.corefactory.spigot.machine;

import io.corefactory.core.logistics.Item;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.ProcessEngine;
import io.corefactory.core.process.Stack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LabPortTest {

    private static final ItemRef ORE = ItemRef.material("IRON_ORE");
    private static final ItemRef INGOT = ItemRef.material("IRON_INGOT");
    private static final ItemRef STONE = ItemRef.material("STONE");
    private static final ItemRef CHARCOAL = ItemRef.material("CHARCOAL");

    /** A slot held in memory, with the same rules as a barrel slot: a count of zero or less empties it. */
    private static final class MemoryStack implements Stack {

        private ItemRef ref;
        private int count;

        MemoryStack(ItemRef ref, int count) {
            this.ref = ref;
            this.count = count;
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
            if (ref == null || count <= 0) {
                this.ref = null;
                this.count = 0;
                return;
            }
            this.ref = ref;
            this.count = count;
        }
    }

    private static MemoryStack empty() {
        return new MemoryStack(null, 0);
    }

    private static LabPort port(List<Stack> inputs, List<Stack> outputs) {
        return new LabPort(inputs, outputs, ref -> ORE.equals(ref), ref -> 64);
    }

    private static Item ore() {
        return Item.material("IRON_ORE");
    }

    @Test
    void anInputTheLabToolTakesGoesIntoAnEmptyInputSlot() {
        MemoryStack first = empty();
        LabPort port = port(List.of(first, empty()), List.of());
        assertThat(port.accepts(ore())).isTrue();
        port.accept(ore());
        assertThat(first.count()).isEqualTo(1);
        assertThat(first.ref()).isEqualTo(ORE);
    }

    @Test
    void anItemTheLabToolDoesNotTakeAsAnInputIsRefused() {
        LabPort port = port(List.of(empty()), List.of());
        assertThat(port.accepts(Item.material("STONE"))).isFalse();
        assertThatThrownBy(() -> port.accept(Item.material("STONE"))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aFuelItemIsNotAnInputSoAPickerDoesNotFeedTheFuelSlot() {
        MemoryStack fuel = new MemoryStack(ItemRef.material("CHARCOAL"), 4);
        LabPort port = port(List.of(fuel, empty()), List.of());
        assertThat(port.accepts(Item.material("CHARCOAL"))).isFalse();
    }

    @Test
    void anItemGoesToTheFirstSlotHoldingTheSameItemWithRoomThenToAnEmptyOne() {
        MemoryStack full = new MemoryStack(ORE, 64);
        MemoryStack partial = new MemoryStack(ORE, 10);
        MemoryStack other = new MemoryStack(STONE, 1);
        MemoryStack spare = empty();
        LabPort port = port(List.of(full, other, partial, spare), List.of());
        port.accept(ore());
        assertThat(partial.count()).isEqualTo(11);
        assertThat(full.count()).isEqualTo(64);
        assertThat(spare.count()).isZero();
    }

    @Test
    void aFullInputSideRefusesAndChangesNothing() {
        MemoryStack a = new MemoryStack(ORE, 64);
        MemoryStack b = new MemoryStack(STONE, 3);
        LabPort port = port(List.of(a, b), List.of());
        assertThat(port.accepts(ore())).isFalse();
        assertThat(a.count()).isEqualTo(64);
        assertThat(b.count()).isEqualTo(3);
    }

    @Test
    void anOutputIsGivenFirstOutputFirstAndTakenOneAtATime() {
        MemoryStack empty = empty();
        MemoryStack ingots = new MemoryStack(INGOT, 3);
        LabPort port = port(List.of(), List.of(empty, ingots));
        assertThat(port.peekOutput()).isEqualTo(Item.material("IRON_INGOT"));
        assertThat(ingots.count()).isEqualTo(3);
        assertThat(port.takeOutput()).isEqualTo(Item.material("IRON_INGOT"));
        assertThat(ingots.count()).isEqualTo(2);
        port.takeOutput();
        port.takeOutput();
        assertThat(ingots.count()).isZero();
        assertThat(port.peekOutput()).isNull();
        assertThat(port.takeOutput()).isNull();
    }

    @Test
    void theStoredCountIsTheItemsInItsInputAndOutputSlots() {
        LabPort port = port(List.of(new MemoryStack(ORE, 5), empty()), List.of(new MemoryStack(INGOT, 2)));
        assertThat(port.storedCount()).isEqualTo(7);
    }

    @Test
    void aComponentItemIsReadAsItsComponentReference() {
        ItemRef belt = ItemRef.item("belt_1");
        MemoryStack output = new MemoryStack(belt, 1);
        LabPort port = port(List.of(), List.of(output));
        assertThat(port.peekOutput()).isEqualTo(Item.component("belt_1"));
        assertThat(Objects.requireNonNull(port.takeOutput())).isEqualTo(Item.component("belt_1"));
    }

    /** A lab tool that burns fuel: the ore is its input, and the fuel takes the fuel rule of the engine. */
    private static LabPort furnace(List<Stack> inputs, List<Stack> fuel, List<Stack> outputs) {
        return new LabPort(inputs, fuel, outputs, ref -> ORE.equals(ref), ProcessEngine.FUEL::contains, ref -> 64);
    }

    @Test
    void aBeltFeedsTheFuelSlotOfALabToolThatBurnsFuel() {
        MemoryStack fuel = empty();
        LabPort port = furnace(List.of(empty()), List.of(fuel), List.of());
        assertThat(port.accepts(Item.material("CHARCOAL"))).isTrue();
        port.accept(Item.material("CHARCOAL"));
        assertThat(fuel.count()).isEqualTo(1);
        assertThat(fuel.ref()).isEqualTo(CHARCOAL);
    }

    @Test
    void aBeltFeedsTheCharcoalComponentToTheFuelSlotToo() {
        MemoryStack fuel = empty();
        LabPort port = furnace(List.of(empty()), List.of(fuel), List.of());
        assertThat(port.accepts(Item.component("charcoal"))).isTrue();
        port.accept(Item.component("charcoal"));
        assertThat(fuel.ref()).isEqualTo(ItemRef.item("charcoal"));
        assertThat(fuel.count()).isEqualTo(1);
    }

    @Test
    void anInputStillGoesToItsInputSlotAndFuelToItsFuelSlot() {
        MemoryStack ore = empty();
        MemoryStack fuel = empty();
        LabPort port = furnace(List.of(ore), List.of(fuel), List.of());
        port.accept(Item.material("IRON_ORE"));
        port.accept(Item.material("CHARCOAL"));
        assertThat(ore.ref()).isEqualTo(ORE);
        assertThat(ore.count()).isEqualTo(1);
        assertThat(fuel.ref()).isEqualTo(CHARCOAL);
        assertThat(fuel.count()).isEqualTo(1);
    }

    @Test
    void aFullFuelSlotAndAnUnrelatedItemAreRefusedAndChangeNothing() {
        MemoryStack fuel = new MemoryStack(CHARCOAL, 64);
        LabPort port = furnace(List.of(empty()), List.of(fuel), List.of());
        assertThat(port.accepts(Item.material("CHARCOAL"))).isFalse();
        assertThat(port.accepts(Item.material("STONE"))).isFalse();
        assertThat(fuel.count()).isEqualTo(64);
    }

    @Test
    void theStoredCountIncludesTheFuelSlot() {
        LabPort port = furnace(List.of(empty()), List.of(new MemoryStack(CHARCOAL, 3)),
                List.of(new MemoryStack(INGOT, 2)));
        assertThat(port.storedCount()).isEqualTo(5);
    }
}
