package com.sentinel;

/**
 * Base exception for all Sentinel domain and integration errors.
 */
public class SentinelException extends RuntimeException {
    private final String errorCode;
    private final int httpStatus;

    public SentinelException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() { return errorCode; }
    public int getHttpStatus() { return httpStatus; }

    public static class InvalidTargetException extends SentinelException {
        public InvalidTargetException(String target) {
            super("INVALID_TARGET", "Target host forbidden. Only 127.0.0.1:9090 is permitted: " + target, 400);
        }
    }

    public static class TargetUnreachableException extends SentinelException {
        public TargetUnreachableException(String message) {
            super("TARGET_UNREACHABLE", message, 503);
        }
    }

    public static class InvalidLoginEventException extends SentinelException {
        public InvalidLoginEventException(String message) {
            super("INVALID_LOGIN_EVENT", message, 400);
        }
    }

    public static class LogParseException extends SentinelException {
        public LogParseException(String message) {
            super("LOG_PARSE_ERROR", message, 400);
        }
    }

    public static class SimulationAlreadyRunningException extends SentinelException {
        public SimulationAlreadyRunningException() {
            super("SIMULATION_RUNNING", "A simulation is already in progress", 409);
        }
    }

    public static class SimulationConfigException extends SentinelException {
        public SimulationConfigException(String message) {
            super("INVALID_SIMULATION_CONFIG", message, 400);
        }
    }

    public static class RuleConfigException extends SentinelException {
        public RuleConfigException(String message) {
            super("INVALID_RULE_CONFIG", message, 400);
        }
    }

    public static class StorageException extends SentinelException {
        public StorageException(String message, Throwable cause) {
            super("STORAGE_ERROR", message, 500);
            initCause(cause);
        }
    }

    public static class AlertNotFoundException extends SentinelException {
        public AlertNotFoundException(String id) {
            super("ALERT_NOT_FOUND", "Alert not found: " + id, 404);
        }
    }
}
