package com.sentinel;

/** Represents an attack scenario that can be simulated. */
public interface AttackScenario {
    /** Short scenario ID, e.g. "BRUTE_FORCE". */
    String id();
    /** Human-readable name. */
    String name();
    /** Run the scenario; feeds events into engine for given duration. */
    void run(DetectionEngine engine, int eventsPerSecond, long durationMs) throws InterruptedException;
}
