package io.corefactory.storage;

import io.corefactory.core.deposit.Deposit;
import io.corefactory.core.deposit.DepositStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The planned deposits, stored in {@code corefactory_deposits}. {@link #take} removes amounts with a conditional update
 * that only succeeds while enough remains, so concurrent drills never remove more than a deposit holds. All methods
 * block on JDBC.
 */
public final class JdbcDepositStore implements DepositStore {

    private static final String SELECT_DEPOSITS = "SELECT id, world, centre_x, centre_z, radius, ore, "
            + "initial_amount, remaining FROM corefactory_deposits";

    private final Database database;

    /**
     * @param database an open database whose migrations have run
     */
    public JdbcDepositStore(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public Optional<Deposit> find(long id) {
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(SELECT_DEPOSITS + " WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet rows = statement.executeQuery()) {
                    return rows.next() ? Optional.of(read(rows)) : Optional.empty();
                }
            }
        });
    }

    /**
     * The centre box is {@code 2 * reach} on each side of the point, which holds every deposit whose circle reaches the
     * point when reach is at least the largest radius. The exact circle test then runs on the candidates.
     */
    @Override
    public List<Deposit> overlapping(String world, int x, int z, int reach) {
        Objects.requireNonNull(world, "world");
        if (reach < 0) {
            throw new IllegalArgumentException("reach must not be negative, not " + reach);
        }
        long box = 2L * reach;
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(SELECT_DEPOSITS
                    + " WHERE world = ? AND centre_x BETWEEN ? AND ? AND centre_z BETWEEN ? AND ? ORDER BY id")) {
                statement.setString(1, world);
                statement.setLong(2, x - box);
                statement.setLong(3, x + box);
                statement.setLong(4, z - box);
                statement.setLong(5, z + box);
                try (ResultSet rows = statement.executeQuery()) {
                    List<Deposit> out = new ArrayList<>();
                    while (rows.next()) {
                        Deposit deposit = read(rows);
                        if (DepositStore.reaches(deposit, x, z, reach)) {
                            out.add(deposit);
                        }
                    }
                    return List.copyOf(out);
                }
            }
        });
    }

    @Override
    public boolean insertIfAbsent(Deposit deposit) {
        Objects.requireNonNull(deposit, "deposit");
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO corefactory_deposits (id, world, centre_x, centre_z, radius, ore, initial_amount, "
                            + "remaining) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT(id) DO NOTHING")) {
                statement.setLong(1, deposit.id());
                statement.setString(2, deposit.world());
                statement.setInt(3, deposit.centreX());
                statement.setInt(4, deposit.centreZ());
                statement.setInt(5, deposit.radius());
                statement.setString(6, deposit.ore());
                statement.setLong(7, deposit.initialAmount());
                statement.setLong(8, deposit.remaining());
                return statement.executeUpdate() > 0;
            }
        });
    }

    /**
     * Each attempt is one statement, {@code SET remaining = remaining - n WHERE id = ? AND remaining >= n}, which the
     * database evaluates against the latest row, so the amount never goes below zero and exactly {@code n} is removed.
     * When the attempt matches no row, the remaining amount is read again and the take is cut to it. The loop ends
     * because the remaining amount only falls.
     */
    @Override
    public long take(long id, long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative, not " + amount);
        }
        if (amount == 0) {
            return 0L;
        }
        return database.query(connection -> {
            long wanted = amount;
            while (true) {
                if (subtract(connection, id, wanted)) {
                    return wanted;
                }
                long remaining = remainingOf(connection, id);
                if (remaining <= 0) {
                    return 0L;
                }
                wanted = Math.min(wanted, remaining);
            }
        });
    }

    @Override
    public List<Deposit> all(String world) {
        Objects.requireNonNull(world, "world");
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    SELECT_DEPOSITS + " WHERE world = ? ORDER BY id")) {
                statement.setString(1, world);
                try (ResultSet rows = statement.executeQuery()) {
                    List<Deposit> out = new ArrayList<>();
                    while (rows.next()) {
                        out.add(read(rows));
                    }
                    return List.copyOf(out);
                }
            }
        });
    }

    @Override
    public int size() {
        return database.query(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM corefactory_deposits");
                 ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        });
    }

    private static boolean subtract(Connection connection, long id, long amount) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE corefactory_deposits SET remaining = remaining - ? WHERE id = ? AND remaining >= ?")) {
            statement.setLong(1, amount);
            statement.setLong(2, id);
            statement.setLong(3, amount);
            return statement.executeUpdate() == 1;
        }
    }

    private static long remainingOf(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT remaining FROM corefactory_deposits WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getLong(1) : 0L;
            }
        }
    }

    private static Deposit read(ResultSet rows) throws SQLException {
        return new Deposit(rows.getLong(1), rows.getString(2), rows.getInt(3), rows.getInt(4), rows.getInt(5),
                rows.getString(6), rows.getLong(7), rows.getLong(8));
    }
}
