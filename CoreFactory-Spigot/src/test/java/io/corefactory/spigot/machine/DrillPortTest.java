package io.corefactory.spigot.machine;

import io.corefactory.core.logistics.Item;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DrillPortTest {

    private static final ItemRef PETROLEUM = ItemRef.item("petroleum");
    private static final ItemRef IRON_ORE = ItemRef.material("IRON_ORE");

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

    @Test
    void aDrillNeverAcceptsAnItemFromAPicker() {
        DrillPort port = new DrillPort(List.of(empty(), empty()));
        assertThat(port.accepts(Item.component("petroleum"))).isFalse();
        assertThat(port.accepts(Item.material("CHARCOAL"))).isFalse();
        assertThat(port.accepts(Item.material("IRON_ORE"))).isFalse();
    }

    @Test
    void aDrillRefusesAnItemThatIsPutInAnywayAndChangesNothing() {
        MemoryStack output = new MemoryStack(PETROLEUM, 4);
        DrillPort port = new DrillPort(List.of(output));
        assertThatThrownBy(() -> port.accept(Item.component("petroleum"))).isInstanceOf(IllegalStateException.class);
        assertThat(output.count()).isEqualTo(4);
        assertThat(port.storedCount()).isEqualTo(4);
    }

    @Test
    void anOutputIsGivenFirstNonEmptyOutputFirstAndTakenOneAtATime() {
        MemoryStack empty = empty();
        MemoryStack petroleum = new MemoryStack(PETROLEUM, 2);
        MemoryStack later = new MemoryStack(IRON_ORE, 5);
        DrillPort port = new DrillPort(List.of(empty, petroleum, later));
        assertThat(port.peekOutput()).isEqualTo(Item.component("petroleum"));
        assertThat(petroleum.count()).isEqualTo(2);
        assertThat(port.takeOutput()).isEqualTo(Item.component("petroleum"));
        assertThat(petroleum.count()).isEqualTo(1);
        assertThat(port.takeOutput()).isEqualTo(Item.component("petroleum"));
        assertThat(petroleum.count()).isZero();
        assertThat(port.peekOutput()).isEqualTo(Item.material("IRON_ORE"));
        assertThat(later.count()).isEqualTo(5);
    }

    @Test
    void anEmptyDrillGivesNothing() {
        DrillPort port = new DrillPort(List.of(empty(), empty()));
        assertThat(port.peekOutput()).isNull();
        assertThat(port.takeOutput()).isNull();
        assertThat(port.storedCount()).isZero();
    }

    @Test
    void theStoredCountIsTheItemsInTheOutputSlotsOnly() {
        DrillPort port = new DrillPort(List.of(new MemoryStack(PETROLEUM, 3), empty(), new MemoryStack(IRON_ORE, 7)));
        assertThat(port.storedCount()).isEqualTo(10);
    }
}
