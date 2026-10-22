package io.corefactory.core.deposit;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DepositScanTest {

    private static Deposit at(long id, int centreX, int centreZ, int radius, String ore) {
        return new Deposit(id, "world", centreX, centreZ, radius, ore, 1_000_000L, 1_000_000L);
    }

    @Test
    void theNearestCentreComesFirst() {
        Deposit far = at(1, 40, 0, 5, "galena_ore");
        Deposit near = at(2, 10, 0, 5, "galena_ore");
        Deposit middle = at(3, 0, 20, 5, "galena_ore");

        assertThat(DepositScan.nearest(List.of(far, near, middle), 0, 0, 96, DepositFilter.ALL, 10))
                .containsExactly(near, middle, far);
    }

    @Test
    void aDepositCountsWhenItsCircleReachesTheRadius() {
        Deposit reaching = at(1, 100, 0, 10, "galena_ore");
        Deposit outside = at(2, 200, 0, 10, "galena_ore");

        assertThat(DepositScan.nearest(List.of(reaching, outside), 0, 0, 96, DepositFilter.ALL, 10))
                .containsExactly(reaching);
    }

    @Test
    void theFilterKeepsOnlyItsOre() {
        Deposit galena = at(1, 10, 0, 5, "galena_ore");
        Deposit coal = at(2, 20, 0, 5, "coal_ore");

        assertThat(DepositScan.nearest(List.of(galena, coal), 0, 0, 96, "coal_ore", 10)).containsExactly(coal);
        assertThat(DepositScan.nearest(List.of(galena, coal), 0, 0, 96, DepositFilter.ALL, 10)).containsExactly(galena, coal);
    }

    @Test
    void theLimitCutsTheListKeepingTheNearest() {
        List<Deposit> found = List.of(at(1, 30, 0, 5, "a"), at(2, 10, 0, 5, "a"), at(3, 20, 0, 5, "a"));

        assertThat(DepositScan.nearest(found, 0, 0, 96, DepositFilter.ALL, 2)).extracting(Deposit::id).containsExactly(2L, 3L);
        assertThat(DepositScan.nearest(found, 0, 0, 96, DepositFilter.ALL, 0)).isEmpty();
    }

    @Test
    void aTieIsOrderedByIdSoTheListIsStable() {
        Deposit first = at(7, 10, 0, 5, "a");
        Deposit second = at(3, 0, 10, 5, "a");

        assertThat(DepositScan.nearest(List.of(first, second), 0, 0, 96, DepositFilter.ALL, 10))
                .containsExactly(second, first);
    }

    @Test
    void theDistanceToTheCentreIsRoundedToTheNearestBlock() {
        Deposit deposit = at(1, 3, 4, 5, "a");

        assertThat(DepositScan.blocksTo(deposit, 0, 0)).isEqualTo(5L);
        assertThat(DepositScan.blocksTo(deposit, 3, 4)).isZero();
        assertThat(DepositScan.blocksTo(at(2, 10, 0, 5, "a"), 0, 0)).isEqualTo(10L);
    }

    @Test
    void aWaypointIsReachedInsideTheCircle() {
        Deposit deposit = at(1, 0, 0, 10, "a");

        assertThat(DepositScan.arrived(deposit, 0, 0)).isTrue();
        assertThat(DepositScan.arrived(deposit, 10, 0)).isTrue();
        assertThat(DepositScan.arrived(deposit, 11, 0)).isFalse();
    }
}
