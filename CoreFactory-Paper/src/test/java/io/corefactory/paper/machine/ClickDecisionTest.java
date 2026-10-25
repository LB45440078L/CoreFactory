package io.corefactory.paper.machine;

import io.corefactory.paper.machine.ClickDecision.Held;
import org.junit.jupiter.api.Test;

import static io.corefactory.paper.machine.ClickDecision.OPEN_MENU;
import static io.corefactory.paper.machine.ClickDecision.PASS_THROUGH;
import static io.corefactory.paper.machine.ClickDecision.PLACE_BY_HAND;
import static org.assertj.core.api.Assertions.assertThat;

/** The decision of a right-click on a machine. Nothing here starts a server. */
class ClickDecisionTest {

    @Test
    void aPlainRightClickOpensTheMenuWhateverIsHeld() {
        for (Held held : Held.values()) {
            assertThat(ClickDecision.of(false, held, true, true)).isEqualTo(OPEN_MENU);
            assertThat(ClickDecision.of(false, held, false, false)).isEqualTo(OPEN_MENU);
        }
    }

    @Test
    void aSneakingClickWithAnEmptyHandOpensTheMenu() {
        assertThat(ClickDecision.of(true, Held.EMPTY, false, false)).isEqualTo(OPEN_MENU);
    }

    @Test
    void aSneakingClickWithABlockPlacesItWhenTheFaceIsKnownAndFree() {
        assertThat(ClickDecision.of(true, Held.BLOCK, true, true)).isEqualTo(PLACE_BY_HAND);
    }

    @Test
    void aSneakingClickWithABlockPassesThroughWhenTheTargetIsNotFree() {
        assertThat(ClickDecision.of(true, Held.BLOCK, true, false)).isEqualTo(PASS_THROUGH);
        assertThat(ClickDecision.of(true, Held.BLOCK, false, true)).isEqualTo(PASS_THROUGH);
        assertThat(ClickDecision.of(true, Held.BLOCK, false, false)).isEqualTo(PASS_THROUGH);
    }

    @Test
    void aSneakingClickWithAnotherItemNeverOpensOrPlaces() {
        assertThat(ClickDecision.of(true, Held.OTHER, true, true)).isEqualTo(PASS_THROUGH);
        assertThat(ClickDecision.of(true, Held.OTHER, false, false)).isEqualTo(PASS_THROUGH);
    }
}
