package io.corefactory.core.drill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DrillStatusHistoryTest {

    @Test
    void aDrillWithTargetsAndNothingLeftStopsDoneInRange() {
        assertThat(DrillRules.status(true, true, false, true)).isEqualTo(DrillStatus.DONE_IN_RANGE);
        assertThat(DrillStatus.DONE_IN_RANGE.text(DrillKind.ORE, DrillTable.level(1)))
                .isEqualTo("Done in range: move the drill");
        assertThat(DrillStatus.DONE_IN_RANGE.text(DrillKind.PETROLEUM, DrillTable.level(5)))
                .isEqualTo("Done in range: move the drill");
    }

    @Test
    void aDrillThatNeverHadTargetsKeepsNoTarget() {
        assertThat(DrillRules.status(true, true, false, false)).isEqualTo(DrillStatus.NO_TARGET);
        assertThat(DrillRules.status(true, true, false)).isEqualTo(DrillStatus.NO_TARGET);
    }

    @Test
    void powerAndRoomStillComeFirst() {
        assertThat(DrillRules.status(false, true, false, true)).isEqualTo(DrillStatus.NO_POWER);
        assertThat(DrillRules.status(true, false, false, true)).isEqualTo(DrillStatus.FULL);
        assertThat(DrillRules.status(true, true, true, true)).isEqualTo(DrillStatus.RUNNING);
    }
}
