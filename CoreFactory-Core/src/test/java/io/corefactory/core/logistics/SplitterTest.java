package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static io.corefactory.core.logistics.Fixtures.A;
import static io.corefactory.core.logistics.Fixtures.B;
import static io.corefactory.core.logistics.Fixtures.C;
import static io.corefactory.core.logistics.Fixtures.at;
import static io.corefactory.core.logistics.Fixtures.belt;
import static io.corefactory.core.logistics.Fixtures.many;
import static io.corefactory.core.logistics.Fixtures.ports;
import static io.corefactory.core.logistics.Fixtures.tickTimes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SplitterTest {

    private static final double DT = 0.05;
    private static final LogisticsLevel L = LogisticsLevel.I;
    private static final String SPLITTER = LogisticsNetwork.splitterId("s");

    /**
     * A source at x 0, a level III picker at x 1, a belt at x 2 feeding a splitter at x 3 flowing east. Straight goes on
     * through a belt at x 4 into a port at x 5; right goes through a belt to a port at z 2; left through a belt to a port at
     * z -2.
     */
    private static LogisticsNetwork rig(Set<Item> left, Set<Item> right) {
        return LogisticsNetwork.builder()
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.III)
                .belt(at(2, 0), Dir.EAST, L)
                .splitter("s", at(3, 0), Dir.EAST, left, right, L)
                .belt(at(4, 0), Dir.EAST, L)
                .belt(at(3, 1), Dir.SOUTH, L)
                .belt(at(3, -1), Dir.NORTH, L)
                .build();
    }

    private static Map<Pos, Port> rigPorts(MemoryPort source, MemoryPort straight, MemoryPort left, MemoryPort right) {
        return ports(at(0, 0), source, at(5, 0), straight, at(3, -2), left, at(3, 2), right);
    }

    @Test
    void aRightOnlyFilterSendsListedItemsRightAndTheRestStraight() {
        LogisticsNetwork network = rig(Set.of(), Set.of(A));
        MemoryPort source = MemoryPort.source(alternate(A, B, 10));
        MemoryPort straight = MemoryPort.sink();
        MemoryPort left = MemoryPort.sink();
        MemoryPort right = MemoryPort.sink();
        tickTimes(network, 800, DT, rigPorts(source, straight, left, right));
        assertThat(right.contents()).containsOnly(A).hasSize(10);
        assertThat(straight.contents()).containsOnly(B).hasSize(10);
        assertThat(left.accepted()).isZero();
    }

    @Test
    void aLeftOnlyFilterSendsListedItemsLeftAndTheRestStraight() {
        LogisticsNetwork network = rig(Set.of(B), Set.of());
        MemoryPort source = MemoryPort.source(alternate(A, B, 10));
        MemoryPort straight = MemoryPort.sink();
        MemoryPort left = MemoryPort.sink();
        MemoryPort right = MemoryPort.sink();
        tickTimes(network, 800, DT, rigPorts(source, straight, left, right));
        assertThat(left.contents()).containsOnly(B).hasSize(10);
        assertThat(straight.contents()).containsOnly(A).hasSize(10);
        assertThat(right.accepted()).isZero();
    }

    @Test
    void anUnfilteredSplitterSendsEverythingStraight() {
        LogisticsNetwork network = rig(Set.of(), Set.of());
        MemoryPort source = MemoryPort.source(alternate(A, C, 10));
        MemoryPort straight = MemoryPort.sink();
        MemoryPort left = MemoryPort.sink();
        MemoryPort right = MemoryPort.sink();
        tickTimes(network, 800, DT, rigPorts(source, straight, left, right));
        assertThat(straight.accepted()).isEqualTo(20);
        assertThat(left.accepted()).isZero();
        assertThat(right.accepted()).isZero();
    }

    @Test
    void aRightListItemGoesToTheRightCell() {
        Splitter splitter = new Splitter("s", at(3, 0), Dir.EAST, Set.of(), Set.of(A), L);
        assertThat(splitter.outputCell(A)).isEqualTo(splitter.rightCell());
        assertThat(splitter.outputCell(B)).isEqualTo(splitter.straightCell());
    }

    @Test
    void aLeftListItemGoesToTheLeftCell() {
        Splitter splitter = new Splitter("s", at(3, 0), Dir.EAST, Set.of(B), Set.of(), L);
        assertThat(splitter.outputCell(B)).isEqualTo(splitter.leftCell());
        assertThat(splitter.outputCell(A)).isEqualTo(splitter.straightCell());
    }

    @Test
    void anItemOnBothListsIsRefusedAtConstruction() {
        assertThatThrownBy(() -> new Splitter("s", at(3, 0), Dir.EAST, Set.of(A, B), Set.of(A), L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("both");
    }

    @Test
    void theBuilderDoesNotBuildASplitterWithAnItemOnBothLists() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder();
        assertThatThrownBy(() -> builder.splitter("s", at(3, 0), Dir.EAST, Set.of(A), Set.of(A), L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aBlockedOutputBlocksTheSplitterAndItsInput() {
        LogisticsNetwork network = rig(Set.of(), Set.of());
        MemoryPort source = MemoryPort.source(many(A, 200));
        MemoryPort straight = MemoryPort.sink();
        straight.setOpen(false);
        MemoryPort left = MemoryPort.sink();
        MemoryPort right = MemoryPort.sink();
        Map<Pos, Port> ports = rigPorts(source, straight, left, right);
        LogisticsResult last = null;
        for (int t = 0; t < 600; t++) {
            last = network.tick(DT, Map.of(), ports);
        }
        assertThat(network.itemsOn(at(2, 0))).hasSize(4);
        assertThat(network.itemsOn(at(4, 0))).hasSize(4);
        assertThat(network.itemCount()).isEqualTo(9);
        assertThat(source.taken()).isEqualTo(9);
        assertThat(last.isBlocked(SPLITTER)).isTrue();
    }

    @Test
    void theBlockedSplitterResumesWhenItsOutputAcceptsAgain() {
        LogisticsNetwork network = rig(Set.of(), Set.of());
        MemoryPort source = MemoryPort.source(many(A, 200));
        MemoryPort straight = MemoryPort.sink();
        straight.setOpen(false);
        MemoryPort left = MemoryPort.sink();
        MemoryPort right = MemoryPort.sink();
        Map<Pos, Port> ports = rigPorts(source, straight, left, right);
        tickTimes(network, 600, DT, ports);
        straight.setOpen(true);
        tickTimes(network, 200, DT, ports);
        assertThat(straight.accepted()).isPositive();
    }

    @Test
    void aSplitterWithAPortOnEachOutputSendsEachItemToItsOwnPort() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.III)
                .belt(at(2, 0), Dir.EAST, L)
                .splitter("s", at(3, 0), Dir.EAST, Set.of(B), Set.of(A), L)
                .build();
        MemoryPort source = MemoryPort.source(A, B, C, A, B, C);
        MemoryPort straight = MemoryPort.sink();
        MemoryPort left = MemoryPort.sink();
        MemoryPort right = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(0, 0), source, at(4, 0), straight, at(3, -1), left, at(3, 1), right);
        tickTimes(network, 400, DT, ports);
        assertThat(right.contents()).containsOnly(A).hasSize(2);
        assertThat(left.contents()).containsOnly(B).hasSize(2);
        assertThat(straight.contents()).containsOnly(C).hasSize(2);
    }

    @Test
    void aSplitterWithABeltOnTheLeftAndAPortOnTheRightRoutesBothWays() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.III)
                .belt(at(2, 0), Dir.EAST, L)
                .splitter("s", at(3, 0), Dir.EAST, Set.of(B), Set.of(A), L)
                .belt(at(3, -1), Dir.NORTH, L)
                .build();
        MemoryPort source = MemoryPort.source(A, B, A, B);
        MemoryPort right = MemoryPort.sink();
        MemoryPort left = MemoryPort.sink();
        MemoryPort straight = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(0, 0), source, at(3, 1), right, at(3, -2), left, at(4, 0), straight);
        tickTimes(network, 400, DT, ports);
        assertThat(right.accepted()).isEqualTo(2);
        assertThat(left.accepted()).isEqualTo(2);
    }

    @Test
    void aLevelOneSplitterPassesTwoItemsPerSecond() {
        LogisticsNetwork network = rig(Set.of(), Set.of());
        MemoryPort source = MemoryPort.source(many(A, 500));
        MemoryPort straight = MemoryPort.sink();
        MemoryPort left = MemoryPort.sink();
        MemoryPort right = MemoryPort.sink();
        Map<Pos, Port> ports = rigPorts(source, straight, left, right);
        tickTimes(network, 100, DT, ports);
        int before = straight.accepted();
        tickTimes(network, 200, DT, ports);
        assertThat(straight.accepted() - before).isBetween(19, 21);
    }

    @Test
    void zeroSatisfactionStopsTheSplitter() {
        LogisticsNetwork network = rig(Set.of(), Set.of());
        MemoryPort source = MemoryPort.source(many(A, 500));
        MemoryPort straight = MemoryPort.sink();
        MemoryPort left = MemoryPort.sink();
        MemoryPort right = MemoryPort.sink();
        Map<Pos, Port> ports = rigPorts(source, straight, left, right);
        for (int t = 0; t < 200; t++) {
            network.tick(DT, Map.of(SPLITTER, 0.0), ports);
        }
        assertThat(straight.accepted()).isZero();
    }

    @Test
    void halfSatisfactionHalvesTheSplitterThroughput() {
        LogisticsNetwork network = rig(Set.of(), Set.of());
        MemoryPort source = MemoryPort.source(many(A, 500));
        MemoryPort straight = MemoryPort.sink();
        MemoryPort left = MemoryPort.sink();
        MemoryPort right = MemoryPort.sink();
        Map<Pos, Port> ports = rigPorts(source, straight, left, right);
        for (int t = 0; t < 200; t++) {
            network.tick(DT, Map.of(SPLITTER, 0.5), ports);
        }
        assertThat(straight.accepted()).isBetween(9, 11);
    }

    @Test
    void aDeliveryIsCountedAsOneMovedItem() {
        LogisticsNetwork network = rig(Set.of(), Set.of());
        MemoryPort source = MemoryPort.source(A);
        MemoryPort straight = MemoryPort.sink();
        MemoryPort left = MemoryPort.sink();
        MemoryPort right = MemoryPort.sink();
        Map<Pos, Port> ports = rigPorts(source, straight, left, right);
        int moved = 0;
        for (int t = 0; t < 400; t++) {
            moved += network.tick(DT, Map.of(), ports).movedBy(SPLITTER);
        }
        assertThat(moved).isEqualTo(1);
        assertThat(straight.contents()).containsExactly(A);
    }

    @Test
    void aSplitterWithNoOutputHoldsTheItemItTook() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.EAST, L)
                .splitter("s", at(1, 0), Dir.EAST, Set.of(), Set.of(), L)
                .build();
        network.insertAtEntry(at(0, 0), A);
        tickTimes(network, 60, DT, Map.of());
        assertThat(network.itemCount()).isEqualTo(1);
        assertThat(network.itemsOn(at(0, 0))).isEmpty();
    }

    private static Item[] alternate(Item first, Item second, int pairs) {
        Item[] items = new Item[pairs * 2];
        for (int i = 0; i < pairs; i++) {
            items[2 * i] = first;
            items[2 * i + 1] = second;
        }
        return items;
    }
}
