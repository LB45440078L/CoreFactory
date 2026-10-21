package io.corefactory.core.logistics;

import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.Pos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.corefactory.core.logistics.Fixtures.A;
import static io.corefactory.core.logistics.Fixtures.at;
import static io.corefactory.core.logistics.Fixtures.belt;
import static io.corefactory.core.logistics.Fixtures.many;
import static io.corefactory.core.logistics.Fixtures.ports;
import static io.corefactory.core.logistics.Fixtures.tickTimes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PowerTest {

    private static final double DT = 0.05;

    private static LogisticsNetwork mixed() {
        return LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.EAST, LogisticsLevel.I)
                .picker("p", at(-1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.II)
                .splitter("s", at(1, 0), Dir.EAST, Set.of(), Set.of(), LogisticsLevel.III)
                .build();
    }

    @Test
    void everyBeltPickerAndSplitterIsAConsumerWithItsLevelDemand() {
        List<Consumer> consumers = mixed().consumers();
        assertThat(consumers).extracting(Consumer::id).containsExactly(
                LogisticsNetwork.beltId(at(0, 0)),
                LogisticsNetwork.pickerId("p"),
                LogisticsNetwork.splitterId("s"));
        assertThat(consumers.get(0).demandW()).isEqualTo(200.0);
        assertThat(consumers.get(1).demandW()).isEqualTo(200.0);
        assertThat(consumers.get(2).demandW()).isEqualTo(1500.0);
    }

    @Test
    void aConsumerSitsOnItsNodesBlock() {
        List<Consumer> consumers = mixed().consumers();
        assertThat(consumers).extracting(Consumer::pos).containsExactly(at(0, 0), at(-1, 0), at(1, 0));
    }

    @Test
    void aBeltDemandGrowsWithItsLevel() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.EAST, LogisticsLevel.I)
                .belt(at(0, 1), Dir.EAST, LogisticsLevel.II)
                .belt(at(0, 2), Dir.EAST, LogisticsLevel.III)
                .build();
        assertThat(network.consumers()).extracting(Consumer::demandW).containsExactly(200.0, 400.0, 800.0);
    }

    @Test
    void aPickerDemandGrowsWithItsLevel() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .picker("a", at(0, 0), Dir.WEST, Dir.EAST, LogisticsLevel.I)
                .picker("b", at(0, 2), Dir.WEST, Dir.EAST, LogisticsLevel.II)
                .picker("c", at(0, 4), Dir.WEST, Dir.EAST, LogisticsLevel.III)
                .build();
        assertThat(network.consumers()).extracting(Consumer::demandW).containsExactly(100.0, 200.0, 400.0);
    }

    @Test
    void anEmptyNetworkHasNoConsumers() {
        assertThat(LogisticsNetwork.builder().build().consumers()).isEmpty();
    }

    @Test
    void aMissingSatisfactionEntryBehavesLikeFullSatisfaction() {
        LogisticsNetwork withMissing = LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.EAST, LogisticsLevel.II)
                .belt(at(1, 0), Dir.EAST, LogisticsLevel.II)
                .build();
        LogisticsNetwork withOne = LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.EAST, LogisticsLevel.II)
                .belt(at(1, 0), Dir.EAST, LogisticsLevel.II)
                .build();
        withMissing.insertAtEntry(at(0, 0), A);
        withOne.insertAtEntry(at(0, 0), A);
        withMissing.tick(DT, Map.of(), Map.of());
        withOne.tick(DT, Map.of(belt(0, 0), 1.0, belt(1, 0), 1.0), Map.of());
        assertThat(withMissing.itemsOn(at(0, 0)).get(0).position())
                .isCloseTo(withOne.itemsOn(at(0, 0)).get(0).position(), within(1e-12));
    }

    @Test
    void aBeltWithZeroSatisfactionDoesNothing() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.EAST, LogisticsLevel.III)
                .belt(at(1, 0), Dir.EAST, LogisticsLevel.III)
                .build();
        network.insertAtEntry(at(0, 0), A);
        tickTimes(network, 3, DT, Map.of());
        List<BeltItem> before = network.itemsOn(at(0, 0));
        for (int t = 0; t < 50; t++) {
            network.tick(DT, Map.of(belt(0, 0), 0.0), Map.of());
        }
        assertThat(network.itemsOn(at(0, 0))).isEqualTo(before);
    }

    @Test
    void aPickerWithZeroSatisfactionDoesNotMoveEvenWithItemsWaiting() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.III)
                .build();
        MemoryPort source = MemoryPort.source(many(A, 20));
        MemoryPort sink = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(0, 0), source, at(2, 0), sink);
        for (int t = 0; t < 200; t++) {
            network.tick(DT, Map.of(LogisticsNetwork.pickerId("p"), 0.0), ports);
        }
        assertThat(source.taken()).isZero();
        assertThat(sink.accepted()).isZero();
    }

    @Test
    void partialSatisfactionKeepsEveryItemWhereItIs() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.III)
                .belt(at(2, 0), Dir.EAST, LogisticsLevel.I)
                .belt(at(3, 0), Dir.EAST, LogisticsLevel.I)
                .build();
        MemoryPort source = MemoryPort.source(many(A, 100));
        MemoryPort sink = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(0, 0), source, at(4, 0), sink);
        Map<String, Double> partial = Map.of(
                LogisticsNetwork.pickerId("p"), 0.3,
                belt(2, 0), 0.6,
                belt(3, 0), 0.0);
        int total = LogisticsItems.totalItems(network, ports);
        for (int t = 0; t < 500; t++) {
            network.tick(DT, partial, ports);
            assertThat(LogisticsItems.totalItems(network, ports)).isEqualTo(total);
        }
        assertThat(source.taken()).isPositive();
    }
}
