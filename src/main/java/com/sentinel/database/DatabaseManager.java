package com.sentinel.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages JDBC connections to the SQLite database.
 * Demonstrates Singleton pattern, encapsulation, and robust JDBC connection management.
 */
public final class DatabaseManager {
    private static volatile DatabaseManager instance;
    private final String dbUrl;

    public DatabaseManager(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
        initDatabase();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    File dataDir = new File("data");
                    if (!dataDir.exists()) {
                        dataDir.mkdirs();
                    }
                    instance = new DatabaseManager("data/sentinel.db");
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("[WARN] SQLite JDBC Driver not found on classpath: " + e.getMessage());
        }

        String sqlAlerts = """
            CREATE TABLE IF NOT EXISTS security_alerts (
                id TEXT PRIMARY KEY,
                rule_id TEXT NOT NULL,
                rule_name TEXT NOT NULL,
                severity TEXT NOT NULL,
                target_ip TEXT NOT NULL,
                username TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                description TEXT NOT NULL,
                mitre_technique TEXT NOT NULL,
                status TEXT NOT NULL,
                resolution_notes TEXT
            );
            """;

        String sqlScenarios = """
            CREATE TABLE IF NOT EXISTS attack_runs (
                id TEXT PRIMARY KEY,
                scenario_name TEXT NOT NULL,
                start_time INTEGER NOT NULL,
                end_time INTEGER,
                total_events INTEGER DEFAULT 0,
                status TEXT NOT NULL
            );
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlAlerts);
            stmt.execute(sqlScenarios);
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to initialize SQLite schema: " + e.getMessage());
        }
    }
}
