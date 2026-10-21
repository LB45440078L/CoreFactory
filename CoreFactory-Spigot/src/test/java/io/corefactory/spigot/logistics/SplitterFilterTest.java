package io.corefactory.spigot.logistics;

import io.corefactory.core.logistics.Item;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.corefactory.spigot.logistics.SplitterFilter.Outcome;
import static io.corefactory.spigot.logistics.SplitterFilter.Side;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SplitterFilterTest {

    private static final Item A = Item.material("A");
    private static final Item B = Item.material("B");
    private static final Item C = Item.component("belt_1");

    @Test
    void aTypeAddedToAListIsOnThatListOnly() {
        SplitterFilter filter = SplitterFilter.empty();
        assertThat(filter.add(Side.LEFT, A)).isEqualTo(Outcome.ADDED);
        assertThat(filter.items(Side.LEFT)).containsExactly(A);
        assertThat(filter.items(Side.RIGHT)).isEmpty();
        assertThat(filter.leftSet()).containsExactly(A);
    }

    @Test
    void theSameTypeTwiceOnOneListChangesNothing() {
        SplitterFilter filter = SplitterFilter.empty();
        filter.add(Side.RIGHT, A);
        assertThat(filter.add(Side.RIGHT, A)).isEqualTo(Outcome.ALREADY_HERE);
        assertThat(filter.items(Side.RIGHT)).containsExactly(A);
    }

    @Test
    void aTypeOnTheOtherListMovesOver() {
        SplitterFilter filter = SplitterFilter.empty();
        filter.add(Side.LEFT, A);
        filter.add(Side.LEFT, B);
        assertThat(filter.add(Side.RIGHT, A)).isEqualTo(Outcome.MOVED);
        assertThat(filter.items(Side.LEFT)).containsExactly(B);
        assertThat(filter.items(Side.RIGHT)).containsExactly(A);
        assertThat(filter.leftSet()).doesNotContain(A);
        assertThat(filter.rightSet()).containsExactly(A);
    }

    @Test
    void aListHoldsAtMostNineTypes() {
        SplitterFilter filter = SplitterFilter.empty();
        for (int i = 0; i < SplitterFilter.LIMIT; i++) {
            assertThat(filter.add(Side.LEFT, Item.material("M" + i))).isEqualTo(Outcome.ADDED);
        }
        assertThat(filter.add(Side.LEFT, Item.material("TENTH"))).isEqualTo(Outcome.FULL);
        assertThat(filter.items(Side.LEFT)).hasSize(SplitterFilter.LIMIT);
    }

    @Test
    void aMoveIntoAFullListIsRefusedAndTheTypeStaysWhereItWas() {
        SplitterFilter filter = SplitterFilter.empty();
        filter.add(Side.RIGHT, A);
        for (int i = 0; i < SplitterFilter.LIMIT; i++) {
            filter.add(Side.LEFT, Item.material("M" + i));
        }
        assertThat(filter.add(Side.LEFT, A)).isEqualTo(Outcome.FULL);
        assertThat(filter.items(Side.RIGHT)).containsExactly(A);
    }

    @Test
    void aTypeIsTakenOffAListByItsPosition() {
        SplitterFilter filter = SplitterFilter.empty();
        filter.add(Side.LEFT, A);
        filter.add(Side.LEFT, C);
        assertThat(filter.remove(Side.LEFT, 0)).contains(A);
        assertThat(filter.items(Side.LEFT)).containsExactly(C);
        assertThat(filter.remove(Side.LEFT, 5)).isEmpty();
        assertThat(filter.remove(Side.LEFT, -1)).isEmpty();
    }

    @Test
    void theSlotsOfEachListAreNineDistinctSlotsAndDoNotOverlap() {
        Set<Integer> left = new HashSet<>();
        for (int slot : SplitterFilter.slots(Side.LEFT)) {
            left.add(slot);
        }
        Set<Integer> right = new HashSet<>();
        for (int slot : SplitterFilter.slots(Side.RIGHT)) {
            right.add(slot);
        }
        assertThat(left).hasSize(SplitterFilter.LIMIT);
        assertThat(right).hasSize(SplitterFilter.LIMIT);
        assertThat(left).doesNotContainAnyElementsOf(right);
        for (int slot : SplitterFilter.slots(Side.LEFT)) {
            assertThat(slot).isBetween(0, SplitterMenu.SIZE - 1);
        }
        for (int slot : SplitterFilter.slots(Side.RIGHT)) {
            assertThat(slot).isBetween(0, SplitterMenu.SIZE - 1);
        }
    }

    @Test
    void aGuiSlotReadsBackItsPositionInItsList() {
        assertThat(SplitterFilter.indexOf(Side.LEFT, SplitterFilter.slots(Side.LEFT)[3])).isEqualTo(3);
        assertThat(SplitterFilter.indexOf(Side.RIGHT, SplitterFilter.slots(Side.RIGHT)[8])).isEqualTo(8);
        assertThat(SplitterFilter.indexOf(Side.LEFT, 0)).isEqualTo(-1);
        assertThat(SplitterFilter.indexOf(Side.LEFT, SplitterFilter.slots(Side.RIGHT)[0])).isEqualTo(-1);
    }

    @Test
    void aFilterMadeFromListsKeepsTheirOrder() {
        SplitterFilter filter = SplitterFilter.of(List.of(A, B), List.of(C));
        assertThat(filter.items(Side.LEFT)).containsExactly(A, B);
        assertThat(filter.items(Side.RIGHT)).containsExactly(C);
    }

    @Test
    void aTypeOnBothListsIsRefused() {
        assertThatThrownBy(() -> SplitterFilter.of(List.of(A), List.of(A))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aListLongerThanNineIsRefused() {
        List<Item> ten = List.of(Item.material("1"), Item.material("2"), Item.material("3"), Item.material("4"),
                Item.material("5"), Item.material("6"), Item.material("7"), Item.material("8"), Item.material("9"),
                Item.material("10"));
        assertThatThrownBy(() -> SplitterFilter.of(ten, List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
