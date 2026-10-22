package io.corefactory.core.deposit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DepositTest {

    private static Deposit deposit(int radius, long initial, long remaining) {
        return new Deposit(1L, "world", 10, -20, radius, "galena_ore", initial, remaining);
    }

    @Test
    void containsIncludesTheEdgeAndNothingBeyondIt() {
        Deposit deposit = deposit(3, 100, 100);

        assertThat(deposit.contains(10, -20)).isTrue();
        assertThat(deposit.contains(13, -20)).isTrue();
        assertThat(deposit.contains(7, -20)).isTrue();
        assertThat(deposit.contains(10, -17)).isTrue();
        assertThat(deposit.contains(12, -22)).isTrue();
        assertThat(deposit.contains(13, -21)).isFalse();
        assertThat(deposit.contains(14, -20)).isFalse();
        assertThat(deposit.contains(10, -24)).isFalse();
    }

    @Test
    void containsHandlesNegativeAndLargeCoordinates() {
        Deposit far = new Deposit(2L, "world", -30_000_000, 30_000_000, 48, "galena_ore", 1, 1);

        assertThat(far.contains(-30_000_000, 30_000_000)).isTrue();
        assertThat(far.contains(-30_000_048, 30_000_000)).isTrue();
        assertThat(far.contains(-30_000_049, 30_000_000)).isFalse();
    }

    @Test
    void withRemainingKeepsEverythingElse() {
        Deposit after = deposit(5, 1000, 1000).withRemaining(400);

        assertThat(after).isEqualTo(new Deposit(1L, "world", 10, -20, 5, "galena_ore", 1000, 400));
    }

    @Test
    void aRadiusBelowOneIsRefused() {
        assertThatThrownBy(() -> deposit(0, 10, 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeAmountsAreRefused() {
        assertThatThrownBy(() -> deposit(1, -1, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> deposit(1, 10, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void remainingCannotExceedTheInitialAmount() {
        assertThatThrownBy(() -> deposit(1, 10, 11)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNullOreIsRefused() {
        assertThatThrownBy(() -> new Deposit(1L, "world", 0, 0, 1, null, 10, 10))
                .isInstanceOf(NullPointerException.class);
    }
}
