package io.corefactory.core.turret;

import io.corefactory.core.config.TestConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TurretSettingsTest {

    @Test
    void aMissingSectionGivesTheDocumentedDefaults() {
        TurretSettings settings = TurretSettings.from(Map.of());

        assertThat(settings.enabled()).isTrue();
        assertThat(settings.range()).isEqualTo(24.0);
        assertThat(settings.arcDegrees()).isEqualTo(360.0);
        assertThat(settings.pitchLimitDegrees()).isEqualTo(90.0);
        assertThat(settings.ammoCapacity()).isEqualTo(128);
        assertThat(settings.ammoPerShot()).isEqualTo(1);
        assertThat(settings.targets()).containsExactly(TargetKind.MONSTER);
        assertThat(settings.targetNamed()).isFalse();
        assertThat(settings.targetTamed()).isFalse();
    }

    @Test
    void theValuesAreReadFromTheTurretsSection() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "turrets.arc-degrees", 90);
        TestConfig.set(config, "turrets.ammo-per-shot", 2);

        TurretSettings settings = TurretSettings.from(config);

        assertThat(settings.arcDegrees()).isEqualTo(90.0);
        assertThat(settings.ammoPerShot()).isEqualTo(2);
    }

    @Test
    void theLevelStatsAreNotReadFromTheRootKeys() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "turrets.range", 30);
        TestConfig.set(config, "turrets.firing-interval-ticks", 4);
        TestConfig.set(config, "turrets.ammo-capacity", 64);
        TestConfig.set(config, "turrets.power-demand-w", 12000);

        TurretSettings settings = TurretSettings.from(config);

        assertThat(settings.range()).isEqualTo(TurretLevels.MACHINE_GUN.level(1).range());
        assertThat(settings.firingIntervalTicks()).isEqualTo(TurretLevels.MACHINE_GUN.level(1).fireIntervalTicks());
        assertThat(settings.ammoCapacity()).isEqualTo(TurretLevels.MACHINE_GUN.level(1).ammoCapacity());
        assertThat(settings.powerDemandW()).isEqualTo(TurretLevels.MACHINE_GUN.level(1).powerDemandW());
    }

    @Test
    void unknownTargetNamesAreSkippedAndPhantomsAndSlimesCanBeAdded() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "turrets.targets", List.of("monsters", "Phantoms", "dragons"));

        TurretSettings settings = TurretSettings.from(config);

        assertThat(settings.targets(TargetKind.MONSTER)).isTrue();
        assertThat(settings.targets(TargetKind.PHANTOM)).isTrue();
        assertThat(settings.targets(TargetKind.SLIME)).isFalse();
        assertThat(settings.targets(TargetKind.OTHER)).isFalse();
    }

    @Test
    void anEmptyTargetListTargetsNothing() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "turrets.targets", List.of());

        assertThat(TurretSettings.from(config).targets()).isEmpty();
    }

    @Test
    void outOfRangeValuesAreClamped() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "turrets.search-interval-ticks", 0);
        TestConfig.set(config, "turrets.arc-degrees", 720);
        TestConfig.set(config, "turrets.ammo-per-shot", 9);

        TurretSettings settings = TurretSettings.from(config);

        assertThat(settings.searchIntervalTicks()).isEqualTo(1);
        assertThat(settings.arcDegrees()).isEqualTo(360.0);
        assertThat(settings.ammoPerShot()).isEqualTo(9);
    }

    @Test
    void eachRoundHasItsOwnValues() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "turrets.rounds.armor-piercing.armour-bypass", 0.6);
        TestConfig.set(config, "turrets.rounds.depleted-uranium.damage", 12);
        TestConfig.set(config, "turrets.rounds.depleted-uranium.spread-factor", 0.25);
        TestConfig.set(config, "turrets.rounds.incendiary.fire-ticks", 100);

        TurretSettings settings = TurretSettings.from(config);

        assertThat(settings.round(RoundType.REGULAR)).isEqualTo(new TurretSettings.Round(6.0, 0.0, 0, 1.0));
        assertThat(settings.round(RoundType.ARMOR_PIERCING).armourBypass()).isEqualTo(0.6);
        assertThat(settings.round(RoundType.ARMOR_PIERCING).damage()).isEqualTo(7.0);
        assertThat(settings.round(RoundType.DEPLETED_URANIUM).damage()).isEqualTo(12.0);
        assertThat(settings.round(RoundType.DEPLETED_URANIUM).spreadFactor()).isEqualTo(0.25);
        assertThat(settings.round(RoundType.INCENDIARY).fireTicks()).isEqualTo(100);
    }

    @Test
    void theDefaultRoundsGiveDepletedUraniumMoreDamageAndTighterSpreadThanRegular() {
        TurretSettings settings = TurretSettings.defaults();

        assertThat(settings.round(RoundType.DEPLETED_URANIUM).damage())
                .isGreaterThan(settings.round(RoundType.REGULAR).damage());
        assertThat(settings.round(RoundType.DEPLETED_URANIUM).spreadFactor())
                .isLessThan(settings.round(RoundType.REGULAR).spreadFactor());
        assertThat(settings.round(RoundType.ARMOR_PIERCING).armourBypass()).isGreaterThan(0.0);
        assertThat(settings.round(RoundType.DEPLETED_URANIUM).armourBypass()).isGreaterThan(0.0);
        assertThat(settings.round(RoundType.INCENDIARY).fireTicks()).isGreaterThan(0);
    }
}
