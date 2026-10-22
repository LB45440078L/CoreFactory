package io.corefactory.core.vertical;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The coordinate entry points of a carry: a check that puts nothing, and a placement at a coordinate a tower had before a rebuild. */
class VerticalCarryPlaceTest {

    private static final int CAPACITY = 4;

    @Test
    void slotForChangesNothingAndNamesTheIndexTheItemWouldTake() {
        VerticalCarry carry = new VerticalCarry(3);
        assertThat(carry.place(2.0, CAPACITY)).isEqualTo(0);

        assertThat(carry.slotFor(0.5, CAPACITY)).isEqualTo(1);
        assertThat(carry.size()).isEqualTo(1);
    }

    @Test
    void anItemTooCloseToAnotherHasNoSlot() {
        VerticalCarry carry = new VerticalCarry(3);
        carry.place(1.0, CAPACITY);

        assertThat(carry.slotFor(1.1, CAPACITY)).isEqualTo(-1);
        assertThat(carry.place(1.1, CAPACITY)).isEqualTo(-1);
        assertThat(carry.size()).isEqualTo(1);
    }

    @Test
    void itemsStayInOrderNearestTheOutputFirst() {
        VerticalCarry carry = new VerticalCarry(3);
        carry.place(0.0, CAPACITY);
        carry.place(2.0, CAPACITY);
        int middle = carry.place(1.0, CAPACITY);

        assertThat(middle).isEqualTo(1);
        assertThat(carry.positions()).containsExactly(2.0, 1.0, 0.0);
    }

    @Test
    void theOutputEndTakesAnItemAtItsCoordinate() {
        VerticalCarry carry = new VerticalCarry(2);

        assertThat(carry.place(2.0, CAPACITY)).isEqualTo(0);
        assertThat(carry.waiting()).isEqualTo(1);
    }

    @Test
    void aCoordinateOutsideTheTowerIsRefused() {
        VerticalCarry carry = new VerticalCarry(2);

        assertThatThrownBy(() -> carry.place(2.5, CAPACITY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> carry.place(-0.1, CAPACITY)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> carry.slotFor(Double.NaN, CAPACITY)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void offerAtABlockAgreesWithPlaceAtItsEntry() {
        VerticalCarry viaOffer = new VerticalCarry(3);
        VerticalCarry viaPlace = new VerticalCarry(3);

        assertThat(viaOffer.offer(1, CAPACITY)).isTrue();
        assertThat(viaPlace.place(1.0, CAPACITY)).isEqualTo(0);
        assertThat(viaOffer.positions()).isEqualTo(viaPlace.positions());
    }
}
