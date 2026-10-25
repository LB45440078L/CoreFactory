package io.corefactory.core.turret;

import io.corefactory.core.config.TestConfig;
import io.corefactory.core.railgun.RailgunLevel;
import io.corefactory.core.railgun.RailgunSettings;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** The railgun's place in the turret kinds: its levels, its upgrade rule and its model keys. */
class RailgunTurretTest {

    @Test
    void theKindsHaveTheirOwnTopLevels() {
        assertThat(TurretType.MACHINE_GUN.maxLevel()).isEqualTo(5);
        assertThat(TurretType.LASER.maxLevel()).isEqualTo(5);
        assertThat(TurretType.RAILGUN.maxLevel()).isEqualTo(3);
        assertThat(TurretType.RAILGUN.section()).isEqualTo("railgun");
    }

    @Test
    void onlyTheMachineGunHasAMagazine() {
        assertThat(TurretType.MACHINE_GUN.hasMagazine()).isTrue();
        assertThat(TurretType.LASER.hasMagazine()).isFalse();
        assertThat(TurretType.RAILGUN.hasMagazine()).isFalse();
    }

    @Test
    void theRailgunTableHasThreeLevelsAndNoMagazineOrSecondCannon() {
        TurretLevels table = TurretLevels.RAILGUN;

        assertThat(table.levels()).hasSize(3);
        assertThat(table.maxLevel()).isEqualTo(3);
        assertThat(table.levels()).allSatisfy(level -> {
            assertThat(level.cannons()).isEqualTo(1);
            assertThat(level.ammoCapacity()).isZero();
            assertThat(level.hasMagazine()).isFalse();
        });
    }

    @Test
    void theRailgunLevelsHoldTheSettingsStats() {
        RailgunSettings settings = RailgunSettings.defaults();
        TurretLevels table = TurretLevels.RAILGUN;

        for (RailgunLevel level : RailgunLevel.values()) {
            RailgunSettings.LevelStats stats = settings.level(level);
            TurretLevel row = table.level(level.number());
            assertThat(row.damage()).isEqualTo(stats.damage());
            assertThat(row.range()).isEqualTo(stats.range());
            assertThat(row.powerDemandW()).isEqualTo(stats.powerDemandW());
            assertThat(row.fireIntervalTicks()).isEqualTo(stats.fireIntervalTicks());
        }
        assertThat(table.level(1).range()).isEqualTo(60.0);
        assertThat(table.level(3).range()).isEqualTo(110.0);
        assertThat(table.level(3).damage()).isEqualTo(3500.0);
        assertThat(table.level(1).fireIntervalTicks()).isEqualTo(320);
    }

    @Test
    void theRailgunTallLevelIsItsThirdAndRunsWithTheSharedRotation() {
        assertThat(TurretLevels.RAILGUN.level(1).tall()).isFalse();
        assertThat(TurretLevels.RAILGUN.level(2).tall()).isFalse();
        assertThat(TurretLevels.RAILGUN.level(3).tall()).isTrue();
        assertThat(TurretLevels.RAILGUN.levels()).allSatisfy(level -> assertThat(level.rotationDegreesPerTick()).isZero());
    }

