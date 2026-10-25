package io.corefactory.core.energy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PylonTest {

    private static Pylon pylon(PylonType type) {
        return new Pylon("p", new Pos(0, 64, 0), type);
    }

    @Test
    void theRangeSquareCoversItsHalfSideOnEachSideIncludingTheEdges() {
        Pylon small = pylon(PylonType.SMALL);
        assertThat(small.covers(new Pos(5, 64, 5))).isTrue();
        assertThat(small.covers(new Pos(-5, 64, -5))).isTrue();
        assertThat(small.covers(new Pos(5, 64, -5))).isTrue();
        assertThat(small.covers(new Pos(0, 64, 0))).isTrue();
    }

    @Test
    void oneBlockBeyondTheSquareIsNotCovered() {
        Pylon small = pylon(PylonType.SMALL);
        assertThat(small.covers(new Pos(6, 64, 0))).isFalse();
        assertThat(small.covers(new Pos(0, 64, -6))).isFalse();
    }

    @Test
    void theHeightOfTheBlockDoesNotMatter() {
        Pylon small = pylon(PylonType.SMALL);
        assertThat(small.covers(new Pos(0, -60, 0))).isTrue();
        assertThat(small.covers(new Pos(0, 300, 0))).isTrue();
    }

    @Test
    void eachSizeHasItsOwnRange() {
        assertThat(pylon(PylonType.MEDIUM).covers(new Pos(9, 64, 9))).isTrue();
        assertThat(pylon(PylonType.MEDIUM).covers(new Pos(10, 64, 0))).isFalse();
        assertThat(pylon(PylonType.LARGE).covers(new Pos(15, 64, -15))).isTrue();
        assertThat(pylon(PylonType.LARGE).covers(new Pos(16, 64, 0))).isFalse();
    }

    @Test
    void theHugeAndColossalRangesAreTheirOwnSquares() {
        assertThat(pylon(PylonType.HUGE).covers(new Pos(25, 64, -25))).isTrue();
        assertThat(pylon(PylonType.HUGE).covers(new Pos(26, 64, 0))).isFalse();
        assertThat(pylon(PylonType.HUGE).covers(new Pos(0, 64, -26))).isFalse();
        assertThat(pylon(PylonType.COLOSSAL).covers(new Pos(40, 64, -40))).isTrue();
        assertThat(pylon(PylonType.COLOSSAL).covers(new Pos(41, 64, 0))).isFalse();
        assertThat(pylon(PylonType.COLOSSAL).covers(new Pos(0, 64, 41))).isFalse();
    }

    @Test
    void theHugeAndColossalPylonsCarryTheirCapacityAndCurrent() {
        assertThat(pylon(PylonType.HUGE).maxCurrentA()).isEqualTo(4_000.0);
        assertThat(pylon(PylonType.HUGE).capacityW()).isEqualTo(920_000.0);
        assertThat(pylon(PylonType.COLOSSAL).maxCurrentA()).isEqualTo(10_000.0);
        assertThat(pylon(PylonType.COLOSSAL).capacityW()).isEqualTo(2_300_000.0);
    }

    @Test
    void aPylonCarriesTheCapacityAndCurrentOfItsType() {
        assertThat(pylon(PylonType.SMALL).capacityW()).isEqualTo(23_000.0);
        assertThat(pylon(PylonType.MEDIUM).maxCurrentA()).isEqualTo(400.0);
    }

    @Test
    void theIdAndTheTypeAreRequired() {
        assertThatThrownBy(() -> new Pylon(" ", new Pos(0, 0, 0), PylonType.SMALL))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Pylon("p", new Pos(0, 0, 0), null)).isInstanceOf(NullPointerException.class);
    }
}
