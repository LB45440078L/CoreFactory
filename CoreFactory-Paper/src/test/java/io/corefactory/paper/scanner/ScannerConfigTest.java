package io.corefactory.paper.scanner;

import io.corefactory.paper.config.YamlMaps;
import io.corefactory.core.deposit.DepositSettings;
import io.corefactory.core.deposit.ScannerSettings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the scanner keys of the shipped config.yml are in the deposits section and read as the defaults. Nothing here starts
 * a server.
 */
class ScannerConfigTest {

    private static final Logger LOG = Logger.getLogger("scanner-config-test");

    @Test
    void theShippedScannerKeysReadAsTheirDefaults() throws IOException {
        assertThat(ScannerSettings.from(YamlMaps.toMap(shipped()), LOG)).isEqualTo(ScannerSettings.defaults());
    }

    @Test
    void theShippedScannerKeysAreInTheDepositsSection() throws IOException {
        YamlConfiguration config = shipped();

        for (String key : new String[]{"scanner-radius", "scan-cooldown-ticks", "outline-seconds"}) {
            assertThat(config.contains(DepositSettings.SECTION + "." + key)).as(key).isTrue();
        }
    }

    private static YamlConfiguration shipped() throws IOException {
        try (InputStream in = ScannerConfigTest.class.getClassLoader().getResourceAsStream("config.yml")) {
            assertThat(in).isNotNull();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
