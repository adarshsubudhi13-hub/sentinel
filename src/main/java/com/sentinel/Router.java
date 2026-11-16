package com.sentinel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Single HTTP Router dispatching REST API routes and serving static web assets.
 */
public final class Router {
    private final DetectionEngine engine;
    private final AuthService auth;
    private final DefenseMode defense;
    private final Scorecard scorecard;
    private final SimulationManager simulation;
    private final ReplayEngine replay;
    private final ChallengeMode challenge;
    private final StorageService storage;
    private final SseBroadcaster sse;
    private final TargetHealthMonitor targetHealth;
    private final TargetClient targetClient;
    private final DefenseClient defenseClient;
    private final ConfigLoader configLoader;

    private final Gson gson = new com.google.gson.GsonBuilder()
        .registerTypeAdapter(java.time.Instant.class,
            (com.google.gson.JsonSerializer<java.time.Instant>) (src, t, ctx) -> ctx.serialize(src.toString()))
        .registerTypeAdapter(java.time.Instant.class,
            (com.google.gson.JsonDeserializer<java.time.Instant>) (json, t, ctx) -> {
                try { return java.time.Instant.parse(json.getAsString()); }
                catch (Exception e) { return java.time.Instant.ofEpochMilli(json.getAsLong()); }
            })
        .create();

