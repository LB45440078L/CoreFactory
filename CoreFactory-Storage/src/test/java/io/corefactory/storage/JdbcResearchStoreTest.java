package io.corefactory.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcResearchStoreTest {

    private static final Logger LOG = Logger.getLogger(JdbcResearchStoreTest.class.getName());
    private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final int THREADS = 8;
    private static final int PER_THREAD = 25;

    @TempDir
    Path dir;

    private Database database;
    private JdbcResearchStore store;

    @BeforeEach
    void open() {
        StorageSettings settings = new StorageSettings(
                StorageSettings.Type.SQLITE, dir.resolve("research.db").toString(), null, 0, null, null, null, 1);
        database = Database.open(settings, LOG);
        store = new JdbcResearchStore(database);
    }

    @AfterEach
    void close() {
        database.close();
    }

    @Test
    void unlockReturnsTrueOnceThenFalse() {
        assertThat(store.unlock(ALICE, "laboratory_basics", 1_000L)).isTrue();
        assertThat(store.unlock(ALICE, "laboratory_basics", 2_000L)).isFalse();
        assertThat(store.unlocked(ALICE)).containsExactly("laboratory_basics");
    }

    @Test
    void unlockedListsOnlyThePlayersOwnNodes() {
        store.unlock(ALICE, "laboratory_basics", 1L);
        store.unlock(ALICE, "radiation_safety", 2L);
        store.unlock(BOB, "metallurgy", 3L);

        assertThat(store.unlocked(ALICE)).containsExactlyInAnyOrder("laboratory_basics", "radiation_safety");
        assertThat(store.unlocked(BOB)).containsExactly("metallurgy");
        assertThat(store.unlocked(UUID.randomUUID())).isEmpty();
    }

    @Test
    void progressDefaultsToZeroAndRoundTrips() {
        assertThat(store.progress(ALICE, "metallurgy")).isZero();

        store.setProgress(ALICE, "metallurgy", 7);
        assertThat(store.progress(ALICE, "metallurgy")).isEqualTo(7);
        assertThat(store.progress(BOB, "metallurgy")).isZero();

        store.setProgress(ALICE, "metallurgy", 3);
        assertThat(store.progress(ALICE, "metallurgy")).isEqualTo(3);
    }

    @Test
    void concurrentUnlocksDoNotLoseRows() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int t = 0; t < THREADS; t++) {
                int thread = t;
                results.add(pool.submit(() -> {
                    start.await();
                    int inserted = 0;
                    for (int i = 0; i < PER_THREAD; i++) {
                        if (store.unlock(ALICE, "node_" + thread + "_" + i, 1L)) {
                            inserted++;
                        }
                    }
                    return inserted;
                }));
            }
            start.countDown();

            int total = 0;
            for (Future<Integer> result : results) {
                total += result.get(30, TimeUnit.SECONDS);
            }
            assertThat(total).isEqualTo(THREADS * PER_THREAD);
        }
        assertThat(store.unlocked(ALICE)).hasSize(THREADS * PER_THREAD);
    }

    @Test
    void racingUnlocksOfOneNodeInsertExactlyOneRow() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int t = 0; t < THREADS; t++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return store.unlock(ALICE, "shared_node", 1L);
                }));
            }
            start.countDown();

            int inserted = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    inserted++;
                }
            }
            assertThat(inserted).isEqualTo(1);
        }
        assertThat(store.unlocked(ALICE)).containsExactly("shared_node");
    }
}
