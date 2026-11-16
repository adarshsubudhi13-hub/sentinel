// Sentinel Frontend Application State & Controllers

let defenseEnabled = false;
let challengeActive = false;
let soundEnabled = true;
let riskHistory = [];
let allAlerts = [];
let audioCtx = null;

// 1. WebAudio Synthesizer for Cyber Alert SFX (Zero external audio files)
function playSound(type) {
    if (!soundEnabled) return;
    try {
        if (!audioCtx) {
            audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        }
        if (audioCtx.state === 'suspended') {
            audioCtx.resume();
        }

        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.connect(gain);
        gain.connect(audioCtx.destination);

        const now = audioCtx.currentTime;
        if (type === 'alert') {
            // High double beep for threat detection
            osc.type = 'sawtooth';
            osc.frequency.setValueAtTime(880, now);
            osc.frequency.exponentialRampToValueAtTime(440, now + 0.15);
            gain.gain.setValueAtTime(0.15, now);
            gain.gain.linearRampToValueAtTime(0, now + 0.18);
            osc.start(now);
            osc.stop(now + 0.18);
        } else if (type === 'block') {
            // Low defense trigger thud
            osc.type = 'triangle';
            osc.frequency.setValueAtTime(200, now);
            osc.frequency.exponentialRampToValueAtTime(50, now + 0.25);
            gain.gain.setValueAtTime(0.25, now);
            gain.gain.linearRampToValueAtTime(0, now + 0.25);
            osc.start(now);
            osc.stop(now + 0.25);
        } else if (type === 'success') {
            osc.type = 'sine';
            osc.frequency.setValueAtTime(523.25, now); // C5
            osc.frequency.setValueAtTime(659.25, now + 0.08); // E5
            gain.gain.setValueAtTime(0.1, now);
            gain.gain.linearRampToValueAtTime(0, now + 0.2);
            osc.start(now);
            osc.stop(now + 0.2);
        }
    } catch (e) {
        // AudioContext not allowed before user gesture
    }
}

function toggleSound() {
    soundEnabled = !soundEnabled;
    const btn = document.getElementById('sound-btn');
    if (btn) {
        btn.innerText = soundEnabled ? '🔊 Audio: ON' : '🔇 Audio: OFF';
        btn.classList.toggle('active', soundEnabled);
    }
}

// 2. Multi-Theme Switching (Cyber Neon, Matrix, Space)
function changeTheme(theme) {
    document.body.classList.remove('theme-matrix', 'theme-space');
    if (theme === 'matrix') {
        document.body.classList.add('theme-matrix');
    } else if (theme === 'space') {
        document.body.classList.add('theme-space');
    }
    localStorage.setItem('sentinel-theme', theme);
}

// 3. Navigation Controller
function switchPage(pageId) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));

    const targetPage = document.getElementById(`page-${pageId}`);
    if (targetPage) targetPage.classList.add('active');

    const activeBtn = Array.from(document.querySelectorAll('.nav-btn'))
        .find(btn => btn.getAttribute('onclick')?.includes(pageId));
    if (activeBtn) activeBtn.classList.add('active');

    if (pageId === 'alerts') loadAlertsAndTimeline();
    if (pageId === 'ips') loadIps();
    if (pageId === 'mitre') updateMitreGrid();
}

// 4. Live Risk Sparkline Renderer
function drawSparkline() {
    const canvas = document.getElementById('risk-sparkline');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const width = canvas.width = canvas.offsetWidth;
    const height = canvas.height = canvas.offsetHeight;

    ctx.clearRect(0, 0, width, height);
    if (riskHistory.length < 2) return;

    ctx.beginPath();
    const step = width / (riskHistory.length - 1);
    riskHistory.forEach((val, i) => {
        const x = i * step;
        const y = height - (val / 100) * (height - 8) - 4;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
    });

    const isHigh = riskHistory[riskHistory.length - 1] >= 60;
    ctx.strokeStyle = isHigh ? '#ef4444' : '#00f2fe';
    ctx.lineWidth = 2.5;
    ctx.stroke();

    // Fill gradient
    ctx.lineTo(width, height);
    ctx.lineTo(0, height);
    ctx.closePath();
    const grad = ctx.createLinearGradient(0, 0, 0, height);
    grad.addColorStop(0, isHigh ? 'rgba(239, 68, 68, 0.3)' : 'rgba(0, 242, 254, 0.3)');
    grad.addColorStop(1, 'transparent');
    ctx.fillStyle = grad;
    ctx.fill();
}

