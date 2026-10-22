package io.corefactory.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseTest {

    private static final Logger LOG = Logger.getLogger(DatabaseTest.class.getName());

    @TempDir
    Path dir;

    @Test
    void migrationsRunTwiceWithoutError() {
        StorageSettings settings = sqlite(dir.resolve("corefactory.db"));

        try (Database first = Database.open(settings, LOG)) {
            assertThat(schemaVersion(first)).isEqualTo(4);
        }
        try (Database second = Database.open(settings, LOG)) {
            assertThat(schemaVersion(second)).isEqualTo(4);
            assertThat(schemaRowCount(second)).isEqualTo(1);
        }
    }

    @Test
    void createsMissingParentDirectoryForSqliteFile() throws Exception {
        Path file = dir.resolve("nested/data/corefactory.db");

        try (Database database = Database.open(sqlite(file), LOG)) {
            assertThat(schemaVersion(database)).isEqualTo(4);
        }
        assertThat(Files.isRegularFile(file)).isTrue();
    }

    @Test
    void theLegacyLedgerTablesAreNotLeftBehind() {
        try (Database database = Database.open(sqlite(dir.resolve("legacy.db")), LOG)) {
            assertThat(tableExists(database, "corefactory_mining_ledger")).isFalse();
            assertThat(tableExists(database, "corefactory_chunk_budget")).isFalse();
            assertThat(tableExists(database, "corefactory_deposits")).isTrue();
        }
    }

    private static boolean tableExists(Database database, String table) {
        return database.query(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
                return rows.next();
            } catch (SQLException e) {
                return false;
            }
        });
    }

    private static int schemaVersion(Database database) {
        return database.query(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT version FROM corefactory_schema WHERE component = 'storage'")) {
                rows.next();
                return rows.getInt(1);
            }
        });
    }

    private static int schemaRowCount(Database database) {
        return database.query(connection -> {
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM corefactory_schema")) {
                rows.next();
                return rows.getInt(1);
            }
        });
    }

    private static StorageSettings sqlite(Path file) {
        return new StorageSettings(StorageSettings.Type.SQLITE, file.toString(), null, 0, null, null, null, 1);
    }
}
