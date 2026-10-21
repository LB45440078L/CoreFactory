package io.corefactory.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The ore budget of each chunk, stored in {@code corefactory_chunk_budget}. A chunk's planned counts are written once,
 * when the chunk is first populated; the mined counts only grow, by atomic increments. All methods block on JDBC.
 */
public final class JdbcChunkBudgets {

    /**
     * The planned and mined counts of one ore in one chunk.
     *
     * @param planned the count planned when the chunk was populated
     * @param mined   the count mined since
     */
    public record Budget(long planned, long mined) {
    }

    private final Database database;

    /**
     * @param database an open database whose migrations have run
     */
    public JdbcChunkBudgets(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    /**
     * @return true when the chunk has any budget row, which means it was populated by a version that keeps budgets
     */
    public boolean isBudgeted(String world, int chunkX, int chunkZ) {
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM corefactory_chunk_budget WHERE world = ? AND cx = ? AND cz = ? LIMIT 1")) {
                statement.setString(1, world);
                statement.setInt(2, chunkX);
                statement.setInt(3, chunkZ);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next();
                }
            }
        });
    }

    /**
     * Writes the planned counts of a chunk, one row per ore, in one transaction. A row that already exists keeps its
     * planned count, so a chunk is planned once. Give a count of 0 for an ore the chunk does not hold, so the chunk
     * still counts as budgeted.
     *
     * @param planned the planned count per ore id
     */
    public void plant(String world, int chunkX, int chunkZ, Map<String, Long> planned) {
        database.execute(connection -> {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO corefactory_chunk_budget (world, cx, cz, ore_id, planned, mined) "
                                + "VALUES (?, ?, ?, ?, ?, 0) ON CONFLICT(world, cx, cz, ore_id) DO NOTHING")) {
                    for (Map.Entry<String, Long> entry : planned.entrySet()) {
                        statement.setString(1, world);
                        statement.setInt(2, chunkX);
                        statement.setInt(3, chunkZ);
                        statement.setString(4, entry.getKey());
                        statement.setLong(5, entry.getValue());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(connection, e);
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        });
    }

    /**
     * @return the budget of one ore in one chunk, or empty when the chunk has no row for it
     */
    public Optional<Budget> find(String world, int chunkX, int chunkZ, String oreId) {
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT planned, mined FROM corefactory_chunk_budget "
                            + "WHERE world = ? AND cx = ? AND cz = ? AND ore_id = ?")) {
                statement.setString(1, world);
                statement.setInt(2, chunkX);
                statement.setInt(3, chunkZ);
                statement.setString(4, oreId);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(new Budget(rows.getLong(1), rows.getLong(2))) : Optional.empty();
                }
            }
        });
    }

    /**
     * Adds to the mined count of one ore in one chunk. Changes nothing when the chunk has no row for the ore.
     *
     * @return true when a row was updated
     */
    public boolean recordMined(String world, int chunkX, int chunkZ, String oreId, long amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be at least 1, not " + amount);
        }
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE corefactory_chunk_budget SET mined = mined + ? "
                            + "WHERE world = ? AND cx = ? AND cz = ? AND ore_id = ?")) {
                statement.setLong(1, amount);
                statement.setString(2, world);
                statement.setInt(3, chunkX);
                statement.setInt(4, chunkZ);
                statement.setString(5, oreId);
                return statement.executeUpdate() > 0;
            }
        });
    }

    private static void rollbackQuietly(Connection connection, Exception cause) {
        try {
            connection.rollback();
        } catch (SQLException suppressed) {
            cause.addSuppressed(suppressed);
        }
    }
}
