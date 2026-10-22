package io.corefactory.spigot.vertical;

import io.corefactory.core.vertical.VerticalLevels;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the vertical-belt section of the shipped config.yml names its settings and reads as the defaults. Nothing here starts
 * a server.
 */
class VerticalConfigTest {

    private static final Logger LOG = Logger.getLogger("vertical-config-test");

    @Test
    void theShippedSpeedsAndCapacitiesReadAsTheDefaults() throws IOException {
        YamlConfiguration config = shipped();

        VerticalSettings read = VerticalSettings.from(config, LOG);

        assertThat(config.contains("vertical-belt.speed")).isTrue();
        assertThat(config.contains("vertical-belt.capacity")).isTrue();
        for (int level = 1; level <= VerticalLevels.LEVELS; level++) {
            assertThat(read.levels().speed(level)).as("speed of level " + level)
                    .isEqualTo(VerticalSettings.DEFAULT_SPEEDS[level - 1]);
            assertThat(read.levels().capacity(level)).as("capacity of level " + level)
                    .isEqualTo(VerticalSettings.DEFAULT_CAPACITIES[level - 1]);
        }
        assertThat(read.maxDisplays()).isEqualTo(VerticalSettings.DEFAULT_MAX_DISPLAYS);
    }

    @Test
    void theShippedListsHaveOneValueForEachLevel() throws IOException {
        YamlConfiguration config = shipped();

        assertThat(config.getDoubleList("vertical-belt.speed")).containsExactly(1.875, 3.75, 7.5);
        assertThat(config.getIntegerList("vertical-belt.capacity")).containsExactly(4, 8, 12);
    }

    @Test
    void aListThatIsNotValidFallsBackToTheDefaults() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("vertical-belt.speed", java.util.List.of(1.0, 2.0));
        config.set("vertical-belt.capacity", java.util.List.of(4, 8, 12));

        VerticalSettings read = VerticalSettings.from(config, LOG);

        assertThat(read.levels().speed(1)).isEqualTo(VerticalSettings.DEFAULT_SPEEDS[0]);
        assertThat(read.levels().capacity(3)).isEqualTo(VerticalSettings.DEFAULT_CAPACITIES[2]);
    }

    private static YamlConfiguration shipped() throws IOException {
        try (InputStream in = VerticalConfigTest.class.getClassLoader().getResourceAsStream("config.yml")) {
            assertThat(in).isNotNull();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
