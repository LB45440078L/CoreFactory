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
    void aFullShareOfTheArmourIgnoredLeavesTheDamageAsItIs() {
        double handed = LaserRules.damageAfterArmour(12.0, 10.0, 1.0);

        assertThat(handed).isCloseTo(20.0, within(1e-9));
        double afterArmour = handed * (1.0 - 10.0 / 25.0);
        assertThat(afterArmour).isCloseTo(12.0, within(1e-9));
    }

    @Test
    void aFullShareHitsAnUnarmouredTargetForTheConfiguredDamage() {
        assertThat(LaserRules.damageAfterArmour(12.0, 0.0, 1.0)).isCloseTo(12.0, within(1e-9));
    }

    @Test
    void aZeroShareLeavesTheArmourToTakeItsUsualShare() {
        assertThat(LaserRules.damageAfterArmour(12.0, 10.0, 0.0)).isCloseTo(12.0, within(1e-9));
    }
}
