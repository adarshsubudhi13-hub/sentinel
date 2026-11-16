package com.sentinel;

import java.time.Instant;

/**
 * Immutable domain record representing an authentication event ingested from DemoBank's audit log.
 */
public record LoginEvent(
    String id,
    Instant timestamp,
    String ip,
    String username,
    boolean success,
    String reason,
    int status,
    String runId
) {
    public LoginEvent(String id, Instant timestamp, String ip, String username, boolean success) {
        this(id, timestamp, ip, username, success, success ? "NONE" : "BAD_PASSWORD", success ? 200 : 401, null);
    }
}
