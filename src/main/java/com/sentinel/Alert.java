package com.sentinel;

import java.time.Instant;

/** Immutable record representing a fired detection alert. */
public record Alert(
    String id,
    Instant timestamp,
    String ruleId,
    String ruleName,
    String ip,
    String username,
    String severity,          // LOW / MEDIUM / HIGH / CRITICAL
    String mitreTag,          // e.g. T1110
    String mitreLabel,        // e.g. Brute Force
    String why,               // why this fired
    String defend,            // how to defend
    String status             // OPEN / ACKNOWLEDGED / RESOLVED
) {}
