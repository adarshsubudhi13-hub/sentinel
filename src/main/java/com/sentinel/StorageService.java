package com.sentinel;

import java.util.List;

/** Persistence abstraction; swappable between file and in-memory implementations. */
public interface StorageService {
    void saveAlert(Alert alert);
    List<Alert> loadAlerts();
    void saveTimeline(TimelineEntry entry);
    List<TimelineEntry> loadTimeline();
    void saveBlocklist(java.util.Set<String> ips);
    java.util.Set<String> loadBlocklist();
    void clearAll();
}
