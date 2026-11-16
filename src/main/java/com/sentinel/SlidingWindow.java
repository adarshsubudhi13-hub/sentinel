package com.sentinel;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe sliding window over LoginEvent timestamps.
 * Uses ReentrantLock for explicit locking (illustrates java.util.concurrent.locks).
 */
public final class SlidingWindow {
    private final Deque<LoginEvent> events = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final long windowMs;

    public SlidingWindow(long windowMs) { this.windowMs = windowMs; }

    /** Add an event and evict expired entries. */
    public void add(LoginEvent e) {
        lock.lock();
        try {
            events.addLast(e);
            evict();
        } finally { lock.unlock(); }
    }

    /** Return snapshot of current window contents. */
    public List<LoginEvent> snapshot() {
        lock.lock();
        try {
            evict();
            return List.copyOf(events);
        } finally { lock.unlock(); }
    }

    public int size() {
        lock.lock();
        try { evict(); return events.size(); }
        finally { lock.unlock(); }
    }

    private void evict() {
        Instant cutoff = Instant.now().minusMillis(windowMs);
        while (!events.isEmpty() && events.peekFirst().timestamp().isBefore(cutoff)) {
            events.pollFirst();
        }
    }
}
