package com.sentinel;

import java.time.Instant;
import java.util.List;
import java.util.Random;

/**
 * All AttackScenario implementations sending real HTTP traffic to DemoBank via TargetClient.
 */
public final class Scenarios {

    private Scenarios() {}

    private static final String[] USERS = {"alice", "bob", "charlie", "admin", "guest", "root", "test", "user", "support", "oracle"};
    private static final String[] PASSWORDS = {"123456", "password", "qwerty", "letmein", "admin", "welcome", "iloveyou", "12345678", "abc123", "password1"};
    private static final TargetClient CLIENT = new TargetClient();

    public static List<AttackScenario> buildAll() {
        return List.of(new BruteForce(), new Spray(), new Distributed(),
                       new Flood(), new Compromise(), new Mixed(), new Normal());
    }

    private static void dispatch(DetectionEngine engine, String ip, String user, String pass, boolean expectSuccess) {
        int status = CLIENT.sendLogin(user, pass, ip);
        if (status == 0) {
            // Target offline or unit test fallback
            engine.submit(new LoginEvent(IdGen.next(), Instant.now(), ip, user, expectSuccess,
                expectSuccess ? "NONE" : "BAD_PASSWORD", expectSuccess ? 200 : 401, null));
        }
    }

    // ── Brute Force: one IP, many passwords against one user ─────────────────

    public static final class BruteForce implements AttackScenario {
        @Override public String id() { return "BRUTE_FORCE"; }
        @Override public String name() { return "Brute Force"; }

        @Override
        public void run(DetectionEngine engine, int eps, long durationMs) throws InterruptedException {
            String ip = "203.0.113.10";
            String user = "admin";
            long end = System.currentTimeMillis() + durationMs;
            long sleepMs = eps > 0 ? 1000L / eps : 200L;
            int idx = 0;
            while (System.currentTimeMillis() < end) {
                dispatch(engine, ip, user, PASSWORDS[idx++ % PASSWORDS.length], false);
                Thread.sleep(sleepMs);
            }
        }
    }

    // ── Credential Spray: one IP, common password, many users ────────────────

    public static final class Spray implements AttackScenario {
        @Override public String id() { return "SPRAY"; }
        @Override public String name() { return "Credential Spray"; }

        @Override
        public void run(DetectionEngine engine, int eps, long durationMs) throws InterruptedException {
            String ip = "203.0.113.20";
            long end = System.currentTimeMillis() + durationMs;
            long sleepMs = eps > 0 ? 1000L / eps : 200L;
            int idx = 0;
            while (System.currentTimeMillis() < end) {
                dispatch(engine, ip, USERS[idx++ % USERS.length], "123456", false);
                Thread.sleep(sleepMs);
            }
        }
    }

    // ── Distributed Attack: many IPs targeting single account ────────────────

    public static final class Distributed implements AttackScenario {
        @Override public String id() { return "DISTRIBUTED"; }
        @Override public String name() { return "Distributed Attack"; }

        @Override
        public void run(DetectionEngine engine, int eps, long durationMs) throws InterruptedException {
            String[] ips = {"203.0.113.31", "203.0.113.32", "203.0.113.33", "198.51.100.34", "198.51.100.35"};
            String user = "admin";
            long end = System.currentTimeMillis() + durationMs;
            long sleepMs = eps > 0 ? 1000L / eps : 200L;
            int idx = 0;
            while (System.currentTimeMillis() < end) {
                String ip = ips[idx % ips.length];
                dispatch(engine, ip, user, PASSWORDS[idx % PASSWORDS.length], false);
                idx++;
                Thread.sleep(sleepMs);
            }
        }
    }

    // ── Compromise: failures followed by valid credentials ───────────────────

    public static final class Compromise implements AttackScenario {
        @Override public String id() { return "COMPROMISE"; }
        @Override public String name() { return "Compromise Sequence"; }

        @Override
        public void run(DetectionEngine engine, int eps, long durationMs) throws InterruptedException {
            String ip = "203.0.113.40";
            String user = "admin";
            // 1. Failures
            for (int i = 0; i < 4; i++) {
                dispatch(engine, ip, user, "badpass_" + i, false);
                Thread.sleep(eps > 0 ? 1000L / eps : 200L);
            }
            // 2. Real admin password
            dispatch(engine, ip, user, "Adm1n$ecure", true);
            Thread.sleep(1000);
        }
    }

    // ── Request Flood: high rate bursts ──────────────────────────────────────

    public static final class Flood implements AttackScenario {
        @Override public String id() { return "FLOOD"; }
        @Override public String name() { return "Request Flood"; }

        @Override
        public void run(DetectionEngine engine, int eps, long durationMs) throws InterruptedException {
            String ip = "203.0.113.50";
            String user = "guest";
            long end = System.currentTimeMillis() + durationMs;
            long sleepMs = Math.max(20L, 1000L / Math.max(eps, 25));
            while (System.currentTimeMillis() < end) {
                dispatch(engine, ip, user, "floodpass", false);
                Thread.sleep(sleepMs);
            }
        }
    }

    // ── Mixed Chaos: Normal traffic + concurrent attack vectors ──────────────

    public static final class Mixed implements AttackScenario {
        @Override public String id() { return "MIXED"; }
        @Override public String name() { return "Mixed Chaos"; }

        @Override
        public void run(DetectionEngine engine, int eps, long durationMs) throws InterruptedException {
            long end = System.currentTimeMillis() + durationMs;
            Thread normal = new Thread(() -> {
                try { new Normal().run(engine, 2, durationMs); } catch (InterruptedException ignored) {}
            });
            Thread brute = new Thread(() -> {
                try { new BruteForce().run(engine, eps, durationMs); } catch (InterruptedException ignored) {}
            });
            Thread spray = new Thread(() -> {
                try { new Spray().run(engine, eps, durationMs); } catch (InterruptedException ignored) {}
            });
            normal.start();
            brute.start();
            spray.start();
            normal.join(durationMs);
            brute.join(durationMs);
            spray.join(durationMs);
        }
    }

    // ── Normal Traffic: Benign user logins ───────────────────────────────────

    public static final class Normal implements AttackScenario {
        @Override public String id() { return "NORMAL"; }
        @Override public String name() { return "Normal Traffic"; }

        @Override
        public void run(DetectionEngine engine, int eps, long durationMs) throws InterruptedException {
            String[][] legit = {
                {"192.0.2.1", "alice", "Wonder@2024"},
                {"192.0.2.2", "bob", "Builder#77"},
                {"192.0.2.3", "charlie", "Choc0late!"},
                {"192.0.2.4", "guest", "guest123"}
            };
            long end = System.currentTimeMillis() + durationMs;
            long sleepMs = eps > 0 ? 1000L / eps : 500L;
            int idx = 0;
            Random rnd = new Random();
            while (System.currentTimeMillis() < end) {
                String[] creds = legit[idx++ % legit.length];
                boolean typo = (rnd.nextInt(10) == 0);
                String pass = typo ? "TypoPass" : creds[2];
                dispatch(engine, creds[0], creds[1], pass, !typo);
                Thread.sleep(sleepMs);
            }
        }
    }
}
