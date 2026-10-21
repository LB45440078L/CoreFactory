package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;

import static io.corefactory.core.logistics.Fixtures.A;
import static io.corefactory.core.logistics.Fixtures.B;
import static io.corefactory.core.logistics.Fixtures.at;
import static io.corefactory.core.logistics.Fixtures.belt;
import static io.corefactory.core.logistics.Fixtures.many;
import static io.corefactory.core.logistics.Fixtures.ports;
import static io.corefactory.core.logistics.Fixtures.tickTimes;
import static org.assertj.core.api.Assertions.assertThat;

class PickerTest {

    private static final double DT = 0.05;
    private static final String PICKER = LogisticsNetwork.pickerId("p");

    private static LogisticsNetwork picker(LogisticsLevel level) {
        return LogisticsNetwork.builder().picker("p", at(1, 0), Dir.WEST, Dir.EAST, level).build();
    }

    private static int movesIn(LogisticsLevel level, double satisfaction, int ticks) {
        LogisticsNetwork network = picker(level);
        MemoryPort source = MemoryPort.source(many(A, 1000));
        MemoryPort sink = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(0, 0), source, at(2, 0), sink);
        for (int t = 0; t < ticks; t++) {
            network.tick(DT, Map.of(PICKER, satisfaction), ports);
        }
        return sink.accepted();
    }

    @Test
    void aPickerMovesOneItemPerPeriodAfterTheDelay() {
        LogisticsNetwork network = picker(LogisticsLevel.I);
        MemoryPort source = MemoryPort.source(many(A, 100));
        MemoryPort sink = MemoryPort.sink();
        for (int t = 0; t < 200; t++) {
            network.tick(DT, Map.of(), ports(at(0, 0), source, at(2, 0), sink));
        }
        assertThat(sink.accepted()).isEqualTo(10);
        assertThat(source.taken()).isEqualTo(10);
    }

    @Test
    void theFirstMoveOfARunWaitsTenTicks() {
        LogisticsNetwork network = picker(LogisticsLevel.I);
        MemoryPort source = MemoryPort.source(many(A, 100));
        MemoryPort sink = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(0, 0), source, at(2, 0), sink);
        tickTimes(network, 9, DT, ports);
        assertThat(sink.accepted()).isZero();
        network.tick(DT, Map.of(), ports);
        assertThat(sink.accepted()).isEqualTo(1);
    }

    @Test
    void aPickerTakesOnlyItemsItsOutputAccepts() {
        LogisticsNetwork network = picker(LogisticsLevel.III);
        MemoryPort source = MemoryPort.source(A, B);
        MemoryPort sink = MemoryPort.sink(item -> item.equals(A));
        Map<Pos, Port> ports = ports(at(0, 0), source, at(2, 0), sink);
        tickTimes(network, 400, DT, ports);
        assertThat(sink.contents()).containsExactly(A);
        assertThat(source.contents()).containsExactly(B);
    }

    @Test
    void aPickerWaitsWhenItsOutputIsFull() {
        LogisticsNetwork network = picker(LogisticsLevel.III);
        MemoryPort source = MemoryPort.source(many(A, 50));
        MemoryPort sink = MemoryPort.sink();
        sink.setOpen(false);
        Map<Pos, Port> ports = ports(at(0, 0), source, at(2, 0), sink);
        LogisticsResult last = null;
        for (int t = 0; t < 100; t++) {
            last = network.tick(DT, Map.of(), ports);
        }
        assertThat(source.taken()).isZero();
        assertThat(source.storedCount()).isEqualTo(50);
        assertThat(last.isBlocked(PICKER)).isTrue();
        assertThat(last.movedBy(PICKER)).isZero();
    }

    @Test
    void aPickerWithNothingToTakeIsNotBlocked() {
        LogisticsNetwork network = picker(LogisticsLevel.I);
        LogisticsResult result = network.tick(DT, Map.of(), ports(at(0, 0), MemoryPort.source(), at(2, 0), MemoryPort.sink()));
        assertThat(result.isBlocked(PICKER)).isFalse();
    }

    @Test
    void aStalledPickerPaysTheDelayAgainWhenItRestarts() {
        LogisticsNetwork network = picker(LogisticsLevel.I);
        MemoryPort source = MemoryPort.source(many(A, 100));
        MemoryPort sink = MemoryPort.sink();
        sink.setOpen(false);
        Map<Pos, Port> ports = ports(at(0, 0), source, at(2, 0), sink);
        tickTimes(network, 60, DT, ports);
        sink.setOpen(true);
        tickTimes(network, 9, DT, ports);
        assertThat(sink.accepted()).isZero();
        network.tick(DT, Map.of(), ports);
        assertThat(sink.accepted()).isEqualTo(1);
    }

    @Test
    void aPickerNeverMovesTwoItemsInOneTick() {
        LogisticsNetwork network = picker(LogisticsLevel.III);
        MemoryPort source = MemoryPort.source(many(A, 100));
        MemoryPort sink = MemoryPort.sink();
        LogisticsResult result = network.tick(10.0, Map.of(), ports(at(0, 0), source, at(2, 0), sink));
        assertThat(result.movedBy(PICKER)).isLessThanOrEqualTo(1);
        assertThat(sink.accepted()).isLessThanOrEqualTo(1);
    }

    @Test
    void aLevelThreePickerMovesMoreThanALevelOnePicker() {
        assertThat(movesIn(LogisticsLevel.III, 1.0, 400)).isGreaterThan(movesIn(LogisticsLevel.I, 1.0, 400));
    }

    @Test
    void halfSatisfactionHalvesTheRate() {
        int full = movesIn(LogisticsLevel.III, 1.0, 400);
        int half = movesIn(LogisticsLevel.III, 0.5, 400);
        assertThat(half).isPositive();
        assertThat(Math.abs(full / 2.0 - half)).isLessThanOrEqualTo(2.0);
    }

    @Test
    void zeroSatisfactionStopsThePicker() {
        assertThat(movesIn(LogisticsLevel.III, 0.0, 400)).isZero();
    }

    @Test
    void aPickerReportsOneMovedItemOnTheTickItMoves() {
        LogisticsNetwork network = picker(LogisticsLevel.I);
        MemoryPort source = MemoryPort.source(many(A, 100));
        MemoryPort sink = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(0, 0), source, at(2, 0), sink);
        int moved = 0;
        for (int t = 0; t < 20; t++) {
            moved += network.tick(DT, Map.of(), ports).movedBy(PICKER);
        }
        assertThat(moved).isEqualTo(1);
    }

    @Test
    void aPickerMovesFromABeltToABelt() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.EAST, LogisticsLevel.I)
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.I)
                .belt(at(2, 0), Dir.EAST, LogisticsLevel.I)
                .build();
        MemoryPort sink = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(3, 0), sink);
        network.insertAtEntry(at(0, 0), A);
        tickTimes(network, 100, DT, ports);
        assertThat(sink.contents()).containsExactly(A);
        assertThat(network.itemsOn(at(0, 0))).isEmpty();
    }

    @Test
    void aPickerMovesFromABeltToAPort() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.EAST, LogisticsLevel.I)
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.I)
                .build();
        MemoryPort sink = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(2, 0), sink);
        network.insertAtEntry(at(0, 0), A);
        tickTimes(network, 100, DT, ports);
        assertThat(sink.contents()).containsExactly(A);
    }

    @Test
    void aPickerMovesFromAPortToABelt() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.I)
                .belt(at(2, 0), Dir.EAST, LogisticsLevel.I)
                .build();
        MemoryPort source = MemoryPort.source(A);
        MemoryPort sink = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(0, 0), source, at(3, 0), sink);
        tickTimes(network, 100, DT, ports);
        assertThat(sink.contents()).containsExactly(A);
        assertThat(source.storedCount()).isZero();
    }

    @Test
    void aPickerMovesFromAPortToAPort() {
        LogisticsNetwork network = picker(LogisticsLevel.I);
        MemoryPort source = MemoryPort.source(A);
        MemoryPort sink = MemoryPort.sink();
        tickTimes(network, 100, DT, ports(at(0, 0), source, at(2, 0), sink));
        assertThat(sink.contents()).containsExactly(A);
    }

    @Test
    void aPickerDoesNotTakeFromABeltThatFacesAway() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.WEST, LogisticsLevel.I)
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.I)
                .build();
        MemoryPort sink = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(2, 0), sink);
        network.insertAtEntry(at(0, 0), A);
        tickTimes(network, 200, DT, ports);
        assertThat(sink.accepted()).isZero();
    }

    @Test
    void aPickerStopsWhenItsOutputBeltIsFullAndItsInputBacksUp() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(0, 0), Dir.EAST, LogisticsLevel.I)
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.I)
                .belt(at(2, 0), Dir.EAST, LogisticsLevel.I)
                .build();
        for (int t = 0; t < 300; t++) {
            network.insertAtEntry(at(0, 0), A);
            network.tick(DT, Map.of(), Map.of());
        }
        assertThat(network.itemsOn(at(2, 0))).hasSize(4);
        assertThat(network.itemsOn(at(0, 0))).hasSize(4);
    }

    /** A picker at (1, 0) with a straight belt flowing east past its north side, at (1, -1). Its output is at (1, 1). */
    private static LogisticsNetwork sideBelt(LogisticsLevel level) {
        return LogisticsNetwork.builder()
                .belt(at(1, -1), Dir.EAST, LogisticsLevel.I)
                .picker("p", at(1, 0), Dir.NORTH, Dir.SOUTH, level)
                .build();
    }

    @Test
    void aPickerBesideAStraightBeltTakesAnItemFromIt() {
        LogisticsNetwork network = sideBelt(LogisticsLevel.I);
        MemoryPort sink = MemoryPort.sink();
        MemoryPort downstream = MemoryPort.sink();
        downstream.setOpen(false);
        Map<Pos, Port> ports = ports(at(1, 1), sink, at(2, -1), downstream);
        network.insertAtEntry(at(1, -1), A);
        tickTimes(network, 100, DT, ports);
        assertThat(sink.contents()).containsExactly(A);
        assertThat(network.itemsOn(at(1, -1))).isEmpty();
    }

    @Test
    void aSidePickLeavesTheItemsItDoesNotTakeOnTheBeltAndTheyKeepMoving() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(1, -1), Dir.EAST, LogisticsLevel.I)
                .belt(at(2, -1), Dir.EAST, LogisticsLevel.I)
                .picker("p", at(1, 0), Dir.NORTH, Dir.SOUTH, LogisticsLevel.I)
                .build();
        MemoryPort picked = MemoryPort.sink();
        MemoryPort passed = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(1, 1), picked, at(3, -1), passed);
        int fed = 0;
        for (int t = 0; t < 200; t++) {
            if (network.insertAtEntry(at(1, -1), A)) {
                fed++;
            }
            network.tick(DT, Map.of(), ports);
        }
        assertThat(picked.accepted()).isBetween(8, 10);
        assertThat(passed.accepted()).isPositive();
        assertThat(picked.accepted() + passed.accepted() + network.itemCount()).isEqualTo(fed);
    }

    @Test
    void aSidePickIsLimitedByThePickerSpeed() {
        int level1 = sideMoves(LogisticsLevel.I, 400);
        int level3 = sideMoves(LogisticsLevel.III, 400);
        assertThat(level1).isBetween(18, 21);
        assertThat(level3).isGreaterThan(level1);
    }

    @Test
    void aPickerBesideACornerTakesNothing() {
        LogisticsNetwork network = LogisticsNetwork.builder()
                .belt(at(1, -2), Dir.SOUTH, LogisticsLevel.I)
                .belt(at(1, -1), Dir.EAST, LogisticsLevel.I)
                .picker("p", at(1, 0), Dir.NORTH, Dir.SOUTH, LogisticsLevel.III)
                .build();
        MemoryPort sink = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(1, 1), sink);
        network.insertAtEntry(at(1, -1), A);
        tickTimes(network, 200, DT, ports);
        assertThat(sink.accepted()).isZero();
        assertThat(network.itemsOn(at(1, -1))).hasSize(1);
    }

    /** Items a picker beside a belt moves in a run of ticks, with the belt kept full at its entry. */
    private static int sideMoves(LogisticsLevel level, int ticks) {
        LogisticsNetwork network = sideBelt(level);
        MemoryPort sink = MemoryPort.sink();
        Map<Pos, Port> ports = ports(at(1, 1), sink);
        for (int t = 0; t < ticks; t++) {
            network.insertAtEntry(at(1, -1), A);
            network.tick(DT, Map.of(), ports);
        }
        return sink.accepted();
    }

    @ParameterizedTest
    @EnumSource(LogisticsLevel.class)
    void everyLevelPickerMovesItsRatePerSecondAtFullSatisfaction(LogisticsLevel level) {
        int moved = movesIn(level, 1.0, 20 * 20);
        double rate = LogisticsTable.pickerItemsPerSecond(level);
        assertThat(moved).isBetween((int) (rate * 20) - 2, (int) (rate * 20) + 1);
    }
}
