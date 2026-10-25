package io.corefactory.core.railgun;

import io.corefactory.core.config.TestConfig;
import io.corefactory.core.turret.TurretSettings;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class RailgunSettingsTest {

    @Test
    void theDefaultsAreTheDocumentedNumbers() {
        RailgunSettings s = RailgunSettings.defaults();

        assertThat(s.enabled()).isTrue();
        assertThat(s.hitDistanceBlocks()).isEqualTo(0.5);
        assertThat(s.armourBypass()).isEqualTo(0.75);
        assertThat(s.helixSpeedLevelOne()).isEqualTo(2.0);
        assertThat(s.helixSpeedLevelThree()).isEqualTo(9.0);
        assertThat(s.spiralDelaySeconds()).isEqualTo(0.5);
        assertThat(s.strandRadiusBlocks()).isEqualTo(0.3);
        assertThat(s.strands()).isEqualTo(4);
        assertThat(s.strandTwistTurnsPerBlock()).isEqualTo(0.5);
        assertThat(s.spiralRadiusBlocks()).isEqualTo(0.75);
        assertThat(s.spiralTurnsPerBlock()).isEqualTo(1.0);
        assertThat(s.particleBudget()).isEqualTo(96);
        assertThat(s.maxLight()).isEqualTo(15);
    }

    @Test
    void theLevelStatsAreTheDocumentedNumbers() {
        RailgunSettings s = RailgunSettings.defaults();

        assertThat(s.level(RailgunLevel.I)).isEqualTo(new RailgunSettings.LevelStats(RailgunLevel.I, 400.0, 2_000_000, 60.0, 6.0, 10.0));
        assertThat(s.level(RailgunLevel.II)).isEqualTo(new RailgunSettings.LevelStats(RailgunLevel.II, 1200.0, 4_000_000, 80.0, 5.0, 9.0));
        assertThat(s.level(RailgunLevel.III)).isEqualTo(new RailgunSettings.LevelStats(RailgunLevel.III, 3500.0, 8_000_000, 110.0, 4.0, 8.0));
    }

    @Test
    void theShotDelayIsTheChargePlusTheReload() {
        RailgunSettings s = RailgunSettings.defaults();

        assertThat(s.level(RailgunLevel.I).fireIntervalTicks()).isEqualTo(320);
        assertThat(s.level(RailgunLevel.II).fireIntervalTicks()).isEqualTo(280);
        assertThat(s.level(RailgunLevel.III).fireIntervalTicks()).isEqualTo(240);
    }

    @Test
    void theHelixSpeedIsSetAtLevelOneAndThreeAndInterpolatedAtTwo() {
        RailgunSettings s = RailgunSettings.defaults();

        assertThat(s.helixSpeed(RailgunLevel.I)).isEqualTo(2.0);
        assertThat(s.helixSpeed(RailgunLevel.II)).isCloseTo(5.5, within(1e-9));
        assertThat(s.helixSpeed(RailgunLevel.III)).isEqualTo(9.0);
    }

    @Test
    void theSpiralDelayIsTenTicksByDefault() {
        assertThat(RailgunSettings.defaults().spiralDelayTicks()).isEqualTo(10);
    }

    @Test
    void aMissingSectionGivesTheDefaults() {
        assertThat(RailgunSettings.from(Map.of())).isEqualTo(RailgunSettings.defaults());
        assertThat(RailgunSettings.from(null)).isEqualTo(RailgunSettings.defaults());
    }

    @Test
    void theShippedConfigYmlReadsAsTheDefaults() throws IOException {
        Map<String, Object> config = TestConfig.yaml(
                Files.readString(Path.of("..", "CoreFactory-Paper", "src", "main", "resources", "config.yml")));

        assertThat(RailgunSettings.from(config)).isEqualTo(RailgunSettings.defaults());
    }

    @Test
    void aLevelOverrideReplacesOnlyTheKeysItNames() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "railgun.levels.2.damage", 2000);
        TestConfig.set(config, "railgun.levels.3.charge-seconds", 3);

        RailgunSettings s = RailgunSettings.from(config);

        assertThat(s.level(RailgunLevel.II).damage()).isEqualTo(2000.0);
        assertThat(s.level(RailgunLevel.II).range()).isEqualTo(80.0);
        assertThat(s.level(RailgunLevel.III).chargeSeconds()).isEqualTo(3.0);
        assertThat(s.level(RailgunLevel.III).reloadSeconds()).isEqualTo(8.0);
        assertThat(s.level(RailgunLevel.I)).isEqualTo(RailgunSettings.defaults().level(RailgunLevel.I));
    }

    @Test
    void aValueThatIsNotANumberGivesItsDefault() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "railgun.helix-speed-level-3", "fast");
        TestConfig.set(config, "railgun.hit-distance-blocks", Double.NaN);
        TestConfig.set(config, "railgun.levels.1.damage", "lots");

        RailgunSettings s = RailgunSettings.from(config);

        assertThat(s.helixSpeedLevelThree()).isEqualTo(9.0);
        assertThat(s.hitDistanceBlocks()).isEqualTo(0.5);
        assertThat(s.level(RailgunLevel.I).damage()).isEqualTo(400.0);
    }

    @Test
    void aValueOutOfRangeIsClampedToItsRange() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "railgun.armour-bypass", 3);
        TestConfig.set(config, "railgun.strands", 0);
        TestConfig.set(config, "railgun.particle-budget", -5);
        TestConfig.set(config, "railgun.max-light", 40);
        TestConfig.set(config, "railgun.levels.3.range", 500);

        RailgunSettings s = RailgunSettings.from(config);

        assertThat(s.armourBypass()).isEqualTo(1.0);
        assertThat(s.strands()).isEqualTo(1);
        assertThat(s.particleBudget()).isZero();
        assertThat(s.maxLight()).isEqualTo(15);
        assertThat(s.level(RailgunLevel.III).range()).isEqualTo(128.0);
    }

    @Test
    void theEnabledSwitchIsRead() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "railgun.enabled", false);

        assertThat(RailgunSettings.from(config).enabled()).isFalse();
    }

    @Test
    void aRecordWithAWrongLevelCountIsRefused() {
        RailgunSettings s = RailgunSettings.defaults();

        assertThatThrownBy(() -> new RailgunSettings(s.enabled(), s.hitDistanceBlocks(), s.armourBypass(), s.helixSpeedLevelOne(),
                s.helixSpeedLevelThree(), s.spiralDelaySeconds(), s.strandRadiusBlocks(), s.strands(), s.strandTwistTurnsPerBlock(),
                s.spiralRadiusBlocks(), s.spiralTurnsPerBlock(), s.sampleSpacingBlocks(), s.spinTurnsPerSecond(),
                s.particleBudget(), s.maxParticlesPerTick(), s.maxLight(),
                List.of(s.level(RailgunLevel.I))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theTurretSettingsOfALevelUseTheMachineGunsTargetingAndItsOwnRangeAndInterval() {
        TurretSettings machineGun = TurretSettings.defaults();

        TurretSettings view = RailgunSettings.defaults().asTurret(machineGun, RailgunLevel.III);

        assertThat(view.range()).isEqualTo(110.0);
        assertThat(view.firingIntervalTicks()).isEqualTo(240);
        assertThat(view.powerDemandW()).isEqualTo(8_000_000);
        assertThat(view.searchIntervalTicks()).isEqualTo(machineGun.searchIntervalTicks());
        assertThat(view.arcDegrees()).isEqualTo(machineGun.arcDegrees());
        assertThat(view.targets()).isEqualTo(machineGun.targets());
        assertThat(view.accuracySpread()).isZero();
        assertThat(view.enabled()).isTrue();
    }

    @Test
    void aRailgunRunsOnlyWhenItsOwnSwitchAndTheTurretsAreOn() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "railgun.enabled", false);

        assertThat(RailgunSettings.from(config).asTurret(TurretSettings.defaults(), RailgunLevel.I).enabled()).isFalse();
    }
}
