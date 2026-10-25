package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.FiringVerdict;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks that every refusal has its line for the player, and that a shot that may be fired has none. */
class ArtilleryVerdictsTest {

    @Test
    void everyRefusalHasAColouredLine() {
        for (FiringVerdict verdict : FiringVerdict.values()) {
            String message = ArtilleryVerdicts.message(verdict);
            if (verdict.ok()) {
                assertThat(message).isEmpty();
            } else {
                assertThat(message).startsWith("&c").hasSizeGreaterThan(10);
            }
        }
    }

    @Test
    void aPieceInAHeldChunkIsAskedToWaitAndOneInAnUnheldChunkToBeWalkedTo() {
        String held = ArtilleryVerdicts.pieceNotLoaded(true, 12, 70, -40);
        String unheld = ArtilleryVerdicts.pieceNotLoaded(false, 12, 70, -40);

        assertThat(held).startsWith("&c").contains("12, 70, -40").contains("loads again in a few seconds").doesNotContain("walk near it to load");
        assertThat(unheld).startsWith("&c").contains("12, 70, -40").contains("walk near it to load it").doesNotContain("loads again");
    }

    @Test
    void aLoadingAimAsksToTryAgainAndAnUnheldAimNamesItsSpot() {
        String loading = ArtilleryVerdicts.aimNotLoaded(true, 100, -7);
        String unheld = ArtilleryVerdicts.aimNotLoaded(false, 100, -7);

        assertThat(loading).isEqualTo("&cThe target area is loading; try again in a few seconds.");
        assertThat(unheld).startsWith("&c").contains("100, -7").contains("walk near it to load it").doesNotContain("loading");
    }

    @Test
    void theLinesAreDifferentForDifferentRefusals() {
        assertThat(ArtilleryVerdicts.message(FiringVerdict.OUT_OF_RANGE))
                .isNotEqualTo(ArtilleryVerdicts.message(FiringVerdict.TOO_CLOSE));
        assertThat(ArtilleryVerdicts.message(FiringVerdict.BLAST_REACHES_ARTILLERY))
                .isNotEqualTo(ArtilleryVerdicts.message(FiringVerdict.BLAST_REACHES_PLAYER));
    }
}
