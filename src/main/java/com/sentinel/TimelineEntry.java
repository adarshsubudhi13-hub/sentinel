package com.sentinel;

import java.time.Instant;

/** Immutable record for timeline entries. */
public record TimelineEntry(
    String id,
    Instant timestamp,
    String type,     // LOGIN_SUCCESS / LOGIN_FAIL / ALERT / IP_BLOCKED / RISK_CHANGE / SIMULATION
    String actor,    // IP or system
    String detail
) {}
