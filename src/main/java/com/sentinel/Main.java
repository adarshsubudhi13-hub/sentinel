package com.sentinel;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Main application bootstrap for Sentinel connecting to DemoBank target.
 */
public final class Main {
    public static void main(String[] args) throws IOException {
        new File(Defaults.DATA_DIR).mkdirs();

        ConfigLoader configLoader = new ConfigLoader();
        try {
            configLoader.load("data/config.properties");
        } catch (Exception e) {
            System.err.println("Note: Using default target configuration: " + e.getMessage());
        }

        TargetClient targetClient = new TargetClient(configLoader.getTargetBaseUrl());
        DefenseClient defenseClient = new DefenseClient(configLoader.getTargetBaseUrl(), configLoader.getTargetAdminToken());

        DefenseMode defense = new DefenseMode();
        defense.setDefenseClient(defenseClient);

        Scorecard scorecard = new Scorecard();
        StorageService storage = new FileStorageService();
        AuthService auth = new AuthService();
        SseBroadcaster sse = new SseBroadcaster();

        List<DetectionRule> rules = Rules.buildAll();
        DetectionEngine engine = new DetectionEngine(rules, storage, defense, scorecard);
        engine.addListener(new Listeners.ConsoleAlertListener());
        engine.addListener(sse);

        // JDBC SQLite Persistence Listener
        com.sentinel.database.JdbcAlertRepository jdbcRepo = new com.sentinel.database.JdbcAlertRepository(com.sentinel.database.DatabaseManager.getInstance());
        engine.addListener(new Listeners.JdbcAlertListener(jdbcRepo));

        engine.start();

        LogParser logParser = new LogParser();
        LogTailer logTailer = new LogTailer(configLoader.getTargetLogPath(), logParser, engine);
        logTailer.start();

        TargetHealthMonitor targetHealth = new TargetHealthMonitor(configLoader.getTargetBaseUrl() + "/health");
        targetHealth.start();

        SimulationManager simulation = new SimulationManager(engine, scorecard, defense);
        ReplayEngine replay = new ReplayEngine();
        ChallengeMode challenge = new ChallengeMode();

        replay.addListener((entry, done) -> {
            sse.broadcast("replay", String.format("{\"type\":\"%s\",\"actor\":\"%s\",\"detail\":\"%s\",\"done\":%b}",
                entry.type(), entry.actor(), entry.detail(), done));
        });

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                int risk = engine.getSystemRisk();
                String data = String.format("{\"risk\":%d,\"level\":\"%s\",\"attempts\":%d,\"failures\":%d,\"alerts\":%d,\"targetConnected\":%b}",
                    risk, RiskScorer.level(risk), engine.getTotalAttempts(), engine.getTotalFailures(),
                    engine.getTotalAlerts(), targetHealth.isConnected());
                sse.broadcast("stats", data);
            } catch (Exception ignored) {}
        }, 1, 1, TimeUnit.SECONDS);

        int port = Defaults.PORT;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            try { port = Integer.parseInt(portEnv.trim()); } catch (NumberFormatException ignored) {}
        }

        Router router = new Router(engine, auth, defense, scorecard, simulation, replay,
            challenge, storage, sse, targetHealth, targetClient, defenseClient, configLoader);
        ApiServer server = new ApiServer(port, router);
        server.start();

        System.out.println("=================================================");
        System.out.println("  Sentinel — Cyberattack Simulation & Detection Lab");
        System.out.println("  Server running on port: " + port);
        System.out.println("  Monitoring DemoBank at: " + configLoader.getTargetBaseUrl());
        System.out.println("  JDBC Database: data/sentinel.db");
        System.out.println("=================================================");

        // Launch Java Swing GUI by default unless explicitly running headless
        boolean headless = java.awt.GraphicsEnvironment.isHeadless();
        for (String arg : args) {
            if ("--headless".equalsIgnoreCase(arg) || "--server-only".equalsIgnoreCase(arg)) {
                headless = true;
                break;
            }
        }
        if (!headless) {
            try {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    com.sentinel.gui.SentinelSwingApp gui = new com.sentinel.gui.SentinelSwingApp();
                    gui.setVisible(true);
                });
            } catch (Throwable t) {
                System.err.println("Note: Swing GUI initialization skipped: " + t.getMessage());
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Sentinel...");
            server.stop();
            engine.stop();
            simulation.stop();
            replay.stop();
            logTailer.stop();
            targetHealth.stop();
            scheduler.shutdownNow();
        }));
    }
}
