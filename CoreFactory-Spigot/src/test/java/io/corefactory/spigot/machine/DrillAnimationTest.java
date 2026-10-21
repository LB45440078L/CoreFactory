package io.corefactory.spigot.machine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DrillAnimationTest {

    @Test
    void theStrokeRestsAtFrameZeroAndSinksToItsLowestAtTheMiddleFrame() {
        assertThat(DrillAnimation.offset(0)).isCloseTo(0.0, within(1e-9));
        assertThat(DrillAnimation.offset(DrillAnimation.FRAMES / 2)).isCloseTo(-DrillAnimation.STROKE, within(1e-9));
    }

    @Test
    void everyKeyframeLiesWithinTheStrokeAndTheCycleReturnsToRest() {
        for (int frame = 0; frame < DrillAnimation.FRAMES; frame++) {
            double offset = DrillAnimation.offset(frame);
            assertThat(offset).isBetween(-DrillAnimation.STROKE, 0.0);
        }
        assertThat(DrillAnimation.offset(DrillAnimation.FRAMES)).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void theStrokeIsSymmetricAboutTheMiddleFrame() {
        assertThat(DrillAnimation.offset(1)).isCloseTo(DrillAnimation.offset(3), within(1e-9));
    }
}
