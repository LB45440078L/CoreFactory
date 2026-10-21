package io.corefactory.storage;

import java.sql.SQLException;

/**
 * A unit of JDBC work that produces no result.
 *
 * @param <T> the input, normally a {@link java.sql.Connection}
 */
@FunctionalInterface
public interface SqlConsumer<T> {

    /**
     * @param input the input
     * @throws SQLException when the database call fails
     */
    void accept(T input) throws SQLException;
}
