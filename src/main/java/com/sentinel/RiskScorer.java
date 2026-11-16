package com.sentinel;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Computes integer risk scores (0–100) for a single IP and system-wide. */
public final class RiskScorer {

    private RiskScorer() {}

    /** Score one IP based on its statistics. */
    public static int scoreIp(IpStats s, List<LoginEvent> window) {
        long windowMs = Defaults.WINDOW_SECONDS * 1000L;
        Instant cutoff = Instant.now().minusMillis(windowMs);
        long rate = window.stream().filter(e -> e.timestamp().isAfter(cutoff)).count();
        int score = s.failures.get() * Defaults.RISK_PER_FAILURE
                  + s.distinct.get() * Defaults.RISK_PER_USER
                  + (int)(rate * Defaults.RISK_PER_RATE)
                  + (s.compromised ? Defaults.RISK_COMPROMISE_BONUS : 0)
                  + (s.blocked ? Defaults.RISK_BLOCKED_BONUS : 0);
        return Math.min(Defaults.RISK_MAX, score);
    }

    /** System risk = 60% of highest IP + 40% of avg of top 3. */
    public static int systemRisk(List<Integer> ipScores) {
        if (ipScores.isEmpty()) return 0;
        List<Integer> sorted = ipScores.stream().sorted((a, b) -> b - a).toList();
        int highest = sorted.get(0);
        double avgTop3 = sorted.stream().limit(3).mapToInt(Integer::intValue).average().orElse(0);
        return (int) Math.min(Defaults.RISK_MAX,
            Defaults.RISK_TOP_WEIGHT * highest + Defaults.RISK_AVG_WEIGHT * avgTop3);
    }

    /** Convert numeric score to level string. */
    public static String level(int score) {
        if (score >= 80) return "CRITICAL";
        if (score >= 60) return "HIGH";
        if (score >= 40) return "MEDIUM";
        if (score >= 20) return "LOW";
        return "NORMAL";
    }
}
