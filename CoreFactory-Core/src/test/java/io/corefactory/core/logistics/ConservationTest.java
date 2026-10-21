package io.corefactory.core.logistics;

import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.Pos;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static io.corefactory.core.logistics.Fixtures.A;
import static io.corefactory.core.logistics.Fixtures.B;
import static io.corefactory.core.logistics.Fixtures.C;
import static io.corefactory.core.logistics.Fixtures.at;
import static io.corefactory.core.logistics.Fixtures.ports;
import static org.assertj.core.api.Assertions.assertThat;

class ConservationTest {

    private static final double GAP = 0.25;

    private static final Item GEAR = Item.component("gear");

    /**
     * A source at x 0, a level III picker at x 1, a line of nine belts from x 2 to x 10, a splitter at x 11 that sends A
     * right and B left, a line of eleven belts from x 12 to x 22, and a sink at x 23. The right and left outputs go to
     * sinks through one belt each.
     */
    private static LogisticsNetwork rig() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder()
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.III);
        for (int x = 2; x <= 10; x++) {
            builder.belt(at(x, 0), Dir.EAST, LogisticsLevel.I);
        }
        return builder
                .splitter("s", at(11, 0), Dir.EAST, Set.of(B), Set.of(A), LogisticsLevel.II)
                .belt(at(11, 1), Dir.SOUTH, LogisticsLevel.I)
                .belt(at(11, -1), Dir.NORTH, LogisticsLevel.I)
                .belt(at(12, 0), Dir.EAST, LogisticsLevel.I)
                .belt(at(13, 0), Dir.EAST, LogisticsLevel.I)
                .belt(at(14, 0), Dir.EAST, LogisticsLevel.I)
                .belt(at(15, 0), Dir.EAST, LogisticsLevel.I)
                .belt(at(16, 0), Dir.EAST, LogisticsLevel.I)
                .belt(at(17, 0), Dir.EAST, LogisticsLevel.I)
                .belt(at(18, 0), Dir.EAST, LogisticsLevel.I)
                .belt(at(19, 0), Dir.EAST, LogisticsLevel.I)
                .belt(at(20, 0), Dir.EAST, LogisticsLevel.I)
                .belt(at(21, 0), Dir.EAST, LogisticsLevel.I)
                .belt(at(22, 0), Dir.EAST, LogisticsLevel.I)
                .build();
    }

    private static Map<Pos, Port> rigPorts(MemoryPort source, MemoryPort main, MemoryPort right, MemoryPort left) {
        return ports(at(0, 0), source, at(23, 0), main, at(11, 2), right, at(11, -2), left);
    }

    private static Item randomItem(Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> A;
            case 1 -> B;
            case 2 -> C;
            default -> GEAR;
        };
    }

    private static void assertSpacedEverywhere(LogisticsNetwork network) {
        for (Consumer consumer : network.consumers()) {
            if (!consumer.id().startsWith("belt:")) {
                continue;
            }
            Pos pos = consumer.pos();
            List<BeltItem> items = network.itemsOn(pos);
            assertThat(items.size()).isLessThanOrEqualTo(LogisticsTable.BELT_CAPACITY);
            for (int i = 0; i < items.size(); i++) {
                assertThat(items.get(i).position()).isBetween(0.0, 1.0 + 1e-9);
                if (i > 0) {
                    assertThat(items.get(i - 1).position() - items.get(i).position()).isGreaterThanOrEqualTo(GAP - 1e-9);
                }
            }
        }
    }

    @Test
    void aSeededRandomRunOfTwoThousandTicksChangesItemCountsOnlyAtTheTransfers() {
        Random random = new Random(20_261_020L);
        LogisticsNetwork network = rig();
        Item[] stock = new Item[400];
        for (int i = 0; i < stock.length; i++) {
            stock[i] = randomItem(random);
        }
        MemoryPort source = MemoryPort.source(stock);
        MemoryPort main = MemoryPort.sink();
        MemoryPort right = MemoryPort.sink();
        MemoryPort left = MemoryPort.sink();
        Map<Pos, Port> ports = rigPorts(source, main, right, left);
        List<String> ids = network.consumers().stream().map(Consumer::id).toList();
        int initial = LogisticsItems.totalItems(network, ports);
        assertThat(initial).isEqualTo(400);

        for (int tick = 0; tick < 2000; tick++) {
            double dt = 0.02 + random.nextDouble() * 0.08;
            Map<String, Double> satisfaction = new HashMap<>();
            for (String id : ids) {
                if (random.nextInt(4) != 0) {
                    satisfaction.put(id, random.nextInt(5) == 0 ? 0.0 : random.nextDouble());
                }
            }
            main.setOpen(random.nextInt(10) < 7);
            right.setOpen(random.nextInt(10) < 7);
            left.setOpen(random.nextInt(10) < 7);

            network.tick(dt, satisfaction, ports);

            assertThat(LogisticsItems.totalItems(network, ports)).isEqualTo(initial);
            int transferredOut = main.accepted() + right.accepted() + left.accepted();
            assertThat(network.itemCount()).isEqualTo(source.taken() - transferredOut);
            assertSpacedEverywhere(network);
        }

        assertThat(source.taken()).isPositive();
        assertThat(main.accepted() + right.accepted() + left.accepted()).isPositive();
    }

    @Test
    void theSameSnapshotSolvedTwiceGivesTheSameResults() {
        Random seedA = new Random(7L);
        Random seedB = new Random(7L);
        LogisticsNetwork first = rig();
        LogisticsNetwork second = rig();
        MemoryPort sourceA = MemoryPort.source(stockFrom(seedA, 200));
        MemoryPort sourceB = MemoryPort.source(stockFrom(seedB, 200));
        MemoryPort mainA = MemoryPort.sink();
        MemoryPort mainB = MemoryPort.sink();
        MemoryPort rightA = MemoryPort.sink();
        MemoryPort rightB = MemoryPort.sink();
        MemoryPort leftA = MemoryPort.sink();
        MemoryPort leftB = MemoryPort.sink();
        Map<Pos, Port> portsA = rigPorts(sourceA, mainA, rightA, leftA);
        Map<Pos, Port> portsB = rigPorts(sourceB, mainB, rightB, leftB);

        for (int tick = 0; tick < 500; tick++) {
            double dt = 0.05;
            Map<String, Double> satisfaction = tick % 3 == 0 ? Map.of(LogisticsNetwork.splitterId("s"), 0.5) : Map.of();
            LogisticsResult a = first.tick(dt, satisfaction, portsA);
            LogisticsResult b = second.tick(dt, satisfaction, portsB);
            assertThat(a).isEqualTo(b);
        }
        assertThat(mainA.contents()).isEqualTo(mainB.contents());
        assertThat(rightA.contents()).isEqualTo(rightB.contents());
        assertThat(leftA.contents()).isEqualTo(leftB.contents());
        assertThat(first.itemCount()).isEqualTo(second.itemCount());
    }

    @Test
    void aResultDoesNotChangeWhenTheSameNetworkIsRebuiltFromTheSameItems() {
        LogisticsNetwork first = rig();
        LogisticsNetwork second = rig();
        first.insertAtEntry(at(2, 0), A);
        second.insertAtEntry(at(2, 0), A);
        Map<Pos, Port> none = Map.of();
        for (int t = 0; t < 100; t++) {
            assertThat(first.tick(0.05, Map.of(), none)).isEqualTo(second.tick(0.05, Map.of(), none));
        }
        assertThat(first.itemsOn(at(2, 0))).isEqualTo(second.itemsOn(at(2, 0)));
    }

    @Test
    void theTotalIsTheSameWhetherAPortIsKeyedOnceOrTwice() {
        LogisticsNetwork network = rig();
        MemoryPort source = MemoryPort.source(A, A, A);
        Map<Pos, Port> once = ports(at(0, 0), source);
        Map<Pos, Port> twice = ports(at(0, 0), source, at(50, 50), source);
        assertThat(LogisticsItems.totalItems(network, twice)).isEqualTo(LogisticsItems.totalItems(network, once));
        assertThat(LogisticsItems.totalItems(network, once)).isEqualTo(3);
    }

    private static Item[] stockFrom(Random random, int count) {
        Item[] stock = new Item[count];
        for (int i = 0; i < count; i++) {
            stock[i] = randomItem(random);
        }
        return stock;
    }
}
