package com.sentinel.database;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Domain entity representing a persistent security alert in the JDBC database.
 * Demonstrates Encapsulation with private attributes and controlled accessors.
 */
public final class AlertRecord {
    private final String id;
    private final String ruleId;
    private final String ruleName;
    private final String severity;
    private final String targetIp;
    private final String username;
    private final long timestamp;
    private final String description;
    private final String mitreTechnique;
    private String status;
    private String resolutionNotes;

    public AlertRecord(String id, String ruleId, String ruleName, String severity,
                       String targetIp, String username, long timestamp,
                       String description, String mitreTechnique,
                       String status, String resolutionNotes) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.ruleId = ruleId != null ? ruleId : "UNKNOWN";
        this.ruleName = ruleName != null ? ruleName : "Generic Alert";
        this.severity = severity != null ? severity : "MEDIUM";
        this.targetIp = targetIp != null ? targetIp : "127.0.0.1";
        this.username = username != null ? username : "-";
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();
        this.description = description != null ? description : "";
        this.mitreTechnique = mitreTechnique != null ? mitreTechnique : "T1078";
        this.status = status != null ? status : "NEW";
        this.resolutionNotes = resolutionNotes != null ? resolutionNotes : "";
    }

    public String getId() { return id; }
    public String getRuleId() { return ruleId; }
    public String getRuleName() { return ruleName; }
    public String getSeverity() { return severity; }
    public String getTargetIp() { return targetIp; }
    public String getUsername() { return username; }
    public long getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
    public String getMitreTechnique() { return mitreTechnique; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String notes) { this.resolutionNotes = notes; }

    public String getFormattedTime() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(timestamp));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlertRecord that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) IP: %s User: %s Status: %s",
                severity, ruleName, mitreTechnique, targetIp, username, status);
    }
}
