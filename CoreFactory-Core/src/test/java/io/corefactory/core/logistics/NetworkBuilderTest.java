package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static io.corefactory.core.logistics.Fixtures.A;
import static io.corefactory.core.logistics.Fixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NetworkBuilderTest {

    private static final LogisticsLevel L = LogisticsLevel.I;

    @Test
    void aBeltOnAnOccupiedCellIsRefused() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder().belt(at(0, 0), Dir.EAST, L);
        assertThatThrownBy(() -> builder.belt(at(0, 0), Dir.NORTH, L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("occupied");
    }

    @Test
    void aPickerOnAnOccupiedCellIsRefused() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder().belt(at(0, 0), Dir.EAST, L);
        assertThatThrownBy(() -> builder.picker("p", at(0, 0), Dir.WEST, Dir.EAST, L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("occupied");
    }

    @Test
    void aSplitterOnAnOccupiedCellIsRefused() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder().belt(at(0, 0), Dir.EAST, L);
        assertThatThrownBy(() -> builder.splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("occupied");
    }

    @Test
    void aPickerWhoseOutputFacesItsInputIsRefused() {
        assertThatThrownBy(() -> new Picker("p", at(0, 0), Dir.WEST, Dir.WEST, L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aPickerIdIsUniqueAmongPickers() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder().picker("p", at(0, 0), Dir.WEST, Dir.EAST, L);
        assertThatThrownBy(() -> builder.picker("p", at(0, 2), Dir.WEST, Dir.EAST, L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void aSplitterIdIsUniqueAmongSplitters() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder()
                .splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L);
        assertThatThrownBy(() -> builder.splitter("s", at(0, 2), Dir.EAST, Set.of(), Set.of(), L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void aPickerAndASplitterMayShareAnId() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .picker("x", at(0, 0), Dir.WEST, Dir.EAST, L)
                .splitter("x", at(0, 2), Dir.EAST, Set.of(), Set.of(), L)
                .build();
        assertThat(network.consumers()).hasSize(2);
    }

    @Test
    void aBeltFeedingAPickerFromANonInputCellIsRefused() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder()
                .picker("p", at(0, 0), Dir.WEST, Dir.EAST, L);
        assertThatThrownBy(() -> builder.belt(at(0, 1), Dir.NORTH, L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BLOCKED");
    }

    @Test
    void aBeltFeedingASplitterFromTheSideIsRefused() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder()
                .splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L);
        assertThatThrownBy(() -> builder.belt(at(0, 1), Dir.NORTH, L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BLOCKED");
    }

    @Test
    void aSplitterFedFromTheSideIsRefusedAsBlocked() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder()
                .belt(at(0, 1), Dir.NORTH, L);
        assertThatThrownBy(() -> builder.splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BLOCKED");
    }

    @Test
    void aSplitterAtTheEndOfABeltHasItsBackFed() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(-1, 0), Dir.EAST, L)
                .splitter("s", at(0, 0), Dir.EAST, Set.of(), Set.of(), L)
                .build();
        assertThat(network.consumers()).hasSize(2);
    }

    @Test
    void aPickerNextToABeltWithoutFeedingItIsAccepted() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.NORTH, L)
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, L)
                .build();
        assertThat(network.consumers()).hasSize(2);
    }

    @Test
    void anEmptyBuilderGivesAnEmptyNetworkThatTicksToNothing() {
        LogisticsNetwork network = LogisticsNetwork.builder().build();
        LogisticsResult result = network.tick(0.05, Map.of(), Map.of());
        assertThat(result.moved()).isEmpty();
        assertThat(result.blocked()).isEmpty();
        assertThat(network.itemCount()).isZero();
    }

    @Test
    void aZeroOrNegativeOrNonFiniteDtIsRefused() {
        LogisticsNetwork network = LogisticsNetwork.builder().build();
        assertThatThrownBy(() -> network.tick(0.0, Map.of(), Map.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> network.tick(-0.05, Map.of(), Map.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> network.tick(Double.NaN, Map.of(), Map.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> network.tick(Double.POSITIVE_INFINITY, Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aSatisfactionOutsideZeroToOneIsRefused() {
        LogisticsNetwork network = LogisticsNetwork.builder().belt(at(0, 0), Dir.EAST, L).build();
        assertThatThrownBy(() -> network.tick(0.05, Map.of(LogisticsNetwork.beltId(at(0, 0)), 1.5), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> network.tick(0.05, Map.of(LogisticsNetwork.beltId(at(0, 0)), -0.1), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> network.tick(0.05, Map.of(LogisticsNetwork.beltId(at(0, 0)), Double.NaN), Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aPortOnANodeCellIsRefused() {
        LogisticsNetwork network = LogisticsNetwork.builder().belt(at(0, 0), Dir.EAST, L).build();
        Map<Pos, Port> ports = Map.of(at(0, 0), MemoryPort.sink());
        assertThatThrownBy(() -> network.tick(0.05, Map.of(), ports)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aMissingSatisfactionMapOrPortMapIsRefused() {
        LogisticsNetwork network = LogisticsNetwork.builder().build();
        assertThatThrownBy(() -> network.tick(0.05, null, Map.of())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> network.tick(0.05, Map.of(), null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void insertingANullItemIsRefused() {
        LogisticsNetwork network = LogisticsNetwork.builder().belt(at(0, 0), Dir.EAST, L).build();
        assertThatThrownBy(() -> network.insertAtEntry(at(0, 0), null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void itemsOnAnUnknownBeltIsRefused() {
        LogisticsNetwork network = LogisticsNetwork.builder().build();
        assertThatThrownBy(() -> network.itemsOn(at(0, 0))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theResultOfAnUnknownNodeIsZeroAndNotBlocked() {
        LogisticsResult result = LogisticsNetwork.builder().build().tick(0.05, Map.of(), Map.of());
        assertThat(result.movedBy("nope")).isZero();
        assertThat(result.isBlocked("nope")).isFalse();
    }

    @Test
    void aResultIsUnmodifiable() {
        LogisticsResult result = LogisticsNetwork.builder().belt(at(0, 0), Dir.EAST, L).build()
                .tick(0.05, Map.of(), Map.of());
        assertThatThrownBy(() -> result.moved().put("x", 1)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.blocked().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void itemIdsAreNamedByTheirKind() {
        assertThat(Item.material("STONE").kind()).isEqualTo(Item.MATERIAL);
        assertThat(Item.component("gear").kind()).isEqualTo(Item.COMPONENT);
    }

    @Test
    void anItemWithAnUnknownKindIsRefused() {
        assertThatThrownBy(() -> new Item("OTHER", "x")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anItemWithABlankIdIsRefused() {
        assertThatThrownBy(() -> Item.material(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anItemIsEqualByKindAndId() {
        assertThat(Item.material("A")).isEqualTo(A);
        assertThat(Item.component("A")).isNotEqualTo(A);
    }

    @Test
    void theNodeIdsAreStableStrings() {
        assertThat(LogisticsNetwork.beltId(at(3, -4))).isEqualTo("belt:3:64:-4");
        assertThat(LogisticsNetwork.pickerId("p1")).isEqualTo("picker:p1");
        assertThat(LogisticsNetwork.splitterId("s1")).isEqualTo("splitter:s1");
    }
}
