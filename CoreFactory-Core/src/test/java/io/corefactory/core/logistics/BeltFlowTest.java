package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static io.corefactory.core.logistics.Fixtures.A;
import static io.corefactory.core.logistics.Fixtures.B;
import static io.corefactory.core.logistics.Fixtures.at;
import static io.corefactory.core.logistics.Fixtures.belt;
import static io.corefactory.core.logistics.Fixtures.many;
import static io.corefactory.core.logistics.Fixtures.ports;
import static io.corefactory.core.logistics.Fixtures.tickTimes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class BeltFlowTest {

    private static final double DT = 0.05;
    private static final double GAP = 0.25;
    private static final LogisticsLevel L = LogisticsLevel.I;

    private static LogisticsNetwork line(int length) {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder();
        for (int x = 0; x < length; x++) {
            builder.belt(at(x, 0), Dir.EAST, L);
        }
        return builder.build();
    }

    /** A source port at x 0, a picker at x 1 taking from it, a line from x 2, and a sink at the end. */
    private static LogisticsNetwork fedLine(int length) {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder()
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.III);
        for (int i = 0; i < length; i++) {
            builder.belt(at(2 + i, 0), Dir.EAST, L);
        }
        return builder.build();
    }

    private static void assertSpaced(List<BeltItem> items) {
        assertThat(items.size()).isLessThanOrEqualTo(LogisticsTable.BELT_CAPACITY);
        for (int i = 0; i < items.size(); i++) {
            assertThat(items.get(i).position()).isBetween(0.0, 1.0 + 1e-9);
            if (i > 0) {
                assertThat(items.get(i - 1).position() - items.get(i).position()).isGreaterThanOrEqualTo(GAP - 1e-9);
            }
        }
    }

    @Test
    void itemsKeepTheirSpacingWhileTheyMove() {
        LogisticsNetwork network = line(5);
        Map<Pos, Port> none = Map.of();
        for (int t = 0; t < 400; t++) {
            network.insertAtEntry(at(0, 0), A);
            network.tick(DT, Map.of(), none);
            for (int x = 0; x < 5; x++) {
                assertSpaced(network.itemsOn(at(x, 0)));
            }
        }
    }

    @Test
    void aBlockedEndStopsTheWholeLine() {
        LogisticsNetwork network = line(3);
        Map<Pos, Port> none = Map.of();
        for (int t = 0; t < 600; t++) {
            network.insertAtEntry(at(0, 0), A);
            network.tick(DT, Map.of(), none);
        }
        List<List<BeltItem>> before = List.of(network.itemsOn(at(0, 0)), network.itemsOn(at(1, 0)), network.itemsOn(at(2, 0)));
        tickTimes(network, 100, DT, none);
        List<List<BeltItem>> after = List.of(network.itemsOn(at(0, 0)), network.itemsOn(at(1, 0)), network.itemsOn(at(2, 0)));
        assertThat(after).isEqualTo(before);
        assertThat(network.itemsOn(at(2, 0))).hasSize(4);
    }

    @Test
    void theEndOfABlockedLineHoldsItemsSpacedFromTheExit() {
        LogisticsNetwork network = line(1);
        for (int t = 0; t < 200; t++) {
            network.insertAtEntry(at(0, 0), A);
            network.tick(DT, Map.of(), Map.of());
        }
        List<BeltItem> items = network.itemsOn(at(0, 0));
        assertThat(items).hasSize(4);
        assertThat(items.get(0).position()).isCloseTo(1.0, within(1e-9));
        assertThat(items.get(1).position()).isCloseTo(0.75, within(1e-9));
        assertThat(items.get(2).position()).isCloseTo(0.5, within(1e-9));
        assertThat(items.get(3).position()).isCloseTo(0.25, within(1e-9));
    }

    @Test
    void aFullLineFillsFromItsEndUntilEveryUnitHoldsFour() {
        LogisticsNetwork network = fedLine(5);
        MemoryPort source = MemoryPort.source(many(A, 100));
        MemoryPort sink = MemoryPort.sink();
        sink.setOpen(false);
        Map<Pos, Port> ports = ports(at(0, 0), source, at(7, 0), sink);
        tickTimes(network, 1000, DT, ports);
        for (int x = 2; x <= 6; x++) {
            assertThat(network.itemsOn(at(x, 0))).hasSize(4);
        }
        assertThat(source.taken()).isEqualTo(20);
    }

    @Test
    void aSinkThatRefusesStopsTheLineAndTheSourceStops() {
        LogisticsNetwork network = fedLine(5);
        MemoryPort source = MemoryPort.source(many(A, 100));
        MemoryPort sink = MemoryPort.sink();
        sink.setOpen(false);
        Map<Pos, Port> ports = ports(at(0, 0), source, at(7, 0), sink);
        tickTimes(network, 1000, DT, ports);
        int taken = source.taken();
        tickTimes(network, 100, DT, ports);
        assertThat(source.taken()).isEqualTo(taken);
        assertThat(sink.accepted()).isZero();
    }

    @Test
    void theFlowResumesWhenTheEndAcceptsAgain() {
        LogisticsNetwork network = fedLine(5);
        MemoryPort source = MemoryPort.source(many(A, 100));
        MemoryPort sink = MemoryPort.sink();
        sink.setOpen(false);
        Map<Pos, Port> ports = ports(at(0, 0), source, at(7, 0), sink);
        tickTimes(network, 800, DT, ports);
        sink.setOpen(true);
        tickTimes(network, 100, DT, ports);
        assertThat(sink.accepted()).isPositive();
        assertThat(source.taken()).isGreaterThan(20);
    }

    @Test
    void itemsAtTheLineEndGoIntoAPort() {
        LogisticsNetwork network = line(2);
        MemoryPort sink = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(2, 0), sink);
        network.insertAtEntry(at(0, 0), A);
        tickTimes(network, 10, DT, ports);
        network.insertAtEntry(at(0, 0), B);
        tickTimes(network, 60, DT, ports);
        assertThat(sink.contents()).containsExactly(A, B);
        assertThat(network.itemCount()).isZero();
    }

    @Test
    void aBeltDoesNotPullFromAPortBehindIt() {
        LogisticsNetwork network = line(2);
        MemoryPort behind = MemoryPort.source(A, A, A);
        Map<Pos, Port> ports = ports(at(-1, 0), behind);
        tickTimes(network, 100, DT, ports);
        assertThat(behind.taken()).isZero();
        assertThat(network.itemCount()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"1.0,0.1875", "0.5,0.09375", "0.0,0.0"})
    void theSpeedOfAnItemScalesWithSatisfaction(double satisfaction, double expected) {
        LogisticsNetwork network = line(2);
        network.insertAtEntry(at(0, 0), A);
        for (int t = 0; t < 2; t++) {
            network.tick(DT, Map.of(belt(0, 0), satisfaction), Map.of());
        }
        assertThat(network.itemsOn(at(0, 0)).get(0).position()).isCloseTo(expected, within(1e-9));
    }

    @Test
    void aMissingSatisfactionEntryCountsAsFull() {
        LogisticsNetwork network = line(2);
        network.insertAtEntry(at(0, 0), A);
        network.tick(DT, Map.of(), Map.of());
        assertThat(network.itemsOn(at(0, 0)).get(0).position()).isCloseTo(1.875 * DT, within(1e-9));
    }

    @Test
    void anItemCrossesIntoTheNextUnitWhenItReachesTheExit() {
        LogisticsNetwork network = line(2);
        network.insertAtEntry(at(0, 0), A);
        int handed = 0;
        for (int t = 0; t < 20; t++) {
            handed += network.tick(DT, Map.of(), Map.of()).movedBy(belt(0, 0));
        }
        assertThat(handed).isEqualTo(1);
        assertThat(network.itemsOn(at(0, 0))).isEmpty();
        assertThat(network.itemsOn(at(1, 0)).get(0).position()).isCloseTo(0.84375, within(1e-6));
    }

    @Test
    void anItemWaitsAtTheExitForAPickerToTakeItAndTheBeltReportsItBlocked() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.EAST, L)
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, L)
                .build();
        MemoryPort sink = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(2, 0), sink);
        network.insertAtEntry(at(0, 0), A);
        boolean sawBlocked = false;
        for (int t = 0; t < 60; t++) {
            LogisticsResult result = network.tick(DT, Map.of(), ports);
            sawBlocked |= result.isBlocked(belt(0, 0));
        }
        assertThat(sawBlocked).isTrue();
        assertThat(sink.contents()).containsExactly(A);
    }

    @Test
    void aBeltWithNothingAtItsOutputHoldsFourItemsAndReportsBlocked() {
        LogisticsNetwork network = line(1);
        LogisticsResult last = null;
        for (int t = 0; t < 200; t++) {
            network.insertAtEntry(at(0, 0), A);
            last = network.tick(DT, Map.of(), Map.of());
        }
        assertThat(network.itemsOn(at(0, 0))).hasSize(4);
        assertThat(last.isBlocked(belt(0, 0))).isTrue();
        assertThat(last.movedBy(belt(0, 0))).isZero();
    }

    @Test
    void insertAtEntryRefusesWhenTheEntryIsStillOccupied() {
        LogisticsNetwork network = line(2);
        assertThat(network.insertAtEntry(at(0, 0), A)).isTrue();
        assertThat(network.insertAtEntry(at(0, 0), B)).isFalse();
    }

    @Test
    void insertAtEntryRefusesAFifthItemOnAFullUnit() {
        LogisticsNetwork network = line(1);
        for (int t = 0; t < 100; t++) {
            network.tick(DT, Map.of(), Map.of());
        }
        int inserted = 0;
        for (int t = 0; t < 100; t++) {
            if (network.insertAtEntry(at(0, 0), A)) {
                inserted++;
            }
            network.tick(DT, Map.of(), Map.of());
        }
        assertThat(inserted).isEqualTo(4);
        assertThat(network.insertAtEntry(at(0, 0), A)).isFalse();
    }

    @Test
    void insertAtEntryOnAnUnknownBeltIsAnError() {
        LogisticsNetwork network = line(1);
        assertThatThrownBy(() -> network.insertAtEntry(at(9, 9), A)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void itemsOnListsTheItemNearestTheExitFirst() {
        LogisticsNetwork network = line(1);
        network.insertAtEntry(at(0, 0), A);
        tickTimes(network, 3, DT, Map.of());
        network.insertAtEntry(at(0, 0), B);
        tickTimes(network, 1, DT, Map.of());
        List<BeltItem> items = network.itemsOn(at(0, 0));
        assertThat(items).hasSize(2);
        assertThat(items.get(0).item()).isEqualTo(A);
        assertThat(items.get(0).position()).isGreaterThan(items.get(1).position());
    }

    @Test
    void aBeltWithNoItemsAndNoSatisfactionChangesNothing() {
        LogisticsNetwork network = line(2);
        network.insertAtEntry(at(0, 0), A);
        tickTimes(network, 5, DT, Map.of());
        List<BeltItem> before = network.itemsOn(at(0, 0));
        network.tick(DT, Map.of(belt(0, 0), 0.0, belt(1, 0), 0.0), Map.of());
        assertThat(network.itemsOn(at(0, 0))).isEqualTo(before);
    }

    @Test
    void theHandoverResultCountsEachItemOnce() {
        LogisticsNetwork network = line(2);
        network.insertAtEntry(at(0, 0), A);
        tickTimes(network, 6, DT, Map.of());
        network.insertAtEntry(at(0, 0), B);
        int handed = 0;
        for (int t = 0; t < 100; t++) {
            handed += network.tick(DT, Map.of(), Map.of()).movedBy(belt(0, 0));
        }
        assertThat(handed).isEqualTo(2);
        assertThat(network.itemsOn(at(1, 0))).hasSize(2);
    }
}
