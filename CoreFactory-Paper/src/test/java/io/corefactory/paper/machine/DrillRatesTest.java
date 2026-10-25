package io.corefactory.paper.machine;

import io.corefactory.core.deposit.DrillYield;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class DrillRatesTest {

    private static final Logger LOG = Logger.getLogger(DrillRatesTest.class.getName());

    @Test
    void theConfiguredRatesAreReadLevelByLevel() {
        DrillYield yield = DrillRates.read(List.of(0.5, 1.0, 2.0, 3.0, 4.5), LOG);

        assertThat(yield.maxLevel()).isEqualTo(5);
        assertThat(yield.itemsPerSecond(1)).isEqualTo(0.5);
        assertThat(yield.itemsPerSecond(5)).isEqualTo(4.5);
    }

    @Test
    void theDefaultsAreAValidTableOfFiveLevels() {
        DrillYield yield = DrillRates.defaults();

        assertThat(yield.maxLevel()).isEqualTo(5);
        for (int level = 1; level < 5; level++) {
            assertThat(yield.itemsPerSecond(level + 1)).isGreaterThanOrEqualTo(yield.itemsPerSecond(level));
        }
    }

    @Test
    void aMissingOrShortListFallsBackToTheDefaults() {
        assertThat(DrillRates.read(null, LOG).itemsPerSecond(3)).isEqualTo(DrillRates.defaults().itemsPerSecond(3));
        assertThat(DrillRates.read(List.of(1.0, 2.0), LOG).itemsPerSecond(1))
                .isEqualTo(DrillRates.defaults().itemsPerSecond(1));
    }

    @Test
    void aRateThatFallsOrIsNegativeFallsBackToTheDefaults() {
        assertThat(DrillRates.read(List.of(2.0, 1.0, 3.0, 4.0, 5.0), LOG).itemsPerSecond(1))
                .isEqualTo(DrillRates.defaults().itemsPerSecond(1));
        assertThat(DrillRates.read(Arrays.asList(1.0, -2.0, 3.0, 4.0, 5.0), LOG).itemsPerSecond(2))
                .isEqualTo(DrillRates.defaults().itemsPerSecond(2));
        assertThat(DrillRates.read(Arrays.asList(1.0, null, 3.0, 4.0, 5.0), LOG).itemsPerSecond(1))
                .isEqualTo(DrillRates.defaults().itemsPerSecond(1));
    }
}
