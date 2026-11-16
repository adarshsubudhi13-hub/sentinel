package com.sentinel;

/** All numeric thresholds and constants; no magic numbers elsewhere. */
public final class Defaults {
    private Defaults() {}

    // Detection thresholds
    public static final int BRUTE_FORCE_THRESHOLD = 5;
    public static final int SPRAY_THRESHOLD = 3;
    public static final int DISTRIBUTED_THRESHOLD = 10;
    public static final int FLOOD_THRESHOLD = 30;
    public static final int WINDOW_SECONDS = 60;

    // Risk scoring weights
    public static final int RISK_PER_FAILURE = 8;
    public static final int RISK_PER_USER = 6;
    public static final int RISK_PER_RATE = 2;
    public static final int RISK_COMPROMISE_BONUS = 30;
    public static final int RISK_BLOCKED_BONUS = 20;
    public static final double RISK_TOP_WEIGHT = 0.6;
    public static final double RISK_AVG_WEIGHT = 0.4;
    public static final int RISK_MAX = 100;

    // Defense mode
    public static final int DEFENSE_AUTO_BLOCK_THRESHOLD = 75;
    public static final long LOCKOUT_MILLIS = 60_000L;

    // Alert cooldown (ms per rule per IP)
    public static final long ALERT_COOLDOWN_MS = 15_000L;

    // SSE / server
    public static final int PORT = 8080;
    public static final int THREAD_POOL = 16;
    public static final long SSE_INTERVAL_MS = 1_000L;
    public static final long SSE_KEEPALIVE_MS = 15_000L;

    // Simulation intensities (events/sec)
    public static final int INTENSITY_LOW = 2;
    public static final int INTENSITY_MEDIUM = 5;
    public static final int INTENSITY_HIGH = 15;

    // Replay
    public static final int REPLAY_SPEED_1X = 1;
    public static final int REPLAY_SPEED_5X = 5;
    public static final int REPLAY_SPEED_10X = 10;

    // Challenge mode
    public static final int CHALLENGE_MAX_COMPROMISES = 3;

    // File paths
    public static final String DATA_DIR = "data";
    public static final String USERS_FILE = DATA_DIR + "/users.csv";
    public static final String ALERTS_FILE = DATA_DIR + "/alerts.jsonl";
    public static final String TIMELINE_FILE = DATA_DIR + "/timeline.jsonl";
    public static final String BLOCKLIST_FILE = DATA_DIR + "/blocklist.txt";
    public static final String CONFIG_FILE = DATA_DIR + "/config.properties";
}
