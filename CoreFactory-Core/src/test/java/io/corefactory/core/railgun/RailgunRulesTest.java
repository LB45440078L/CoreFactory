package io.corefactory.core.railgun;

import io.corefactory.core.config.TestConfig;
import io.corefactory.core.turret.LaserRules;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RailgunRulesTest {

    @Test
    void aShotOnAnUnarmouredTargetDealsItsFullDamage() {
        RailgunSettings s = RailgunSettings.defaults();

        assertThat(RailgunRules.damageAfterArmour(s, RailgunLevel.I, 0.0, 0.0)).isEqualTo(400.0);
        assertThat(RailgunRules.damageAfterArmour(s, RailgunLevel.III, 0.0, 0.0)).isEqualTo(3500.0);
    }

    @Test
    void theArmourTheShotDoesNotIgnoreTakesItsShare() {
        RailgunSettings s = RailgunSettings.defaults();

        // Armour 20 at a bypass of 0.75 leaves 5 armour: the reduction is 1 point, which takes 4 per cent of the damage.
        assertThat(RailgunRules.damageAfterArmour(s, RailgunLevel.I, 20.0, 0.0)).isCloseTo(400.0 * 0.96, within(1e-9));
    }

    @Test
    void aBypassOfOneIgnoresAllTheArmour() {
        RailgunSettings s = RailgunSettings.from(withBypass(1.0));

        assertThat(RailgunRules.damageAfterArmour(s, RailgunLevel.II, 20.0, 9.0)).isEqualTo(1200.0);
    }

    @Test
    void theRuleIsTheLasersOnTheLevelsDamageAndTheSectionsBypass() {
        RailgunSettings s = RailgunSettings.from(withBypass(0.5));

        assertThat(RailgunRules.damageAfterArmour(s, RailgunLevel.III, 12.0, 2.0))
                .isEqualTo(LaserRules.damageAfterArmour(3500.0, 12.0, 2.0, 0.5));
    }

    @Test
    void theDamageIsNeverNegative() {
        RailgunSettings s = RailgunSettings.defaults();

        assertThat(RailgunRules.damageAfterArmour(s, RailgunLevel.I, 1000.0, 50.0)).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void theLightRisesFromZeroToTheMaximumOverTheCharge() {
        RailgunSettings s = RailgunSettings.defaults();

        assertThat(RailgunRules.lightLevel(s, RailgunLevel.I, 0.0)).isZero();
        assertThat(RailgunRules.lightLevel(s, RailgunLevel.I, 3.0)).isEqualTo(7);
        assertThat(RailgunRules.lightLevel(s, RailgunLevel.I, 5.9)).isEqualTo(14);
        assertThat(RailgunRules.lightLevel(s, RailgunLevel.I, 6.0)).isEqualTo(15);
        assertThat(RailgunRules.lightLevel(s, RailgunLevel.I, 60.0)).isEqualTo(15);
    }

    @Test
    void theLightOfLevelThreeReachesItsMaximumInFourSeconds() {
        RailgunSettings s = RailgunSettings.defaults();

        assertThat(RailgunRules.lightLevel(s, RailgunLevel.III, 2.0)).isEqualTo(7);
        assertThat(RailgunRules.lightLevel(s, RailgunLevel.III, 4.0)).isEqualTo(15);
    }

    @Test
    void noChargeIsNoLightAndNotANumberIsNoCharge() {
        RailgunSettings s = RailgunSettings.defaults();

        assertThat(RailgunRules.lightLevel(s, RailgunLevel.I, -1.0)).isZero();
        assertThat(RailgunRules.lightLevel(s, RailgunLevel.I, Double.NaN)).isZero();
    }

    private static Map<String, Object> withBypass(double bypass) {
        Map<String, Object> config = TestConfig.config();
        TestConfig.set(config, "railgun.armour-bypass", bypass);
        return config;
    }
}
