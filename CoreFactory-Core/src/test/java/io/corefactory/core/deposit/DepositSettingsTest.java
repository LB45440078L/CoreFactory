package io.corefactory.core.deposit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DepositSettingsTest {

    private static DepositSettings with(int cellSize, double density, double oilShare, int minRadius, int maxRadius,
                                        long minAmount, long maxAmount, double oreBlockDensity) {
        return new DepositSettings(cellSize, density, oilShare, minRadius, maxRadius, minAmount, maxAmount, oreBlockDensity);
    }

    @Test
    void theDefaultsMatchTheDesign() {
        DepositSettings defaults = DepositSettings.defaults();

        assertThat(defaults.cellSize()).isEqualTo(160);
        assertThat(defaults.density()).isEqualTo(0.6);
        assertThat(defaults.oilShare()).isEqualTo(0.25);
        assertThat(defaults.minRadius()).isEqualTo(24);
        assertThat(defaults.maxRadius()).isEqualTo(48);
        assertThat(defaults.minAmount()).isEqualTo(500_000L);
        assertThat(defaults.maxAmount()).isEqualTo(25_000_000L);
        assertThat(defaults.oreBlockDensity()).isBetween(0.0, 1.0);
    }

    @Test
    void theDefaultsAreValid() {
        assertThatCode(DepositSettings::defaults).doesNotThrowAnyException();
    }

    @Test
    void aCellTooSmallForItsRadiusIsRefused() {
        assertThatThrownBy(() -> with(95, 0.6, 0.25, 24, 48, 1, 10, 0.3)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> with(96, 0.6, 0.25, 24, 48, 1, 10, 0.3)).doesNotThrowAnyException();
    }

    @Test
    void aCellLargerThanTheLimitIsRefused() {
        assertThatThrownBy(() -> with(DepositSettings.MAX_CELL_SIZE + 1, 0.6, 0.25, 24, 48, 1, 10, 0.3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theRadiusRangeMustBeOrderedAndPositive() {
        assertThatThrownBy(() -> with(160, 0.6, 0.25, 0, 48, 1, 10, 0.3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> with(160, 0.6, 0.25, 40, 30, 1, 10, 0.3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> with(600, 0.6, 0.25, 24, DepositSettings.MAX_RADIUS + 1, 1, 10, 0.3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sharesOutsideZeroToOneAreRefused() {
        assertThatThrownBy(() -> with(160, -0.1, 0.25, 24, 48, 1, 10, 0.3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> with(160, 1.1, 0.25, 24, 48, 1, 10, 0.3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> with(160, 0.6, 2.0, 24, 48, 1, 10, 0.3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> with(160, 0.6, 0.25, 24, 48, 1, 10, -0.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> with(160, Double.NaN, 0.25, 24, 48, 1, 10, 0.3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theAmountRangeMustStartAtOneAndNotEndBelowItsStart() {
        assertThatThrownBy(() -> with(160, 0.6, 0.25, 24, 48, 0, 10, 0.3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> with(160, 0.6, 0.25, 24, 48, 10, 9, 0.3)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> with(160, 0.6, 0.25, 24, 48, 10, 10, 0.3)).doesNotThrowAnyException();
    }
}
