package io.corefactory.spigot.machine;

import io.corefactory.core.upgrade.LabLevels;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class LabUpgradeRatesTest {

    private static final Logger LOG = Logger.getLogger(LabUpgradeRatesTest.class.getName());

    @Test
    void theConfiguredFactorsAreReadLevelByLevel() {
        LabLevels levels = LabUpgradeRates.read(List.of(1.0, 1.25, 3.0), LOG);

        assertThat(levels.speed(2)).isEqualTo(1.25);
        assertThat(levels.speed(3)).isEqualTo(3.0);
    }

    @Test
    void theDefaultsAreTheDefaultLevelTable() {
        LabLevels defaults = LabLevels.defaults();

        assertThat(LabUpgradeRates.read(LabUpgradeRates.DEFAULTS, LOG).speed(2)).isEqualTo(defaults.speed(2));
        assertThat(LabUpgradeRates.read(LabUpgradeRates.DEFAULTS, LOG).speed(3)).isEqualTo(defaults.speed(3));
    }

    @Test
    void aMissingOrShortListFallsBackToTheDefaults() {
        assertThat(LabUpgradeRates.read(null, LOG).speed(3)).isEqualTo(LabLevels.defaults().speed(3));
        assertThat(LabUpgradeRates.read(List.of(1.0, 2.0), LOG).speed(2)).isEqualTo(LabLevels.defaults().speed(2));
    }

    @Test
    void aFactorThatIsNotPositiveOrFallsBackToTheDefaults() {
        assertThat(LabUpgradeRates.read(Arrays.asList(1.0, 0.0, 2.0), LOG).speed(2))
                .isEqualTo(LabLevels.defaults().speed(2));
        assertThat(LabUpgradeRates.read(Arrays.asList(1.0, null, 2.0), LOG).speed(2))
                .isEqualTo(LabLevels.defaults().speed(2));
    }

    @Test
    void aFactorThatFallsFromTheLevelBelowFallsBackToTheDefaults() {
        assertThat(LabUpgradeRates.read(List.of(1.0, 2.0, 1.5), LOG).speed(3))
                .isEqualTo(LabLevels.defaults().speed(3));
    }
}
