package io.corefactory.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * What each drill has mined, per ore, stored in {@code corefactory_mining_ledger}. Increments are single upserts, so
 * concurrent writers never lose a count. All methods block on JDBC.
 */
public final class JdbcMiningLedger {

    private final Database database;

    /**
     * @param database an open database whose migrations have run
     */
    public JdbcMiningLedger(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    /**
     * Adds to the count a drill holds for one ore.
     *
     * @param drillKey the drill, as {@code world:x:y:z}
     * @param oreId    the ore id
     * @param amount   how many to add, at least 1
     */
    public void add(String drillKey, String oreId, long amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be at least 1, not " + amount);
        }
        database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO corefactory_mining_ledger (drill_key, ore_id, mined) VALUES (?, ?, ?) "
                            + "ON CONFLICT(drill_key, ore_id) DO UPDATE SET "
                            + "mined = corefactory_mining_ledger.mined + excluded.mined")) {
                statement.setString(1, drillKey);
                statement.setString(2, oreId);
                statement.setLong(3, amount);
                statement.executeUpdate();
            }
        });
    }

    /**
     * @param drillKey the drill, as {@code world:x:y:z}
     * @param oreId    the ore id
     * @return how many of the ore the drill has mined, 0 when none
     */
    public long mined(String drillKey, String oreId) {
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT mined FROM corefactory_mining_ledger WHERE drill_key = ? AND ore_id = ?")) {
                statement.setString(1, drillKey);
                statement.setString(2, oreId);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? rows.getLong(1) : 0L;
                }
            }
        });
    }

    /**
     * @param drillKey the drill, as {@code world:x:y:z}
     * @return the mined count per ore id, empty when the drill has mined nothing
     */
    public Map<String, Long> minedBy(String drillKey) {
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT ore_id, mined FROM corefactory_mining_ledger WHERE drill_key = ?")) {
                statement.setString(1, drillKey);
                try (ResultSet rows = statement.executeQuery()) {
                    Map<String, Long> out = new HashMap<>();
                    while (rows.next()) {
                        out.put(rows.getString(1), rows.getLong(2));
                    }
                    return Map.copyOf(out);
                }
            }
        });
    }
}
