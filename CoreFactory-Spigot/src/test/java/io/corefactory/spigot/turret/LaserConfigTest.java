package io.corefactory.spigot.turret;

import io.corefactory.core.turret.LaserSettings;
import io.corefactory.core.turret.TurretLevels;
import io.corefactory.core.turret.TurretType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the laser-turret section of the shipped config.yml reads as the documented defaults, and that the stats a level owns are not
 * repeated at its root. Nothing here starts a server.
 */
class LaserConfigTest {

    @Test
    void theShippedLaserSectionReadsAsItsDefaults() throws IOException {
        assertThat(LaserSettings.from(shipped())).isEqualTo(LaserSettings.defaults());
    }

    @Test
    void theShippedLaserLevelsReadAsTheirDefaults() throws IOException {
        assertThat(TurretLevels.from(TurretType.LASER, shipped())).isEqualTo(TurretLevels.LASER);
    }

    @Test
    void theShippedLaserScansEveryFiveTicks() throws IOException {
        assertThat(LaserSettings.from(shipped()).searchIntervalTicks()).isEqualTo(5);
    }

    @Test
    void theShippedLaserSectionHoldsNoStatThatALevelOwns() throws IOException {
        YamlConfiguration config = shipped();

        assertThat(config.contains("laser-turret.range")).isFalse();
        assertThat(config.contains("laser-turret.damage")).isFalse();
        assertThat(config.contains("laser-turret.firing-interval-ticks")).isFalse();
        assertThat(config.contains("laser-turret.power-demand-w")).isFalse();
        assertThat(config.contains("laser-turret.levels.1.range")).isTrue();
    }

    private static YamlConfiguration shipped() throws IOException {
        try (InputStream in = LaserConfigTest.class.getClassLoader().getResourceAsStream("config.yml")) {
            assertThat(in).isNotNull();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
