package com.sentinel.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Abstract base class for all JDBC repositories.
 * Demonstrates Inheritance, Abstraction, and Generics.
 *
 * @param <T>  The domain entity type
 * @param <ID> The primary key type
 */
public abstract class AbstractJdbcRepository<T, ID> implements CrudRepository<T, ID> {
    protected final DatabaseManager dbManager;

    protected AbstractJdbcRepository(DatabaseManager dbManager) {
        this.dbManager = Objects.requireNonNull(dbManager, "DatabaseManager cannot be null");
    }

    protected Connection getConnection() throws SQLException {
        return dbManager.getConnection();
    }

    protected DatabaseException wrapSqlException(String action, SQLException e) {
        return new DatabaseException("Failed to execute " + action + ": " + e.getMessage(), e);
    }
}
