package io.corefactory.spigot.turret;

import io.corefactory.core.turret.DualTargeting;
import io.corefactory.core.turret.TurretLevels;
import io.corefactory.core.turret.TurretSettings;
import io.corefactory.core.turret.TurretType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks that the upgrade levels and the dual spacing in the shipped config.yml read as the defaults in code, and that the machine gun section
 * holds no stat a level owns at its root. Nothing here starts a server.
 */
class TurretLevelConfigTest {

    @Test
    void theShippedMachineGunLevelsReadAsTheirDefaults() throws IOException {
        assertThat(TurretLevels.from(TurretType.MACHINE_GUN, shipped())).isEqualTo(TurretLevels.MACHINE_GUN);
    }

    @Test
    void theShippedLaserLevelsReadAsTheirDefaults() throws IOException {
        assertThat(TurretLevels.from(TurretType.LASER, shipped())).isEqualTo(TurretLevels.LASER);
    }

    @Test
    void theShippedDualSpacingIsTheDefault() throws IOException {
        assertThat(DualTargeting.spacingOf(shipped())).isEqualTo(DualTargeting.DEFAULT_SPACING_BLOCKS);
    }

    @Test
    void theShippedArcAndTiltAllowTheWholeSphere() throws IOException {
        TurretSettings settings = TurretSettings.from(shipped());

        assertThat(settings.arcDegrees()).isEqualTo(360.0);
        assertThat(settings.pitchLimitDegrees()).isEqualTo(90.0);
    }

    @Test
    void theShippedMachineGunSectionHoldsNoStatThatALevelOwns() throws IOException {
        YamlConfiguration config = shipped();

        assertThat(config.contains("turrets.range")).isFalse();
        assertThat(config.contains("turrets.firing-interval-ticks")).isFalse();
        assertThat(config.contains("turrets.ammo-capacity")).isFalse();
        assertThat(config.contains("turrets.power-demand-w")).isFalse();
        assertThat(config.contains("turrets.levels.5.ammo-capacity")).isTrue();
    }

    private static YamlConfiguration shipped() throws IOException {
        try (InputStream in = TurretLevelConfigTest.class.getClassLoader().getResourceAsStream("config.yml")) {
            assertThat(in).isNotNull();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }
}
