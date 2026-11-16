package com.sentinel;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Six DetectionRule implementations matching Part B Section 1.6 of the specification.
 */
public final class Rules {

    private Rules() {}

    public static List<DetectionRule> buildAll() {
        return List.of(new BruteForce(), new Spray(), new Distributed(),
                       new SuccessAfterFailure(), new Flood(), new BlockedIpAccess());
    }

    // ── R1: Brute Force ──────────────────────────────────────────────────────

    static final class BruteForce implements DetectionRule {
        @Override public String ruleId() { return "R1"; }

        @Override
        public Alert evaluate(LoginEvent e, IpStats stats, SlidingWindow window) {
            long failures = window.snapshot().stream().filter(x -> !x.success()).count();
            if (failures < Defaults.BRUTE_FORCE_THRESHOLD) return null;
            return AlertBuilder.build("R1", "Brute Force", e, "HIGH",
                "T1110", "Brute Force",
                "Same IP tried " + failures + " passwords in 60s.",
                "Lock account after 5 failures; add MFA; use CAPTCHA.");
        }
    }

    // ── R2: Credential Spray ─────────────────────────────────────────────────

    static final class Spray implements DetectionRule {
        @Override public String ruleId() { return "R2"; }

        @Override
        public Alert evaluate(LoginEvent e, IpStats stats, SlidingWindow window) {
            long distinct = window.snapshot().stream()
                .filter(x -> !x.success())
                .map(LoginEvent::username).distinct().count();
            if (distinct < Defaults.SPRAY_THRESHOLD) return null;
            return AlertBuilder.build("R2", "Credential Spray", e, "HIGH",
                "T1110.003", "Password Spraying",
                "One IP tried " + distinct + " different accounts.",
                "Rate-limit per IP; alert on cross-account failures.");
        }
    }

    // ── R3: Distributed Attack ────────────────────────────────────────────────

    static final class Distributed implements DetectionRule {
        private static final ConcurrentHashMap<String, ArrayDeque<LoginEvent>> userFailures = new ConcurrentHashMap<>();
        private static final Object lock = new Object();

        @Override public String ruleId() { return "R3"; }

        @Override
        public Alert evaluate(LoginEvent e, IpStats stats, SlidingWindow window) {
            if (e.success()) return null;
            synchronized (lock) {
                long cutoff = System.currentTimeMillis() - 120_000L;
                var deque = userFailures.computeIfAbsent(e.username(), u -> new ArrayDeque<>());
                deque.addLast(e);
                while (!deque.isEmpty() && deque.peekFirst().timestamp().toEpochMilli() < cutoff) {
                    deque.pollFirst();
                }
                long distinctIps = deque.stream().map(LoginEvent::ip).distinct().count();
                if (deque.size() >= 8 && distinctIps >= 3) {
                    return AlertBuilder.build("R3", "Distributed Attack", e, "HIGH",
                        "T1110.004", "Distributed Credential Stuffing",
                        deque.size() + " failures on account '" + e.username() + "' across " + distinctIps + " distinct IPs within 120s.",
                        "Enforce CAPTCHA; apply global rate-limiting across distributed IP pools.");
                }
            }
            return null;
        }
    }

    // ── R4: Success After Failures ────────────────────────────────────────────

    static final class SuccessAfterFailure implements DetectionRule {
        @Override public String ruleId() { return "R4"; }

        @Override
        public Alert evaluate(LoginEvent e, IpStats stats, SlidingWindow window) {
            if (!e.success() || stats.failures.get() < 3) return null;
            return AlertBuilder.build("R4", "Compromise Detected", e, "CRITICAL",
                "T1078", "Valid Accounts",
                "Login succeeded after " + stats.failures.get() + " failures — possible account compromise.",
                "Reset credential; force MFA; review session tokens.");
        }
    }

    // ── R5: Request Flood ─────────────────────────────────────────────────────

    static final class Flood implements DetectionRule {
        @Override public String ruleId() { return "R5"; }

        @Override
        public Alert evaluate(LoginEvent e, IpStats stats, SlidingWindow window) {
            int total = window.size();
            if (total < Defaults.FLOOD_THRESHOLD && total < 20) return null;
            return AlertBuilder.build("R5", "Request Flood", e, "MEDIUM",
                "T1499", "Endpoint Denial of Service",
                "IP sent " + total + " requests in short window.",
                "Apply rate limiting; use a WAF; consider geo-blocking.");
        }
    }

    // ── R6: Blocked IP Activity ───────────────────────────────────────────────

    static final class BlockedIpAccess implements DetectionRule {
        @Override public String ruleId() { return "R6"; }

        @Override
        public Alert evaluate(LoginEvent e, IpStats stats, SlidingWindow window) {
            if (!stats.blocked && !"IP_BLOCKED".equalsIgnoreCase(e.reason())) return null;
            return AlertBuilder.build("R6", "Blocked IP Activity", e, "MEDIUM",
                "T1110", "Brute Force",
                "Traffic received from a blocked IP " + e.ip() + ".",
                "Ensure firewall rules are enforced; consider null-routing.");
        }
    }

    // ── Shared builder ────────────────────────────────────────────────────────

    private static final class AlertBuilder {
        static Alert build(String id, String name, LoginEvent e,
                           String severity, String mitreTag, String mitreLabel,
                           String why, String defend) {
            return new Alert(IdGen.next(), e.timestamp(), id, name,
                e.ip(), e.username(), severity, mitreTag, mitreLabel,
                why, defend, "OPEN");
        }
    }
}
