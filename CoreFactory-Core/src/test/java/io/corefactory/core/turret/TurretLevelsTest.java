package io.corefactory.core.turret;

import org.bukkit.configuration.MemoryConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TurretLevelsTest {

    @Test
    void theMachineGunTableHasTheDocumentedStatsAtEveryLevel() {
        TurretLevels table = TurretLevels.MACHINE_GUN;

        assertThat(table.levels()).hasSize(TurretLevels.MAX_LEVEL);
        assertThat(table.level(1)).isEqualTo(new TurretLevel(1, 6.0, 0, 10, 24.0, 128, 1, 24_000, false));
        assertThat(table.level(2)).isEqualTo(new TurretLevel(2, 6.0, 10, 8, 26.0, 256, 1, 33_000, false));
        assertThat(table.level(3)).isEqualTo(new TurretLevel(3, 6.0, 20, 7, 28.0, 384, 1, 42_000, true));
        assertThat(table.level(4)).isEqualTo(new TurretLevel(4, 6.0, 35, 5, 30.0, 512, 2, 51_000, true));
        assertThat(table.level(5)).isEqualTo(new TurretLevel(5, 6.0, 50, 4, 32.0, 768, 2, 60_000, true));
    }

    @Test
    void theLaserTableHasTheDocumentedStatsAndNoMagazine() {
        TurretLevels table = TurretLevels.LASER;

        assertThat(table.level(1).range()).isEqualTo(40.0);
        assertThat(table.level(5).range()).isEqualTo(60.0);
        assertThat(table.level(1).powerDemandW()).isEqualTo(60_000);
        assertThat(table.level(5).powerDemandW()).isEqualTo(120_000);
        assertThat(table.levels()).allSatisfy(level -> {
            assertThat(level.cannons()).isEqualTo(1);
            assertThat(level.ammoCapacity()).isZero();
            assertThat(level.hasMagazine()).isFalse();
        });
        assertThat(table.level(1).tall()).isFalse();
        assertThat(table.level(3).tall()).isTrue();
    }

    @Test
    void theMachineGunHasAMagazineAtEveryLevel() {
        assertThat(TurretLevels.MACHINE_GUN.levels()).allSatisfy(level -> assertThat(level.hasMagazine()).isTrue());
        assertThat(TurretLevels.MACHINE_GUN.level(5).ammoCapacity()).isEqualTo(768);
    }

    @Test
    void theRangesRiseAndTheIntervalsFall() {
        for (TurretLevels table : new TurretLevels[] {TurretLevels.MACHINE_GUN, TurretLevels.LASER}) {
            for (int n = 2; n <= TurretLevels.MAX_LEVEL; n++) {
                assertThat(table.level(n).range()).isGreaterThan(table.level(n - 1).range());
                assertThat(table.level(n).fireIntervalTicks()).isLessThan(table.level(n - 1).fireIntervalTicks());
                assertThat(table.level(n).powerDemandW()).isGreaterThan(table.level(n - 1).powerDemandW());
            }
        }
    }

    @Test
    void theMachineGunHasTwoCannonsFromLevelFourAndTheTallLevelsAreThreeAndUp() {
        TurretLevels table = TurretLevels.MACHINE_GUN;

        assertThat(table.level(3).cannons()).isEqualTo(1);
        assertThat(table.level(4).cannons()).isEqualTo(2);
        assertThat(table.level(5).cannons()).isEqualTo(2);
        for (int n = 1; n <= TurretLevels.MAX_LEVEL; n++) {
            assertThat(table.level(n).tall()).isEqualTo(n >= TurretUpgrade.TALL_FROM);
        }
    }

    @Test
    void theBoostRaisesTheDamageOfEachLevel() {
        assertThat(TurretLevels.MACHINE_GUN.level(1).effectiveDamage()).isEqualTo(6.0);
        assertThat(TurretLevels.MACHINE_GUN.level(5).effectiveDamage()).isEqualTo(9.0);
        assertThat(TurretLevels.LASER.level(5).effectiveDamage()).isEqualTo(18.0);
    }

    @Test
    void theLevelOneRowsMatchTheTurretSettingsDefaults() {
        TurretSettings settings = TurretSettings.defaults();
        TurretLevel one = TurretLevels.MACHINE_GUN.level(1);

        assertThat(one.range()).isEqualTo(settings.range());
        assertThat(one.fireIntervalTicks()).isEqualTo(settings.firingIntervalTicks());
        assertThat(one.ammoCapacity()).isEqualTo(settings.ammoCapacity());
        assertThat(one.powerDemandW()).isEqualTo(settings.powerDemandW());
        assertThat(one.damage()).isEqualTo(settings.round(RoundType.REGULAR).damage());
        assertThat(TurretLevels.LASER.level(1).range()).isEqualTo(LaserSettings.defaults().range());
        assertThat(TurretLevels.LASER.level(1).damage()).isEqualTo(LaserSettings.defaults().damage());
    }

    @Test
    void aMissingSectionGivesTheDefaults() {
        assertThat(TurretLevels.from(TurretType.MACHINE_GUN, new MemoryConfiguration())).isEqualTo(TurretLevels.MACHINE_GUN);
        assertThat(TurretLevels.from(TurretType.LASER, new MemoryConfiguration())).isEqualTo(TurretLevels.LASER);
        assertThat(TurretLevels.from(TurretType.LASER, null)).isEqualTo(TurretLevels.LASER);
    }

    @Test
    void aLevelOverrideReplacesOnlyTheKeysItNames() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.levels.4.damage-boost-percent", 40);
        config.set("turrets.levels.3.range", 29);

        TurretLevels table = TurretLevels.from(TurretType.MACHINE_GUN, config);

        assertThat(table.level(4).damageBoostPercent()).isEqualTo(40);
        assertThat(table.level(4).range()).isEqualTo(30.0);
        assertThat(table.level(4).ammoCapacity()).isEqualTo(512);
        assertThat(table.level(3).range()).isEqualTo(29.0);
        assertThat(table.level(3).damageBoostPercent()).isEqualTo(20);
        assertThat(table.level(1)).isEqualTo(TurretLevels.MACHINE_GUN.level(1));
    }

    @Test
    void theLaserLevelsTurnFasterFromLevelThreeAndTheMachineGunKeepsTheSharedSpeed() {
        assertThat(TurretLevels.LASER.level(1).rotationDegreesPerTick()).isEqualTo(6.0);
        assertThat(TurretLevels.LASER.level(2).rotationDegreesPerTick()).isEqualTo(6.0);
        assertThat(TurretLevels.LASER.level(3).rotationDegreesPerTick()).isEqualTo(8.0);
        assertThat(TurretLevels.LASER.level(5).rotationDegreesPerTick()).isEqualTo(12.0);
        assertThat(TurretLevels.LASER.level(5).rotationDegreesOr(6.0)).isEqualTo(12.0);
        assertThat(TurretLevels.MACHINE_GUN.levels())
                .allSatisfy(level -> assertThat(level.rotationDegreesOr(6.0)).isEqualTo(6.0));
    }

    @Test
    void aLaserLevelsRotationIsReadFromItsSectionAndAMachineGunLevelDoesNotReadIt() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("laser-turret.levels.5.rotation-degrees-per-tick", 15);
        config.set("turrets.levels.5.rotation-degrees-per-tick", 15);

        assertThat(TurretLevels.from(TurretType.LASER, config).level(5).rotationDegreesPerTick()).isEqualTo(15.0);
        assertThat(TurretLevels.from(TurretType.MACHINE_GUN, config).level(5).rotationDegreesPerTick()).isZero();
    }

    @Test
    void everyStatOfALevelCanBeOverridden() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.levels.2.damage", 7.5);
        config.set("turrets.levels.2.damage-boost-percent", 15);
        config.set("turrets.levels.2.fire-interval-ticks", 9);
        config.set("turrets.levels.2.range", 27);
        config.set("turrets.levels.2.ammo-capacity", 300);
        config.set("turrets.levels.2.cannons", 2);
        config.set("turrets.levels.2.power-demand-w", 35000);

        assertThat(TurretLevels.from(TurretType.MACHINE_GUN, config).level(2))
                .isEqualTo(new TurretLevel(2, 7.5, 15, 9, 27.0, 300, 2, 35_000, false));
    }

    @Test
    void theLaserReadsItsOwnSectionAndIgnoresAnAmmoCapacity() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("laser-turret.levels.5.range", 66);
        config.set("laser-turret.levels.5.damage", 15);
        config.set("laser-turret.levels.1.ammo-capacity", 64);
        config.set("turrets.levels.5.range", 50);

        TurretLevels laser = TurretLevels.from(TurretType.LASER, config);

        assertThat(laser.level(5).range()).isEqualTo(64.0);
        assertThat(laser.level(5).damage()).isEqualTo(15.0);
        assertThat(laser.level(1).ammoCapacity()).isZero();
    }

    @Test
    void aMachineGunNeverHasAnEmptyBelt() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.levels.1.ammo-capacity", 0);

        assertThat(TurretLevels.from(TurretType.MACHINE_GUN, config).level(1).ammoCapacity()).isEqualTo(1);
    }

    @Test
    void theTallFlagFollowsTheUpgradeRuleNotTheConfig() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.levels.2.tall", true);
        config.set("turrets.levels.3.tall", false);

        TurretLevels table = TurretLevels.from(TurretType.MACHINE_GUN, config);

        assertThat(table.level(2).tall()).isFalse();
        assertThat(table.level(3).tall()).isTrue();
    }

    @Test
    void oddValuesAreClampedToTheirRanges() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("turrets.levels.1.cannons", 9);
        config.set("turrets.levels.2.range", 500);
        config.set("turrets.levels.3.fire-interval-ticks", 0);
        config.set("turrets.levels.4.damage-boost-percent", -20);
        config.set("turrets.levels.5.power-demand-w", -5);

        TurretLevels table = TurretLevels.from(TurretType.MACHINE_GUN, config);

        assertThat(table.level(1).cannons()).isEqualTo(DualTargeting.MAX_CANNONS);
        assertThat(table.level(2).range()).isEqualTo(64.0);
        assertThat(table.level(3).fireIntervalTicks()).isEqualTo(1);
        assertThat(table.level(4).damageBoostPercent()).isZero();
        assertThat(table.level(5).powerDemandW()).isZero();
    }

    @Test
    void aLevelOutsideTheTableIsRefused() {
        assertThatThrownBy(() -> TurretLevels.MACHINE_GUN.level(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TurretLevels.MACHINE_GUN.level(TurretLevels.MAX_LEVEL + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
