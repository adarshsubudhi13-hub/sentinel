package com.sentinel;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory StorageService for unit tests.
 * Demonstrates the Strategy pattern — same interface, different backing store.
 */
public final class InMemoryStorageService implements StorageService {
    private final List<Alert> alerts = new CopyOnWriteArrayList<>();
    private final List<TimelineEntry> timeline = new CopyOnWriteArrayList<>();
    private final Set<String> blocklist = new HashSet<>();

    @Override public void saveAlert(Alert a) { alerts.add(a); }
    @Override public List<Alert> loadAlerts() { return List.copyOf(alerts); }
    @Override public void saveTimeline(TimelineEntry e) { timeline.add(e); }
    @Override public List<TimelineEntry> loadTimeline() { return List.copyOf(timeline); }
    @Override public synchronized void saveBlocklist(Set<String> ips) { blocklist.clear(); blocklist.addAll(ips); }
    @Override public synchronized Set<String> loadBlocklist() { return Set.copyOf(blocklist); }
    @Override public void clearAll() { alerts.clear(); timeline.clear(); synchronized(this) { blocklist.clear(); } }
}
