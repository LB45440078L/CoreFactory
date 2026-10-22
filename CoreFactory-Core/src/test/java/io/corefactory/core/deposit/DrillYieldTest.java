package io.corefactory.core.deposit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DrillYieldTest {

    private static final double[] TABLE = {0.5, 1.0, 1.0, 2.5, 6.0};

    @Test
    void theRateOfEachLevelComesFromTheTable() {
        DrillYield yield = new DrillYield(TABLE);

        assertThat(yield.maxLevel()).isEqualTo(5);
        assertThat(yield.itemsPerSecond(1)).isEqualTo(0.5);
        assertThat(yield.itemsPerSecond(4)).isEqualTo(2.5);
        assertThat(yield.itemsPerSecond(5)).isEqualTo(6.0);
    }

    @Test
    void theRatesRiseWithTheLevel() {
        DrillYield yield = new DrillYield(TABLE);

        for (int level = 1; level < yield.maxLevel(); level++) {
            assertThat(yield.itemsPerSecond(level + 1)).isGreaterThanOrEqualTo(yield.itemsPerSecond(level));
        }
    }

    @Test
    void aLevelOutsideTheTableIsRefused() {
        DrillYield yield = new DrillYield(TABLE);

        assertThatThrownBy(() -> yield.itemsPerSecond(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> yield.itemsPerSecond(6)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theTableIsCopiedOnConstruction() {
        double[] source = {1.0, 2.0};
        DrillYield yield = new DrillYield(source);
        source[0] = 99;

        assertThat(yield.itemsPerSecond(1)).isEqualTo(1.0);
    }

    @Test
    void aBadTableIsRefused() {
        assertThatThrownBy(() -> new DrillYield(new double[0])).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DrillYield(new double[] {1.0, 0.5})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DrillYield(new double[] {-1.0})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DrillYield(new double[] {Double.NaN})).isInstanceOf(IllegalArgumentException.class);
    }
}
