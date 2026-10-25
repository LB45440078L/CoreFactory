package io.corefactory.core.logistics;

import io.corefactory.core.energy.Pos;
import io.corefactory.core.process.ItemRef;
import io.corefactory.core.process.Stack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.corefactory.core.logistics.Fixtures.A;
import static io.corefactory.core.logistics.Fixtures.B;
import static io.corefactory.core.logistics.Fixtures.at;
import static io.corefactory.core.logistics.Fixtures.belt;
import static io.corefactory.core.logistics.Fixtures.ports;
import static org.assertj.core.api.Assertions.assertThat;

/** A crafter's grid at the end of a belt, and a picker that takes from it. */
class CrafterFlowTest {

    private static final double DT = 0.05;
    private static final LogisticsLevel L = LogisticsLevel.I;
    private static final int LIMIT = 64;

    /** A belt of three cells flowing east, from x 0 to x 2, with its end at x 3. */
    private static LogisticsNetwork beltLine() {
        LogisticsNetwork.Builder builder = LogisticsNetwork.builder();
        for (int x = 0; x < 3; x++) {
            builder.belt(at(x, 0), Dir.EAST, L);
        }
        return builder.build();
    }

    private static CrafterGrid emptyGrid() {
        List<Stack> slots = new ArrayList<>();
        for (int i = 0; i < CrafterGrid.SLOTS; i++) {
            slots.add(MemorySlot.empty());
        }
        return new CrafterGrid(slots, new boolean[CrafterGrid.SLOTS], ref -> LIMIT);
    }

    /** Keeps the belt entry fed with A for the given ticks. */
    private static void feed(LogisticsNetwork network, Map<Pos, Port> ports, int ticks) {
        for (int t = 0; t < ticks; t++) {
            network.insertAtEntry(at(0, 0), A);
            network.tick(DT, Map.of(), ports);
        }
    }

    @Test
    void aBeltGivesItemsToACrafterAtTheSameRateAsToAnyOtherSink() {
        int ticks = 400;

        LogisticsNetwork toSink = beltLine();
        MemoryPort sink = MemoryPort.sink();
        feed(toSink, ports(at(3, 0), sink), ticks);

        LogisticsNetwork toCrafter = beltLine();
        CrafterGrid crafter = emptyGrid();
        feed(toCrafter, ports(at(3, 0), crafter), ticks);

        assertThat(sink.accepted()).isPositive();
        assertThat(crafter.storedCount()).isEqualTo(sink.accepted());
    }

    @Test
    void aBeltStopsWhenTheCrafterIsFull() {
        LogisticsNetwork network = beltLine();
        CrafterGrid crafter = emptyGrid();
        Map<Pos, Port> ports = ports(at(3, 0), crafter);

        feed(network, ports, 4000);

        assertThat(crafter.storedCount()).isEqualTo(CrafterGrid.SLOTS * LIMIT);
        assertThat(crafter.accepts(A)).isFalse();
        network.insertAtEntry(at(0, 0), A);
        LogisticsResult result = network.tick(DT, Map.of(), ports);
        assertThat(result.movedBy(belt(2, 0))).isZero();
        assertThat(result.isBlocked(belt(2, 0))).isTrue();
        assertThat(crafter.storedCount()).isEqualTo(CrafterGrid.SLOTS * LIMIT);
    }

    @Test
    void aPickerTakesFromTheCrafterInSlotOrderAndLeavesADisabledSlotAlone() {
        List<Stack> slots = new ArrayList<>();
        slots.add(MemorySlot.of(ItemRef.material("A"), 2));
        slots.add(MemorySlot.of(ItemRef.material("B"), 1));
        slots.add(MemorySlot.of(ItemRef.material("C"), 4));
        for (int i = 3; i < CrafterGrid.SLOTS; i++) {
            slots.add(MemorySlot.empty());
        }
        boolean[] disabled = new boolean[CrafterGrid.SLOTS];
        disabled[2] = true;
        CrafterGrid crafter = new CrafterGrid(slots, disabled, ref -> LIMIT);
        MemoryPort sink = MemoryPort.sink();

        LogisticsNetwork network = LogisticsNetwork.builder()
                .picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.III)
                .build();
        for (int t = 0; t < 300; t++) {
            network.tick(DT, Map.of(), ports(at(0, 0), crafter, at(2, 0), sink));
        }

        assertThat(sink.contents()).containsExactly(A, A, B);
        assertThat(crafter.storedCount()).isEqualTo(4);
    }
}
