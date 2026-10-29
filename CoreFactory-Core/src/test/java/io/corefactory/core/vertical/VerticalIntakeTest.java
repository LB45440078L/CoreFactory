package io.corefactory.core.vertical;

import io.corefactory.core.logistics.Item;
import io.corefactory.core.logistics.Port;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VerticalIntakeTest {

    private static final Item STONE = Item.material("minecraft:stone");
    private static final Item DIRT = Item.material("minecraft:dirt");

    /** A port that gives the items it holds, in order, and refuses nothing. */
    private static class StockPort implements Port {

        private final Deque<Item> stock = new ArrayDeque<>();
        private int taken;

        StockPort(Item... items) {
            for (Item item : items) {
                stock.add(item);
            }
        }

        @Override
        public boolean accepts(Item item) {
            return false;
        }

        @Override
        public void accept(Item item) {
            throw new IllegalStateException("a source takes nothing");
        }

        @Override
        public Item peekOutput() {
            return stock.peek();
        }

        @Override
        public Item takeOutput() {
            taken++;
            return stock.poll();
        }

        @Override
        public int storedCount() {
            return stock.size();
        }
    }

    @Test
    void pullsOneItemFromTheSourceIntoTheInputBlock() {
        VerticalCarry carry = new VerticalCarry(5);
        List<Item> items = new ArrayList<>();
        StockPort chest = new StockPort(STONE, DIRT);

        Item pulled = VerticalIntake.pull(carry, items, 1, chest);

        assertThat(pulled).isEqualTo(STONE);
        assertThat(items).containsExactly(STONE);
        assertThat(carry.positions()).containsExactly(0.0);
        assertThat(chest.storedCount()).isEqualTo(1);
    }

    @Test
    void newItemGoesBehindItemsAlreadyOnTheTower() {
        VerticalCarry carry = new VerticalCarry(5);
        List<Item> items = new ArrayList<>();
        carry.place(2, 1);
        items.add(DIRT);

        VerticalIntake.pull(carry, items, 1, new StockPort(STONE));

        assertThat(items).containsExactly(DIRT, STONE);
        assertThat(carry.positions()).containsExactly(2.0, 0.0);
    }

    @Test
    void takesNothingWhenTheInputBlockHasNoRoom() {
        VerticalCarry carry = new VerticalCarry(5);
        List<Item> items = new ArrayList<>();
        carry.place(0, 1);
        items.add(DIRT);
        StockPort chest = new StockPort(STONE);

        assertThat(VerticalIntake.pull(carry, items, 1, chest)).isNull();
        assertThat(items).containsExactly(DIRT);
        assertThat(chest.storedCount()).isEqualTo(1);
    }

    @Test
    void takesNothingFromAnEmptySource() {
        VerticalCarry carry = new VerticalCarry(5);
        List<Item> items = new ArrayList<>();

        assertThat(VerticalIntake.pull(carry, items, 1, new StockPort())).isNull();
        assertThat(items).isEmpty();
        assertThat(carry.size()).isZero();
    }

    @Test
    void takesNothingWhenTheSourceOffersNothingToTake() {
        VerticalCarry carry = new VerticalCarry(5);
        List<Item> items = new ArrayList<>();
        Port claimed = new StockPort(STONE) {
            @Override
            public Item peekOutput() {
                return null;
            }
        };

        assertThat(VerticalIntake.pull(carry, items, 1, claimed)).isNull();
        assertThat(items).isEmpty();
    }
}
