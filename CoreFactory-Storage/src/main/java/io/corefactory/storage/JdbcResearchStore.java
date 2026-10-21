package io.corefactory.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Research unlocks and partial progress, stored in {@code corefactory_research_unlocks} and
 * {@code corefactory_research_progress}. All methods block on JDBC.
 */
public final class JdbcResearchStore {

    private final Database database;

    /**
     * @param database an open database whose migrations have run
     */
    public JdbcResearchStore(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    /**
     * @param player the player
     * @return the nodes the player has unlocked
     */
    public Set<String> unlocked(UUID player) {
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT node_id FROM corefactory_research_unlocks WHERE player_id = ?")) {
                statement.setString(1, player.toString());
                try (ResultSet rows = statement.executeQuery()) {
                    Set<String> nodes = new HashSet<>();
                    while (rows.next()) {
                        nodes.add(rows.getString(1));
                    }
                    return Set.copyOf(nodes);
                }
            }
        });
    }

    /**
     * @param player the player
     * @param node the research node
     * @param epochMillis when the node was unlocked
     * @return true if the row was inserted, false if the player already had the node
     */
    public boolean unlock(UUID player, String node, long epochMillis) {
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO corefactory_research_unlocks (player_id, node_id, unlocked_at) VALUES (?, ?, ?) "
                            + "ON CONFLICT(player_id, node_id) DO NOTHING")) {
                statement.setString(1, player.toString());
                statement.setString(2, node);
                statement.setLong(3, epochMillis);
                return statement.executeUpdate() > 0;
            }
        });
    }

    /**
     * @param player the player
     * @param node the research node
     * @return the progress units stored for the node, 0 when none
     */
    public int progress(UUID player, String node) {
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT units FROM corefactory_research_progress WHERE player_id = ? AND node_id = ?")) {
                statement.setString(1, player.toString());
                statement.setString(2, node);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? rows.getInt(1) : 0;
                }
            }
        });
    }

    /**
     * @param player the player
     * @param node the research node
     * @param units the progress units to store, replacing any previous value
     */
    public void setProgress(UUID player, String node, int units) {
        database.execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO corefactory_research_progress (player_id, node_id, units) VALUES (?, ?, ?) "
                            + "ON CONFLICT(player_id, node_id) DO UPDATE SET units = excluded.units")) {
                statement.setString(1, player.toString());
                statement.setString(2, node);
                statement.setInt(3, units);
                statement.executeUpdate();
            }
        });
    }
}