    @Test
    void aRailgunLevelFourIsRefusedAndLevelZeroToo() {
        assertThatThrownBy(() -> TurretLevels.RAILGUN.level(4)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TurretLevels.RAILGUN.level(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aTableOfTheWrongLengthIsRefusedPerKind() {
        assertThatThrownBy(() -> new TurretLevels(TurretType.RAILGUN, TurretLevels.MACHINE_GUN.levels()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theRailgunReadsItsLevelsFromItsOwnSection() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "railgun.levels.2.damage", 1500);
        TestConfig.set(config, "laser-turret.levels.2.damage", 99);

        TurretLevels table = TurretLevels.from(TurretType.RAILGUN, config);

        assertThat(table.level(2).damage()).isEqualTo(1500.0);
        assertThat(table.level(1)).isEqualTo(TurretLevels.RAILGUN.level(1));
        assertThat(TurretLevels.from(TurretType.RAILGUN, Map.of())).isEqualTo(TurretLevels.RAILGUN);
    }

    @Test
    void theMachineGunAndTheLaserKeepTheirCeilings() {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "turrets.levels.2.range", 500);
        TestConfig.set(config, "laser-turret.levels.2.range", 500);
        TestConfig.set(config, "laser-turret.levels.2.damage", 5000);
        TestConfig.set(config, "laser-turret.levels.2.power-demand-w", 5_000_000);

        assertThat(TurretLevels.from(TurretType.MACHINE_GUN, config).level(2).range()).isEqualTo(64.0);
        TurretLevel laser = TurretLevels.from(TurretType.LASER, config).level(2);
        assertThat(laser.range()).isEqualTo(64.0);
        assertThat(laser.damage()).isEqualTo(1000.0);
        assertThat(laser.powerDemandW()).isEqualTo(1_000_000);
    }

    @Test
    void theMachineGunAndTheLaserStillHaveFiveLevels() {
        assertThat(TurretLevels.MACHINE_GUN.levels()).hasSize(5);
        assertThat(TurretLevels.LASER.levels()).hasSize(5);
        assertThatThrownBy(() -> TurretLevels.LASER.level(6)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theUpgradeRuleStopsARailgunAtLevelThree() {
        assertThat(TurretUpgrade.canUpgrade(TurretType.RAILGUN, 1, 2)).isEqualTo(TurretUpgrade.Reason.OK);
        assertThat(TurretUpgrade.canUpgrade(TurretType.RAILGUN, 2, 3)).isEqualTo(TurretUpgrade.Reason.OK);
        assertThat(TurretUpgrade.canUpgrade(TurretType.RAILGUN, 3, 4)).isEqualTo(TurretUpgrade.Reason.MAX_LEVEL);
        assertThat(TurretUpgrade.canUpgrade(TurretType.RAILGUN, 2, 4)).isEqualTo(TurretUpgrade.Reason.WRONG_LEVEL);
        assertThat(TurretUpgrade.canUpgrade(TurretType.RAILGUN, 1, 3)).isEqualTo(TurretUpgrade.Reason.WRONG_LEVEL);
    }

    @Test
    void aRailgunMovingToItsTallLevelNeedsTheBlockAbove() {
        assertThat(TurretUpgrade.canUpgrade(TurretType.RAILGUN, 2, 3, false)).isEqualTo(TurretUpgrade.Reason.BLOCKED_ABOVE);
        assertThat(TurretUpgrade.canUpgrade(TurretType.RAILGUN, 2, 3, true)).isEqualTo(TurretUpgrade.Reason.OK);
        assertThat(TurretUpgrade.canUpgrade(TurretType.RAILGUN, 3, 4, true)).isEqualTo(TurretUpgrade.Reason.MAX_LEVEL);
    }

    @Test
    void theUntypedUpgradeOverloadsStillGiveAMachineGunOrLaserFiveLevels() {
        assertThat(TurretUpgrade.canUpgrade(TurretType.MACHINE_GUN, 4, 5)).isEqualTo(TurretUpgrade.Reason.OK);
        assertThat(TurretUpgrade.canUpgrade(TurretType.LASER, 5, 6)).isEqualTo(TurretUpgrade.Reason.MAX_LEVEL);
        assertThat(TurretUpgrade.canUpgrade(4, 5)).isEqualTo(TurretUpgrade.canUpgrade(TurretType.LASER, 4, 5));
        assertThat(TurretUpgrade.canUpgrade(5, 6)).isEqualTo(TurretUpgrade.Reason.MAX_LEVEL);
    }

    @Test
    void theModelKeysOfTheRailgunFollowTheirLevels() {
        assertThat(ModelKeys.of(TurretType.RAILGUN, 1, false)).isEqualTo("machine/railgun_1_body");
        assertThat(ModelKeys.of(TurretType.RAILGUN, 2, false)).isEqualTo("machine/railgun_2_body");
        assertThat(ModelKeys.of(TurretType.RAILGUN, 3, false)).isEqualTo("machine/railgun_3_body");
        assertThat(ModelKeys.of(TurretType.RAILGUN, 3, true)).isEqualTo("machine/railgun_3_top");
        assertThat(ModelKeys.barrel(TurretType.RAILGUN, 2)).isEqualTo("machine/railgun_2_barrel");
    }

    @Test
    void theRailgunHasNoModelAboveItsTopLevelOrUpperHalfBelowLevelThree() {
        assertThatThrownBy(() -> ModelKeys.of(TurretType.RAILGUN, 4, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModelKeys.of(TurretType.RAILGUN, 2, true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theRailgunsTargetingIsTheMachineGunsAndItsRangeIsItsLevels() {
        TurretSettings machineGun = TurretSettings.defaults();

        TurretSettings view = RailgunSettings.defaults().asTurret(machineGun, RailgunLevel.II);

        assertThat(view.range()).isCloseTo(80.0, within(1e-9));
        assertThat(view.targets()).isEqualTo(machineGun.targets());
    }
}
