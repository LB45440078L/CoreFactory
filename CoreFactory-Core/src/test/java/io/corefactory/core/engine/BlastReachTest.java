package io.corefactory.core.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlastReachTest {

    @Test
    void pointInsideTheSphereIsReached() {
        assertThat(BlastReach.within(0.5, 0.5, 0.5, 2.5, 0.5, 0.5, 4.0)).isTrue();
        assertThat(BlastReach.within(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 4.0)).isTrue();
    }

    @Test
    void pointExactlyOnTheEdgeIsReached() {
        assertThat(BlastReach.within(0, 0, 0, 4, 0, 0, 4.0)).isTrue();
        assertThat(BlastReach.within(0, 0, 0, 0, -4, 0, 4.0)).isTrue();
    }

    @Test
    void pointOutsideTheSphereIsNotReached() {
        assertThat(BlastReach.within(0, 0, 0, 4.01, 0, 0, 4.0)).isFalse();
    }

    @Test
    void distanceIsThreeDimensional() {
        // (3, 4, 0) is 5 blocks from the origin: a horizontal-only test would call it 3 or 4 blocks away.
        assertThat(BlastReach.within(0, 0, 0, 3, 4, 0, 5.0)).isTrue();
        assertThat(BlastReach.within(0, 0, 0, 3, 4, 0, 4.99)).isFalse();
        // Two blocks straight up is reached by a radius of 2 even though its horizontal offset is zero.
        assertThat(BlastReach.within(0, 0, 0, 0, 2, 0, 2.0)).isTrue();
        assertThat(BlastReach.within(0, 0, 0, 2, 2, 2, 3.46)).isFalse();
        assertThat(BlastReach.within(0, 0, 0, 2, 2, 2, 3.47)).isTrue();
    }

    @Test
    void negativeRadiusReachesNothing() {
        assertThat(BlastReach.within(0, 0, 0, 0, 0, 0, -1.0)).isFalse();
    }

    @Test
    void reachedUnprotectedDeviceIsDestroyed() {
        assertThat(BlastReach.destroyed(true, false)).isTrue();
    }

    @Test
    void protectedDeviceIsSpared() {
        assertThat(BlastReach.destroyed(true, true)).isFalse();
    }

    @Test
    void deviceOutsideTheBlastIsSpared() {
        assertThat(BlastReach.destroyed(false, false)).isFalse();
        assertThat(BlastReach.destroyed(false, true)).isFalse();
    }
}
