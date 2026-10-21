package io.corefactory.core.turret;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TurretSettingsTest {

    @Test
    void aMissingSectionGivesTheDocumentedDefaults() {
        TurretSettings settings = TurretSettings.from(new MemoryConfiguration());

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
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.arc-degrees", 90);
        config.set("turrets.ammo-per-shot", 2);

        TurretSettings settings = TurretSettings.from(config);

        assertThat(settings.arcDegrees()).isEqualTo(90.0);
        assertThat(settings.ammoPerShot()).isEqualTo(2);
    }

    @Test
    void theLevelStatsAreNotReadFromTheRootKeys() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.range", 30);
        config.set("turrets.firing-interval-ticks", 4);
        config.set("turrets.ammo-capacity", 64);
        config.set("turrets.power-demand-w", 12000);

        TurretSettings settings = TurretSettings.from(config);

        assertThat(settings.range()).isEqualTo(TurretLevels.MACHINE_GUN.level(1).range());
        assertThat(settings.firingIntervalTicks()).isEqualTo(TurretLevels.MACHINE_GUN.level(1).fireIntervalTicks());
        assertThat(settings.ammoCapacity()).isEqualTo(TurretLevels.MACHINE_GUN.level(1).ammoCapacity());
        assertThat(settings.powerDemandW()).isEqualTo(TurretLevels.MACHINE_GUN.level(1).powerDemandW());
    }

    @Test
    void unknownTargetNamesAreSkippedAndPhantomsAndSlimesCanBeAdded() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.targets", List.of("monsters", "Phantoms", "dragons"));

        TurretSettings settings = TurretSettings.from(config);

        assertThat(settings.targets(TargetKind.MONSTER)).isTrue();
        assertThat(settings.targets(TargetKind.PHANTOM)).isTrue();
        assertThat(settings.targets(TargetKind.SLIME)).isFalse();
        assertThat(settings.targets(TargetKind.OTHER)).isFalse();
    }

    @Test
    void anEmptyTargetListTargetsNothing() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.targets", List.of());

        assertThat(TurretSettings.from(config).targets()).isEmpty();
    }

    @Test
    void outOfRangeValuesAreClamped() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.search-interval-ticks", 0);
        config.set("turrets.arc-degrees", 720);
        config.set("turrets.ammo-per-shot", 9);

        TurretSettings settings = TurretSettings.from(config);

        assertThat(settings.searchIntervalTicks()).isEqualTo(1);
        assertThat(settings.arcDegrees()).isEqualTo(360.0);
        assertThat(settings.ammoPerShot()).isEqualTo(9);
    }

    @Test
    void eachRoundHasItsOwnValues() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.rounds.armor-piercing.armour-bypass", 0.6);
        config.set("turrets.rounds.depleted-uranium.damage", 12);
        config.set("turrets.rounds.depleted-uranium.spread-factor", 0.25);
        config.set("turrets.rounds.incendiary.fire-ticks", 100);

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
