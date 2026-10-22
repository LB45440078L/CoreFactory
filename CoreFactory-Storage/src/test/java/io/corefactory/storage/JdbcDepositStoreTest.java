package io.corefactory.storage;

import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DepositStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcDepositStoreTest extends DepositStoreContract {

    private static final Logger LOG = Logger.getLogger(JdbcDepositStoreTest.class.getName());

    @TempDir
    Path dir;

    private Database database;
    private JdbcDepositStore store;

    @BeforeEach
    void open() {
        database = Database.open(sqlite(dir.resolve("deposits.db")), LOG);
        store = new JdbcDepositStore(database);
    }

    @AfterEach
    void close() {
        database.close();
    }

    @Override
    protected DepositStore store() {
        return store;
    }

    @Test
    void depositsAndRemainingAmountsSurviveReopeningTheDatabase() {
        store.insertIfAbsent(deposit(1, "world", 10, 0, 4, 100));
        store.insertIfAbsent(deposit(2, "world", 50, 50, 4, 100));
        assertThat(store.take(1, 30)).isEqualTo(30);

        database.close();
        database = Database.open(sqlite(dir.resolve("deposits.db")), LOG);
        store = new JdbcDepositStore(database);

        assertThat(store.size()).isEqualTo(2);
        assertThat(store.find(1)).map(Deposit::remaining).contains(70L);
        assertThat(store.insertIfAbsent(deposit(1, "world", 10, 0, 4, 100))).isFalse();
        assertThat(store.overlapping("world", 0, 0, 8)).extracting(Deposit::id).containsExactly(1L);
        assertThat(store.take(1, 100)).isEqualTo(70);
    }

    private static StorageSettings sqlite(Path file) {
        return new StorageSettings(StorageSettings.Type.SQLITE, file.toString(), null, 0, null, null, null, 1);
    }
}
