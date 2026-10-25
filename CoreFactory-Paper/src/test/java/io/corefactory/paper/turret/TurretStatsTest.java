package io.corefactory.paper.turret;

import io.corefactory.paper.config.YamlMaps;
import io.corefactory.core.turret.DualTargeting;
import io.corefactory.core.turret.TargetKind;
import io.corefactory.core.turret.TurretSettings;
import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks that a turret's stats come from its level, and that the general settings still come from the turrets section. */
class TurretStatsTest {

    @Test
    void aMissingConfigGivesTheLevelOneStats() {
        TurretStats stats = TurretStats.from(YamlMaps.toMap(new MemoryConfiguration()));

        TurretSettings view = stats.view(TurretKind.MACHINE_GUN, 1);
        assertThat(view.range()).isEqualTo(24.0);
        assertThat(view.firingIntervalTicks()).isEqualTo(10);
        assertThat(view.ammoCapacity()).isEqualTo(128);
        assertThat(view.powerDemandW()).isEqualTo(24_000);
        assertThat(stats.damage(TurretKind.MACHINE_GUN, 1)).isEqualTo(6.0);
        assertThat(stats.cannons(TurretKind.MACHINE_GUN, 1)).isEqualTo(1);
    }

    @Test
    void eachLevelHasItsOwnRangeIntervalAmmunitionPowerDamageAndCannons() {
        TurretStats stats = TurretStats.from(YamlMaps.toMap(new MemoryConfiguration()));

        TurretSettings top = stats.view(TurretKind.MACHINE_GUN, 5);
        assertThat(top.range()).isEqualTo(32.0);
        assertThat(top.firingIntervalTicks()).isEqualTo(4);
        assertThat(top.ammoCapacity()).isEqualTo(768);
        assertThat(top.powerDemandW()).isEqualTo(60_000);
        assertThat(stats.damage(TurretKind.MACHINE_GUN, 5)).isEqualTo(9.0);
        assertThat(stats.cannons(TurretKind.MACHINE_GUN, 5)).isEqualTo(2);
        assertThat(stats.cannons(TurretKind.MACHINE_GUN, 3)).isEqualTo(1);
    }

    @Test
    void aLaserLevelFiveIsTheBlueOneWithItsOwnStats() {
        TurretStats stats = TurretStats.from(YamlMaps.toMap(new MemoryConfiguration()));

        TurretSettings view = stats.view(TurretKind.LASER, 5);
        assertThat(view.range()).isEqualTo(60.0);
        assertThat(view.firingIntervalTicks()).isEqualTo(4);
        assertThat(view.powerDemandW()).isEqualTo(120_000);
        assertThat(view.accuracySpread()).isZero();
        assertThat(view.ammoCapacity()).isEqualTo(1);
        assertThat(stats.damage(TurretKind.LASER, 5)).isEqualTo(18.0);
        assertThat(stats.cannons(TurretKind.LASER, 5)).isEqualTo(1);
    }

    @Test
    void theGeneralSettingsAreSharedByBothKinds() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.targets", List.of("monsters", "phantoms"));
        config.set("turrets.arc-degrees", 90);
        config.set("turrets.rotation-speed-degrees", 4);
        TurretStats stats = TurretStats.from(YamlMaps.toMap(config));

        for (TurretKind kind : TurretKind.values()) {
            TurretSettings view = stats.view(kind, 3);
            assertThat(view.arcDegrees()).isEqualTo(90.0);
            assertThat(view.targets(TargetKind.PHANTOM)).isTrue();
        }
        assertThat(stats.view(TurretKind.MACHINE_GUN, 3).rotationSpeedDegrees()).isEqualTo(4.0);
    }

    @Test
    void aLaserTurnsAtItsLevelsRotationAndScansAtItsOwnIntervalWhileTheMachineGunKeepsItsShared() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.search-interval-ticks", 40);
        config.set("laser-turret.search-interval-ticks", 3);
        TurretStats stats = TurretStats.from(YamlMaps.toMap(config));

        assertThat(stats.view(TurretKind.LASER, 1).rotationSpeedDegrees()).isEqualTo(6.0);
        assertThat(stats.view(TurretKind.LASER, 3).rotationSpeedDegrees()).isEqualTo(8.0);
        assertThat(stats.view(TurretKind.LASER, 5).rotationSpeedDegrees()).isEqualTo(12.0);
        assertThat(stats.view(TurretKind.LASER, 5).searchIntervalTicks()).isEqualTo(3);
        assertThat(stats.view(TurretKind.MACHINE_GUN, 5).rotationSpeedDegrees()).isEqualTo(6.0);
        assertThat(stats.view(TurretKind.MACHINE_GUN, 5).searchIntervalTicks()).isEqualTo(40);
    }

    @Test
    void aLevelKeyOverridesOnlyTheStatItNames() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.levels.5.range", 40);
        TurretStats stats = TurretStats.from(YamlMaps.toMap(config));

        assertThat(stats.view(TurretKind.MACHINE_GUN, 5).range()).isEqualTo(40.0);
        assertThat(stats.view(TurretKind.MACHINE_GUN, 4).range()).isEqualTo(30.0);
    }

    @Test
    void aLevelOutsideTheRangeUsesTheNearestLevel() {
        TurretStats stats = TurretStats.from(YamlMaps.toMap(new MemoryConfiguration()));

        assertThat(stats.view(TurretKind.MACHINE_GUN, 0).range()).isEqualTo(24.0);
        assertThat(stats.view(TurretKind.MACHINE_GUN, 9).range()).isEqualTo(32.0);
    }

    @Test
    void theDualSpacingIsReadFromTheTurretsSection() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.dual-spacing-blocks", 4);

        assertThat(TurretStats.from(YamlMaps.toMap(config)).spacing()).isEqualTo(4.0);
        assertThat(TurretStats.from(YamlMaps.toMap(new MemoryConfiguration())).spacing()).isEqualTo(DualTargeting.DEFAULT_SPACING_BLOCKS);
    }

    @Test
    void theMagazineOfAMachineGunIsItsLevelsBeltAndALaserHasOne() {
        TurretStats stats = TurretStats.from(YamlMaps.toMap(new MemoryConfiguration()));

        assertThat(stats.magazineCapacity(TurretKind.MACHINE_GUN, 2)).isEqualTo(256);
        assertThat(stats.magazineCapacity(TurretKind.LASER, 2)).isEqualTo(1);
    }
}
