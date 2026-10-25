package io.corefactory.core.drill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DrillStatusTest {

    @Test
    void aDrillIsRunningOnlyWithPowerRoomADepositAndSomethingLeftToWorkOn() {
        assertThat(DrillRules.status(true, true, true, true)).isEqualTo(DrillStatus.RUNNING);
    }

    @Test
    void noDepositInItsSquareIsNoTarget() {
        assertThat(DrillRules.status(true, true, false, false)).isEqualTo(DrillStatus.NO_TARGET);
    }

    @Test
    void depositsInRangeThatAreAllEmptyAreExhausted() {
        assertThat(DrillRules.status(true, true, true, false)).isEqualTo(DrillStatus.EXHAUSTED);
    }

    @Test
    void powerAndRoomComeFirst() {
        assertThat(DrillRules.status(false, true, true, true)).isEqualTo(DrillStatus.NO_POWER);
        assertThat(DrillRules.status(false, false, false, false)).isEqualTo(DrillStatus.NO_POWER);
        assertThat(DrillRules.status(true, false, true, true)).isEqualTo(DrillStatus.FULL);
        assertThat(DrillRules.status(true, false, false, false)).isEqualTo(DrillStatus.FULL);
    }

    @Test
    void theStatusTextsArePlayerFacing() {
        assertThat(DrillStatus.RUNNING.text(DrillKind.ORE)).isEqualTo("Running");
        assertThat(DrillStatus.NO_POWER.text(DrillKind.PETROLEUM)).isEqualTo("No power");
        assertThat(DrillStatus.NO_POWER.text(DrillKind.ORE)).isEqualTo("No petroleum");
        assertThat(DrillStatus.FULL.text(DrillKind.ORE)).isEqualTo("Output full");
        assertThat(DrillStatus.FULL.text(DrillKind.PETROLEUM)).isEqualTo("Network full");
        assertThat(DrillStatus.NO_TARGET.text(DrillKind.ORE)).isEqualTo("No deposit in range");
        assertThat(DrillStatus.EXHAUSTED.text(DrillKind.PETROLEUM))
                .isEqualTo("Deposit exhausted");
    }
}
