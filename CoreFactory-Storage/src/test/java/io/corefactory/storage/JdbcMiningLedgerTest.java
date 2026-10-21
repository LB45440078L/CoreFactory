package io.corefactory.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcMiningLedgerTest {

    private static final Logger LOG = Logger.getLogger(JdbcMiningLedgerTest.class.getName());
    private static final String DRILL = "world:10:64:-3";
    private static final String OTHER_DRILL = "world:11:64:-3";
    private static final int THREADS = 8;
    private static final int PER_THREAD = 50;

    @TempDir
    Path dir;

    private Database database;
    private JdbcMiningLedger ledger;

    @BeforeEach
    void open() {
        StorageSettings settings = new StorageSettings(
                StorageSettings.Type.SQLITE, dir.resolve("ledger.db").toString(), null, 0, null, null, null, 1);
        database = Database.open(settings, LOG);
        ledger = new JdbcMiningLedger(database);
    }

    @AfterEach
    void close() {
        database.close();
    }

    @Test
    void minedDefaultsToZeroAndAddsAccumulate() {
        assertThat(ledger.mined(DRILL, "uraninite_ore")).isZero();

        ledger.add(DRILL, "uraninite_ore", 1);
        ledger.add(DRILL, "uraninite_ore", 4);
        assertThat(ledger.mined(DRILL, "uraninite_ore")).isEqualTo(5);
    }

    @Test
    void countsAreKeptPerDrillAndPerOre() {
        ledger.add(DRILL, "uraninite_ore", 2);
        ledger.add(DRILL, "galena_ore", 3);
        ledger.add(OTHER_DRILL, "uraninite_ore", 7);

        assertThat(ledger.minedBy(DRILL)).containsExactlyInAnyOrderEntriesOf(
                Map.of("uraninite_ore", 2L, "galena_ore", 3L));
        assertThat(ledger.minedBy(OTHER_DRILL)).containsExactly(Map.entry("uraninite_ore", 7L));
        assertThat(ledger.minedBy("world:0:0:0")).isEmpty();
    }

    @Test
    void rejectsAmountsBelowOne() {
        assertThatThrownBy(() -> ledger.add(DRILL, "galena_ore", 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void concurrentAddsDoNotLoseCounts() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int t = 0; t < THREADS; t++) {
                results.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < PER_THREAD; i++) {
                        ledger.add(DRILL, "fluorite_ore", 1);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> result : results) {
                result.get(30, TimeUnit.SECONDS);
            }
        }
        assertThat(ledger.mined(DRILL, "fluorite_ore")).isEqualTo((long) THREADS * PER_THREAD);
    }
}
