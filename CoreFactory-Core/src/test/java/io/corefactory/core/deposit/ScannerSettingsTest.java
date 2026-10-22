package io.corefactory.core.deposit;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScannerSettingsTest {

    private static final Logger LOG = Logger.getLogger("scanner-settings-test");

    private static YamlConfiguration yaml(String text) {
        return YamlConfiguration.loadConfiguration(new StringReader(text));
    }

    @Test
    void theDefaultsAre96BlocksSixtyTicksAndTwentySeconds() {
        assertThat(ScannerSettings.defaults()).isEqualTo(new ScannerSettings(96, 60, 20));
    }

    @Test
    void aMissingSectionGivesTheDefaults() {
        assertThat(ScannerSettings.from(null, LOG)).isEqualTo(ScannerSettings.defaults());
        assertThat(ScannerSettings.from(yaml("other: 1\n"), LOG)).isEqualTo(ScannerSettings.defaults());
    }

    @Test
    void theValuesOfTheSectionAreRead() {
        ScannerSettings read = ScannerSettings.from(yaml("""
                deposits:
                  scanner-radius: 128
                  scan-cooldown-ticks: 20
                  outline-seconds: 45
                """), LOG);

        assertThat(read).isEqualTo(new ScannerSettings(128, 20, 45));
    }

    @Test
    void aSectionThatBreaksARuleFallsBackToTheDefaults() {
        ScannerSettings read = ScannerSettings.from(yaml("""
                deposits:
                  scanner-radius: 96
                  outline-seconds: 0
                """), LOG);

        assertThat(read).isEqualTo(ScannerSettings.defaults());
    }

    @Test
    void theRangesAreChecked() {
        assertThatThrownBy(() -> new ScannerSettings(0, 60, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScannerSettings(513, 60, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScannerSettings(96, -1, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScannerSettings(96, 1201, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScannerSettings(96, 60, 121)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new ScannerSettings(512, 0, 120)).isNotNull();
    }
}
