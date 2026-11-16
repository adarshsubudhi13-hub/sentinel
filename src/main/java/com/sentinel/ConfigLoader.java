package com.sentinel;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Loads and validates configuration properties from config.properties.
 */
public final class ConfigLoader {
    private String targetBaseUrl = "http://127.0.0.1:9090";
    private String targetLogPath = "../demobank/data/auth.log";
    private String targetAdminToken = "demo-admin-token";
    private int tailIntervalMs = 100;
    private int healthIntervalMs = 2000;

    private int r1Threshold = 5;
    private int r1WindowSec = 60;
    private int r2Threshold = 5;
    private int r2WindowSec = 60;
    private int r3Failures = 8;
    private int r3DistinctIps = 3;
    private int r3WindowSec = 120;
    private int r4PriorFailures = 3;
    private int r4WindowSec = 120;
    private int r5Threshold = 20;
    private int r5WindowSec = 10;
    private int cooldownSec = 30;
    private int autoBlockRiskThreshold = 75;
    private int defenseAccountLockSec = 60;

    public void load(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) return;
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
            targetBaseUrl = props.getProperty("target.baseUrl", targetBaseUrl);
            targetLogPath = props.getProperty("target.logPath", targetLogPath);
            targetAdminToken = props.getProperty("target.adminToken", targetAdminToken);

            r1Threshold = Integer.parseInt(props.getProperty("r1.threshold", String.valueOf(r1Threshold)));
            r1WindowSec = Integer.parseInt(props.getProperty("r1.windowSec", String.valueOf(r1WindowSec)));
            r2Threshold = Integer.parseInt(props.getProperty("r2.threshold", String.valueOf(r2Threshold)));
            r2WindowSec = Integer.parseInt(props.getProperty("r2.windowSec", String.valueOf(r2WindowSec)));
            r3Failures = Integer.parseInt(props.getProperty("r3.failures", String.valueOf(r3Failures)));
            r3DistinctIps = Integer.parseInt(props.getProperty("r3.distinctIps", String.valueOf(r3DistinctIps)));
            r3WindowSec = Integer.parseInt(props.getProperty("r3.windowSec", String.valueOf(r3WindowSec)));
            r4PriorFailures = Integer.parseInt(props.getProperty("r4.priorFailures", String.valueOf(r4PriorFailures)));
            r4WindowSec = Integer.parseInt(props.getProperty("r4.windowSec", String.valueOf(r4WindowSec)));
            r5Threshold = Integer.parseInt(props.getProperty("r5.threshold", String.valueOf(r5Threshold)));
            r5WindowSec = Integer.parseInt(props.getProperty("r5.windowSec", String.valueOf(r5WindowSec)));

            validate();
        } catch (IOException | NumberFormatException e) {
            throw new SentinelException.RuleConfigException("Failed to parse config: " + e.getMessage());
        }
    }

    public void validate() {
        TargetClient.validateTarget(targetBaseUrl);
        if (r1Threshold < 1 || r2Threshold < 1 || r3Failures < 1 || r5Threshold < 1) {
            throw new SentinelException.RuleConfigException("Thresholds must be at least 1");
        }
        if (r1WindowSec < 1 || r1WindowSec > 3600 || r2WindowSec < 1 || r2WindowSec > 3600 ||
            r3WindowSec < 1 || r3WindowSec > 3600 || r4WindowSec < 1 || r4WindowSec > 3600) {
            throw new SentinelException.RuleConfigException("Time windows must be between 1 and 3600 seconds");
        }
    }

    public String getTargetBaseUrl() { return targetBaseUrl; }
    public String getTargetLogPath() { return targetLogPath; }
    public String getTargetAdminToken() { return targetAdminToken; }
    public int getR1Threshold() { return r1Threshold; }
    public int getR2Threshold() { return r2Threshold; }
    public int getR3Failures() { return r3Failures; }
    public int getR3DistinctIps() { return r3DistinctIps; }
    public int getR4PriorFailures() { return r4PriorFailures; }
    public int getR5Threshold() { return r5Threshold; }
    public int getCooldownSec() { return cooldownSec; }
    public int getAutoBlockRiskThreshold() { return autoBlockRiskThreshold; }
    public int getDefenseAccountLockSec() { return defenseAccountLockSec; }
}
