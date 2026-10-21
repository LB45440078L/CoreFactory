package io.corefactory.core.logistics;

import org.junit.jupiter.api.Test;

import static io.corefactory.core.logistics.Fixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PickSideTest {

    /** A picker at (1, 0) whose input is the cell to the west, at (0, 0). */
    private static final Picker WEST_INPUT = new Picker("p", at(1, 0), Dir.WEST, Dir.EAST, LogisticsLevel.I);

    private static BeltUnit belt(Dir flow) {
        return new BeltUnit(at(0, 0), flow, LogisticsLevel.I);
    }

    @Test
    void aBeltFlowingIntoThePickerIsTakenFromTheFront() {
        assertThat(PickSide.of(WEST_INPUT, belt(Dir.EAST), BeltShape.STRAIGHT)).isEqualTo(PickSide.FRONT);
    }

    @Test
    void aStraightBeltFlowingPastThePickersSideIsTakenFromTheSide() {
        assertThat(PickSide.of(WEST_INPUT, belt(Dir.NORTH), BeltShape.STRAIGHT)).isEqualTo(PickSide.SIDE);
        assertThat(PickSide.of(WEST_INPUT, belt(Dir.SOUTH), BeltShape.STRAIGHT)).isEqualTo(PickSide.SIDE);
    }

    @Test
    void aCornerBesideThePickerGivesNothing() {
        assertThat(PickSide.of(WEST_INPUT, belt(Dir.NORTH), BeltShape.CORNER_LEFT)).isEqualTo(PickSide.NONE);
        assertThat(PickSide.of(WEST_INPUT, belt(Dir.SOUTH), BeltShape.CORNER_RIGHT)).isEqualTo(PickSide.NONE);
    }

    @Test
    void aBeltFlowingAwayFromThePickerGivesNothing() {
        assertThat(PickSide.of(WEST_INPUT, belt(Dir.WEST), BeltShape.STRAIGHT)).isEqualTo(PickSide.NONE);
    }

    @Test
    void noBeltAtTheInputGivesNothing() {
        assertThat(PickSide.of(WEST_INPUT, null, null)).isEqualTo(PickSide.NONE);
    }

    @Test
    void aMissingPickerIsRefused() {
        assertThatThrownBy(() -> PickSide.of(null, belt(Dir.NORTH), BeltShape.STRAIGHT)).isInstanceOf(NullPointerException.class);
    }
}
