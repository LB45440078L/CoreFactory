package io.corefactory.storage;

import java.sql.SQLException;

/**
 * A unit of JDBC work that produces a result.
 *
 * @param <I> the input, normally a {@link java.sql.Connection}
 * @param <O> the result
 */
@FunctionalInterface
public interface SqlFunction<I, O> {

    /**
     * @param input the input
     * @return the result
     * @throws SQLException when the database call fails
     */
    O apply(I input) throws SQLException;
}
