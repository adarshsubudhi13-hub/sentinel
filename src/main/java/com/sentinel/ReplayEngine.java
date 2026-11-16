package com.sentinel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ReplayEngine: replays a saved timeline at 1x/5x/10x speed,
 * broadcasting events over SSE to drive dashboard widgets.
 */
public final class ReplayEngine {
    private final List<ReplayListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile Thread replayThread;

    public interface ReplayListener { void onEntry(TimelineEntry entry, boolean done); }

    public void addListener(ReplayListener l) { listeners.add(l); }

    public boolean isRunning() { return running.get(); }

    /** Start replaying entries at given speed multiplier. */
    public void start(List<TimelineEntry> entries, int speedMultiplier) {
        if (!running.compareAndSet(false, true)) return;
        replayThread = new Thread(() -> {
            try { replay(entries, speedMultiplier); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { running.set(false); }
        }, "replay-thread");
        replayThread.setDaemon(true);
        replayThread.start();
    }

    public void stop() {
        if (replayThread != null) replayThread.interrupt();
        running.set(false);
    }

    private void replay(List<TimelineEntry> entries, int speed) throws InterruptedException {
        if (entries.isEmpty()) { notifyDone(); return; }
        long baseMs = entries.get(0).timestamp().toEpochMilli();
        long wallStart = System.currentTimeMillis();
        for (int i = 0; i < entries.size(); i++) {
            TimelineEntry entry = entries.get(i);
            long logicalMs = entry.timestamp().toEpochMilli() - baseMs;
            long wallTarget = wallStart + logicalMs / speed;
            long sleepMs = wallTarget - System.currentTimeMillis();
            if (sleepMs > 0) Thread.sleep(sleepMs);
            boolean done = i == entries.size() - 1;
            for (ReplayListener l : listeners) l.onEntry(entry, done);
        }
    }

    private void notifyDone() {
        for (ReplayListener l : listeners) {
            l.onEntry(new TimelineEntry("", java.time.Instant.now(), "REPLAY_DONE", "system", ""), true);
        }
    }
}
