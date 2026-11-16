package com.sentinel;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core engine: accepts LoginEvents, runs DetectionRules, fires AlertListeners.
 * Uses LinkedBlockingQueue as producer-consumer channel (illustrates BlockingQueue).
 */
public final class DetectionEngine {
    private final LinkedBlockingQueue<LoginEvent> queue = new LinkedBlockingQueue<>(10_000);
    private final ConcurrentHashMap<String, IpStats> ipRegistry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SlidingWindow> windows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<AlertListener> listeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<TimelineEntry> timeline = new CopyOnWriteArrayList<>();
    private final List<DetectionRule> rules;
    private final StorageService storage;
    private final DefenseMode defense;
    private final Scorecard scorecard;
    private final AtomicInteger totalAttempts = new AtomicInteger();
    private final AtomicInteger totalSuccesses = new AtomicInteger();
    private final AtomicInteger totalFailures = new AtomicInteger();
    private final AtomicInteger totalAlerts = new AtomicInteger();
    private volatile Thread consumerThread;

    public DetectionEngine(List<DetectionRule> rules, StorageService storage,
                           DefenseMode defense, Scorecard scorecard) {
        this.rules = rules;
        this.storage = storage;
        this.defense = defense;
        this.scorecard = scorecard;
    }

    /** Start background consumer thread. */
    public void start() {
        consumerThread = new Thread(this::consume, "engine-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    public void stop() {
        if (consumerThread != null) consumerThread.interrupt();
    }

    /** Submit an event (non-blocking; drops if full). */
    public boolean submit(LoginEvent event) {
        return queue.offer(event);
    }

    public void addListener(AlertListener l) { listeners.add(l); }

    private void consume() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                LoginEvent event = queue.take();
                process(event);
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void process(LoginEvent event) {
        totalAttempts.incrementAndGet();
        IpStats stats = ipRegistry.computeIfAbsent(event.ip(), IpStats::new);
        SlidingWindow window = windows.computeIfAbsent(event.ip(),
            ip -> new SlidingWindow(Defaults.WINDOW_SECONDS * 1000L));

        if (event.success()) {
            totalSuccesses.incrementAndGet();
            if (stats.failures.get() > 0) stats.compromised = true;
        } else {
            totalFailures.incrementAndGet();
            stats.failures.incrementAndGet();
        }

        window.add(event);
        addTimeline(event);
        evaluateRules(event, stats, window);
        updateRisk(stats, window);

        if (defense.isEnabled() && stats.riskScore >= Defaults.DEFENSE_AUTO_BLOCK_THRESHOLD) {
            blockIp(stats);
        }
    }

    private void evaluateRules(LoginEvent event, IpStats stats, SlidingWindow window) {
        for (DetectionRule rule : rules) {
            String cooldownKey = rule.ruleId() + ":" + event.ip();
            long now = System.currentTimeMillis();
            Long last = cooldowns.get(cooldownKey);
            if (last != null && now - last < Defaults.ALERT_COOLDOWN_MS) continue;
            Alert alert = rule.evaluate(event, stats, window);
            if (alert != null) {
                cooldowns.put(cooldownKey, now);
                totalAlerts.incrementAndGet();
                scorecard.recordAlert(alert);
                storage.saveAlert(alert);
                listeners.forEach(l -> l.onAlert(alert));
                addTimelineAlert(alert);
            }
        }
    }

    private void updateRisk(IpStats stats, SlidingWindow window) {
        stats.riskScore = RiskScorer.scoreIp(stats, window.snapshot());
    }

    private void blockIp(IpStats stats) {
        if (!stats.blocked) {
            stats.blocked = true;
            stats.lockedUntil = Instant.now().plusMillis(Defaults.LOCKOUT_MILLIS);
            defense.recordBlock(stats.ip);
            addTimelineBlock(stats.ip);
            storage.saveBlocklist(getBlockedIps());
        }
    }

    private void addTimeline(LoginEvent e) {
        String type = e.success() ? "LOGIN_SUCCESS" : "LOGIN_FAIL";
        addEntry(new TimelineEntry(IdGen.next(), e.timestamp(), type, e.ip(),
            e.username() + (e.success() ? " logged in" : " failed")));
    }

    private void addTimelineAlert(Alert a) {
        addEntry(new TimelineEntry(IdGen.next(), a.timestamp(), "ALERT", a.ip(),
            a.ruleName() + " — " + a.severity()));
    }

    private void addTimelineBlock(String ip) {
        addEntry(new TimelineEntry(IdGen.next(), Instant.now(), "IP_BLOCKED", ip,
            "Auto-blocked by Defense Mode"));
    }

    private void addEntry(TimelineEntry e) {
        timeline.add(e);
        storage.saveTimeline(e);
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public List<TimelineEntry> getTimeline() { return List.copyOf(timeline); }

    public ConcurrentHashMap<String, IpStats> getIpRegistry() { return ipRegistry; }

    public int getTotalAttempts() { return totalAttempts.get(); }
    public int getTotalSuccesses() { return totalSuccesses.get(); }
    public int getTotalFailures() { return totalFailures.get(); }
    public int getTotalAlerts() { return totalAlerts.get(); }

    public int getSystemRisk() {
        List<Integer> scores = ipRegistry.values().stream()
            .map(s -> s.riskScore).toList();
        return RiskScorer.systemRisk(scores);
    }

    public Set<String> getBlockedIps() {
        ConcurrentHashMap.KeySetView<String, Boolean> set = ConcurrentHashMap.newKeySet();
        ipRegistry.values().stream().filter(s -> s.blocked).forEach(s -> set.add(s.ip));
        return set;
    }

    public void blockIpManual(String ip) {
        IpStats stats = ipRegistry.computeIfAbsent(ip, IpStats::new);
        blockIp(stats);
    }

    public void unblockIp(String ip) {
        IpStats stats = ipRegistry.get(ip);
        if (stats != null) { stats.blocked = false; stats.lockedUntil = null; }
        storage.saveBlocklist(getBlockedIps());
    }

    public void reset() {
        queue.clear();
        ipRegistry.clear();
        windows.clear();
        cooldowns.clear();
        timeline.clear();
        totalAttempts.set(0);
        totalSuccesses.set(0);
        totalFailures.set(0);
        totalAlerts.set(0);
        scorecard.reset();
        defense.reset();
        storage.clearAll();
    }
}
