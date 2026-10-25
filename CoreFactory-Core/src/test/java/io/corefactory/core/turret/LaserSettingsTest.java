package io.corefactory.core.turret;

import io.corefactory.core.config.TestConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LaserSettingsTest {

    @Test
    void aMissingSectionGivesTheDocumentedDefaults() {
        LaserSettings laser = LaserSettings.from(Map.of());

        assertThat(laser.range()).isEqualTo(40.0);
        assertThat(laser.damage()).isEqualTo(12.0);
        assertThat(laser.armourBypass()).isEqualTo(1.0);
        assertThat(laser.firingIntervalTicks()).isEqualTo(10);
        assertThat(laser.powerDemandW()).isEqualTo(60000);
        assertThat(laser.beamParticles()).isEqualTo(24);
    }

    @Test
    void aLaserScansEveryFiveTicksUnlessTheSectionSaysOtherwise() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "laser-turret.search-interval-ticks", 2);

        assertThat(LaserSettings.from(Map.of()).searchIntervalTicks()).isEqualTo(5);
        assertThat(LaserSettings.from(config).searchIntervalTicks()).isEqualTo(2);
        assertThat(LaserSettings.defaults().searchIntervalTicks()).isEqualTo(LaserSettings.DEFAULT_SEARCH_INTERVAL_TICKS);
    }

    @Test
    void theValuesAreReadFromTheLaserSection() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "laser-turret.armour-bypass", 0.25);
        TestConfig.set(config, "laser-turret.beam-particles", 12);

        LaserSettings laser = LaserSettings.from(config);

        assertThat(laser.armourBypass()).isEqualTo(0.25);
        assertThat(laser.beamParticles()).isEqualTo(12);
    }

    @Test
    void theLevelStatsAreNotReadFromTheRootKeys() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "laser-turret.range", 52);
        TestConfig.set(config, "laser-turret.damage", 9.5);
        TestConfig.set(config, "laser-turret.firing-interval-ticks", 6);
        TestConfig.set(config, "laser-turret.power-demand-w", 30000);

        LaserSettings laser = LaserSettings.from(config);

        assertThat(laser.range()).isEqualTo(TurretLevels.LASER.level(1).range());
        assertThat(laser.damage()).isEqualTo(TurretLevels.LASER.level(1).damage());
        assertThat(laser.firingIntervalTicks()).isEqualTo(TurretLevels.LASER.level(1).fireIntervalTicks());
        assertThat(laser.powerDemandW()).isEqualTo(TurretLevels.LASER.level(1).powerDemandW());
    }

    @Test
    void theValuesAreClampedToTheirRanges() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "laser-turret.armour-bypass", 3);
        TestConfig.set(config, "laser-turret.beam-particles", 100);

        LaserSettings laser = LaserSettings.from(config);

        assertThat(laser.armourBypass()).isEqualTo(1.0);
        assertThat(laser.beamParticles()).isEqualTo(BeamPlan.MAX_BEAM_PARTICLES);
    }

    @Test
    void theTurretViewKeepsTheMachineGunsTargetingAndTurningAndTakesTheLasersStats() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "turrets.targets", List.of("monsters", "phantoms"));
        TestConfig.set(config, "turrets.target-named", true);
        TestConfig.set(config, "turrets.arc-degrees", 90);
        TestConfig.set(config, "turrets.rotation-speed-degrees", 4);
        TurretSettings machineGun = TurretSettings.from(config);

        TurretSettings view = LaserSettings.defaults().asTurret(machineGun);

        assertThat(view.range()).isEqualTo(40.0);
        assertThat(view.firingIntervalTicks()).isEqualTo(10);
        assertThat(view.powerDemandW()).isEqualTo(60000);
        assertThat(view.accuracySpread()).isZero();
        assertThat(view.arcDegrees()).isEqualTo(90.0);
        assertThat(view.rotationSpeedDegrees()).isEqualTo(4.0);
        assertThat(view.targets(TargetKind.MONSTER)).isTrue();
        assertThat(view.targets(TargetKind.PHANTOM)).isTrue();
        assertThat(view.targets(TargetKind.SLIME)).isFalse();
        assertThat(view.targetNamed()).isTrue();
        assertThat(view.targetTamed()).isFalse();
    }

    @Test
    void aMobBeyondTheMachineGunsRangeIsInTheLasersRange() {
        TurretSettings machineGun = TurretSettings.defaults();
        TurretSettings laser = LaserSettings.defaults().asTurret(machineGun);
        TargetRules.Candidate zombie = new TargetRules.Candidate(TargetKind.MONSTER, false, false, false, false, false, false,
                false, 35.0, true, "ZOMBIE", TargetRules.Relation.NONE);

        assertThat(TargetRules.verdict(zombie, machineGun)).isEqualTo(TargetRules.Verdict.OUT_OF_RANGE);
        assertThat(TargetRules.verdict(zombie, laser)).isEqualTo(TargetRules.Verdict.VALID);
    }
}
