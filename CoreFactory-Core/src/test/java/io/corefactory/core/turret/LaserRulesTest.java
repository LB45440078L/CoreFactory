package io.corefactory.core.turret;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class LaserRulesTest {

    @Test
    void theGlowAndTheLightLastFourTicksAndTheLightIsLevelTwelve() {
        assertThat(LaserRules.GLOW_TICKS).isEqualTo(4);
        assertThat(LaserRules.LIGHT_LEVEL).isEqualTo(12);
        assertThat(LaserRules.FULL_LIGHT).isEqualTo(15);
    }

    @Test
    void aFullShareOfTheArmourIgnoredLeavesTheConfiguredDamageAfterAnyArmour() {
        assertThat(LaserRules.damageAfterArmour(12.0, 10.0, 0.0, 1.0)).isEqualTo(12.0);
        assertThat(LaserRules.damageAfterArmour(12.0, 40.0, 8.0, 1.0)).isEqualTo(12.0);
    }

    @Test
    void aFullShareHitsAnUnarmouredTargetForTheConfiguredDamage() {
        assertThat(LaserRules.damageAfterArmour(12.0, 0.0, 0.0, 1.0)).isCloseTo(12.0, within(1e-9));
    }

    @Test
    void aZeroShareLeavesTheArmourToTakeItsUsualShare() {
        // Reduction: max(10 / 5, 10 - 12 / 2) = 4 points of 25, so 12 keeps 84 per cent.
        assertThat(LaserRules.damageAfterArmour(12.0, 10.0, 0.0, 0.0)).isCloseTo(10.08, within(1e-9));
    }

    @Test
    void aLevelFiveLaserDeals18AfterTheArmourOfATwentyArmourTarget() {
        double damage = TurretLevels.LASER.level(5).effectiveDamage();

        assertThat(damage).isCloseTo(18.0, within(1e-9));
        assertThat(LaserRules.damageAfterArmour(damage, 20.0, 0.0, 1.0)).isCloseTo(18.0, within(1e-9));
    }
}
