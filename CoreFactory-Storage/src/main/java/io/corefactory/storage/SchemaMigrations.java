package io.corefactory.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.logging.Logger;

/**
 * Versioned schema for both backends. Every statement is valid on SQLite and PostgreSQL, and each migration
 * is recorded in {@code corefactory_schema} in the same transaction as its statements.
 */
final class SchemaMigrations {

    private static final String COMPONENT = "storage";

    private static final List<Migration> MIGRATIONS = List.of(
            new Migration(1, List.of(
                    """
                    CREATE TABLE IF NOT EXISTS corefactory_research_unlocks (
                        player_id TEXT NOT NULL,
                        node_id TEXT NOT NULL,
                        unlocked_at BIGINT NOT NULL,
                        PRIMARY KEY (player_id, node_id)
                    )""",
                    """
                    CREATE TABLE IF NOT EXISTS corefactory_research_progress (
                        player_id TEXT NOT NULL,
                        node_id TEXT NOT NULL,
                        units INTEGER NOT NULL,
                        PRIMARY KEY (player_id, node_id)
                    )""")),
            new Migration(2, List.of(
                    """
                    CREATE TABLE IF NOT EXISTS corefactory_mining_ledger (
                        drill_key TEXT NOT NULL,
                        ore_id TEXT NOT NULL,
                        mined BIGINT NOT NULL,
                        PRIMARY KEY (drill_key, ore_id)
                    )""",
                    """
                    CREATE TABLE IF NOT EXISTS corefactory_chunk_budget (
                        world TEXT NOT NULL,
                        cx INTEGER NOT NULL,
                        cz INTEGER NOT NULL,
                        ore_id TEXT NOT NULL,
                        planned BIGINT NOT NULL,
                        mined BIGINT NOT NULL,
                        PRIMARY KEY (world, cx, cz, ore_id)
                    )""")),
            new Migration(3, List.of(
                    """
                    CREATE TABLE IF NOT EXISTS corefactory_deposits (
                        id BIGINT PRIMARY KEY,
                        world VARCHAR NOT NULL,
                        centre_x INTEGER NOT NULL,
                        centre_z INTEGER NOT NULL,
                        radius INTEGER NOT NULL,
                        ore VARCHAR NOT NULL,
                        initial_amount BIGINT NOT NULL,
                        remaining BIGINT NOT NULL CHECK (remaining >= 0)
                    )""",
                    """
                    CREATE INDEX IF NOT EXISTS corefactory_deposits_centre
                        ON corefactory_deposits (world, centre_x, centre_z)""")),
            new Migration(4, List.of(
                    "DROP TABLE IF EXISTS corefactory_mining_ledger",
                    "DROP TABLE IF EXISTS corefactory_chunk_budget")));

    private SchemaMigrations() {
    }

    static void apply(Connection connection, Logger logger) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS corefactory_schema (
                        component TEXT PRIMARY KEY,
                        version INTEGER NOT NULL
                    )""");
        }
        int current = currentVersion(connection);
        for (Migration migration : MIGRATIONS) {
            if (migration.version() <= current) {
                continue;
            }
            runInTransaction(connection, migration);
            logger.info("Applied storage schema version " + migration.version());
        }
    }

    private static int currentVersion(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT version FROM corefactory_schema WHERE component = ?")) {
            statement.setString(1, COMPONENT);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getInt(1) : 0;
            }
        }
    }

    private static void runInTransaction(Connection connection, Migration migration) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            try (Statement statement = connection.createStatement()) {
                for (String sql : migration.statements()) {
                    statement.execute(sql);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO corefactory_schema (component, version) VALUES (?, ?) "
                            + "ON CONFLICT(component) DO UPDATE SET version = excluded.version")) {
                statement.setString(1, COMPONENT);
                statement.setInt(2, migration.version());
                statement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private record Migration(int version, List<String> statements) {
    }
}