// 5. Real-time Polling & SSE
function initSSE() {
    const es = new EventSource('/api/stream');
    es.addEventListener('stats', (e) => {
        const stats = JSON.parse(e.data);
        updateDashboard(stats);
    });
    es.addEventListener('alert', (e) => {
        const alert = JSON.parse(e.data);
        playSound('alert');
        loadAlertsAndTimeline();
        updateMitreGrid();
    });
    es.addEventListener('replay', (e) => {
        const entry = JSON.parse(e.data);
        const ind = document.getElementById('replay-indicator');
        if (ind) {
            ind.style.display = entry.done ? 'none' : 'block';
            ind.innerText = `Replaying [${entry.type}]: ${entry.actor} - ${entry.detail}`;
        }
    });
}

async function fetchStats() {
    try {
        const [statsRes, scRes] = await Promise.all([
            fetch('/api/stats'),
            fetch('/api/scorecard')
        ]);
        const stats = await statsRes.json();
        const scorecard = await scRes.json();

        updateDashboard(stats);
        updateScorecard(scorecard);
        defenseEnabled = stats.defenseEnabled;
        updateDefenseUI();
    } catch (err) {
        console.error('Stats poll error', err);
    }
}

function updateDashboard(data) {
    const risk = data.systemRisk || data.risk || 0;
    const riskElem = document.getElementById('dash-risk');
    const badge = document.getElementById('dash-risk-badge');
    if (riskElem) riskElem.innerText = risk;
    if (badge) {
        const lvl = data.riskLevel || data.level || 'NORMAL';
        badge.innerText = lvl;
        badge.className = `badge ${lvl}`;
    }

    // Target DemoBank health status
    if (data.targetConnected !== undefined) {
        const pill = document.getElementById('target-status-pill');
        const warn = document.getElementById('sim-target-warning');
        const startBtn = document.getElementById('sim-start-btn');
        if (pill) {
            const ok = !!data.targetConnected;
            pill.innerText = ok ? '● DemoBank: Connected (:9090)' : '○ DemoBank: Unreachable';
            pill.style.background = ok ? 'rgba(34, 197, 94, 0.2)' : 'rgba(239, 68, 68, 0.2)';
            pill.style.color = ok ? '#22c55e' : '#ef4444';
            pill.style.borderColor = ok ? '#22c55e' : '#ef4444';
        }
        if (warn && startBtn) {
            if (!data.targetConnected) {
                warn.style.display = 'block';
                startBtn.disabled = true;
                startBtn.style.opacity = '0.5';
            } else {
                warn.style.display = 'none';
                startBtn.disabled = false;
                startBtn.style.opacity = '1';
            }
        }
    }

    // Keep last 30 data points for sparkline
    riskHistory.push(risk);
    if (riskHistory.length > 30) riskHistory.shift();
    drawSparkline();

    const attemptsElem = document.getElementById('dash-attempts');
    if (attemptsElem) attemptsElem.innerText = data.totalAttempts || data.attempts || 0;

    const successesElem = document.getElementById('dash-successes');
    if (successesElem) successesElem.innerText = data.totalSuccesses || 0;

    const failuresElem = document.getElementById('dash-failures');
    if (failuresElem) failuresElem.innerText = data.totalFailures || data.failures || 0;

    const alertsElem = document.getElementById('dash-alerts');
    if (alertsElem) alertsElem.innerText = data.totalAlerts || data.alerts || 0;

    const gradeElem = document.getElementById('dash-grade');
    if (gradeElem) gradeElem.innerText = data.scorecardGrade || 'N/A';
}

function updateScorecard(sc) {
    const accElem = document.getElementById('dash-accuracy');
    const ttdElem = document.getElementById('dash-ttd');
    if (accElem && sc) {
        const total = (sc.attacksDetected || 0) + (sc.falsePositives || 0);
        const rate = total === 0 ? 100 : Math.round((sc.attacksDetected / total) * 100);
        accElem.innerText = `${rate}%`;
    }
    if (ttdElem && sc) {
        ttdElem.innerText = `${sc.timeToDetectMs || 0}ms`;
    }
}

// 6. Reset Lab Functionality
async function resetLab() {
    if (!confirm('Are you sure you want to reset all lab telemetry, alerts, and counters?')) return;
    try {
        await fetch('/api/reset', { method: 'POST' });
        riskHistory = [];
        allAlerts = [];
        await fetchStats();
        await loadAlertsAndTimeline();
        await loadIps();
        updateMitreGrid();
        playSound('success');
    } catch (e) {
        alert('Failed to reset lab: ' + e.message);
    }
}

