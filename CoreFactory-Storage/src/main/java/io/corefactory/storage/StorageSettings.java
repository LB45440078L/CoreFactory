package io.corefactory.storage;

import java.nio.file.Path;

/**
 * Connection settings for the storage layer. SQLite reads only {@code sqliteFile}; PostgreSQL reads the
 * host, port, database, user, password and pool size. A driver that is not on the classpath is downloaded into
 * {@code libraries} the first time it is needed.
 *
 * @param type backend to open
 * @param sqliteFile SQLite database file; missing parent directories are created
 * @param host PostgreSQL host name
 * @param port PostgreSQL port
 * @param database PostgreSQL database name
 * @param user PostgreSQL user name
 * @param password PostgreSQL password
 * @param poolSize maximum PostgreSQL pool size; SQLite always uses a single connection
 * @param libraries the folder driver jars are downloaded into
 */
public record StorageSettings(Type type, String sqliteFile, String host, int port, String database,
                              String user, String password, int poolSize, Path libraries) {

    /** Supported database backends. */
    public enum Type {
        SQLITE,
        POSTGRESQL
    }

    /** Settings whose driver jars go to a {@code libraries} folder in the working directory. */
    public StorageSettings(Type type, String sqliteFile, String host, int port, String database,
                           String user, String password, int poolSize) {
        this(type, sqliteFile, host, port, database, user, password, poolSize, Path.of("libraries"));
    }
}
