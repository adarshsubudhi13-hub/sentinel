package com.sentinel;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test running Sentinel against a stub DemoBank server adhering to Part A integration contract.
 */
public class EndToEndIntegrationTest {
    private HttpServer stubBankServer;
    private Path tempLog;
    private final Set<String> blockedIps = ConcurrentHashMap.newKeySet();
    private BufferedWriter logWriter;

    private DetectionEngine engine;
    private LogTailer logTailer;
    private TargetClient targetClient;
    private DefenseClient defenseClient;
    private DefenseMode defenseMode;
    private Scorecard scorecard;

    @BeforeEach
    void setUp() throws IOException {
        tempLog = Files.createTempFile("stub_auth", ".log");
        logWriter = new BufferedWriter(new FileWriter(tempLog.toFile(), true));

        // Start stub DemoBank at 127.0.0.1:9090
        stubBankServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 9090), 0);
        stubBankServer.createContext("/health", ex -> send(ex, 200, "{\"status\":\"ok\"}"));
        stubBankServer.createContext("/api/login", ex -> {
            String ip = ex.getRequestHeaders().getFirst("X-Forwarded-For");
            if (ip == null) ip = "127.0.0.1";
            if (blockedIps.contains(ip)) {
                writeLog(ip, "admin", false, "IP_BLOCKED", 403);
                send(ex, 403, "{\"error\":\"IP_BLOCKED\"}");
                return;
            }
            writeLog(ip, "admin", false, "BAD_PASSWORD", 401);
            send(ex, 401, "{\"error\":\"BAD_PASSWORD\"}");
        });
        stubBankServer.createContext("/admin/block", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (body.contains("203.0.113.88")) blockedIps.add("203.0.113.88");
            send(ex, 200, "{\"changed\":true}");
        });
        stubBankServer.createContext("/admin/reset", ex -> {
            blockedIps.clear();
            send(ex, 200, "{\"reset\":true}");
        });
        stubBankServer.start();

        scorecard = new Scorecard();
        defenseClient = new DefenseClient("http://127.0.0.1:9090", "demo-admin-token");
        defenseMode = new DefenseMode();
        defenseMode.setDefenseClient(defenseClient);
        targetClient = new TargetClient("http://127.0.0.1:9090");

        List<DetectionRule> rules = Rules.buildAll();
        engine = new DetectionEngine(rules, new InMemoryStorageService(), defenseMode, scorecard);
        engine.start();

        LogParser logParser = new LogParser();
        logTailer = new LogTailer(tempLog.toString(), logParser, engine);
        logTailer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (logTailer != null) logTailer.stop();
        if (engine != null) engine.stop();
        if (stubBankServer != null) stubBankServer.stop(0);
        if (logWriter != null) logWriter.close();
        if (tempLog != null) Files.deleteIfExists(tempLog);
    }

    private synchronized void writeLog(String ip, String user, boolean success, String reason, int status) {
        try {
            String line = String.format("{\"ts\":\"%s\",\"ip\":\"%s\",\"username\":\"%s\",\"success\":%b,\"reason\":\"%s\",\"status\":%d}",
                Instant.now().toString(), ip, user, success, reason, status);
            logWriter.write(line);
            logWriter.newLine();
            logWriter.flush();
        } catch (IOException ignored) {}
    }

    private void send(HttpExchange ex, int code, String json) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(b); }
    }

    @Test
    void testEndToEndBruteForceDetectionAndAutonomousDefense() throws Exception {
        String attackerIp = "203.0.113.88";
        defenseMode.setEnabled(true);

        // 1. Send 5 failed login attempts to stub DemoBank
        for (int i = 0; i < 5; i++) {
            int code = targetClient.sendLogin("admin", "wrong_pass_" + i, attackerIp);
            assertEquals(401, code);
        }

        // Wait for log tailer to process lines from disk
        Thread.sleep(600);

        // Verify Sentinel detected R1 Brute Force
        assertTrue(engine.getTotalAlerts() >= 1, "R1 alert should be generated");
        assertTrue(engine.getSystemRisk() >= 40, "Risk score should rise");

        // 2. Push risk score to trigger auto-block
        for (int i = 5; i < 10; i++) {
            targetClient.sendLogin("admin", "wrong_pass_" + i, attackerIp);
        }
        Thread.sleep(600);

        // Attacker IP should now be blocked
        assertTrue(blockedIps.contains(attackerIp), "DemoBank stub should have received block request");

        // 3. Next attempt gets 403 Forbidden
        int codeAfterBlock = targetClient.sendLogin("admin", "any_pass", attackerIp);
        assertEquals(403, codeAfterBlock);

        Thread.sleep(400);

        // Verify R6 Blocked IP Activity triggered
        assertTrue(engine.getTotalAlerts() >= 2, "R6 Blocked IP Activity alert should be generated");

        // 4. Test Reset
        defenseClient.resetTarget();
        assertFalse(blockedIps.contains(attackerIp));
    }
}
