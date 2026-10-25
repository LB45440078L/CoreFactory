package io.corefactory.core.deposit;

import io.corefactory.core.config.TestConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class DepositSettingsFromConfigTest {

    private static final Logger LOG = Logger.getLogger("deposit-settings-test");

    private static Map<String, Object> yaml(String text) {
        return TestConfig.yaml(text);
    }

    @Test
    void aMissingSectionGivesTheDefaults() {
        assertThat(DepositSettings.from(null, LOG)).isEqualTo(DepositSettings.defaults());
        assertThat(DepositSettings.from(yaml("other: 1\n"), LOG)).isEqualTo(DepositSettings.defaults());
    }

    @Test
    void theValuesOfTheSectionAreRead() {
        DepositSettings read = DepositSettings.from(yaml("""
                deposits:
                  cell-size: 200
                  density: 0.5
                  oil-share: 0.1
                  min-radius: 20
                  max-radius: 60
                  min-amount: 1000
                  max-amount: 2000000
                  ore-block-density: 0.4
                """), LOG);

        assertThat(read).isEqualTo(new DepositSettings(200, 0.5, 0.1, 20, 60, 1000L, 2_000_000L, 0.4));
    }

    @Test
    void aMissingKeyTakesItsDefault() {
        DepositSettings read = DepositSettings.from(yaml("deposits:\n  density: 0.2\n"), LOG);

        assertThat(read).isEqualTo(new DepositSettings(160, 0.2, 0.25, 24, 48, 500_000L, 25_000_000L, 0.3));
    }

    @Test
    void aSectionThatBreaksARuleFallsBackToTheDefaultsAsAWhole() {
        DepositSettings read = DepositSettings.from(yaml("""
                deposits:
                  density: 0.9
                  oil-share: 3.0
                """), LOG);

        assertThat(read).isEqualTo(DepositSettings.defaults());
    }

    @Test
    void aCellTooSmallForTheRadiusFallsBackToTheDefaults() {
        assertThat(DepositSettings.from(yaml("deposits:\n  cell-size: 90\n"), LOG)).isEqualTo(DepositSettings.defaults());
    }
}
