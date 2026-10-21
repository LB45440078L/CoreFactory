package io.corefactory.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Pooled JDBC access to the CoreFactory database. Schema migrations run when the database is opened.
 */
public final class Database implements AutoCloseable {

    private static final String POOL_NAME = "CoreFactory-Storage";
    private static final int SQLITE_BUSY_TIMEOUT_MILLIS = 5_000;

    private final HikariDataSource dataSource;

    private Database(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Opens the pool for the configured backend and brings the schema up to date.
     *
     * @param settings backend and connection settings
     * @param logger receives migration progress
     * @return the open database
     * @throws StorageException when the pool cannot start or a migration fails
     */
    public static Database open(StorageSettings settings, Logger logger) {
        HikariConfig config = new HikariConfig();
        config.setPoolName(POOL_NAME);
        boolean sqlite = settings.type() == StorageSettings.Type.SQLITE;
        String driverClass = sqlite ? DriverLoader.SQLITE_CLASS : DriverLoader.POSTGRESQL_CLASS;
        config.setDriverClassName(driverClass);
        ClassLoader driverLoader = DriverLoader.ensure(driverClass,
                sqlite ? DriverLoader.SQLITE : DriverLoader.POSTGRESQL, settings.libraries(), logger);
        if (sqlite) {
            Path file = Path.of(settings.sqliteFile()).toAbsolutePath();
            createParentDirectories(file);
            config.setJdbcUrl("jdbc:sqlite:" + file);
            config.setMaximumPoolSize(1);
            config.setConnectionInitSql("PRAGMA busy_timeout = " + SQLITE_BUSY_TIMEOUT_MILLIS);
        } else {
            config.setJdbcUrl("jdbc:postgresql://" + settings.host() + ":" + settings.port()
                    + "/" + settings.database());
            config.setUsername(settings.user());
            config.setPassword(settings.password());
            config.setMaximumPoolSize(Math.max(1, settings.poolSize()));
        }

        Database database = new Database(createPool(config, driverLoader));
        try {
            if (sqlite) {
                database.execute(Database::enableWriteAheadLog);
            }
            database.execute(connection -> SchemaMigrations.apply(connection, logger));
        } catch (RuntimeException e) {
            database.close();
            throw e;
        }
        logger.info("Storage ready: " + settings.type());
        return database;
    }

    /**
     * Runs read or write work on a pooled connection.
     *
     * @param work the JDBC work
     * @param <T> the result type
     * @return the result of the work
     * @throws StorageException when the work throws an {@link SQLException}
     */
    public <T> T query(SqlFunction<Connection, T> work) {
        try (Connection connection = dataSource.getConnection()) {
            return work.apply(connection);
        } catch (SQLException e) {
            throw new StorageException("Database call failed", e);
        }
    }

    /**
     * Runs work on a pooled connection that produces no result.
     *
     * @param work the JDBC work
     * @throws StorageException when the work throws an {@link SQLException}
     */
    public void execute(SqlConsumer<Connection> work) {
        try (Connection connection = dataSource.getConnection()) {
            work.accept(connection);
        } catch (SQLException e) {
            throw new StorageException("Database call failed", e);
        }
    }

    @Override
    public void close() {
        dataSource.close();
    }

    /**
     * Starts the pool. Hikari looks up the driver class through the thread's context class loader when it starts, so
     * a downloaded driver is found by setting that loader for the duration.
     */
    private static HikariDataSource createPool(HikariConfig config, ClassLoader driverLoader) {
        if (driverLoader == null) {
            return new HikariDataSource(config);
        }
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(driverLoader);
        try {
            return new HikariDataSource(config);
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    private static void createParentDirectories(Path file) {
        Path parent = file.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new StorageException("Cannot create directory " + parent, e);
        }
    }

    private static void enableWriteAheadLog(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode = WAL");
        }
    }
}
