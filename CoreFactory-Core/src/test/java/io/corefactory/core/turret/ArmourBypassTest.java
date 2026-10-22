package io.corefactory.core.turret;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ArmourBypassTest {

    @Test
    void aRoundWithNoBypassIsDealtItsDamage() {
        assertThat(ArmourBypass.adjustedDamage(8.0, 12.0, 0.0, 0.0)).isEqualTo(8.0);
    }

    @Test
    void noArmourNeedsNoAdjustment() {
        assertThat(ArmourBypass.adjustedDamage(8.0, 0.0, 0.0, 0.5)).isEqualTo(8.0);
    }

    @Test
    void aNonPositiveDamageIsDealtNothing() {
        assertThat(ArmourBypass.adjustedDamage(0.0, 10.0, 0.0, 1.0)).isZero();
        assertThat(ArmourBypass.adjustedDamage(-3.0, 10.0, 0.0, 1.0)).isZero();
    }

    @Test
    void theReductionNeverPassesTwentyPointsOfArmour() {
        assertThat(ArmourBypass.afterArmour(10.0, 40.0, 0.0)).isCloseTo(2.0, within(1e-9));
        assertThat(ArmourBypass.afterArmour(10.0, 100.0, 0.0)).isCloseTo(2.0, within(1e-9));
    }

    @Test
    void aFullBypassOfTenArmourTakesTheHandedDamageToTheWantedEighteenAfterTheServersArmour() {
        double handed = ArmourBypass.adjustedDamage(18.0, 10.0, 0.0, 1.0);

        assertThat(handed).isCloseTo(18.0 / 0.92, within(1e-6));
        assertThat(ArmourBypass.afterArmour(handed, 10.0, 0.0)).isCloseTo(18.0, within(1e-9));
    }

    @Test
    void aHandOverOfThirtyLeavesTwentySevenPointSixAfterTenArmour() {
        assertThat(ArmourBypass.afterArmour(30.0, 10.0, 0.0)).isCloseTo(27.6, within(1e-9));
    }

    @Test
    void aTwentyArmourTargetIsHandedTheSolvedDamageNotTheLinearNinety() {
        double handed = ArmourBypass.adjustedDamage(18.0, 20.0, 0.0, 1.0);

        assertThat(handed).isCloseTo(25.4138, within(1e-3));
        assertThat(ArmourBypass.afterArmour(handed, 20.0, 0.0)).isCloseTo(18.0, within(1e-9));
    }

    @Test
    void theToughnessAddsToTheDivisorAsVanillaDoes() {
        double withToughness = ArmourBypass.adjustedDamage(18.0, 10.0, 8.0, 1.0);

        assertThat(withToughness).isCloseTo(21.9615, within(1e-3));
        assertThat(withToughness).isNotCloseTo(ArmourBypass.adjustedDamage(18.0, 10.0, 0.0, 1.0), within(1e-3));
        assertThat(ArmourBypass.afterArmour(withToughness, 10.0, 8.0)).isCloseTo(18.0, within(1e-9));
    }

    @Test
    void theServerAppliesTheWantedDamageForEveryArmourToughnessAndBypass() {
        double[] armours = {2.0, 10.0, 20.0, 30.0};
        double[] toughnesses = {0.0, 4.0, 8.0};
        double[] bypasses = {0.5, 1.0};
        double[] damages = {6.0, 12.0, 18.0};
        for (double armour : armours) {
            for (double toughness : toughnesses) {
                for (double bypass : bypasses) {
                    for (double damage : damages) {
                        double handed = ArmourBypass.adjustedDamage(damage, armour, toughness, bypass);
                        double served = ArmourBypass.afterArmour(handed, armour, toughness);
                        double wanted = ArmourBypass.afterArmour(damage, armour * (1.0 - bypass), toughness);

                        assertThat(served)
                                .as("damage %s, armour %s, toughness %s, bypass %s", damage, armour, toughness, bypass)
                                .isCloseTo(wanted, within(1e-9));
                    }
                }
            }
        }
    }

    @Test
    void aFullBypassDeliversTheConfiguredDamageAfterAnyArmour() {
        double[] armours = {1.0, 5.0, 10.0, 20.0, 40.0};
        double[] toughnesses = {0.0, 8.0};
        for (double armour : armours) {
            for (double toughness : toughnesses) {
                double handed = ArmourBypass.adjustedDamage(18.0, armour, toughness, 1.0);

                assertThat(ArmourBypass.afterArmour(handed, armour, toughness))
                        .as("armour %s, toughness %s", armour, toughness)
                        .isCloseTo(18.0, within(1e-9));
            }
        }
    }

    @Test
    void aBypassedShareOfTheArmourRaisesTheDamageHandedOver() {
        assertThat(ArmourBypass.adjustedDamage(8.0, 20.0, 0.0, 0.5)).isGreaterThan(8.0);
    }
}
