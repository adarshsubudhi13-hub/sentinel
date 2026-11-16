package com.sentinel;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class TargetIntegrationTest {

    @Test
    void testTargetAllowlistStrictness() {
        assertDoesNotThrow(() -> TargetClient.validateTarget("http://127.0.0.1:9090"));
        assertThrows(SentinelException.InvalidTargetException.class,
            () -> TargetClient.validateTarget("http://google.com"));
        assertThrows(SentinelException.InvalidTargetException.class,
            () -> TargetClient.validateTarget("http://192.168.1.100:9090"));
        assertThrows(SentinelException.InvalidTargetException.class,
            () -> TargetClient.validateTarget("http://10.0.0.1:8080"));
    }

    @Test
    void testLogParserValidMalformedAndIncomplete() {
        LogParser parser = new LogParser();

        // 1. Valid line
        String validLine = "{\"ts\":\"2026-11-09T10:15:30.123Z\",\"ip\":\"203.0.113.45\",\"username\":\"admin\",\"success\":false,\"reason\":\"BAD_PASSWORD\",\"status\":401}";
        Optional<LoginEvent> ev1 = parser.parseLine(validLine);
        assertTrue(ev1.isPresent());
        assertEquals("203.0.113.45", ev1.get().ip());
        assertEquals("admin", ev1.get().username());
        assertFalse(ev1.get().success());
        assertEquals(401, ev1.get().status());
        assertEquals("BAD_PASSWORD", ev1.get().reason());

        // 2. Malformed JSON
        String malformed = "{ this is not json }";
        Optional<LoginEvent> ev2 = parser.parseLine(malformed);
        assertTrue(ev2.isEmpty());
        assertTrue(parser.getMalformedCount() >= 1);

        // 3. Half-written / missing fields
        String halfWritten = "{\"ts\":\"2026-11-09T10:15:30.123Z\",\"ip\":\"203.0.113.45\"}";
        Optional<LoginEvent> ev3 = parser.parseLine(halfWritten);
        assertTrue(ev3.isEmpty());
    }

    @Test
    void testConfigValidation() {
        ConfigLoader loader = new ConfigLoader();
        assertDoesNotThrow(loader::validate);
        assertEquals("http://127.0.0.1:9090", loader.getTargetBaseUrl());
        assertEquals("demo-admin-token", loader.getTargetAdminToken());
    }

    @Test
    void testBlockedIpRuleR6() {
        DetectionRule rule = new Rules.BlockedIpAccess();
        IpStats stats = new IpStats("203.0.113.99");
        SlidingWindow window = new SlidingWindow(60_000L);

        // Non-blocked -> null
        LoginEvent normal = new LoginEvent(IdGen.next(), Instant.now(), "203.0.113.99", "admin", false, "BAD_PASSWORD", 401, null);
        assertNull(rule.evaluate(normal, stats, window));

        // Blocked attempt -> R6 alert
        LoginEvent blocked = new LoginEvent(IdGen.next(), Instant.now(), "203.0.113.99", "admin", false, "IP_BLOCKED", 403, null);
        Alert a = rule.evaluate(blocked, stats, window);
        assertNotNull(a);
        assertEquals("R6", a.ruleId());
        assertEquals("MEDIUM", a.severity());
        assertEquals("T1110", a.mitreTag());
    }

    @Test
    void testRequestFloodRuleR5() {
        DetectionRule rule = new Rules.Flood();
        IpStats stats = new IpStats("203.0.113.50");
        SlidingWindow window = new SlidingWindow(60_000L);

        for (int i = 0; i < 25; i++) {
            LoginEvent ev = new LoginEvent(IdGen.next(), Instant.now(), "203.0.113.50", "guest", false);
            window.add(ev);
        }

        LoginEvent trigger = new LoginEvent(IdGen.next(), Instant.now(), "203.0.113.50", "guest", false);
        Alert a = rule.evaluate(trigger, stats, window);
        assertNotNull(a);
        assertEquals("R5", a.ruleId());
        assertEquals("T1499", a.mitreTag());
    }
}
