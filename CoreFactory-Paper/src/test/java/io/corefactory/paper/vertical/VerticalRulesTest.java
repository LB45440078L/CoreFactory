package io.corefactory.paper.vertical;

import io.corefactory.core.vertical.VerticalDir;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VerticalRulesTest {

    @Test
    void theUpButtonSetsUpAndTheDownButtonSetsDown() {
        assertThat(VerticalRules.buttonAt(VerticalRules.UP_SLOT)).contains(VerticalDir.UP);
        assertThat(VerticalRules.buttonAt(VerticalRules.DOWN_SLOT)).contains(VerticalDir.DOWN);
    }

    @Test
    void theInfoSlotAndTheOtherSlotsAreNoButton() {
        assertThat(VerticalRules.buttonAt(VerticalRules.INFO_SLOT)).isEmpty();
        assertThat(VerticalRules.buttonAt(0)).isEmpty();
        assertThat(VerticalRules.buttonAt(VerticalRules.SIZE - 1)).isEmpty();
    }

    @Test
    void aPlayerWhoMayUseTheBeltChangesTheDirectionOfTheTower() {
        assertThat(VerticalRules.decide(VerticalDir.DOWN, VerticalDir.UP, true)).isEqualTo(VerticalRules.Verdict.SET);
    }

    @Test
    void aButtonForTheCurrentDirectionChangesNothing() {
        assertThat(VerticalRules.decide(VerticalDir.UP, VerticalDir.UP, true)).isEqualTo(VerticalRules.Verdict.SAME);
    }

    @Test
    void aPlayerWhoMayNotUseTheBeltIsRefused() {
        assertThat(VerticalRules.decide(VerticalDir.DOWN, VerticalDir.UP, false)).isEqualTo(VerticalRules.Verdict.DENIED);
        assertThat(VerticalRules.decide(VerticalDir.UP, VerticalDir.UP, false)).isEqualTo(VerticalRules.Verdict.DENIED);
    }

    @Test
    void aBeltPlacedBesideATowerTakesTheClickedTowersDirection() {
        assertThat(VerticalRules.adoptDirection(Optional.of(VerticalDir.DOWN), Optional.of(VerticalDir.UP)))
                .isEqualTo(VerticalDir.DOWN);
    }

    @Test
    void aBeltPlacedOnOneTowerTakesThatTowersDirection() {
        assertThat(VerticalRules.adoptDirection(Optional.empty(), Optional.of(VerticalDir.DOWN))).isEqualTo(VerticalDir.DOWN);
        assertThat(VerticalRules.adoptDirection(Optional.of(VerticalDir.DOWN), Optional.empty())).isEqualTo(VerticalDir.DOWN);
    }

    @Test
    void aBeltThatJoinsNoTowerGoesUp() {
        assertThat(VerticalRules.adoptDirection(Optional.empty(), Optional.empty())).isEqualTo(VerticalDir.UP);
    }
}
