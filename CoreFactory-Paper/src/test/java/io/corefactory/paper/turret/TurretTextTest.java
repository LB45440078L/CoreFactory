package io.corefactory.paper.turret;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Checks the status line of a turret's hologram: the power share it shows, and that a share below full says the fire is slowed by it. */
class TurretTextTest {

    @Test
    void aPartialShareIsShownAsAPercentAndSaysTheFireIsSlowedByIt() {
        assertThat(TurretText.statusLine(TurretStatus.ENGAGING, 0.25))
                .isEqualTo("&7Status: &fEngaging&7 - power &f25% (fire slowed by the power)");
    }

    @Test
    void aFullShareIsNotCalledSlowed() {
        assertThat(TurretText.statusLine(TurretStatus.ENGAGING, 1.0))
                .isEqualTo("&7Status: &fEngaging&7 - power &f100%");
    }

    @Test
    void noPowerIsNotCalledSlowedBecauseTheTurretDoesNotFireAtAll() {
        assertThat(TurretText.statusLine(TurretStatus.NO_POWER, 0.0))
                .isEqualTo("&7Status: &fNo power&7 - power &f0%");
    }

    @Test
    void aShareOutOfRangeIsClampedBeforeItIsShown() {
        assertThat(TurretText.statusLine(TurretStatus.ENGAGING, 3.0))
                .isEqualTo("&7Status: &fEngaging&7 - power &f100%");
        assertThat(TurretText.statusLine(TurretStatus.ENGAGING, -1.0))
                .isEqualTo("&7Status: &fEngaging&7 - power &f0%");
    }
}
