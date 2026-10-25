package io.corefactory.core.logistics;

import io.corefactory.core.energy.Consumer;
import io.corefactory.core.energy.Pos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.corefactory.core.logistics.Fixtures.A;
import static io.corefactory.core.logistics.Fixtures.at;
import static io.corefactory.core.logistics.Fixtures.many;
import static io.corefactory.core.logistics.Fixtures.ports;
import static io.corefactory.core.logistics.Fixtures.tickTimes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DuplexTest {

    private static final double DT = 0.05;
    private static final LogisticsLevel L = LogisticsLevel.I;
    private static final String DUPLEX = LogisticsNetwork.duplexId("d");

    /**
     * A source at x 0, a level III picker at x 1, a belt at x 2 flowing east into a duplex whose anchor is at (3, 0) and whose second cell
     * is (3, 1). Its outputs are the cells (4, 0) and (4, 1), each a port.
     */
    private static LogisticsNetwork rig(LogisticsLevel level) {
        return LogisticsNetwork.builder()
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.III)
                .belt(at(2, 0), Dir.EAST, L)
                .duplex("d", at(3, 0), Dir.EAST, level)
                .build();
    }

    private static Map<Pos, Port> rigPorts(MemoryPort source, MemoryPort anchorOutput, MemoryPort secondOutput) {
        return ports(at(0, 0), source, at(4, 0), anchorOutput, at(4, 1), secondOutput);
    }

    /** A long line of level III belts, full of items, ending in a duplex of the given level at (80, 0) flowing east. */
    private static LogisticsNetwork fullLine(LogisticsLevel level) {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder();
        for (int x = 0; x < 80; x++) {
            builder.belt(at(x, 0), Dir.EAST, LogisticsLevel.III);
        }
        LogisticsNetwork network = builder.duplex("d", at(80, 0), Dir.EAST, level).build();
        for (int x = 0; x < 80; x++) {
            for (double position : new double[] {0.0, 0.25, 0.5, 0.75}) {
                assertThat(network.insertAt(at(x, 0), A, position)).isTrue();
            }
        }
        return network;
    }

    @Test
    void theSecondCellIsOnTheRightOfTheFlow() {
        assertThat(Duplex.secondCell(at(3, 0), Dir.EAST)).isEqualTo(at(3, 1));
        assertThat(Duplex.secondCell(at(5, 5), Dir.NORTH)).isEqualTo(at(6, 5));
        assertThat(Duplex.secondCell(at(5, 5), Dir.WEST)).isEqualTo(at(5, 4));
        assertThat(Duplex.secondCell(at(5, 5), Dir.SOUTH)).isEqualTo(at(4, 5));
    }

    @Test
    void aDuplexFlowingEastOccupiesTwoCellsAndTakesFromTheWestAndGivesToTheEast() {
        Duplex duplex = new Duplex("d", at(3, 0), Dir.EAST, L);
        assertThat(duplex.cells()).containsExactly(at(3, 0), at(3, 1));
        assertThat(duplex.backCells()).containsExactly(at(2, 0), at(2, 1));
        assertThat(duplex.outputCells()).containsExactly(at(4, 0), at(4, 1));
    }

    @Test
    void aDuplexFlowingNorthTakesFromTheSouthAndGivesToTheNorth() {
        Duplex duplex = new Duplex("d", at(5, 5), Dir.NORTH, L);
        assertThat(duplex.cells()).containsExactly(at(5, 5), at(6, 5));
        assertThat(duplex.backCells()).containsExactly(at(5, 6), at(6, 6));
        assertThat(duplex.outputCells()).containsExactly(at(5, 4), at(6, 4));
    }

    @Test
    void aDuplexFlowingWestTakesFromTheEastAndGivesToTheWest() {
        Duplex duplex = new Duplex("d", at(5, 5), Dir.WEST, L);
        assertThat(duplex.cells()).containsExactly(at(5, 5), at(5, 4));
        assertThat(duplex.backCells()).containsExactly(at(6, 5), at(6, 4));
        assertThat(duplex.outputCells()).containsExactly(at(4, 5), at(4, 4));
    }

    @Test
    void aDuplexFlowingSouthTakesFromTheNorthAndGivesToTheSouth() {
        Duplex duplex = new Duplex("d", at(5, 5), Dir.SOUTH, L);
        assertThat(duplex.cells()).containsExactly(at(5, 5), at(4, 5));
        assertThat(duplex.backCells()).containsExactly(at(5, 4), at(4, 4));
        assertThat(duplex.outputCells()).containsExactly(at(5, 6), at(4, 6));
    }

    @Test
    void itemsAlternateBetweenTheTwoOutputs() {
        LogisticsNetwork network = rig(L);
        MemoryPort source = MemoryPort.source(many(A, 10));
        MemoryPort anchorOutput = MemoryPort.sink();
        MemoryPort secondOutput = MemoryPort.sink();
        tickTimes(network, 800, DT, rigPorts(source, anchorOutput, secondOutput));
        assertThat(anchorOutput.accepted()).isEqualTo(5);
        assertThat(secondOutput.accepted()).isEqualTo(5);
    }

    @Test
    void anOutputThatRefusesSendsItsItemsToTheOther() {
        LogisticsNetwork network = rig(L);
        MemoryPort source = MemoryPort.source(many(A, 10));
        MemoryPort anchorOutput = MemoryPort.sink();
        MemoryPort secondOutput = MemoryPort.sink();
        secondOutput.setOpen(false);
        tickTimes(network, 800, DT, rigPorts(source, anchorOutput, secondOutput));
        assertThat(anchorOutput.accepted()).isEqualTo(10);
        assertThat(secondOutput.accepted()).isZero();
    }

    @Test
    void theOtherOutputRefusingSendsItsItemsToTheAnchorOutput() {
        LogisticsNetwork network = rig(L);
        MemoryPort source = MemoryPort.source(many(A, 10));
        MemoryPort anchorOutput = MemoryPort.sink();
        MemoryPort secondOutput = MemoryPort.sink();
        anchorOutput.setOpen(false);
        tickTimes(network, 800, DT, rigPorts(source, anchorOutput, secondOutput));
        assertThat(secondOutput.accepted()).isEqualTo(10);
        assertThat(anchorOutput.accepted()).isZero();
    }

    @Test
    void whenNeitherOutputTakesAnItemTheDuplexHoldsItAndIsBlocked() {
        LogisticsNetwork network = rig(L);
        MemoryPort source = MemoryPort.source(many(A, 200));
        MemoryPort anchorOutput = MemoryPort.sink();
        MemoryPort secondOutput = MemoryPort.sink();
        anchorOutput.setOpen(false);
        secondOutput.setOpen(false);
        Map<Pos, Port> ports = rigPorts(source, anchorOutput, secondOutput);
        LogisticsResult last = null;
        for (int t = 0; t < 600; t++) {
            last = network.tick(DT, Map.of(), ports);
        }
        assertThat(network.heldBy("d")).isPresent();
        assertThat(last.isBlocked(DUPLEX)).isTrue();
        assertThat(anchorOutput.accepted() + secondOutput.accepted()).isZero();
        assertThat(network.itemCount()).isEqualTo(source.taken());
        assertThat(network.itemsOn(at(2, 0))).hasSizeLessThanOrEqualTo(4);
    }

    @Test
    void theBlockedDuplexResumesWhenAnOutputAcceptsAgain() {
        LogisticsNetwork network = rig(L);
        MemoryPort source = MemoryPort.source(many(A, 200));
        MemoryPort anchorOutput = MemoryPort.sink();
        MemoryPort secondOutput = MemoryPort.sink();
        anchorOutput.setOpen(false);
        secondOutput.setOpen(false);
        Map<Pos, Port> ports = rigPorts(source, anchorOutput, secondOutput);
        tickTimes(network, 600, DT, ports);
        secondOutput.setOpen(true);
        tickTimes(network, 200, DT, ports);
        assertThat(secondOutput.accepted()).isPositive();
        assertThat(anchorOutput.accepted()).isZero();
    }

    @Test
    void levelOnePassesSevenAndAHalfItemsPerSecond() {
        LogisticsNetwork network = fullLine(LogisticsLevel.I);
        MemoryPort anchorOutput = MemoryPort.sink();
        MemoryPort secondOutput = MemoryPort.sink();
        tickTimes(network, 200, DT, ports(at(81, 0), anchorOutput, at(81, 1), secondOutput));
        assertThat(anchorOutput.accepted() + secondOutput.accepted()).isBetween(70, 80);
    }

    @Test
    void levelTwoPassesFifteenItemsPerSecond() {
        LogisticsNetwork network = fullLine(LogisticsLevel.II);
        MemoryPort anchorOutput = MemoryPort.sink();
        MemoryPort secondOutput = MemoryPort.sink();
        tickTimes(network, 200, DT, ports(at(81, 0), anchorOutput, at(81, 1), secondOutput));
        assertThat(anchorOutput.accepted() + secondOutput.accepted()).isBetween(140, 160);
    }

    /**
     * A level III duplex fed by a full line. A belt hands over at most one item a tick, which is about 20 items a second at this tick
     * length, so the line, not the duplex's 22.5 items a second, is the limit here: the test checks that the faster level keeps up with
     * the line and beats level II.
     */
    @Test
    void levelThreeKeepsUpWithAFullLineAndBeatsLevelTwo() {
        int levelTwo = passedBy(fullLine(LogisticsLevel.II));
        int levelThree = passedBy(fullLine(LogisticsLevel.III));
        assertThat(levelThree).isBetween(190, 235);
        assertThat(levelThree).isGreaterThan(levelTwo);
    }

    private static int passedBy(LogisticsNetwork network) {
        MemoryPort anchorOutput = MemoryPort.sink();
        MemoryPort secondOutput = MemoryPort.sink();
        tickTimes(network, 200, DT, ports(at(81, 0), anchorOutput, at(81, 1), secondOutput));
        return anchorOutput.accepted() + secondOutput.accepted();
    }

    @Test
    void zeroSatisfactionStopsTheDuplex() {
        LogisticsNetwork network = rig(L);
        MemoryPort source = MemoryPort.source(many(A, 200));
        MemoryPort anchorOutput = MemoryPort.sink();
        MemoryPort secondOutput = MemoryPort.sink();
        Map<Pos, Port> ports = rigPorts(source, anchorOutput, secondOutput);
        for (int t = 0; t < 200; t++) {
            network.tick(DT, Map.of(DUPLEX, 0.0), ports);
        }
        assertThat(anchorOutput.accepted() + secondOutput.accepted()).isZero();
    }

    @Test
    void aHeldItemCanBeRestoredIntoAnEmptyDuplex() {
        LogisticsNetwork network = rig(L);
        assertThat(network.holdAt("d", A)).isTrue();
        assertThat(network.heldBy("d")).contains(A);
        assertThat(network.holdAt("d", A)).isFalse();
        assertThat(network.itemCount()).isEqualTo(1);
    }

    @Test
    void theDuplexDrawsItsPowerPerLevel() {
        LogisticsNetwork network = rig(LogisticsLevel.II);
        assertThat(network.consumers()).contains(new Consumer(DUPLEX, at(3, 0), LogisticsTable.duplexPowerW(LogisticsLevel.II)));
    }

    @Test
    void theRatesAndPowerAreTheListedNumbers() {
        assertThat(LogisticsTable.duplexItemsPerSecond(LogisticsLevel.I)).isEqualTo(7.5);
        assertThat(LogisticsTable.duplexItemsPerSecond(LogisticsLevel.II)).isEqualTo(15.0);
        assertThat(LogisticsTable.duplexItemsPerSecond(LogisticsLevel.III)).isEqualTo(22.5);
        assertThat(LogisticsTable.duplexPowerW(LogisticsLevel.I)).isEqualTo(500.0);
        assertThat(LogisticsTable.duplexPowerW(LogisticsLevel.II)).isEqualTo(1000.0);
        assertThat(LogisticsTable.duplexPowerW(LogisticsLevel.III)).isEqualTo(1500.0);
    }

    @Test
    void aBeltBehindTheAnchorFeedsTheDuplex() {
        assertThatCode(() -> LogisticsNetwork.builder()
                .belt(at(2, 0), Dir.EAST, L)
                .duplex("d", at(3, 0), Dir.EAST, L)
                .build()).doesNotThrowAnyException();
    }

    @Test
    void aBeltBehindTheSecondCellFeedsTheDuplex() {
        assertThatCode(() -> LogisticsNetwork.builder()
                .belt(at(2, 1), Dir.EAST, L)
                .duplex("d", at(3, 0), Dir.EAST, L)
                .build()).doesNotThrowAnyException();
    }

    @Test
    void aBeltFlowingIntoTheSideOfTheAnchorIsRefused() {
        assertThatThrownBy(() -> LogisticsNetwork.builder()
                .belt(at(3, -1), Dir.SOUTH, L)
                .duplex("d", at(3, 0), Dir.EAST, L)
                .build()).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("BLOCKED");
        assertThatThrownBy(() -> LogisticsNetwork.builder()
                .duplex("d", at(3, 0), Dir.EAST, L)
                .belt(at(3, -1), Dir.SOUTH, L)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("BLOCKED");
    }

    @Test
    void aBeltFlowingIntoTheSideOfTheSecondCellIsRefused() {
        assertThatThrownBy(() -> LogisticsNetwork.builder()
                .belt(at(3, 2), Dir.NORTH, L)
                .duplex("d", at(3, 0), Dir.EAST, L)
                .build()).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("BLOCKED");
    }

    @Test
    void aBeltFlowingHeadOnIntoTheDuplexIsRefused() {
        assertThatThrownBy(() -> LogisticsNetwork.builder()
                .belt(at(4, 0), Dir.WEST, L)
                .duplex("d", at(3, 0), Dir.EAST, L)
                .build()).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("BLOCKED");
    }

    @Test
    void twoInputBeltsAreRefusedAsAMerge() {
        assertThatThrownBy(() -> LogisticsNetwork.builder()
                .belt(at(2, 0), Dir.EAST, L)
                .belt(at(2, 1), Dir.EAST, L)
                .duplex("d", at(3, 0), Dir.EAST, L)
                .build()).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("MERGE");
    }

    @Test
    void aPickerFeedingTheDuplexIsRefused() {
        assertThatThrownBy(() -> LogisticsNetwork.builder()
                .picker("p", at(2, 0), Dir.WEST, Dir.EAST, LogisticsLevel.I)
                .duplex("d", at(3, 0), Dir.EAST, L)
                .build()).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("BLOCKED");
    }

    @Test
    void theDuplexGivesToBeltsOnBothOutputs() {
        assertThatCode(() -> LogisticsNetwork.builder()
                .belt(at(2, 0), Dir.EAST, L)
                .duplex("d", at(3, 0), Dir.EAST, L)
                .belt(at(4, 0), Dir.EAST, L)
                .belt(at(4, 1), Dir.EAST, L)
                .build()).doesNotThrowAnyException();
    }

    @Test
    void aDuplexCannotBePlacedOverABeltOnItsSecondCell() {
        List<BeltUnit> units = List.of(new BeltUnit(at(3, 1), Dir.SOUTH, L));
        assertThat(Placement.canPlaceDuplex(new Duplex("d", at(3, 0), Dir.EAST, L), units, Endpoints.none()))
                .isEqualTo(PlacementReason.OCCUPIED);
    }

    @Test
    void aDuplexCannotBePlacedOverAPortOnItsSecondCell() {
        Endpoints ports = new Endpoints(List.of(), List.of(), Set.of(at(3, 1)));
        assertThat(Placement.canPlaceDuplex(new Duplex("d", at(3, 0), Dir.EAST, L), List.of(), ports))
                .isEqualTo(PlacementReason.OCCUPIED);
    }

    @Test
    void aDuplexWithABeltBehindItIsPlaceable() {
        List<BeltUnit> units = List.of(new BeltUnit(at(2, 0), Dir.EAST, L));
        assertThat(Placement.canPlaceDuplex(new Duplex("d", at(3, 0), Dir.EAST, L), units, Endpoints.none()))
                .isEqualTo(PlacementReason.OK);
    }

    @Test
    void aBeltOrPickerCannotBePlacedOnADuplexCell() {
        Endpoints endpoints = new Endpoints(List.of(), List.of(), List.of(new Duplex("d", at(3, 0), Dir.EAST, L)), Set.of());
        assertThat(Placement.canPlace(at(3, 1), Dir.EAST, List.of(), endpoints)).isEqualTo(PlacementReason.OCCUPIED);
        assertThat(Placement.canPlacePicker(new Picker("q", at(3, 1), Dir.WEST, Dir.EAST, L), List.of(), endpoints))
                .isEqualTo(PlacementReason.OCCUPIED);
    }
}
