package io.corefactory.core.turret;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ArmourBypassTest {

    @Test
    void aRoundWithNoBypassIsDealtItsDamage() {
        assertThat(ArmourBypass.adjustedDamage(8.0, 12.0, 0.0)).isEqualTo(8.0);
    }

    @Test
    void noArmourNeedsNoAdjustment() {
        assertThat(ArmourBypass.adjustedDamage(8.0, 0.0, 0.5)).isCloseTo(8.0, within(1e-9));
    }

    @Test
    void theAdjustedDamageGivesTheResultOfLowerArmourAfterTheServersArmour() {
        double armour = 10.0;
        double bypass = 0.5;
        double handed = ArmourBypass.adjustedDamage(8.0, armour, bypass);

        double afterServerArmour = handed * (1.0 - armour / 25.0);
        double wantedAfterLowerArmour = 8.0 * (1.0 - armour * (1.0 - bypass) / 25.0);

        assertThat(afterServerArmour).isCloseTo(wantedAfterLowerArmour, within(1e-9));
    }

    @Test
    void aBypassedShareOfTheArmourRaisesTheDamageHandedOver() {
        assertThat(ArmourBypass.adjustedDamage(8.0, 20.0, 0.5)).isGreaterThan(8.0);
    }

    @Test
    void theArmourCapAppliesAsItDoesInVanilla() {
        assertThat(ArmourBypass.adjustedDamage(8.0, 40.0, 0.5))
                .isCloseTo(ArmourBypass.adjustedDamage(8.0, 20.0, 0.5), within(1e-9));
    }

    @Test
    void theFullBypassLeavesTheRoundTheArmourlessDamage() {
        double handed = ArmourBypass.adjustedDamage(8.0, 20.0, 1.0);

        assertThat(handed * (1.0 - 20.0 / 25.0)).isCloseTo(8.0, within(1e-9));
    }
}
