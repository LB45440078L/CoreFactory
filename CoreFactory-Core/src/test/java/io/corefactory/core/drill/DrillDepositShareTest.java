package io.corefactory.core.drill;

import io.corefactory.core.deposit.Deposit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** The deposit a drill picks when its square holds several deposits: the largest remaining share, then the lower id. */
class DrillDepositShareTest {

    private static final String GALENA = "galena_ore";

    /** A deposit of galena that started with {@code initial} and has {@code remaining} left. */
    private static Deposit deposit(long id, long initial, long remaining) {
        return new Deposit(id, "world", 0, 0, 5, GALENA, initial, remaining);
    }

    @Test
    void threeDepositsWithDifferentSharesAreMinedInShareOrder() {
        // Each row gives the remaining amounts of x (id 1), y (id 2) and z (id 3), each started at 1000, and the deposit picked.
        long[][] rows = {
                {200, 500, 900, 3},
                {200, 500, 450, 2},
                {200, 100, 450, 3},
                {200, 100, 150, 1},
                {50, 100, 150, 3},
                {50, 100, 0, 2},
                {50, 0, 0, 1},
        };
        for (long[] row : rows) {
            List<Deposit> deposits = List.of(
                    deposit(1, 1_000, row[0]),
                    deposit(2, 1_000, row[1]),
                    deposit(3, 1_000, row[2]));
            assertThat(DrillDeposits.choose(deposits).orElseThrow().id())
                    .as("x %d, y %d, z %d", row[0], row[1], row[2])
                    .isEqualTo(row[3]);
        }
        assertThat(DrillDeposits.choose(List.of(deposit(1, 1_000, 0), deposit(2, 1_000, 0), deposit(3, 1_000, 0))))
                .isEmpty();
    }

    @Test
    void twoDepositsAreMinedToTheSameFractionOverManyCycles() {
        long cycle = 1_000;
        long initial = 500_000;
        List<Deposit> deposits = new ArrayList<>(List.of(deposit(1, initial, initial), deposit(2, initial, 300_000)));
        double oneCycle = (double) cycle / initial;
        boolean levelled = false;
        int cycles = 0;
        Optional<Deposit> chosen = DrillDeposits.choose(deposits);
        while (chosen.isPresent()) {
            Deposit mined = chosen.get();
            long take = Math.min(cycle, mined.remaining());
            deposits.replaceAll(d -> d.id() == mined.id() ? d.withRemaining(d.remaining() - take) : d);
            cycles++;
            assertThat(cycles).as("the run ends").isLessThan(10_000);
            Deposit first = deposits.get(0);
            Deposit second = deposits.get(1);
            if (first.remaining() > 0 && second.remaining() > 0) {
                double gap = Math.abs(share(first) - share(second));
                if (gap <= oneCycle + 1e-9) {
                    levelled = true;
                }
                if (levelled) {
                    assertThat(gap).as("after cycle %d", cycles).isLessThanOrEqualTo(oneCycle + 1e-9);
                }
            }
            chosen = DrillDeposits.choose(deposits);
        }
        assertThat(levelled).isTrue();
        assertThat(deposits).allMatch(d -> d.remaining() == 0);
    }

    @Test
    void aDepositWithNothingLeftIsSkippedWhileAnotherIsMined() {
        Deposit empty = deposit(1, 1_000, 0);
        Deposit left = deposit(2, 1_000, 20);
        assertThat(DrillDeposits.choose(List.of(empty, left))).contains(left);
    }

    @Test
    void whenEveryDepositIsEmptyTheDrillHasNoTarget() {
        assertThat(DrillDeposits.choose(List.of(deposit(1, 1_000, 0), deposit(2, 500, 0)))).isEmpty();
        assertThat(DrillDeposits.choose(List.of())).isEmpty();
    }

    @Test
    void aTieOfSharesIsBrokenByTheLowerIdWhateverTheAmounts() {
        // Both are half full: 1000 of 2000 and 3 of 6.
        Deposit big = deposit(7, 2_000, 1_000);
        Deposit small = deposit(4, 6, 3);
        assertThat(DrillDeposits.choose(List.of(big, small))).contains(small);
        assertThat(DrillDeposits.choose(List.of(small, big))).contains(small);
    }

    private static double share(Deposit deposit) {
        return (double) deposit.remaining() / deposit.initialAmount();
    }
}
