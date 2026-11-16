package com.sentinel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DetectionRulesTest {
    private SlidingWindow window;
    private IpStats stats;

    @BeforeEach
    void setup() {
        window = new SlidingWindow(60_000L);
        stats = new IpStats("192.168.1.50");
    }

    @Test
    void testBruteForceThresholds() {
        DetectionRule rule = new Rules.BruteForce();
        // Below threshold (4 failures)
        for (int i = 0; i < 4; i++) {
            LoginEvent ev = new LoginEvent(IdGen.next(), Instant.now(), "192.168.1.50", "alice", false);
            window.add(ev);
            stats.failures.incrementAndGet();
            assertNull(rule.evaluate(ev, stats, window));
        }

        // At threshold (5 failures)
        LoginEvent fifth = new LoginEvent(IdGen.next(), Instant.now(), "192.168.1.50", "alice", false);
        window.add(fifth);
        stats.failures.incrementAndGet();
        Alert alert = rule.evaluate(fifth, stats, window);
        assertNotNull(alert);
        assertEquals("R1", alert.ruleId());
        assertEquals("T1110", alert.mitreTag());
        assertEquals("HIGH", alert.severity());
    }

    @Test
    void testSprayRuleThresholds() {
        DetectionRule rule = new Rules.Spray();
        String[] users = {"alice", "bob", "carol"};

        // 2 distinct users -> null
        for (int i = 0; i < 2; i++) {
            LoginEvent ev = new LoginEvent(IdGen.next(), Instant.now(), "192.168.1.50", users[i], false);
            window.add(ev);
            assertNull(rule.evaluate(ev, stats, window));
        }

        // 3 distinct users -> triggers R2
        LoginEvent third = new LoginEvent(IdGen.next(), Instant.now(), "192.168.1.50", users[2], false);
        window.add(third);
        Alert alert = rule.evaluate(third, stats, window);
        assertNotNull(alert);
        assertEquals("R2", alert.ruleId());
        assertEquals("T1110.003", alert.mitreTag());
    }

    @Test
    void testSuccessAfterFailureCompromise() {
        DetectionRule rule = new Rules.SuccessAfterFailure();

        // Direct success without failures -> null
        LoginEvent successDirect = new LoginEvent(IdGen.next(), Instant.now(), "192.168.1.50", "alice", true);
        assertNull(rule.evaluate(successDirect, stats, window));

        // After failure -> CRITICAL alert
        stats.failures.set(3);
        LoginEvent successAfterFail = new LoginEvent(IdGen.next(), Instant.now(), "192.168.1.50", "alice", true);
        Alert alert = rule.evaluate(successAfterFail, stats, window);
        assertNotNull(alert);
        assertEquals("R4", alert.ruleId());
        assertEquals("CRITICAL", alert.severity());
        assertEquals("T1078", alert.mitreTag());
    }

    @Test
    void testSlidingWindowExpiry() throws InterruptedException {
        SlidingWindow smallWindow = new SlidingWindow(100L); // 100ms
        LoginEvent ev = new LoginEvent(IdGen.next(), Instant.now(), "10.0.0.1", "bob", false);
        smallWindow.add(ev);
        assertEquals(1, smallWindow.size());

        Thread.sleep(150);
        assertEquals(0, smallWindow.size());
    }
}
