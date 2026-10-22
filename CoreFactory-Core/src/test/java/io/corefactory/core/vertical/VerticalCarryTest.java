package io.corefactory.core.vertical;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class VerticalCarryTest {

    private static final double SPEED_LEVEL_1 = 1.875;
    private static final int CAPACITY_LEVEL_1 = 4;

    @Test
    void anItemMovesOneBlockPerCarryPeriod() {
        VerticalCarry carry = new VerticalCarry(3);
        assertThat(carry.offer(0, CAPACITY_LEVEL_1)).isTrue();

        carry.step(1.0 / SPEED_LEVEL_1, SPEED_LEVEL_1, CAPACITY_LEVEL_1);

        assertThat(carry.positions().get(0)).isCloseTo(1.0, within(1e-9));
        carry.step(1.0 / SPEED_LEVEL_1, SPEED_LEVEL_1, CAPACITY_LEVEL_1);
        assertThat(carry.positions().get(0)).isCloseTo(2.0, within(1e-9));
    }

    @Test
    void anItemAt1Point875BlocksPerSecondMovesABlockIn0Point5333Seconds() {
        VerticalCarry carry = new VerticalCarry(3);
        carry.offer(0, CAPACITY_LEVEL_1);

        carry.step(0.5333, SPEED_LEVEL_1, CAPACITY_LEVEL_1);

        assertThat(carry.positions().get(0)).isCloseTo(1.0, within(1e-3));
        assertThat(carry.positions().get(0)).isLessThanOrEqualTo(1.0);
    }

    @Test
    void anItemEntersFromTheMiddleOfTheTower() {
        VerticalCarry carry = new VerticalCarry(5);

        carry.offer(2, CAPACITY_LEVEL_1);
        carry.step(0.5333, SPEED_LEVEL_1, CAPACITY_LEVEL_1);

        assertThat(carry.positions().get(0)).isCloseTo(3.0, within(1e-3));
    }

    @Test
    void anItemLeavesWhenTheOutputTakesIt() {
        VerticalCarry carry = new VerticalCarry(1);
        carry.offer(0, CAPACITY_LEVEL_1);

        int waiting = carry.step(1.0, SPEED_LEVEL_1, CAPACITY_LEVEL_1);

        assertThat(waiting).isEqualTo(1);
        assertThat(carry.waiting()).isEqualTo(1);
        carry.output(waiting);
        assertThat(carry.size()).isZero();
    }

    @Test
    void aFullTowerHoldsItsCapacityAndTheOutputCountIsWhatItWaits() {
        VerticalCarry carry = new VerticalCarry(2);
        for (int i = 0; i < 100; i++) {
            carry.offer(0, CAPACITY_LEVEL_1);
            carry.step(1.0, SPEED_LEVEL_1, CAPACITY_LEVEL_1);
        }
        // Two blocks of four, plus four waiting at the output end.
        assertThat(carry.size()).isEqualTo(12);
        assertThat(carry.waiting()).isEqualTo(4);
        assertThat(carry.offer(0, CAPACITY_LEVEL_1)).isFalse();

        assertThat(carry.step(1.0, SPEED_LEVEL_1, CAPACITY_LEVEL_1)).isEqualTo(4);

        carry.output(4);
        assertThat(carry.size()).isEqualTo(8);
        assertThat(carry.step(1.0, SPEED_LEVEL_1, CAPACITY_LEVEL_1)).isEqualTo(4);
    }

    @Test
    void theOutputCannotTakeMoreThanWaits() {
        VerticalCarry carry = new VerticalCarry(1);
        carry.offer(0, CAPACITY_LEVEL_1);
        carry.step(1.0, SPEED_LEVEL_1, CAPACITY_LEVEL_1);

        assertThatThrownBy(() -> carry.output(2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> carry.output(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(carry.size()).isEqualTo(1);
    }

    @Test
    void anItemIsRefusedWhenItWouldBeTooCloseToAnotherOne() {
        VerticalCarry carry = new VerticalCarry(2);

        assertThat(carry.offer(0, CAPACITY_LEVEL_1)).isTrue();
        assertThat(carry.offer(0, CAPACITY_LEVEL_1)).isFalse();
        assertThat(carry.size()).isEqualTo(1);
    }

    @Test
    void badArgumentsAreRejected() {
        VerticalCarry carry = new VerticalCarry(2);

        assertThatThrownBy(() -> carry.offer(2, CAPACITY_LEVEL_1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> carry.offer(0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> carry.step(-1.0, SPEED_LEVEL_1, CAPACITY_LEVEL_1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> carry.step(1.0, 0.0, CAPACITY_LEVEL_1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VerticalCarry(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
