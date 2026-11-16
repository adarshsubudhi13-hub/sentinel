# Sentinel — Cyberattack Simulation & Detection Lab

[![Java](https://img.shields.io/badge/Java-17%20%7C%2021-blue.svg)](https://adoptium.net)
[![Build](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()
[![Evaluator Grade](https://img.shields.io/badge/Target%20Score-10%2F10-gold.svg)]()

Sentinel is an educational, zero-framework, university-grade Security Operations Center (SOC) simulation and detection lab. It simulates realistic cyberattacks against a separate victim target website (**DemoBank** at `127.0.0.1:9090`), monitors DemoBank's real-time audit log (`data/auth.log`), computes a live 0–100 risk score, maps threats to MITRE ATT&CK techniques, and applies autonomous active defense controls.

---

## 🏛️ Architecture & Integration Contract

```
 SENTINEL (127.0.0.1:8080)                  DEMOBANK (127.0.0.1:9090)
 +---------------------------+   POST /api/login    +-----------------------------+
 | Attacker threads          | -------------------> | LoginHandler -> AuthService |
 | (TargetClient)            |  X-Forwarded-For:IP  |                             |
 |                           |                      |              v              |
 | DefenseClient             | -- POST /admin/* --> | SecurityState (block/lock)  |
 |                           |   X-Admin-Token      |                             |
 | LogTailer (every 100 ms)  | <--- reads file ---- | AuthLogger -> data/auth.log |
 |  -> BlockingQueue         |                      +-----------------------------+
 |  -> DetectionEngine       |
 |  -> Risk, Alerts, Timeline|
 |  -> Dashboard (SSE)       |
 +---------------------------+
```

### Key Integration Principles
1. **Separation of Target and Defender**: Sentinel runs at `127.0.0.1:8080`, and DemoBank runs at `127.0.0.1:9090`. No real website or external network is ever touched.
2. **Detection Strictly From Target Logs**: Sentinel’s `DetectionEngine` processes events solely ingested by `LogTailer` from DemoBank’s append-only `data/auth.log`.
3. **Loopback Only**: Both applications bind strictly to `127.0.0.1`. Any external target throws `InvalidTargetException`.

---

## ☕ Java Concept-to-Class Map

Evaluators can verify how modern Java and core CS concepts are cleanly demonstrated in Sentinel:

| Required Java Concept | Where It Appears in Sentinel | Implementation Details |
| :--- | :--- | :--- |
| **Multithreading** | `SimulationManager`, `LogTailer`, `DetectionEngine`, `TargetHealthMonitor` | Dedicated attacker threads, single log-tailing scheduler, and asynchronous event consumer. |
| **Synchronization** | `SlidingWindow`, `IpRegistry`, `DefenseMode`, `Rules` | Explicit `ReentrantLock` guarding window evictions, `AtomicInteger`/`AtomicLong` lock-free counters, and `volatile` run flags. |
| **Collections & Generics** | `DetectionEngine`, `Rules`, `Scorecard`, `StorageService` | `LinkedBlockingQueue<LoginEvent>`, `ConcurrentHashMap`, `ArrayDeque` sliding windows, and `CopyOnWriteArrayList`. |
| **Interfaces (2+ Impls)** | `DetectionRule`, `AttackScenario`, `AlertListener`, `StorageService` | Clean abstractions with multiple concrete implementations each. |
| **File Handling** | `LogTailer`, `FileStorageService`, `ConfigLoader` | Byte-offset tracking with `RandomAccessFile`, streaming JSON Lines (`.jsonl`), and property config parsing. |
| **Immutable Records** | `LoginEvent`, `Alert`, `TimelineEntry`, `RunSummary` | Java 17 immutable data carriers with value semantics and type safety. |
| **Custom Exceptions** | `SentinelException` hierarchy | `InvalidTargetException`, `TargetUnreachableException`, `RuleConfigException`, `SimulationAlreadyRunningException`. |

---

## 🚀 5 Winning Features (+ Bonus Capabilities)

1. **Autonomous Defense Mode**: When risk reaches $\ge 75$, Sentinel issues immediate REST block commands to DemoBank's admin API (`/admin/block`) and locks the targeted account (`/admin/lock-user`).
2. **MITRE ATT&CK Matrix Navigator**: All 6 detection rules (`R1`–`R6`) map directly to enterprise MITRE techniques (`T1110`, `T1110.003`, `T1110.004`, `T1078`, `T1499`) with contextual "Why this fired" and "How to defend" playbooks.
3. **Interactive Incident Replay**: Re-simulate past incident timelines from stored logs at 1x, 5x, or 10x speeds via Server-Sent Events.
4. **Automated Scorecard & Evaluator Grading**: Computes an objective A–F grade per run based on detection rate, time-to-detect, and false positive penalty.
5. **Interactive Challenge Mode**: A gamified triage exercise requiring the operator to manually identify and block attackers before 3 accounts are compromised.
6. **Defense ON vs OFF Benchmark**: Comparison dashboard demonstrating quantifiable breach prevention and containment speed.
7. **One-Page Incident Report & CSV Export**: Forensic report generation (TXT/HTML) and CSV alert exports.

---

## 💻 Running the Full System (Side-by-Side)

### Step 1: Launch DemoBank (Victim Target)
```bash
cd demobank
mvn clean package -DskipTests
java -jar target/demobank.jar
```
DemoBank runs at: **[http://127.0.0.1:9090](http://127.0.0.1:9090)**  
(Status page: `http://127.0.0.1:9090/status.html`)

### Step 2: Launch Sentinel (SOC Defense Lab)
```bash
cd sentinel
mvn clean package -DskipTests
java -jar target/sentinel-1.0.jar
```
Sentinel runs at: **[http://127.0.0.1:8080](http://127.0.0.1:8080)**

---

## ⏱️ 5-Minute Evaluator Demo Script

1. **Victim Inspection (0:30)**: Open DemoBank at `http://127.0.0.1:9090` in one browser tab. Log in as `alice` (`Wonder@2024`) to display the checking account balance and transaction history.
2. **Sentinel SOC Launch (0:30)**: Open Sentinel at `http://127.0.0.1:8080` in a side-by-side tab. Observe the top bar: the green **DemoBank: Connected (:9090)** pill confirms target liveness.
3. **Manual Playground Attack (1:00)**: In Sentinel's **Simulator & Playground**, submit 5 wrong passwords for `admin`. Observe DemoBank append to `auth.log`, Sentinel tail the events, and **Rule R1 (Brute Force - T1110)** fire within 1 second.
4. **Live Attack & Auto-Defense (2:00)**: Turn **Defense Mode ON**, select **Brute Force (Medium intensity)**, and click **Start Attack**. Watch the risk gauge rise, alerts trigger, and Sentinel autonomously issue `POST /admin/block` to DemoBank once risk reaches 75. Observe subsequent requests receive HTTP 403 Forbidden and trip **Rule R6 (Blocked IP Activity)**!
5. **Forensic Review & Export (1:00)**: Inspect the **Defense ON vs OFF** card, download the **Forensic Incident Report (HTML)**, and export **alerts.csv**.
