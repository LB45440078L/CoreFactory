package io.corefactory.paper.vertical;

import io.corefactory.core.logistics.Item;
import io.corefactory.core.vertical.VerticalCarry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerticalBeltPortTest {

    private static final int CAPACITY_LEVEL_1 = 4;
    private static final Item STONE = Item.material("STONE");
    private static final Item DIRT = Item.material("DIRT");

    private final VerticalCarry carry = new VerticalCarry(3);
    private final List<Item> items = new ArrayList<>();

    private VerticalBeltPort port(int block, boolean permitted) {
        return new VerticalBeltPort(carry, items, block, CAPACITY_LEVEL_1, () -> permitted);
    }

    private void putAt(int block, Item item) {
        int slot = carry.place(block, CAPACITY_LEVEL_1);
        items.add(slot, item);
    }

    @Test
    void aPickerTakesTheItemOfItsCellNearestTheOutputFirst() {
        putAt(1, STONE);
        carry.place(1.8, CAPACITY_LEVEL_1);
        items.add(0, DIRT);
        VerticalBeltPort port = port(1, true);

        assertThat(port.peekOutput()).isEqualTo(DIRT);
        assertThat(port.takeOutput()).isEqualTo(DIRT);
        assertThat(port.storedCount()).isEqualTo(1);
        assertThat(port.takeOutput()).isEqualTo(STONE);
        assertThat(port.storedCount()).isZero();
    }

    @Test
    void anEmptyCellGivesNothing() {
        VerticalBeltPort port = port(0, true);

        assertThat(port.peekOutput()).isNull();
        assertThat(port.takeOutput()).isNull();
        assertThat(port.storedCount()).isZero();
    }

    @Test
    void aClaimThatRefusesTheTakeLeavesTheItemInPlace() {
        putAt(0, STONE);
        VerticalBeltPort port = port(0, false);

        assertThat(port.peekOutput()).isNull();
        assertThatThrownBy(port::takeOutput).isInstanceOf(IllegalStateException.class);
        assertThat(items).containsExactly(STONE);
        assertThat(port.storedCount()).isEqualTo(1);
    }

    @Test
    void aPutIsAcceptedWhileTheTowerHasRoomAtTheEntry() {
        VerticalBeltPort port = port(0, true);

        assertThat(port.accepts(STONE)).isTrue();
        port.accept(STONE);

        assertThat(items).containsExactly(STONE);
        assertThat(port.storedCount()).isEqualTo(1);
    }

    @Test
    void aPutIsRefusedWhenTheTowerHasNoRoomAtTheEntry() {
        carry.place(0.0, CAPACITY_LEVEL_1);
        items.add(DIRT);
        VerticalBeltPort port = port(0, true);

        assertThat(port.accepts(STONE)).isFalse();
        assertThatThrownBy(() -> port.accept(STONE)).isInstanceOf(IllegalStateException.class);
        assertThat(items).containsExactly(DIRT);
    }
}
