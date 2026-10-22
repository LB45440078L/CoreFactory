package io.corefactory.core.turret;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ArmourBypassTest {

    @Test
    void aRoundWithNoBypassMeetsTheArmourByTheGamesFormula() {
        // Reduction: max(10 / 5, 10 - 8 / 2) = 6 points of 25, so 8 keeps 76 per cent.
        assertThat(ArmourBypass.finalDamage(8.0, 10.0, 0.0, 0.0)).isCloseTo(6.08, within(1e-9));
    }

    @Test
    void noArmourLeavesTheDamageWhole() {
        assertThat(ArmourBypass.finalDamage(8.0, 0.0, 0.0, 0.5)).isEqualTo(8.0);
        assertThat(ArmourBypass.finalDamage(8.0, 0.0, 8.0, 0.0)).isEqualTo(8.0);
    }

    @Test
    void aNonPositiveDamageIsDealtNothing() {
        assertThat(ArmourBypass.finalDamage(0.0, 10.0, 0.0, 1.0)).isZero();
        assertThat(ArmourBypass.finalDamage(-3.0, 10.0, 0.0, 1.0)).isZero();
    }

    @Test
    void theReductionNeverPassesTwentyPointsOfArmour() {
        assertThat(ArmourBypass.finalDamage(10.0, 40.0, 0.0, 0.0)).isCloseTo(2.0, within(1e-9));
        assertThat(ArmourBypass.finalDamage(10.0, 100.0, 0.0, 0.0)).isCloseTo(2.0, within(1e-9));
    }

    @Test
    void aLaserWithAFullBypassDealsItsConfiguredDamageAfterAnyArmour() {
        double[] armours = {0.0, 1.0, 5.0, 10.0, 20.0, 40.0};
        double[] toughnesses = {0.0, 8.0};
        for (double armour : armours) {
            for (double toughness : toughnesses) {
                assertThat(ArmourBypass.finalDamage(18.0, armour, toughness, 1.0))
                        .as("armour %s, toughness %s", armour, toughness)
                        .isEqualTo(18.0);
            }
        }
    }

    @Test
    void aWardenWithTwentyArmourTakesTenPointOhEightFromAHitOfEighteenWithNoBypass() {
        assertThat(ArmourBypass.finalDamage(18.0, 20.0, 0.0, 0.0)).isCloseTo(10.08, within(1e-9));
    }

    @Test
    void theToughnessCountsWhenTheRoundDoesNotBypass() {
        // With toughness 8 the divisor is 4, so the reduction is max(2, 10 - 4.5) = 5.5 points: 18 keeps 78 per cent.
        double withToughness = ArmourBypass.finalDamage(18.0, 10.0, 8.0, 0.0);
        double withoutToughness = ArmourBypass.finalDamage(18.0, 10.0, 0.0, 0.0);

        assertThat(withToughness).isCloseTo(14.04, within(1e-9));
        assertThat(withoutToughness).isCloseTo(16.56, within(1e-9));
        assertThat(withToughness).isLessThan(withoutToughness);
    }

    @Test
    void aFullBypassRemovesTheArmoursEffectWhateverItsToughness() {
        assertThat(ArmourBypass.finalDamage(18.0, 20.0, 8.0, 1.0)).isEqualTo(18.0);
    }

    @Test
    void aHalfBypassMeetsOnlyTheHalfOfTheArmourThatIsLeft() {
        // Ten armour is left: max(2, 10 - 9) = 2 points, so 18 keeps 92 per cent.
        assertThat(ArmourBypass.finalDamage(18.0, 20.0, 0.0, 0.5)).isCloseTo(16.56, within(1e-9));
    }
}
