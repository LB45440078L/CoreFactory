package io.corefactory.core.deposit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DrillAccumulatorTest {

    @Test
    void aFractionalRateCarriesItsCreditToTheNextStep() {
        DrillAccumulator drill = new DrillAccumulator(1.0);

        long[] taken = new long[8];
        for (int i = 0; i < taken.length; i++) {
            taken[i] = drill.step(0.25, 1000);
        }

        assertThat(taken).containsExactly(0, 0, 0, 1, 0, 0, 0, 1);
        assertThat(drill.credit()).isZero();
    }

    @Test
    void theCreditHoldsTheUnfinishedPartOfAnItem() {
        DrillAccumulator drill = new DrillAccumulator(1.0);

        assertThat(drill.step(0.5, 1000)).isZero();
        assertThat(drill.credit()).isEqualTo(0.5);
        assertThat(drill.step(0.25, 1000)).isZero();
        assertThat(drill.credit()).isEqualTo(0.75);
    }

    @Test
    void theTotalMatchesTheRateOverTime() {
        DrillAccumulator drill = new DrillAccumulator(0.125);

        long total = 0;
        for (int i = 0; i < 16; i++) {
            total += drill.step(2.5, 1_000_000);
        }

        assertThat(total).isEqualTo(5);
        assertThat(drill.credit()).isZero();
    }

    @Test
    void neverTakesMoreThanTheDepositHolds() {
        DrillAccumulator drill = new DrillAccumulator(1.0);

        assertThat(drill.step(100, 5)).isEqualTo(5);
        assertThat(drill.credit()).isZero();
    }

    @Test
    void zeroSatisfactionGivesNothingAndKeepsTheCredit() {
        DrillAccumulator drill = new DrillAccumulator(1.0);
        drill.step(0.5, 1000);

        assertThat(drill.step(0, 1000)).isZero();
        assertThat(drill.step(0, 1000)).isZero();
        assertThat(drill.credit()).isEqualTo(0.5);
    }

    @Test
    void anExhaustedDepositStopsTheOutputAndResetsTheCredit() {
        DrillAccumulator drill = new DrillAccumulator(1.0);
        assertThat(drill.step(1.5, 1000)).isEqualTo(1);
        assertThat(drill.credit()).isEqualTo(0.5);

        assertThat(drill.step(1.5, 1)).isEqualTo(1);
        assertThat(drill.credit()).isZero();

        assertThat(drill.step(5, 0)).isZero();
        assertThat(drill.credit()).isZero();
    }

    @Test
    void aStepWithNothingLeftDropsTheCreditItHeld() {
        DrillAccumulator drill = new DrillAccumulator(1.0);
        drill.step(0.5, 1000);

        assertThat(drill.step(0.5, 0)).isZero();
        assertThat(drill.credit()).isZero();
    }

    @Test
    void invalidArgumentsAreRefused() {
        assertThatThrownBy(() -> new DrillAccumulator(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DrillAccumulator(Double.POSITIVE_INFINITY)).isInstanceOf(IllegalArgumentException.class);

        DrillAccumulator drill = new DrillAccumulator(1.0);
        assertThatThrownBy(() -> drill.step(-1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> drill.step(Double.NaN, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> drill.step(1, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
