package io.corefactory.core.engine;

import io.corefactory.core.engine.IgnitionRules.Verdict;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IgnitionRulesTest {

    @Test
    void freshCustomTntIgnitesFromEveryVanillaCause() {
        for (IgniteCause cause : IgniteCause.values()) {
            assertThat(IgnitionRules.decide(true, cause, false, false))
                    .as("cause %s", cause)
                    .isEqualTo(Verdict.IGNITE);
        }
    }

    @Test
    void requestWithoutACauseIgnitesToo() {
        assertThat(IgnitionRules.decide(true, null, false, false)).isEqualTo(Verdict.IGNITE);
    }

    @Test
    void vanillaBlockWithoutPlacedTntIsLeftToVanilla() {
        assertThat(IgnitionRules.decide(false, IgniteCause.FLINT_AND_STEEL, false, false))
                .isEqualTo(Verdict.NOT_CUSTOM);
    }

    @Test
    void alreadyIgnitedTntIsNotIgnitedAgain() {
        assertThat(IgnitionRules.decide(true, IgniteCause.ARROW, true, false)).isEqualTo(Verdict.ALREADY_IGNITED);
    }

    @Test
    void secondRequestOnTheSameTickIsDropped() {
        assertThat(IgnitionRules.decide(true, IgniteCause.LAVA, false, true)).isEqualTo(Verdict.SAME_TICK);
    }

    @Test
    void notCustomTakesPrecedenceOverTheOtherChecks() {
        assertThat(IgnitionRules.decide(false, null, true, true)).isEqualTo(Verdict.NOT_CUSTOM);
    }

    @Test
    void alreadyIgnitedTakesPrecedenceOverTheSameTickCheck() {
        assertThat(IgnitionRules.decide(true, null, true, true)).isEqualTo(Verdict.ALREADY_IGNITED);
    }
}