// 7. Defense Mode Toggle
async function toggleDefense() {
    defenseEnabled = !defenseEnabled;
    await fetch('/api/defense', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ enabled: defenseEnabled })
    });
    playSound('block');
    updateDefenseUI();
}

function updateDefenseUI() {
    const status = document.getElementById('dash-defense-status');
    const badge = document.getElementById('dash-defense-badge');
    const btn = document.getElementById('defense-toggle-btn');
    if (status && btn) {
        status.innerText = defenseEnabled ? 'ACTIVE (AUTO-BLOCKING)' : 'OFF';
        status.style.color = defenseEnabled ? 'var(--accent-green)' : 'var(--text-secondary)';
        btn.innerText = defenseEnabled ? 'Disable Defense Mode' : 'Enable Defense Mode';
        btn.className = defenseEnabled ? 'btn btn-danger' : 'btn btn-success';
    }
    if (badge) {
        badge.innerText = defenseEnabled ? 'ACTIVE' : 'INACTIVE';
        badge.style.background = defenseEnabled ? 'var(--accent-green)' : 'rgba(148, 163, 184, 0.2)';
        badge.style.color = defenseEnabled ? '#fff' : 'var(--text-secondary)';
    }
}

// 8. Simulations & Demo
async function triggerDemo() {
    await fetch('/api/simulation/demo', { method: 'POST' });
    playSound('success');
    alert('Started 90-second Demo Simulation Story!');
}

async function startSimulation() {
    const scenario = document.getElementById('sim-scenario').value;
    const eps = parseInt(document.getElementById('sim-eps').value, 10);
    await fetch('/api/simulation/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ scenario, eps, duration: 15000 })
    });
    playSound('alert');
    alert(`Simulation started: ${scenario}`);
}

async function stopSimulation() {
    await fetch('/api/simulation/stop', { method: 'POST' });
}

// 9. Challenge Mode
async function toggleChallenge() {
    challengeActive = !challengeActive;
    const res = await fetch('/api/challenge', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: challengeActive ? 'start' : 'stop' })
    });
    const data = await res.json();
    document.getElementById('challenge-status').innerText = challengeActive ? 'Status: RUNNING!' : 'Status: Inactive';
    document.getElementById('challenge-stats').style.display = challengeActive ? 'block' : 'none';
}

// 10. Interactive Replay
async function startReplay(speed) {
    await fetch('/api/replay', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ speed })
    });
}

