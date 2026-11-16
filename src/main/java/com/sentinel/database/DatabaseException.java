package com.sentinel.database;

import com.sentinel.SentinelException;

/**
 * Custom checked exception for JDBC database operations.
 * Demonstrates Java Exception Handling hierarchy.
 */
public class DatabaseException extends SentinelException {
    public DatabaseException(String message) {
        super("DATABASE_ERROR", message, 500);
    }

    public DatabaseException(String message, Throwable cause) {
        super("DATABASE_ERROR", message + (cause != null ? ": " + cause.getMessage() : ""), 500);
        if (cause != null) {
            initCause(cause);
        }
    }
}

