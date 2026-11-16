package com.sentinel;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RiskAndWinningFeaturesTest {

    @Test
    void testRiskScorerFormulas() {
        IpStats stats = new IpStats("10.0.0.1");
        stats.failures.set(5); // 5 * 8 = 40
        stats.distinct.set(2); // 2 * 6 = 12
        stats.compromised = true; // + 30
        stats.blocked = false;

        int score = RiskScorer.scoreIp(stats, List.of());
        assertEquals(82, score);
        assertEquals("CRITICAL", RiskScorer.level(score));

        // System risk: 60% highest + 40% avg top 3
        // 80, 50, 20 -> avg top 3 = 50. 0.6*80 + 0.4*50 = 48 + 20 = 68
        int sysRisk = RiskScorer.systemRisk(List.of(80, 50, 20, 10));
        assertEquals(68, sysRisk);
    }

    @Test
    void testDefenseModeAutoBlock() {
        DefenseMode defense = new DefenseMode();
        assertFalse(defense.isEnabled());

        defense.setEnabled(true);
        assertTrue(defense.isEnabled());

        defense.recordBlock("192.168.1.99");
        defense.recordBlock("192.168.1.99"); // duplicate block
        assertEquals(1, defense.getBlocksIssued());
        assertTrue(defense.getBlockedIps().contains("192.168.1.99"));
    }

    @Test
    void testScorecardGrading() {
        Scorecard scorecard = new Scorecard();
        scorecard.startRun(10);

        // Grade with 0 detected
        assertEquals("F", scorecard.grade());

        // 10 alerts recorded, 0 false positives
        for (int i = 0; i < 10; i++) {
            scorecard.recordAlert(new Alert("id" + i, java.time.Instant.now(), "R1", "Brute Force",
                "1.1.1.1", "u", "HIGH", "T1110", "BF", "why", "defend", "OPEN"));
        }
        assertEquals("A", scorecard.grade());
    }

    @Test
    void testChallengeMode() {
        ChallengeMode mode = new ChallengeMode();
        mode.start();
        assertTrue(mode.isActive());

        mode.recordManualBlock(true);
        mode.recordManualBlock(false);
        assertEquals(2, mode.getManualBlocks());
        assertEquals(1, mode.getCorrectBlocks());

        mode.recordCompromise();
        mode.recordCompromise();
        mode.recordCompromise(); // 3rd compromise triggers stop
        assertFalse(mode.isActive());
        assertEquals(0, mode.calculateScore()); // failed challenge
    }

    @Test
    void testStorageRoundTrip() {
        InMemoryStorageService memory = new InMemoryStorageService();
        Alert alert = new Alert("a1", java.time.Instant.now(), "R1", "Brute Force",
            "10.0.0.1", "admin", "HIGH", "T1110", "BF", "why", "defend", "OPEN");

        memory.saveAlert(alert);
        List<Alert> loaded = memory.loadAlerts();
        assertEquals(1, loaded.size());
        assertEquals("a1", loaded.get(0).id());
    }
}