// 11. Login Playground
async function submitPlaygroundLogin() {
    const username = document.getElementById('play-user').value;
    const password = document.getElementById('play-pass').value;
    const resultDiv = document.getElementById('play-result');

    try {
        const res = await fetch('/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });
        const data = await res.json();
        const code = data.status || res.status;
        if (data.success) {
            playSound('success');
            resultDiv.innerHTML = `<span style="color: var(--accent-green); font-weight: bold;">✔ Authentication Success (HTTP 200)!</span><div style="font-size:0.78rem; color:var(--text-secondary); margin-top:4px;">Logged to DemoBank auth.log with reason NONE</div>`;
        } else {
            playSound('alert');
            resultDiv.innerHTML = `<span style="color: var(--accent-red); font-weight: bold;">✖ Access Denied (HTTP ${code}): ${data.reason || data.error}</span><div style="font-size:0.78rem; color:var(--text-secondary); margin-top:4px;">Appended to DemoBank auth.log &amp; ingested into Sentinel event queue</div>`;
        }
    } catch (err) {
        resultDiv.innerHTML = '<span style="color: var(--accent-red);">Target unreachable or network error</span>';
    }
}

function exportAlertsCsv() {
    window.location.href = '/api/export/alerts.csv';
}

// 12. Alerts & Timeline Loader
async function loadAlertsAndTimeline() {
    try {
        const [alertsRes, tlRes] = await Promise.all([
            fetch('/api/alerts'),
            fetch('/api/timeline')
        ]);
        allAlerts = await alertsRes.json();
        const timeline = await tlRes.json();

        renderAlertsTable(allAlerts);

        const tlList = document.getElementById('timeline-list');
        if (tlList) {
            tlList.innerHTML = '';
            timeline.slice(-30).reverse().forEach(t => {
                const li = document.createElement('li');
                li.className = `timeline-item ${t.type}`;
                li.innerHTML = `
                    <div class="timeline-meta">${t.timestamp} &bull; ${t.type}</div>
                    <div><strong>${t.actor}</strong>: ${t.detail}</div>
                `;
                tlList.appendChild(li);
            });
        }
    } catch (e) {
        console.error('Failed to load alerts/timeline', e);
    }
}

function filterAlerts(severity) {
    if (severity === 'ALL') {
        renderAlertsTable(allAlerts);
    } else {
        const filtered = allAlerts.filter(a => a.severity === severity);
        renderAlertsTable(filtered);
    }
}

function renderAlertsTable(alerts) {
    const tbody = document.getElementById('alerts-tbody');
    if (!tbody) return;
    tbody.innerHTML = '';
    alerts.forEach(a => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><span class="badge ${a.severity}">${a.severity}</span></td>
            <td><strong>${a.ruleName}</strong></td>
            <td><span class="mitre-badge">${a.mitreTag}</span> <span style="font-size: 0.78rem; color: var(--text-secondary);">(${a.mitreLabel})</span></td>
            <td><code>${a.username}</code> @ <code>${a.ip}</code></td>
            <td><div style="font-size: 0.8rem;"><strong>Why:</strong> ${a.why}</div><div style="font-size: 0.8rem; color: var(--accent-cyan); margin-top: 3px;"><strong>Defend:</strong> ${a.defend}</div></td>
        `;
        tbody.appendChild(tr);
    });
}

// 13. MITRE Matrix Real-time Detections Count
async function updateMitreGrid() {
    try {
        const res = await fetch('/api/alerts');
        const alerts = await res.json();
        const counts = { 'T1110': 0, 'T1110.003': 0, 'T1110.004': 0, 'T1078': 0, 'T1499': 0 };

        alerts.forEach(a => {
            if (counts[a.mitreTag] !== undefined) {
                counts[a.mitreTag]++;
            }
        });

        for (const [tag, count] of Object.entries(counts)) {
            const idKey = tag.toLowerCase().replace('.', '-');
            const elem = document.getElementById(`mcount-${idKey}`);
            const cell = document.getElementById(`mitre-${idKey}`);
            if (elem) elem.innerText = count;
            if (cell) cell.classList.toggle('hit', count > 0);
        }
    } catch (e) {}
}

// 14. IP Threat Registry Loader
async function loadIps() {
    try {
        const res = await fetch('/api/ips');
        const ips = await res.json();
        const tbody = document.getElementById('ips-tbody');
        if (!tbody) return;
        tbody.innerHTML = '';
        ips.forEach(ip => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><code>${ip.ip}</code></td>
                <td>${ip.failures?.value || ip.failures || 0}</td>
                <td>${ip.distinct?.value || ip.distinct || 0}</td>
                <td><strong style="color: ${ip.riskScore >= 75 ? 'var(--accent-red)' : 'inherit'};">${ip.riskScore}</strong></td>
                <td><span class="badge ${ip.blocked ? 'CRITICAL' : 'NORMAL'}">${ip.blocked ? 'BLOCKED' : 'ALLOW'}</span></td>
                <td>
                    <button class="btn btn-secondary" style="padding: 4px 10px; font-size: 0.78rem;" onclick="toggleIpBlock('${ip.ip}', ${!ip.blocked})">
                        ${ip.blocked ? '🔓 Unblock' : '🚫 Block'}
                    </button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (e) {
        console.error('Failed to load IPs', e);
    }
}

async function toggleIpBlock(ip, block) {
    await fetch('/api/ips', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ip, block })
    });
    playSound('block');
    loadIps();
}

// 15. Report Downloader
function downloadReport(format) {
    window.location.href = `/api/report/${format}`;
}

// Bootstrap
window.addEventListener('DOMContentLoaded', () => {
    const savedTheme = localStorage.getItem('sentinel-theme') || 'cyber';
    const selector = document.getElementById('theme-selector');
    if (selector) selector.value = savedTheme;
    changeTheme(savedTheme);

    fetchStats();
    setInterval(fetchStats, 2000);
    initSSE();
});

function fillPlayground(u, p) {
    const userInput = document.getElementById('play-user');
    const passInput = document.getElementById('play-pass');
    if (userInput && passInput) {
        userInput.value = u;
        passInput.value = p;
        userInput.focus();
    }
}
