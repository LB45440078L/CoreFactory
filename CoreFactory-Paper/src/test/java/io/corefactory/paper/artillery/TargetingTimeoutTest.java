package io.corefactory.paper.artillery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the listening mode's time limit: open while the player clicks often enough, closed once the limit has passed with no click. */
class TargetingTimeoutTest {

    @Test
    void aSessionStaysOpenBeforeTheLimit() {
        long clicked = 1_000L;
        assertThat(TargetingTimeout.expired(clicked, clicked + 19_999L, 20)).isFalse();
    }

    @Test
    void aSessionEndsAtTheLimitWithNoClick() {
        long clicked = 1_000L;
        assertThat(TargetingTimeout.expired(clicked, clicked + 20_000L, 20)).isTrue();
        assertThat(TargetingTimeout.expired(clicked, clicked + 25_000L, 20)).isTrue();
    }

    @Test
    void aClickResetsTheLimit() {
        long firstClick = 1_000L;
        long secondClick = 15_000L;
        assertThat(TargetingTimeout.expired(secondClick, secondClick + 19_000L, 20)).isFalse();
        assertThat(TargetingTimeout.expired(firstClick, secondClick + 19_000L, 20)).isTrue();
    }
}
