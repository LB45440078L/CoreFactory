package io.corefactory.core.logistics;

import io.corefactory.core.energy.EnergyTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class LogisticsTableTest {

    @Test
    void beltCapacityIsFourItemsPerUnit() {
        assertThat(LogisticsTable.BELT_CAPACITY).isEqualTo(4);
    }

    @Test
    void pickerDelayIsTenTicksForEveryLevel() {
        assertThat(LogisticsTable.PICKER_DELAY_TICKS).isEqualTo(10);
        assertThat(LogisticsTable.PICKER_DELAY_SECONDS).isCloseTo(0.5, within(1e-12));
        assertThat(LogisticsTable.PICKER_DELAY_SECONDS).isCloseTo(10 * EnergyTable.SECONDS_PER_TICK, within(1e-12));
    }

    @Test
    void beltSpeedsAreTheListedBlocksPerSecond() {
        assertThat(LogisticsTable.beltSpeedBlocksPerSecond(LogisticsLevel.I)).isEqualTo(1.875);
        assertThat(LogisticsTable.beltSpeedBlocksPerSecond(LogisticsLevel.II)).isEqualTo(3.75);
        assertThat(LogisticsTable.beltSpeedBlocksPerSecond(LogisticsLevel.III)).isEqualTo(7.5);
    }

    @Test
    void beltPowerIsTheListedPerLevel() {
        assertThat(LogisticsTable.beltPowerW(LogisticsLevel.I)).isEqualTo(200.0);
        assertThat(LogisticsTable.beltPowerW(LogisticsLevel.II)).isEqualTo(400.0);
        assertThat(LogisticsTable.beltPowerW(LogisticsLevel.III)).isEqualTo(800.0);
    }

    @Test
    void pickerRatesAreTheListedItemsPerSecond() {
        assertThat(LogisticsTable.pickerItemsPerSecond(LogisticsLevel.I)).isEqualTo(1.0);
        assertThat(LogisticsTable.pickerItemsPerSecond(LogisticsLevel.II)).isEqualTo(2.0);
        assertThat(LogisticsTable.pickerItemsPerSecond(LogisticsLevel.III)).isEqualTo(4.0);
    }

    @Test
    void pickerPowerIsTheListedPerLevel() {
        assertThat(LogisticsTable.pickerPowerW(LogisticsLevel.I)).isEqualTo(100.0);
        assertThat(LogisticsTable.pickerPowerW(LogisticsLevel.II)).isEqualTo(200.0);
        assertThat(LogisticsTable.pickerPowerW(LogisticsLevel.III)).isEqualTo(400.0);
    }

    @Test
    void splitterRatesAreTheListedItemsPerSecond() {
        assertThat(LogisticsTable.splitterItemsPerSecond(LogisticsLevel.I)).isEqualTo(2.0);
        assertThat(LogisticsTable.splitterItemsPerSecond(LogisticsLevel.II)).isEqualTo(4.0);
        assertThat(LogisticsTable.splitterItemsPerSecond(LogisticsLevel.III)).isEqualTo(8.0);
    }

    @Test
    void splitterPowerIsTheListedPerLevel() {
        assertThat(LogisticsTable.splitterPowerW(LogisticsLevel.I)).isEqualTo(500.0);
        assertThat(LogisticsTable.splitterPowerW(LogisticsLevel.II)).isEqualTo(1000.0);
        assertThat(LogisticsTable.splitterPowerW(LogisticsLevel.III)).isEqualTo(1500.0);
    }

    @ParameterizedTest
    @EnumSource(LogisticsLevel.class)
    void everyLevelHasPositiveNumbers(LogisticsLevel level) {
        assertThat(LogisticsTable.pickerItemsPerSecond(level)).isPositive();
        assertThat(LogisticsTable.pickerPowerW(level)).isPositive();
        if (level.compareTo(LogisticsLevel.III) <= 0) {
            assertThat(LogisticsTable.beltSpeedBlocksPerSecond(level)).isPositive();
            assertThat(LogisticsTable.beltPowerW(level)).isPositive();
            assertThat(LogisticsTable.splitterItemsPerSecond(level)).isPositive();
            assertThat(LogisticsTable.splitterPowerW(level)).isPositive();
        }
    }

    @Test
    void theLevelsAboveIIIAreForPickersOnly() {
        assertThatThrownBy(() -> LogisticsTable.beltSpeedBlocksPerSecond(LogisticsLevel.IV)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LogisticsTable.splitterPowerW(LogisticsLevel.V)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LogisticsTable.duplexItemsPerSecond(LogisticsLevel.IV)).isInstanceOf(IllegalArgumentException.class);
        assertThat(LogisticsTable.pickerItemsPerSecond(LogisticsLevel.V)).isEqualTo(16.0);
        assertThat(LogisticsTable.pickerPowerW(LogisticsLevel.IV)).isEqualTo(800.0);
    }

    @ParameterizedTest
    @EnumSource(LogisticsLevel.class)
    void higherLevelsAreNeverSlower(LogisticsLevel level) {
        if (level == LogisticsLevel.I) {
            return;
        }
        LogisticsLevel below = LogisticsLevel.values()[level.ordinal() - 1];
        assertThat(LogisticsTable.pickerItemsPerSecond(level)).isGreaterThan(LogisticsTable.pickerItemsPerSecond(below));
        if (level.compareTo(LogisticsLevel.III) <= 0) {
            assertThat(LogisticsTable.beltSpeedBlocksPerSecond(level)).isGreaterThan(LogisticsTable.beltSpeedBlocksPerSecond(below));
            assertThat(LogisticsTable.splitterItemsPerSecond(level)).isGreaterThan(LogisticsTable.splitterItemsPerSecond(below));
        }
    }
}
