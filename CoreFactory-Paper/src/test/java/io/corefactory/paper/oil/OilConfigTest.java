package io.corefactory.paper.oil;

import io.corefactory.paper.config.YamlMaps;
import io.corefactory.core.oil.OilSettings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks that the oil section of the shipped config.yml reads as the code's defaults. Nothing here starts a server. */
class OilConfigTest {

    @Test
    void theShippedOilSectionReadsAsItsDefaults() throws IOException {
        assertThat(OilSettings.from(YamlMaps.toMap(shipped()))).isEqualTo(OilSettings.defaults());
    }

    @Test
    void theShippedOilSectionHoldsOneHundredLitresAndFlowsFourLitresASecond() throws IOException {
        OilSettings settings = OilSettings.from(YamlMaps.toMap(shipped()));

        assertThat(settings.tubeCapacityLitres()).isEqualTo(100.0);
        assertThat(settings.pipeFlowLitresPerSecond()).isEqualTo(4.0);
    }

    private static YamlConfiguration shipped() throws IOException {
        try (InputStream in = OilConfigTest.class.getClassLoader().getResourceAsStream("config.yml")) {
            assertThat(in).isNotNull();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
