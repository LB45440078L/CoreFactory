package io.corefactory.paper.artillery;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Checks the recoil of a barrel: its curve over the ticks of one shot. */
class ArtilleryRecoilTest {

    private static final double EPS = 1e-9;

    private static final double FULL = 0.5;

    @Test
    void theBarrelIsAtRestBeforeTheShotAndAfterTheRecoil() {
        assertThat(ArtilleryRecoil.displacement(0, FULL)).isEqualTo(0.0);
        assertThat(ArtilleryRecoil.displacement(-3, FULL)).isEqualTo(0.0);
        assertThat(ArtilleryRecoil.displacement(ArtilleryRecoil.TOTAL_TICKS, FULL)).isEqualTo(0.0);
        assertThat(ArtilleryRecoil.displacement(ArtilleryRecoil.TOTAL_TICKS + 5, FULL)).isEqualTo(0.0);
    }

    @Test
    void theBarrelKicksBackToTheFullDistanceAtTheEndOfTheKick() {
        assertThat(ArtilleryRecoil.displacement(ArtilleryRecoil.KICK_TICKS, FULL)).isCloseTo(FULL, within(EPS));
        assertThat(ArtilleryRecoil.displacement(1, FULL)).isCloseTo(FULL / ArtilleryRecoil.KICK_TICKS, within(EPS));
    }

    @Test
    void theBarrelNeverGoesBeyondTheFullDistance() {
        for (int tick = 0; tick <= ArtilleryRecoil.TOTAL_TICKS; tick++) {
            double displacement = ArtilleryRecoil.displacement(tick, FULL);
            assertThat(displacement).as("tick %d", tick).isBetween(0.0, FULL);
        }
    }

    @Test
    void theKickRisesAndTheReturnFalls() {
        for (int tick = 1; tick <= ArtilleryRecoil.KICK_TICKS; tick++) {
            assertThat(ArtilleryRecoil.displacement(tick, FULL))
                    .isGreaterThan(ArtilleryRecoil.displacement(tick - 1, FULL));
        }
        for (int tick = ArtilleryRecoil.KICK_TICKS + 1; tick < ArtilleryRecoil.TOTAL_TICKS; tick++) {
            assertThat(ArtilleryRecoil.displacement(tick, FULL))
                    .isLessThan(ArtilleryRecoil.displacement(tick - 1, FULL));
        }
    }

    @Test
    void theDistanceScalesWithTheFullKick() {
        assertThat(ArtilleryRecoil.displacement(3, 1.0)).isCloseTo(2 * ArtilleryRecoil.displacement(3, 0.5), within(EPS));
        assertThat(ArtilleryRecoil.displacement(ArtilleryRecoil.KICK_TICKS, 0.0)).isEqualTo(0.0);
    }
}
