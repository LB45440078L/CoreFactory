package io.corefactory.paper.logistics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CapWarningThrottleTest {

    private static final long MINUTE = 60_000L;

    @Test
    void theFirstCallLogs() {
        CapWarningThrottle throttle = new CapWarningThrottle(true, 10 * MINUTE);
        assertThat(throttle.allow(1_000L)).isTrue();
    }

    @Test
    void aCallWithinTheIntervalDoesNotLog() {
        CapWarningThrottle throttle = new CapWarningThrottle(true, 10 * MINUTE);
        assertThat(throttle.allow(0L)).isTrue();
        assertThat(throttle.allow(10 * MINUTE - 1)).isFalse();
    }

    @Test
    void aCallAfterTheIntervalLogsAgain() {
        CapWarningThrottle throttle = new CapWarningThrottle(true, 10 * MINUTE);
        assertThat(throttle.allow(0L)).isTrue();
        assertThat(throttle.allow(10 * MINUTE)).isTrue();
        assertThat(throttle.allow(10 * MINUTE + 1)).isFalse();
        assertThat(throttle.allow(20 * MINUTE)).isTrue();
    }

    @Test
    void aDisabledThrottleNeverLogs() {
        CapWarningThrottle throttle = new CapWarningThrottle(false, 10 * MINUTE);
        assertThat(throttle.allow(0L)).isFalse();
        assertThat(throttle.allow(100 * MINUTE)).isFalse();
    }

    @Test
    void zeroIntervalLogsOnceOnly() {
        CapWarningThrottle throttle = new CapWarningThrottle(true, 0L);
        assertThat(throttle.allow(0L)).isTrue();
        assertThat(throttle.allow(100 * MINUTE)).isFalse();
    }

    @Test
    void minutesOfZeroOrLessMeanOnceOnly() {
        CapWarningThrottle zero = CapWarningThrottle.of(true, 0);
        CapWarningThrottle negative = CapWarningThrottle.of(true, -5);
        assertThat(zero.allow(0L)).isTrue();
        assertThat(zero.allow(100 * MINUTE)).isFalse();
        assertThat(negative.allow(0L)).isTrue();
        assertThat(negative.allow(100 * MINUTE)).isFalse();
    }

    @Test
    void minutesAreConvertedToMillis() {
        CapWarningThrottle throttle = CapWarningThrottle.of(true, 2.5);
        assertThat(throttle.allow(0L)).isTrue();
        assertThat(throttle.allow(150_000L - 1)).isFalse();
        assertThat(throttle.allow(150_000L)).isTrue();
    }
}
