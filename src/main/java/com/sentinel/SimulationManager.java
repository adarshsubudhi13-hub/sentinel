package com.sentinel;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Executes scripted attack simulations or runs custom scenarios asynchronously.
 */
public final class SimulationManager {
    private final DetectionEngine engine;
    private final Scorecard scorecard;
    private final DefenseMode defense;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<String> currentScenario = new AtomicReference<>("IDLE");
    private volatile Thread workerThread;

    public SimulationManager(DetectionEngine engine, Scorecard scorecard, DefenseMode defense) {
        this.engine = engine;
        this.scorecard = scorecard;
        this.defense = defense;
    }

    public boolean isRunning() {
        return running.get();
    }

    public String getCurrentScenario() {
        return currentScenario.get();
    }

    public synchronized boolean start(String scenarioId, int eps, long durationMs) {
        if (!running.compareAndSet(false, true)) return false;
        currentScenario.set(scenarioId);
        scorecard.startRun(1);

        AttackScenario scenario = findScenario(scenarioId);
        workerThread = new Thread(() -> {
            try {
                scenario.run(engine, eps, durationMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                running.set(false);
                currentScenario.set("IDLE");
            }
        }, "simulation-runner");
        workerThread.setDaemon(true);
        workerThread.start();
        return true;
    }

    public synchronized void stop() {
        if (workerThread != null) {
            workerThread.interrupt();
        }
        running.set(false);
        currentScenario.set("IDLE");
    }

    /** 90-second Demo Mode: baseline -> spray -> brute force -> compromise -> auto-block -> summary */
    public synchronized boolean startDemoMode() {
        if (!running.compareAndSet(false, true)) return false;
        currentScenario.set("DEMO_MODE");
        scorecard.startRun(4);

        workerThread = new Thread(() -> {
            try {
                // 1. Baseline normal traffic (15s)
                new Scenarios.Normal().run(engine, 2, 15_000L);
                if (Thread.interrupted()) return;

                // 2. Credential Spray (15s)
                new Scenarios.Spray().run(engine, 3, 15_000L);
                if (Thread.interrupted()) return;

                // 3. Brute Force (15s)
                new Scenarios.BruteForce().run(engine, 5, 15_000L);
                if (Thread.interrupted()) return;

                // 4. Compromise (10s)
                new Scenarios.Compromise().run(engine, 2, 10_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                running.set(false);
                currentScenario.set("IDLE");
            }
        }, "demo-runner");
        workerThread.setDaemon(true);
        workerThread.start();
        return true;
    }

    private AttackScenario findScenario(String id) {
        for (AttackScenario s : Scenarios.buildAll()) {
            if (s.id().equalsIgnoreCase(id)) return s;
        }
        return new Scenarios.BruteForce();
    }
}
