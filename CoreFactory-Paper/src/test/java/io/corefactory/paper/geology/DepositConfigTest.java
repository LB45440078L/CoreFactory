package io.corefactory.paper.geology;

import io.corefactory.paper.config.YamlMaps;
import io.corefactory.core.deposit.DepositSettings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the deposits section of the shipped config.yml lists every setting and reads as the defaults. Nothing here
 * starts a server.
 */
class DepositConfigTest {

    private static final Logger LOG = Logger.getLogger("deposit-config-test");

    @Test
    void theShippedDepositsSectionReadsAsItsDefaults() throws IOException {
        assertThat(DepositSettings.from(YamlMaps.toMap(shipped()), LOG)).isEqualTo(DepositSettings.defaults());
    }

    @Test
    void theShippedDepositsSectionNamesEverySetting() throws IOException {
        YamlConfiguration config = shipped();

        for (String key : new String[]{"cell-size", "density", "oil-share", "min-radius", "max-radius", "min-amount",
                "max-amount", "ore-block-density"}) {
            assertThat(config.contains(DepositSettings.SECTION + "." + key)).as(key).isTrue();
        }
    }

    private static YamlConfiguration shipped() throws IOException {
        try (InputStream in = DepositConfigTest.class.getClassLoader().getResourceAsStream("config.yml")) {
            assertThat(in).isNotNull();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
