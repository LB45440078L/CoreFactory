package io.corefactory.core.lock;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PinAttemptsTest {

    private static final long LOCKOUT = 30_000L;

    @Test
    void threeWrongPinsStartAThirtySecondLockout() {
        PinAttempts tries = new PinAttempts(3, LOCKOUT);

        assertThat(tries.wrong(1_000)).isFalse();
        assertThat(tries.wrong(2_000)).isFalse();
        assertThat(tries.remainingMillis(2_000)).isZero();

        assertThat(tries.wrong(3_000)).isTrue();
        assertThat(tries.remainingMillis(3_000)).isEqualTo(LOCKOUT);
    }

    @Test
    void attemptsDuringTheLockoutAreNotCounted() {
        PinAttempts tries = new PinAttempts(3, LOCKOUT);
        tries.wrong(0);
        tries.wrong(0);
        tries.wrong(0);

        assertThat(tries.wrong(10_000)).isFalse();

        assertThat(tries.remainingMillis(10_000)).isEqualTo(20_000L);
    }

    @Test
    void theLockoutEndsAfterThirtySeconds() {
        PinAttempts tries = new PinAttempts(3, LOCKOUT);
        tries.wrong(0);
        tries.wrong(0);
        tries.wrong(0);

        assertThat(tries.remainingMillis(29_999)).isEqualTo(1L);
        assertThat(tries.remainingMillis(LOCKOUT)).isZero();
    }

    @Test
    void theCountStartsAgainOnceTheLockoutHasEnded() {
        PinAttempts tries = new PinAttempts(3, LOCKOUT);
        tries.wrong(0);
        tries.wrong(0);
        tries.wrong(0);

        assertThat(tries.wrong(LOCKOUT)).isFalse();
        assertThat(tries.attemptsLeft()).isEqualTo(2);
        assertThat(tries.remainingMillis(LOCKOUT)).isZero();
    }

    @Test
    void aCorrectPinClearsTheCount() {
        PinAttempts tries = new PinAttempts(3, LOCKOUT);
        tries.wrong(0);
        tries.wrong(1);

        tries.succeeded();

        assertThat(tries.attemptsLeft()).isEqualTo(3);
        assertThat(tries.wrong(2)).isFalse();
        assertThat(tries.remainingMillis(2)).isZero();
    }

    @Test
    void attemptsLeftCountDown() {
        PinAttempts tries = new PinAttempts(3, LOCKOUT);
        assertThat(tries.attemptsLeft()).isEqualTo(3);

        tries.wrong(0);

        assertThat(tries.attemptsLeft()).isEqualTo(2);
    }

    @Test
    void aZeroLockoutNeverHoldsTheKeypad() {
        PinAttempts tries = new PinAttempts(1, 0L);

        assertThat(tries.wrong(5)).isTrue();

        assertThat(tries.remainingMillis(5)).isZero();
    }
}
