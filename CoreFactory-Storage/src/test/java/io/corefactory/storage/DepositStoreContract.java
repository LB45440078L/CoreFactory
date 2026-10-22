package io.corefactory.storage;

import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DepositStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The behaviour every {@link DepositStore} must have. The memory and the database store each extend it.
 */
abstract class DepositStoreContract {

    private static final int THREADS = 8;
    private static final int REQUESTS = 200;
    private static final long REQUEST_AMOUNT = 10L;

    /** @return the store under test, empty at the start of each test */
    protected abstract DepositStore store();

    protected static Deposit deposit(long id, String world, int centreX, int centreZ, int radius, long amount) {
        return new Deposit(id, world, centreX, centreZ, radius, "galena_ore", amount, amount);
    }

    @Test
    void insertIfAbsentStoresADepositOnce() {
        Deposit first = deposit(1, "world", 10, 0, 4, 100);

        assertThat(store().insertIfAbsent(first)).isTrue();
        assertThat(store().insertIfAbsent(deposit(1, "world", 99, 99, 9, 7))).isFalse();
        assertThat(store().size()).isEqualTo(1);
        assertThat(store().find(1)).contains(first);
    }

    @Test
    void findReturnsEmptyForAnUnknownId() {
        assertThat(store().find(42)).isEmpty();

        store().insertIfAbsent(deposit(42, "world", 0, 0, 4, 100));

        assertThat(store().find(42)).isPresent();
        assertThat(store().find(43)).isEmpty();
    }

    @Test
    void overlappingReturnsTheDepositsWithinReachAndNotTheFarOnes() {
        store().insertIfAbsent(deposit(1, "world", 10, 0, 4, 100));
        store().insertIfAbsent(deposit(2, "world", 12, 0, 4, 100));
        store().insertIfAbsent(deposit(3, "world", 13, 0, 4, 100));
        store().insertIfAbsent(deposit(4, "world", 100, 100, 4, 100));
        store().insertIfAbsent(deposit(5, "nether", 10, 0, 4, 100));

        // Within 4 + 8 = 12 blocks: 1 at 10 and 2 at exactly 12 reach; 3 at 13 does not.
        assertThat(store().overlapping("world", 0, 0, 8)).extracting(Deposit::id).containsExactly(1L, 2L);
    }

    @Test
    void overlappingUsesTheRadiusOfEachDeposit() {
        store().insertIfAbsent(deposit(8, "world", 0, 18, 8, 100));
        store().insertIfAbsent(deposit(9, "world", 0, 18, 4, 100));

        // Reach 10: 8 + 10 = 18 covers the centre at 18, 4 + 10 = 14 does not.
        assertThat(store().overlapping("world", 0, 0, 10)).extracting(Deposit::id).containsExactly(8L);
    }

    @Test
    void overlappingIsEmptyFarFromEveryDeposit() {
        store().insertIfAbsent(deposit(1, "world", 10, 0, 4, 100));

        assertThat(store().overlapping("world", 1000, 1000, 8)).isEmpty();
        assertThat(store().overlapping("world", -1000, 0, 8)).isEmpty();
    }

    @Test
    void overlappingRejectsANegativeReach() {
        assertThatThrownBy(() -> store().overlapping("world", 0, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void takeReturnsTheActualAmountAndNeverGoesBelowZero() {
        store().insertIfAbsent(deposit(1, "world", 0, 0, 4, 25));

        assertThat(store().take(1, 10)).isEqualTo(10);
        assertThat(store().take(1, 10)).isEqualTo(10);
        assertThat(store().take(1, 10)).isEqualTo(5);
        assertThat(store().take(1, 10)).isZero();
        assertThat(store().find(1)).map(Deposit::remaining).contains(0L);
        assertThat(store().find(1)).map(Deposit::initialAmount).contains(25L);
    }

    @Test
    void takeOfZeroRemovesNothing() {
        store().insertIfAbsent(deposit(1, "world", 0, 0, 4, 25));

        assertThat(store().take(1, 0)).isZero();
        assertThat(store().find(1)).map(Deposit::remaining).contains(25L);
    }

    @Test
    void takeFromAnUnknownDepositReturnsZero() {
        assertThat(store().take(404, 10)).isZero();
        assertThat(store().size()).isZero();
    }

    @Test
    void takeRejectsANegativeAmount() {
        store().insertIfAbsent(deposit(1, "world", 0, 0, 4, 25));

        assertThatThrownBy(() -> store().take(1, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(store().find(1)).map(Deposit::remaining).contains(25L);
    }

    @Test
    void allListsOneWorldInIdOrder() {
        store().insertIfAbsent(deposit(3, "world", 0, 0, 4, 100));
        store().insertIfAbsent(deposit(1, "world", 50, 50, 4, 100));
        store().insertIfAbsent(deposit(2, "nether", 0, 0, 4, 100));

        assertThat(store().all("world")).extracting(Deposit::id).containsExactly(1L, 3L);
        assertThat(store().all("nether")).extracting(Deposit::id).containsExactly(2L);
        assertThat(store().all("end")).isEmpty();
    }

    @Test
    void racingTakesNeverRemoveMoreThanTheDepositHolds() throws Exception {
        store().insertIfAbsent(deposit(1, "world", 0, 0, 4, 1000));
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> results = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 0; i < REQUESTS; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return store().take(1, REQUEST_AMOUNT);
                }));
            }
            start.countDown();

            long total = 0;
            for (Future<Long> result : results) {
                long taken = result.get(30, TimeUnit.SECONDS);
                assertThat(taken).isBetween(0L, REQUEST_AMOUNT);
                total += taken;
            }
            assertThat(total).isEqualTo(1000L);
        }
        assertThat(store().find(1)).map(Deposit::remaining).contains(0L);
    }
}
