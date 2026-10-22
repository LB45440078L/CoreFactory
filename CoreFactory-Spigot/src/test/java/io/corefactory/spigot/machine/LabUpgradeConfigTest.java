package io.corefactory.spigot.machine;

import io.corefactory.core.upgrade.LabLevels;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the machine-upgrades section of the shipped config.yml names its setting and reads as the defaults. Nothing here
 * starts a server.
 */
class LabUpgradeConfigTest {

    private static final Logger LOG = Logger.getLogger("lab-upgrade-config-test");

    @Test
    void theShippedSpeedsReadAsTheDefaults() throws IOException {
        YamlConfiguration config = shipped();

        LabLevels read = LabUpgradeRates.read(config.getDoubleList("machine-upgrades.speed"), LOG);

        assertThat(config.contains("machine-upgrades.speed")).isTrue();
        for (int level = LabLevels.MIN_LEVEL; level <= LabLevels.MAX_LEVEL; level++) {
            assertThat(read.speed(level)).as("level " + level).isEqualTo(LabLevels.defaults().speed(level));
        }
    }

    @Test
    void theShippedSpeedListHasOneFactorForEachLevel() throws IOException {
        assertThat(shipped().getDoubleList("machine-upgrades.speed")).containsExactly(1.0, 1.5, 2.0);
    }

    private static YamlConfiguration shipped() throws IOException {
        try (InputStream in = LabUpgradeConfigTest.class.getClassLoader().getResourceAsStream("config.yml")) {
            assertThat(in).isNotNull();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
