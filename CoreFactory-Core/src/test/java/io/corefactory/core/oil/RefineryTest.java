package io.corefactory.core.oil;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class RefineryTest {

    @Test
    void aFullSpeedRefineryDrawsOneTenthOfALitreOfPetroleumEachSecond() {
        assertThat(Refinery.PETROLEUM_PER_SECOND).isCloseTo(0.1, within(1e-12));
        assertThat(Refinery.petroleumWanted(1.0)).isCloseTo(0.1, within(1e-12));
    }

    @Test
    void aPassOfThreeLitresOfPetroleumMakesTwoLitresOfDiesel() {
        assertThat(Refinery.dieselFrom(3.0)).isCloseTo(2.0, within(1e-12));
        assertThat(Refinery.dieselFrom(0.0)).isZero();
    }

    @Test
    void aFullPassTakesThirtySecondsOfWantedPetroleum() {
        double wanted = 0.0;
        for (int tick = 0; tick < 600; tick++) {
            wanted += Refinery.petroleumWanted(1.0 / 20.0);
        }

        assertThat(wanted).isCloseTo(3.0, within(1e-9));
    }

    @Test
    void aTickWithNoTimeOrANegativeAmountIsRefused() {
        assertThatThrownBy(() -> Refinery.petroleumWanted(0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Refinery.dieselFrom(-1.0)).isInstanceOf(IllegalArgumentException.class);
    }
}
