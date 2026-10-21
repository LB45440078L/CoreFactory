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

class JdbcChunkBudgetsTest {

    private static final Logger LOG = Logger.getLogger(JdbcChunkBudgetsTest.class.getName());
    private static final int THREADS = 8;
    private static final int PER_THREAD = 40;

    @TempDir
    Path dir;

    private Database database;
    private JdbcChunkBudgets budgets;

    @BeforeEach
    void open() {
        StorageSettings settings = new StorageSettings(
                StorageSettings.Type.SQLITE, dir.resolve("budget.db").toString(), null, 0, null, null, null, 1);
        database = Database.open(settings, LOG);
        budgets = new JdbcChunkBudgets(database);
    }

    @AfterEach
    void close() {
        database.close();
    }

    @Test
    void unplantedChunkIsNotBudgeted() {
        assertThat(budgets.isBudgeted("world", 3, -2)).isFalse();
        assertThat(budgets.find("world", 3, -2, "galena_ore")).isEmpty();
    }

    @Test
    void plantWritesOneRowPerOreAndMarksTheChunk() {
        budgets.plant("world", 3, -2, Map.of("galena_ore", 12L, "thorite_ore", 0L));

        assertThat(budgets.isBudgeted("world", 3, -2)).isTrue();
        assertThat(budgets.isBudgeted("world", 3, -3)).isFalse();
        assertThat(budgets.isBudgeted("nether", 3, -2)).isFalse();
        assertThat(budgets.find("world", 3, -2, "galena_ore")).contains(new JdbcChunkBudgets.Budget(12, 0));
        assertThat(budgets.find("world", 3, -2, "thorite_ore")).contains(new JdbcChunkBudgets.Budget(0, 0));
    }

    @Test
    void plantingTwiceKeepsTheFirstPlan() {
        budgets.plant("world", 0, 0, Map.of("galena_ore", 5L));
        budgets.plant("world", 0, 0, Map.of("galena_ore", 99L, "fluorite_ore", 7L));

        assertThat(budgets.find("world", 0, 0, "galena_ore")).contains(new JdbcChunkBudgets.Budget(5, 0));
        assertThat(budgets.find("world", 0, 0, "fluorite_ore")).contains(new JdbcChunkBudgets.Budget(7, 0));
    }

    @Test
    void recordMinedIncrementsOnlyAnExistingRow() {
        budgets.plant("world", 1, 1, Map.of("galena_ore", 10L));

        assertThat(budgets.recordMined("world", 1, 1, "galena_ore", 3)).isTrue();
        assertThat(budgets.recordMined("world", 1, 1, "galena_ore", 2)).isTrue();
        assertThat(budgets.recordMined("world", 1, 1, "cinnabar_ore", 1)).isFalse();
        assertThat(budgets.recordMined("world", 9, 9, "galena_ore", 1)).isFalse();

        assertThat(budgets.find("world", 1, 1, "galena_ore")).contains(new JdbcChunkBudgets.Budget(10, 5));
        assertThat(budgets.find("world", 1, 1, "cinnabar_ore")).isEmpty();
    }

    @Test
    void mineThenPlantAgainDoesNotResetMined() {
        budgets.plant("world", 2, 2, Map.of("galena_ore", 4L));
        budgets.recordMined("world", 2, 2, "galena_ore", 1);
        budgets.plant("world", 2, 2, Map.of("galena_ore", 4L));

        assertThat(budgets.find("world", 2, 2, "galena_ore")).contains(new JdbcChunkBudgets.Budget(4, 1));
    }

    @Test
    void concurrentRecordsDoNotLoseCounts() throws Exception {
        budgets.plant("world", 5, 5, Map.of("galena_ore", (long) THREADS * PER_THREAD));
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();

        try (ExecutorService pool = Executors.newFixedThreadPool(THREADS)) {
            for (int t = 0; t < THREADS; t++) {
                results.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < PER_THREAD; i++) {
                        budgets.recordMined("world", 5, 5, "galena_ore", 1);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> result : results) {
                result.get(30, TimeUnit.SECONDS);
            }
        }
        assertThat(budgets.find("world", 5, 5, "galena_ore"))
                .contains(new JdbcChunkBudgets.Budget(THREADS * PER_THREAD, THREADS * PER_THREAD));
    }
}
