package io.corefactory.paper.water;

import io.corefactory.paper.config.YamlMaps;
import io.corefactory.core.water.WaterSettings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the water section of the shipped config.yml reads as the code's defaults. Nothing here starts a server.
 */
class WaterConfigTest {

    @Test
    void theShippedWaterSectionReadsAsItsDefaults() throws IOException {
        assertThat(WaterSettings.from(YamlMaps.toMap(shipped()))).isEqualTo(WaterSettings.defaults());
    }

    @Test
    void theShippedWaterSectionScansTheSourceEveryTwoHundredTicks() throws IOException {
        assertThat(WaterSettings.from(YamlMaps.toMap(shipped())).sourceCheckTicks()).isEqualTo(200);
        assertThat(WaterSettings.from(YamlMaps.toMap(shipped())).sourceMinBlocks()).isEqualTo(100);
    }

    private static YamlConfiguration shipped() throws IOException {
        try (InputStream in = WaterConfigTest.class.getClassLoader().getResourceAsStream("config.yml")) {
            assertThat(in).isNotNull();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