    public Router(DetectionEngine engine, AuthService auth, DefenseMode defense,
                  Scorecard scorecard, SimulationManager simulation, ReplayEngine replay,
                  ChallengeMode challenge, StorageService storage, SseBroadcaster sse,
                  TargetHealthMonitor targetHealth, TargetClient targetClient,
                  DefenseClient defenseClient, ConfigLoader configLoader) {
        this.engine = engine;
        this.auth = auth;
        this.defense = defense;
        this.scorecard = scorecard;
        this.simulation = simulation;
        this.replay = replay;
        this.challenge = challenge;
        this.storage = storage;
        this.sse = sse;
        this.targetHealth = targetHealth;
        this.targetClient = targetClient;
        this.defenseClient = defenseClient;
        this.configLoader = configLoader;
    }

    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.startsWith("/api/")) {
                handleApi(exchange, path, method);
            } else {
                handleStatic(exchange, path);
            }
        } catch (SentinelException e) {
            sendJson(exchange, e.getHttpStatus(), Map.of("error", e.getErrorCode(), "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, Map.of("error", "BAD_REQUEST", "message", e.getMessage()));
        } catch (NoSuchElementException e) {
            sendJson(exchange, 404, Map.of("error", "NOT_FOUND", "message", e.getMessage()));
        } catch (IllegalStateException e) {
            sendJson(exchange, 409, Map.of("error", "CONFLICT", "message", e.getMessage()));
        } catch (Exception e) {
            sendJson(exchange, 500, Map.of("error", "INTERNAL_ERROR", "message", "Internal server error: " + e.getMessage()));
        }
    }

    private void handleApi(HttpExchange exchange, String path, String method) throws Exception {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        switch (path) {
            case "/api/stream" -> sse.addClient(exchange);
            case "/api/stats" -> handleStats(exchange);
            case "/api/target/health" -> handleTargetHealth(exchange);
            case "/api/login" -> handleLogin(exchange);
            case "/api/alerts" -> handleAlerts(exchange);
            case "/api/timeline" -> handleTimeline(exchange);
            case "/api/ips" -> handleIps(exchange, method);
            case "/api/simulation/start" -> handleSimulationStart(exchange);
            case "/api/simulation/stop" -> handleSimulationStop(exchange);
            case "/api/simulation/demo", "/api/demo/start" -> handleSimulationDemo(exchange);
            case "/api/simulation/status" -> handleSimulationStatus(exchange);
            case "/api/defense" -> handleDefense(exchange, method);
            case "/api/scorecard" -> handleScorecard(exchange);
            case "/api/replay", "/api/replay/start" -> handleReplay(exchange, method);
            case "/api/challenge", "/api/challenge/start" -> handleChallenge(exchange, method);
            case "/api/reset", "/api/simulation/reset" -> handleReset(exchange);
            case "/api/report/txt" -> handleReport(exchange, "text/plain");
            case "/api/report/html", "/api/export/incident-report" -> handleReport(exchange, "text/html");
            case "/api/export/alerts.csv" -> handleExportCsv(exchange);
            case "/api/config" -> handleConfig(exchange, method);
            default -> sendJson(exchange, 404, Map.of("error", "NOT_FOUND", "message", "Endpoint not found: " + path));
        }
    }

    private void handleTargetHealth(HttpExchange exchange) throws IOException {
        boolean connected = targetHealth != null && targetHealth.isConnected();
        sendJson(exchange, 200, Map.of(
            "connected", connected,
            "target", targetClient != null ? targetClient.getBaseUrl() : "http://127.0.0.1:9090",
            "status", connected ? "DemoBank Connected" : "DemoBank Unreachable"
        ));
    }

    private void handleSimulationStatus(HttpExchange exchange) throws IOException {
        sendJson(exchange, 200, Map.of(
            "running", simulation.isRunning(),
            "scenario", simulation.getCurrentScenario(),
            "targetConnected", targetHealth != null && targetHealth.isConnected()
        ));
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAttempts", engine.getTotalAttempts());
        stats.put("totalSuccesses", engine.getTotalSuccesses());
        stats.put("totalFailures", engine.getTotalFailures());
        stats.put("totalAlerts", engine.getTotalAlerts());
        stats.put("systemRisk", engine.getSystemRisk());
        stats.put("riskLevel", RiskScorer.level(engine.getSystemRisk()));
        stats.put("defenseEnabled", defense.isEnabled());
        stats.put("simulationRunning", simulation.isRunning());
        stats.put("activeScenario", simulation.getCurrentScenario());
        stats.put("scorecardGrade", scorecard.grade());
        stats.put("targetConnected", targetHealth != null && targetHealth.isConnected());
        sendJson(exchange, 200, stats);
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject json = gson.fromJson(body, JsonObject.class);
        if (json == null || !json.has("username") || !json.has("password")) {
            throw new IllegalArgumentException("Missing username or password");
        }
        String user = json.get("username").getAsString();
        String pass = json.get("password").getAsString();
        String ip = "192.0.2.1"; // Simulated client IP for playground

        // Route through DemoBank if connected
        int httpStatus = 0;
        if (targetClient != null) {
            httpStatus = targetClient.sendLogin(user, pass, ip);
        }

        boolean ok;
        if (httpStatus > 0) {
            ok = (httpStatus == 200);
        } else {
            // Standalone fallback
            ok = auth.verify(user, pass);
            LoginEvent event = new LoginEvent(IdGen.next(), java.time.Instant.now(), ip, user, ok);
            engine.submit(event);
        }

        sendJson(exchange, 200, Map.of(
            "success", ok,
            "status", httpStatus > 0 ? httpStatus : (ok ? 200 : 401),
            "reason", ok ? "SUCCESS" : "INVALID_CREDENTIALS"
        ));
    }

    private void handleAlerts(HttpExchange exchange) throws IOException {
        sendJson(exchange, 200, storage.loadAlerts());
    }

    private void handleTimeline(HttpExchange exchange) throws IOException {
        sendJson(exchange, 200, engine.getTimeline());
    }

    private void handleIps(HttpExchange exchange, String method) throws IOException {
        if ("POST".equalsIgnoreCase(method)) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            String ip = json.get("ip").getAsString();
            boolean block = !json.has("block") || json.get("block").getAsBoolean();
            if (block) {
                engine.blockIpManual(ip);
                if (defenseClient != null) defenseClient.blockIp(ip);
                if (challenge.isActive()) challenge.recordManualBlock(true);
            } else {
                engine.unblockIp(ip);
                if (defenseClient != null) defenseClient.unblockIp(ip);
            }
            sendJson(exchange, 200, Map.of("status", "ok", "ip", ip, "blocked", block));
            return;
        }
        sendJson(exchange, 200, engine.getIpRegistry().values());
    }

    private void handleSimulationStart(HttpExchange exchange) throws IOException {
        if (targetHealth != null && !targetHealth.isConnected()) {
            throw new SentinelException.TargetUnreachableException(
                "DemoBank is unreachable at 127.0.0.1:9090. Please launch DemoBank first to enable real attack telemetry.");
        }

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject json = gson.fromJson(body, JsonObject.class);
        String scenario = json != null && json.has("scenario") ? json.get("scenario").getAsString() : "BRUTE_FORCE";
        int eps = json != null && json.has("eps") ? json.get("eps").getAsInt() : 5;
        long duration = json != null && json.has("duration") ? json.get("duration").getAsLong() : 15000L;

        boolean started = simulation.start(scenario, eps, duration);
        if (!started) throw new SentinelException.SimulationAlreadyRunningException();
        sendJson(exchange, 200, Map.of("status", "RUNNING", "scenario", scenario));
    }

    private void handleSimulationStop(HttpExchange exchange) throws IOException {
        simulation.stop();
        sendJson(exchange, 200, Map.of("status", "STOPPED"));
    }

    private void handleSimulationDemo(HttpExchange exchange) throws IOException {
        if (targetHealth != null && !targetHealth.isConnected()) {
            throw new SentinelException.TargetUnreachableException(
                "DemoBank is unreachable at 127.0.0.1:9090. Please launch DemoBank first to enable real attack telemetry.");
        }
        boolean started = simulation.startDemoMode();
        if (!started) throw new SentinelException.SimulationAlreadyRunningException();
        sendJson(exchange, 200, Map.of("status", "DEMO_RUNNING"));
    }

    private void handleDefense(HttpExchange exchange, String method) throws IOException {
        if ("POST".equalsIgnoreCase(method)) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            boolean on = json.get("enabled").getAsBoolean();
            defense.setEnabled(on);
            sendJson(exchange, 200, Map.of("defenseEnabled", defense.isEnabled()));
            return;
        }
        sendJson(exchange, 200, Map.of(
            "enabled", defense.isEnabled(),
            "blocksIssued", defense.getBlocksIssued(),
            "blockedIps", defense.getBlockedIps()
        ));
    }

    private void handleScorecard(HttpExchange exchange) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("grade", scorecard.grade());
        map.put("attacksLaunched", scorecard.getAttacksLaunched());
        map.put("attacksDetected", scorecard.getAttacksDetected());
        map.put("falsePositives", scorecard.getFalsePositives());
        map.put("compromises", scorecard.getCompromises());
        map.put("timeToDetectMs", scorecard.getTimeToDetectMs());
        sendJson(exchange, 200, map);
    }

    private void handleReplay(HttpExchange exchange, String method) throws IOException {
        if ("POST".equalsIgnoreCase(method)) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            int speed = json != null && json.has("speed") ? json.get("speed").getAsInt() : 1;
            List<TimelineEntry> entries = storage.loadTimeline();
            replay.start(entries, speed);
            sendJson(exchange, 200, Map.of("status", "REPLAY_STARTED", "speed", speed, "entries", entries.size()));
            return;
        }
        sendJson(exchange, 200, Map.of("running", replay.isRunning()));
    }

    private void handleChallenge(HttpExchange exchange, String method) throws IOException {
        if ("POST".equalsIgnoreCase(method)) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            String action = json != null && json.has("action") ? json.get("action").getAsString() : "stop";
            if ("start".equalsIgnoreCase(action)) {
                challenge.start();
                simulation.start("MIXED", 3, 45000L);
            } else {
                challenge.stop();
                simulation.stop();
            }
        }
        sendJson(exchange, 200, Map.of(
            "active", challenge.isActive(),
            "compromises", challenge.getCompromises(),
            "manualBlocks", challenge.getManualBlocks(),
            "correctBlocks", challenge.getCorrectBlocks(),
            "score", challenge.calculateScore(),
            "durationSec", challenge.getDurationSeconds(),
            "won", challenge.isWon()
        ));
    }

    private void handleReset(HttpExchange exchange) throws IOException {
        simulation.stop();
        replay.stop();
        challenge.stop();
        engine.reset();
        if (defenseClient != null) {
            defenseClient.resetTarget();
        }
        sendJson(exchange, 200, Map.of("status", "RESET_COMPLETE"));
    }

    private void handleExportCsv(HttpExchange exchange) throws IOException {
        List<Alert> alerts = storage.loadAlerts();
        StringBuilder csv = new StringBuilder();
        csv.append("id,timestamp,ruleId,ruleName,severity,ip,username,mitreTag,status\n");
        for (Alert a : alerts) {
            csv.append(String.join(",",
                a.id(),
                a.timestamp().toString(),
                a.ruleId(),
                "\"" + a.ruleName() + "\"",
                a.severity(),
                a.ip(),
                a.username(),
                a.mitreTag(),
                a.status()
            )).append("\n");
        }
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=utf-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"alerts.csv\"");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private void handleConfig(HttpExchange exchange, String method) throws IOException {
        if ("PUT".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method)) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = gson.fromJson(body, JsonObject.class);
            if (json != null) {
                // Apply update validation
            }
        }
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("targetBaseUrl", configLoader != null ? configLoader.getTargetBaseUrl() : "http://127.0.0.1:9090");
        cfg.put("r1Threshold", configLoader != null ? configLoader.getR1Threshold() : Defaults.BRUTE_FORCE_THRESHOLD);
        cfg.put("r2Threshold", configLoader != null ? configLoader.getR2Threshold() : Defaults.SPRAY_THRESHOLD);
        cfg.put("r3Failures", configLoader != null ? configLoader.getR3Failures() : 8);
        cfg.put("r4PriorFailures", configLoader != null ? configLoader.getR4PriorFailures() : 3);
        cfg.put("r5Threshold", configLoader != null ? configLoader.getR5Threshold() : Defaults.FLOOD_THRESHOLD);
        cfg.put("cooldownSec", configLoader != null ? configLoader.getCooldownSec() : 30);
        cfg.put("autoBlockRiskThreshold", Defaults.DEFENSE_AUTO_BLOCK_THRESHOLD);
        sendJson(exchange, 200, cfg);
    }

    private void handleReport(HttpExchange exchange, String mime) throws IOException {
        List<Alert> alerts = storage.loadAlerts();
        List<TimelineEntry> timeline = engine.getTimeline();
        byte[] bytes = "text/html".equals(mime)
            ? IncidentReport.generateHtml(scorecard, defense, alerts, timeline).getBytes(StandardCharsets.UTF_8)
            : IncidentReport.generateText(scorecard, defense, alerts, timeline).getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set("Content-Type", mime + "; charset=utf-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"sentinel-incident-report." + ("text/html".equals(mime) ? "html" : "txt") + "\"");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private void handleStatic(HttpExchange exchange, String path) throws IOException {
        if ("/".equals(path) || path.isEmpty()) path = "/index.html";
        Path localPath = Path.of("web" + path).normalize();
        if (!localPath.startsWith(Path.of("web")) || !Files.exists(localPath)) {
            sendJson(exchange, 404, Map.of("error", "NOT_FOUND", "message", "Static file not found"));
            return;
        }

        String mime = "text/plain";
        if (path.endsWith(".html")) mime = "text/html";
        else if (path.endsWith(".css")) mime = "text/css";
        else if (path.endsWith(".js")) mime = "application/javascript";
        else if (path.endsWith(".json")) mime = "application/json";

        byte[] bytes = Files.readAllBytes(localPath);
        exchange.getResponseHeaders().set("Content-Type", mime + "; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private void sendJson(HttpExchange exchange, int status, Object data) throws IOException {
        byte[] bytes = gson.toJson(data).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}
