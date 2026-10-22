package io.corefactory.spigot.vertical;

import io.corefactory.core.vertical.VerticalDir;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VerticalRulesTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-00000000000b");

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
    void theOwnerChangesTheDirectionOfTheTower() {
        assertThat(VerticalRules.decide(VerticalDir.DOWN, VerticalDir.UP, OWNER, OWNER)).isEqualTo(VerticalRules.Verdict.SET);
    }

    @Test
    void aButtonForTheCurrentDirectionChangesNothing() {
        assertThat(VerticalRules.decide(VerticalDir.UP, VerticalDir.UP, OWNER, OWNER)).isEqualTo(VerticalRules.Verdict.SAME);
    }

    @Test
    void aPlayerWhoDidNotPlaceTheBeltIsRefused() {
        assertThat(VerticalRules.decide(VerticalDir.DOWN, VerticalDir.UP, OWNER, OTHER)).isEqualTo(VerticalRules.Verdict.DENIED);
    }

    @Test
    void aBeltWithNoStoredOwnerMayBeChangedByAnyone() {
        assertThat(VerticalRules.decide(VerticalDir.DOWN, VerticalDir.UP, null, OTHER)).isEqualTo(VerticalRules.Verdict.SET);
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
