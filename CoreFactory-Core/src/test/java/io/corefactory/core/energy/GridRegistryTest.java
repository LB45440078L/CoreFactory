package io.corefactory.core.energy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GridRegistryTest {

    private static Set<String> set(String... ids) {
        return Set.of(ids);
    }

    private static GridRegistry registry(Map<String, Integer> ids, int next) {
        return new GridRegistry(ids, next);
    }

    @Test
    void firstComponentsGetFreshIdsInOrderOfTheirSmallestPylon() {
        GridRegistry first = GridRegistry.EMPTY.assign(List.of(set("c"), set("a", "b")));
        assertThat(first.idOf("a")).isZero();
        assertThat(first.idOf("b")).isZero();
        assertThat(first.idOf("c")).isEqualTo(1);
        assertThat(first.nextFreshId()).isEqualTo(2);
    }

    @Test
    void theSameComponentsKeepTheirIds() {
        GridRegistry first = GridRegistry.EMPTY.assign(List.of(set("a"), set("b")));
        GridRegistry second = first.assign(List.of(set("b"), set("a")));
        assertThat(second).isEqualTo(first);
    }

    @Test
    void aJoinKeepsTheSmallestIdOfTheGridsInvolved() {
        GridRegistry before = registry(Map.of("a", 3, "b", 7), 8);
        GridRegistry joined = before.assign(List.of(set("a", "b")));
        assertThat(joined.idOf("a")).isEqualTo(3);
        assertThat(joined.idOf("b")).isEqualTo(3);
        assertThat(joined.nextFreshId()).isEqualTo(8);
    }

    @Test
    void aJoinOfThreeGridsKeepsTheSmallest() {
        GridRegistry before = registry(Map.of("a", 5, "b", 2, "c", 9), 10);
        GridRegistry joined = before.assign(List.of(set("a", "b", "c")));
        assertThat(joined.idOf("c")).isEqualTo(2);
        assertThat(joined.idOf("a")).isEqualTo(2);
    }

    @Test
    void aSplitGivesTheSmallestKeptIdToTheComponentWithTheSmallestPylon() {
        GridRegistry before = registry(Map.of("a", 4, "b", 4, "c", 4), 5);
        GridRegistry split = before.assign(List.of(set("a"), set("b", "c")));
        assertThat(split.idOf("a")).isEqualTo(4);
        assertThat(split.idOf("b")).isEqualTo(5);
        assertThat(split.idOf("c")).isEqualTo(5);
        assertThat(split.nextFreshId()).isEqualTo(6);
    }

    @Test
    void theComponentHoldingTheSmallestOldPylonKeepsTheIdEvenWhenItIsNotFirst() {
        GridRegistry before = registry(Map.of("a", 4, "b", 4, "c", 4), 5);
        GridRegistry split = before.assign(List.of(set("b"), set("a", "c")));
        assertThat(split.idOf("a")).isEqualTo(4);
        assertThat(split.idOf("c")).isEqualTo(4);
        assertThat(split.idOf("b")).isEqualTo(5);
    }

    @Test
    void aRemovedSmallestPylonPassesTheIdToTheNextSmallestOne() {
        GridRegistry before = registry(Map.of("a", 4, "b", 4), 5);
        GridRegistry after = before.assign(List.of(set("b")));
        assertThat(after.idOf("b")).isEqualTo(4);
        assertThat(after.nextFreshId()).isEqualTo(5);
    }

    @Test
    void aDissolvedIdIsNeverGivenToALaterGrid() {
        GridRegistry one = GridRegistry.EMPTY.assign(List.of(set("a"), set("b")));
        GridRegistry joined = one.assign(List.of(set("a", "b")));
        GridRegistry later = joined.assign(List.of(set("a"), set("b")));
        assertThat(later.idOf("a")).isEqualTo(0);
        assertThat(later.idOf("b")).isEqualTo(2);
    }

    @Test
    void everyPylonInTheResultHasAnIdAndNoTwoComponentsShareOne() {
        GridRegistry before = registry(Map.of("a", 0, "b", 0, "c", 1, "d", 1), 2);
        GridRegistry after = before.assign(List.of(set("a"), set("b", "c"), set("d")));
        assertThat(after.idOf("a")).isEqualTo(0);
        assertThat(after.idOf("b")).isEqualTo(1);
        assertThat(after.idOf("c")).isEqualTo(1);
        assertThat(after.idOf("d")).isEqualTo(2);
        assertThat(Set.copyOf(after.idByPylon().values())).hasSize(3);
    }

    @Test
    void aPylonInTwoComponentsIsRefused() {
        assertThatThrownBy(() -> GridRegistry.EMPTY.assign(List.of(set("a"), set("a", "b"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anEmptyComponentIsRefused() {
        assertThatThrownBy(() -> GridRegistry.EMPTY.assign(List.of(Set.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anUnknownPylonHasNoId() {
        assertThatThrownBy(() -> GridRegistry.EMPTY.idOf("ghost")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theNextFreshIdCannotBeNegative() {
        assertThatThrownBy(() -> registry(Map.of(), -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
