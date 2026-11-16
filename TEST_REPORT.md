# Sentinel — Automated Test Execution Report

Execution Date: 2026-11-09  
Environment: Windows 11, OpenJDK 64-Bit Server VM (build 21.0.6), Apache Maven 3.9  
Framework: JUnit 5 (Jupiter 5.10.2)

---

## 📊 Sentinel Test Suite Summary

| Test Suite | Tests Run | Failures | Errors | Result |
| :--- | :--- | :--- | :--- | :--- |
| `com.sentinel.ConcurrencyTest` | 1 | 0 | 0 | **PASS** |
| `com.sentinel.DetectionRulesTest` | 4 | 0 | 0 | **PASS** |
| `com.sentinel.RiskAndWinningFeaturesTest` | 5 | 0 | 0 | **PASS** |
| `com.sentinel.TargetIntegrationTest` | 5 | 0 | 0 | **PASS** |
| `com.sentinel.EndToEndIntegrationTest` | 1 | 0 | 0 | **PASS** |
| **Total Sentinel Unit & Integration Tests** | **16** | **0** | **0** | **ALL GREEN (100% PASS)** |

---

## 📋 Integration Matrix Validation (Part A Section A8)

| Check | Expected Result | Actual Result | Status |
| :--- | :--- | :--- | :--- |
| **Target down handling** | Indicator shows unreachable, Start disabled, no crash | Validated via `TargetHealthMonitor` and UI warning | **PASS** |
| **Target allowlist enforcement** | Non-127.0.0.1:9090 throws `InvalidTargetException` | Verified in `TargetIntegrationTest` | **PASS** |
| **Log line ingestion** | Complete lines in `auth.log` ingested within 100ms | Tested in `EndToEndIntegrationTest` | **PASS** |
| **Malformed log parsing** | Malformed lines skipped without crash | Tested in `TargetIntegrationTest` | **PASS** |
| **Rule R1 (Brute Force)** | Fires after 5 failures from 1 IP within 60s, tagged T1110 | Tested in `DetectionRulesTest` | **PASS** |
| **Rule R2 (Credential Spray)** | Fires after failures across 3+ accounts, tagged T1110.003 | Tested in `DetectionRulesTest` | **PASS** |
| **Rule R3 (Distributed Attack)**| Fires on 8+ failures on 1 user from 3+ distinct IPs | Verified in rule engine | **PASS** |
| **Rule R4 (Compromise)** | Success after failures raises CRITICAL alert, tagged T1078 | Tested in `DetectionRulesTest` | **PASS** |
| **Rule R5 (Request Flood)** | High volume requests raise MEDIUM alert, tagged T1499 | Tested in `TargetIntegrationTest` | **PASS** |
| **Rule R6 (Blocked IP Activity)**| Subsequent attempt from blocked host logs IP_BLOCKED | Tested in `EndToEndIntegrationTest` | **PASS** |
| **Defense Mode Auto-Block** | Auto-blocks IP at risk >= 75 via DemoBank `/admin/block` | Verified in `EndToEndIntegrationTest` | **PASS** |
| **Reset Target Synchronization**| Reset calls `/admin/reset` on DemoBank to clear state | Verified in `EndToEndIntegrationTest` | **PASS** |
| **20-Thread Concurrency** | 10,000 operations with atomic counters and zero deadlock | Verified in `ConcurrencyTest` | **PASS** |
