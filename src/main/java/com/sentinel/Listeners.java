package com.sentinel;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Console and File alert listeners demonstrating AlertListener interface implementations.
 */
public final class Listeners {
    private Listeners() {}

    public static final class ConsoleAlertListener implements AlertListener {
        @Override
        public void onAlert(Alert alert) {
            System.out.printf("[ALERT] %s [%s] %s (%s) IP: %s User: %s%n",
                alert.severity(), alert.mitreTag(), alert.ruleName(), alert.ruleId(), alert.ip(), alert.username());
        }
    }

    public static final class FileAlertListener implements AlertListener {
        private final String filepath;

        public FileAlertListener(String filepath) {
            this.filepath = filepath;
        }

        @Override
        public synchronized void onAlert(Alert alert) {
            try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filepath, true)))) {
                pw.printf("%s|%s|%s|%s|%s|%s|%s%n",
                    alert.timestamp(), alert.severity(), alert.mitreTag(),
                    alert.ruleId(), alert.ip(), alert.username(), alert.why());
            } catch (IOException e) {
                System.err.println("FileAlertListener write error: " + e.getMessage());
            }
        }
    }

    public static final class JdbcAlertListener implements AlertListener {
        private final com.sentinel.database.JdbcAlertRepository repository;

        public JdbcAlertListener(com.sentinel.database.JdbcAlertRepository repository) {
            this.repository = repository;
        }

        @Override
        public void onAlert(Alert alert) {
            try {
                com.sentinel.database.AlertRecord record = new com.sentinel.database.AlertRecord(
                    alert.id(),
                    alert.ruleId(),
                    alert.ruleName(),
                    alert.severity(),
                    alert.ip(),
                    alert.username(),
                    alert.timestamp().toEpochMilli(),
                    alert.why(),
                    alert.mitreTag(),
                    "NEW",
                    "Automated detection from real-time stream"
                );
                repository.save(record);
            } catch (com.sentinel.database.DatabaseException e) {
                System.err.println("[WARN] Failed to persist alert to JDBC SQLite database: " + e.getMessage());
            }
        }
    }
}
