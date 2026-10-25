package io.corefactory.paper.artillery;

import org.bukkit.map.MapPalette;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the loading tile's sweep: the step advances with the ticks and wraps, and the band moves across the tile. */
class LoadingAnimationTest {

    @Test
    void theStepAdvancesEveryStepTicksAndWraps() {
        assertThat(LoadingAnimation.phase(0)).isZero();
        assertThat(LoadingAnimation.phase(LoadingAnimation.STEP_TICKS - 1)).isZero();
        assertThat(LoadingAnimation.phase(LoadingAnimation.STEP_TICKS)).isEqualTo(1);
        assertThat(LoadingAnimation.phase(LoadingAnimation.STEP_TICKS * LoadingAnimation.STEPS)).isZero();
        assertThat(LoadingAnimation.phase(LoadingAnimation.STEP_TICKS * (LoadingAnimation.STEPS + 1))).isEqualTo(1);
    }

    @Test
    void theBandStartsAtTheLowestCornerAndLeavesAtTheOther() {
        assertThat(LoadingAnimation.colour(0, 0, 0)).isEqualTo(MapPalette.LIGHT_GRAY);
        assertThat(LoadingAnimation.colour(0, 0, LoadingAnimation.STEPS - 1)).isEqualTo(MapPalette.DARK_GRAY);
        assertThat(LoadingAnimation.colour(15, 15, LoadingAnimation.STEPS - 1)).isEqualTo(MapPalette.LIGHT_GRAY);
    }

    @Test
    void theBandMovesAcrossTheTileAsTheStepAdvances() {
        // The column at the lowest corner is lit at the first steps, and dark once the band has moved past it.
        assertThat(LoadingAnimation.colour(0, 0, 4)).isEqualTo(MapPalette.LIGHT_GRAY);
        assertThat(LoadingAnimation.colour(0, 0, 5)).isEqualTo(MapPalette.DARK_GRAY);
        // The column at the far corner is dark until the band reaches it.
        assertThat(LoadingAnimation.colour(15, 15, 29)).isEqualTo(MapPalette.DARK_GRAY);
        assertThat(LoadingAnimation.colour(15, 15, 30)).isEqualTo(MapPalette.LIGHT_GRAY);
    }

    @Test
    void theColourOfAColumnIsTheSameInEveryChunk() {
        assertThat(LoadingAnimation.colour(16 + 2, -16 + 3, 7)).isEqualTo(LoadingAnimation.colour(2, 3, 7));
    }
}
