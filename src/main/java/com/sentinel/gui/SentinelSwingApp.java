package com.sentinel.gui;

import com.sentinel.Alert;
import com.sentinel.DetectionEngine;
import com.sentinel.Rules;
import com.sentinel.Scenarios;
import com.sentinel.TargetClient;
import com.sentinel.database.AlertRecord;
import com.sentinel.database.DatabaseException;
import com.sentinel.database.DatabaseManager;
import com.sentinel.database.JdbcAlertRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Full-featured Java Swing Graphical User Interface (GUI) for the Sentinel SOC Lab.
 * Demonstrates:
 * - GUI: Swing components (JFrame, JTabbedPane, JTable, JButton, JProgressBar, JTextArea, JComboBox)
 * - OOP: Classes, Encapsulation, Polymorphism, Abstraction, Interfaces
 * - Advanced Java: Multithreading (SwingWorker, CompletableFuture), Exception Handling, Collections, Streams
 * - Database: JDBC CRUD integration (Create, Read, Update, Delete) via JdbcAlertRepository
 */
public class SentinelSwingApp extends JFrame {

    private final JdbcAlertRepository alertRepo;
    private final TargetClient targetClient;

    // Swing components
    private JTable alertTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> severityFilter;
    private JTextArea logArea;
    private JProgressBar riskBar;
    private JLabel riskLabel;
    private JLabel statsLabel;

    // JDBC CRUD & SQL Tab Fields
    private JTextField txtAlertId;
    private JTextField txtRuleName;
    private JComboBox<String> comboSeverity;
    private JTextField txtTargetIp;
    private JTextField txtUsername;
    private JTextField txtDescription;
    private JTextField txtMitre;
    private JComboBox<String> comboStatus;
    private JTextField txtSqlQuery;
    private JTable sqlResultTable;
    private DefaultTableModel sqlResultModel;
    private JLabel lblSqlStatus;

    public SentinelSwingApp() {
        this.alertRepo = new JdbcAlertRepository(DatabaseManager.getInstance());
        this.targetClient = new TargetClient("http://127.0.0.1:9090");
        initUI();
        refreshAlertTable();
    }

    private void initUI() {
        setTitle("Sentinel — Autonomous SOC Cyberdefense Console (Java Swing GUI)");
        setSize(1100, 720);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Apply dark cyber palette
        applyDarkTheme();

        // Main layout
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        mainPanel.setBackground(new Color(15, 23, 42));

        // Top Header
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(30, 41, 59));
        tabbedPane.setForeground(new Color(241, 245, 249));
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        tabbedPane.addTab("🚨 SOC Alert Feed (JDBC)", createAlertsPanel());
        tabbedPane.addTab("⚡ Attack Simulation Lab", createSimulationPanel());
        tabbedPane.addTab("💾 Database CRUD Manager (JDBC)", createDatabaseCrudPanel());
        tabbedPane.addTab("🛡️ System Defense & Metrics", createDefensePanel());
        tabbedPane.addTab("🧩 OOP & Architecture Guide", createArchitecturePanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Bottom Status Bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(30, 41, 59));
        statusBar.setBorder(new EmptyBorder(6, 12, 6, 12));
        statsLabel = new JLabel("System: ONLINE | Target: DemoBank (127.0.0.1:9090) | JDBC: SQLite Connected");
        statsLabel.setForeground(new Color(148, 163, 184));
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusBar.add(statsLabel, BorderLayout.WEST);

        mainPanel.add(statusBar, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(15, 10));
        header.setBackground(new Color(30, 41, 59));
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        JLabel title = new JLabel("SENTINEL SOC CYBERDEFENSE CONSOLE");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(56, 189, 248)); // Cyan

        JLabel subtitle = new JLabel("Autonomous Detection, Real-Time Threat Analysis & JDBC SQLite Persistence");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(148, 163, 184));
        titlePanel.add(title);
        titlePanel.add(subtitle);
        header.add(titlePanel, BorderLayout.WEST);

        // Risk Meter
        JPanel riskPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        riskPanel.setOpaque(false);
        riskLabel = new JLabel("DEFCON RISK: 15 (NORMAL)");
        riskLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        riskLabel.setForeground(new Color(34, 197, 94)); // Green

        riskBar = new JProgressBar(0, 100);
        riskBar.setValue(15);
        riskBar.setPreferredSize(new Dimension(160, 20));
        riskBar.setForeground(new Color(34, 197, 94));
        riskBar.setBackground(new Color(15, 23, 42));

        riskPanel.add(riskLabel);
        riskPanel.add(riskBar);
        header.add(riskPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createAlertsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(new Color(15, 23, 42));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Controls bar
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolBar.setBackground(new Color(30, 41, 59));
        toolBar.setBorder(new EmptyBorder(6, 6, 6, 6));

        JLabel filterLbl = new JLabel("Filter Severity:");
        filterLbl.setForeground(Color.WHITE);
        severityFilter = new JComboBox<>(new String[]{"ALL", "CRITICAL", "HIGH", "MEDIUM", "LOW"});
        severityFilter.addActionListener(e -> applyFilter());

        JButton btnRefresh = createStyledButton("🔄 Refresh (JDBC)", new Color(2, 132, 199));
        btnRefresh.addActionListener(e -> refreshAlertTable());

        JButton btnUpdateStatus = createStyledButton("✏️ Update Status", new Color(13, 148, 136));
        btnUpdateStatus.addActionListener(this::handleUpdateStatus);

        JButton btnDelete = createStyledButton("❌ Delete Selected", new Color(225, 29, 72));
        btnDelete.addActionListener(this::handleDeleteAlert);

        JButton btnPurge = createStyledButton("🧹 Purge All", new Color(159, 18, 57));
        btnPurge.addActionListener(this::handlePurgeAlerts);

        toolBar.add(filterLbl);
        toolBar.add(severityFilter);
        toolBar.add(btnRefresh);
        toolBar.add(btnUpdateStatus);
        toolBar.add(btnDelete);
        toolBar.add(btnPurge);
        panel.add(toolBar, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Time", "Severity", "Rule Name", "MITRE", "Target IP", "User", "Status", "Description"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        alertTable = new JTable(tableModel);
        alertTable.setRowHeight(28);
        alertTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        alertTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        alertTable.getTableHeader().setBackground(new Color(30, 41, 59));
        alertTable.getTableHeader().setForeground(Color.WHITE);
        alertTable.setBackground(new Color(15, 23, 42));
        alertTable.setForeground(new Color(241, 245, 249));
        alertTable.setGridColor(new Color(51, 65, 85));

        // Severity renderer
        alertTable.getColumnModel().getColumn(2).setCellRenderer(new SeverityRenderer());

        JScrollPane scrollPane = new JScrollPane(alertTable);
        scrollPane.getViewport().setBackground(new Color(15, 23, 42));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSimulationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(15, 23, 42));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Attack Buttons Grid
        JPanel btnGrid = new JPanel(new GridLayout(2, 3, 10, 10));
        btnGrid.setOpaque(false);

        JButton btnBrute = createStyledButton("💥 Brute Force (R1)", new Color(225, 29, 72));
        btnBrute.addActionListener(e -> runAttack("BRUTE_FORCE", "Single IP password guessing attack"));

        JButton btnSpray = createStyledButton("🌊 Password Spray (R2)", new Color(217, 119, 6));
        btnSpray.addActionListener(e -> runAttack("SPRAY", "Single password tested across many accounts"));

        JButton btnDist = createStyledButton("🌐 Distributed Botnet (R3)", new Color(147, 51, 234));
        btnDist.addActionListener(e -> runAttack("DISTRIBUTED", "Coordinated multi-IP credential assault"));

        JButton btnComp = createStyledButton("🎯 Account Takeover (R4)", new Color(190, 24, 93));
        btnComp.addActionListener(e -> runAttack("COMPROMISE", "Repeated failed logins followed by success"));

        JButton btnFlood = createStyledButton("⚡ Request Flood (R5)", new Color(234, 88, 12));
        btnFlood.addActionListener(e -> runAttack("FLOOD", "High-frequency DoS volumetric burst"));

        JButton btnNormal = createStyledButton("👥 Normal Traffic", new Color(16, 185, 129));
        btnNormal.addActionListener(e -> runAttack("NORMAL", "Legitimate user login patterns"));

        btnGrid.add(btnBrute);
        btnGrid.add(btnSpray);
        btnGrid.add(btnDist);
        btnGrid.add(btnComp);
        btnGrid.add(btnFlood);
        btnGrid.add(btnNormal);
        panel.add(btnGrid, BorderLayout.NORTH);

        // Simulation Console Output
        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setOpaque(false);
        JLabel conLabel = new JLabel("⚡ Attack Execution & Target Live Response Log:");
        conLabel.setForeground(new Color(148, 163, 184));
        conLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        conLabel.setBorder(new EmptyBorder(6, 0, 4, 0));
        consolePanel.add(conLabel, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(10, 15, 29));
        logArea.setForeground(new Color(56, 189, 248));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setText("Sentinel SOC Lab Simulation Terminal Ready.\nTarget: DemoBank (http://127.0.0.1:9090)\n");

        JScrollPane logScroll = new JScrollPane(logArea);
        consolePanel.add(logScroll, BorderLayout.CENTER);

        panel.add(consolePanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createDatabaseCrudPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(15, 23, 42));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Form for CRUD operations
        JPanel formPanel = new JPanel(new GridLayout(4, 4, 10, 8));
        formPanel.setBackground(new Color(30, 41, 59));
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85)),
                "JDBC Incident Record Form (CRUD Operations)",
                0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.WHITE));

        txtAlertId = new JTextField("ALT-" + UUID.randomUUID().toString().substring(0, 8));
        txtRuleName = new JTextField("Brute Force Attack");
        comboSeverity = new JComboBox<>(new String[]{"CRITICAL", "HIGH", "MEDIUM", "LOW"});
        txtTargetIp = new JTextField("192.168.1.105");
        txtUsername = new JTextField("adarsh");
        txtDescription = new JTextField("Exceeded 5 failed login attempts in 10s window");
        txtMitre = new JTextField("T1110");
        comboStatus = new JComboBox<>(new String[]{"NEW", "INVESTIGATING", "CONFIRMED_INCIDENT", "RESOLVED"});

        formPanel.add(createFormLabel("Alert ID (Primary Key):"));
        formPanel.add(txtAlertId);
        formPanel.add(createFormLabel("Rule Name:"));
        formPanel.add(txtRuleName);

        formPanel.add(createFormLabel("Severity:"));
        formPanel.add(comboSeverity);
        formPanel.add(createFormLabel("Target IP:"));
        formPanel.add(txtTargetIp);

        formPanel.add(createFormLabel("Username:"));
        formPanel.add(txtUsername);
        formPanel.add(createFormLabel("MITRE Technique:"));
        formPanel.add(txtMitre);

        formPanel.add(createFormLabel("Status:"));
        formPanel.add(comboStatus);
        formPanel.add(createFormLabel("Description:"));
        formPanel.add(txtDescription);

        panel.add(formPanel, BorderLayout.NORTH);

        // CRUD Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        actionPanel.setBackground(new Color(15, 23, 42));

        JButton btnCreate = createStyledButton("➕ C: Insert Incident (JDBC INSERT)", new Color(16, 185, 129));
        btnCreate.addActionListener(this::handleCreateCrud);

        JButton btnRead = createStyledButton("🔍 R: Query / View All (JDBC SELECT)", new Color(2, 132, 199));
        btnRead.addActionListener(e -> refreshAlertTable());

        JButton btnUpdate = createStyledButton("✏️ U: Update Record (JDBC UPDATE)", new Color(217, 119, 6));
        btnUpdate.addActionListener(this::handleUpdateCrud);

        JButton btnDelete = createStyledButton("❌ D: Delete Record (JDBC DELETE)", new Color(225, 29, 72));
        btnDelete.addActionListener(this::handleDeleteCrud);

        actionPanel.add(btnCreate);
        actionPanel.add(btnRead);
        actionPanel.add(btnUpdate);
        actionPanel.add(btnDelete);

        // Center Panel: CRUD Buttons + Live SQL Executor
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(actionPanel, BorderLayout.NORTH);

        // SQL Query Runner Section
        JPanel sqlPanel = new JPanel(new BorderLayout(6, 6));
        sqlPanel.setBackground(new Color(30, 41, 59));
        sqlPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85)),
                "SQL Query Runner (JDBC PreparedStatement & Statement)",
                0, 0, new Font("Segoe UI", Font.BOLD, 12), Color.WHITE));

        JPanel sqlInputBar = new JPanel(new BorderLayout(6, 6));
        sqlInputBar.setOpaque(false);
        sqlInputBar.setBorder(new EmptyBorder(4, 6, 4, 6));

        txtSqlQuery = new JTextField("SELECT id, rule_name, severity, target_ip, username, status FROM security_alerts ORDER BY timestamp DESC");
        txtSqlQuery.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtSqlQuery.setBackground(new Color(15, 23, 42));
        txtSqlQuery.setForeground(new Color(56, 189, 248));
        txtSqlQuery.setCaretColor(Color.WHITE);

        JButton btnRunSql = createStyledButton("▶️ Execute SQL (JDBC)", new Color(14, 165, 233));
        btnRunSql.addActionListener(this::handleExecuteSql);

        sqlInputBar.add(new JLabel(" SQL: ") {{ setForeground(Color.WHITE); setFont(new Font("Segoe UI", Font.BOLD, 12)); }}, BorderLayout.WEST);
        sqlInputBar.add(txtSqlQuery, BorderLayout.CENTER);
        sqlInputBar.add(btnRunSql, BorderLayout.EAST);
        sqlPanel.add(sqlInputBar, BorderLayout.NORTH);

        sqlResultModel = new DefaultTableModel();
        sqlResultTable = new JTable(sqlResultModel);
        sqlResultTable.setRowHeight(22);
        sqlResultTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sqlResultTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        sqlResultTable.getTableHeader().setBackground(new Color(15, 23, 42));
        sqlResultTable.getTableHeader().setForeground(Color.WHITE);
        sqlResultTable.setBackground(new Color(15, 23, 42));
        sqlResultTable.setForeground(new Color(241, 245, 249));

        JScrollPane sqlScroll = new JScrollPane(sqlResultTable);
        sqlScroll.setPreferredSize(new Dimension(800, 140));
        sqlPanel.add(sqlScroll, BorderLayout.CENTER);

        lblSqlStatus = new JLabel(" Ready. Enter any valid SQL DDL/DML statement to execute via JDBC.");
        lblSqlStatus.setForeground(new Color(148, 163, 184));
        lblSqlStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblSqlStatus.setBorder(new EmptyBorder(4, 6, 4, 6));
        sqlPanel.add(lblSqlStatus, BorderLayout.SOUTH);

        centerPanel.add(sqlPanel, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);

        // SQL / Info Box
        JTextArea infoText = new JTextArea();
        infoText.setEditable(false);
        infoText.setBackground(new Color(10, 15, 29));
        infoText.setForeground(new Color(148, 163, 184));
        infoText.setFont(new Font("Consolas", Font.PLAIN, 12));
        infoText.setText("""
            [JDBC Persistence Layer Active]
            Database: SQLite (data/sentinel.db)
            Driver: org.sqlite.JDBC (Java Database Connectivity)
            Schema: CREATE TABLE IF NOT EXISTS security_alerts (
                        id TEXT PRIMARY KEY, rule_id TEXT, rule_name TEXT, severity TEXT,
                        target_ip TEXT, username TEXT, timestamp INTEGER, description TEXT,
                        mitre_technique TEXT, status TEXT, resolution_notes TEXT);
            Statements: PreparedStatement with Parameterized Queries (Safe against SQL Injection)
            """);
        panel.add(infoText, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createDefensePanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 12, 12));
        panel.setBackground(new Color(15, 23, 42));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        panel.add(createCard("Autonomous Containment", """
            When DEFCON Risk score reaches >= 75:
            - Malicious IPs are auto-blocked via DemoBank /admin/block API.
            - Compromised user accounts are locked via /admin/lock-user API.
            - Defense Mode dynamically mitigates live attacks within 100ms.
            """));

        panel.add(createCard("Detection Engine Rules", """
            R1: Brute Force Detection (T1110) - 5 fails / 10s
            R2: Credential Spray (T1110.003) - 3 users / 30s
            R3: Distributed Attack (T1110.004) - 3 IPs / 60s
            R4: Compromise Detection (T1078) - Fails followed by Success
            R5: Request Flood (T1499) - 10 reqs / 2s
            R6: Geolocation / Anomalous Pattern
            """));

        panel.add(createCard("Multithreading Architecture", """
            - Ingestion Thread: RandomAccessFile polling auth.log every 100ms
            - Processing Worker: LinkedBlockingQueue consumer thread
            - HTTP Server: com.sun.net.httpserver with 16-thread fixed pool
            - GUI Event Dispatch Thread: SwingWorker / CompletableFuture non-blocking
            """));

        panel.add(createCard("Seed Credentials Available", """
            Registered Accounts with Full Access:
            • alice    (Wonder@2024)   Balance: $14,850
            • bob      (Builder#77)    Balance: $3,420
            • charlie  (Choc0late!)    Balance: $8,910
            • admin    (Adm1n$ecure)   Vault: $1,250,000
            • guest    (guest123)      Balance: $500
            • adarsh   (Adarsh@123)    Balance: $50,000
            """));

        return panel;
    }

    private JPanel createArchitecturePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(15, 23, 42));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JTextArea archText = new JTextArea();
        archText.setEditable(false);
        archText.setBackground(new Color(10, 15, 29));
        archText.setForeground(new Color(241, 245, 249));
        archText.setFont(new Font("Consolas", Font.PLAIN, 12));
        archText.setText("""
            ========================================================================================
            SENTINEL + DEMOBANK: OOP & ADVANCED JAVA CONCEPTS IMPLEMENTATION ARCHITECTURE
            ========================================================================================

            1. OBJECT-ORIENTED PROGRAMMING CONCEPTS:
               • Classes & Objects: Concrete domain entities (AlertRecord, SecurityEvent, UserRecord, IpStats).
               • Encapsulation: Strict private fields, constructor validation, immutable Java Records, getters/setters.
               • Inheritance: Abstract base classes (AbstractJdbcRepository<T, ID>, AbstractDetectionRule).
               • Polymorphism: Dynamic method dispatch across CrudRepository<T, ID> and DetectionRule implementations.
               • Abstraction & Interfaces: CrudRepository<T, ID>, AlertListener, StorageService, DetectionRule.

            2. ADVANCED JAVA CONCEPTS:
               • Exception Handling: Custom hierarchy (SentinelException -> DatabaseException, AuthenticationException),
                 rigorous try-with-resources on Connection, PreparedStatement, and ResultSet.
               • Multithreading & Concurrency: Executors.newFixedThreadPool, LinkedBlockingQueue, ConcurrentHashMap,
                 AtomicInteger, CopyOnWriteArrayList, background log tailer daemon.
               • Collections & Generics: Type-safe CrudRepository<T, ID>, PagedResult<T>, Map<String, IpStats>,
                 List<AlertRecord>, Set<String>.
               • Java Streams & Lambdas: Fluent stream pipelines (.filter(), .map(), .sorted(), .collect()) used for
                 threat score aggregation, timeline generation, and severity filtering.

            3. DATABASE CONNECTIVITY (JDBC):
               • Driver: org.sqlite.JDBC connecting to data/sentinel.db.
               • CRUD Operations:
                 - Create: save(AlertRecord) via INSERT INTO security_alerts.
                 - Read: findById(id), findAll(), findBySeverity(sev) via SELECT with ResultSet mapping.
                 - Update: update(AlertRecord), updateStatus(id, status, notes) via UPDATE.
                 - Delete: deleteById(id), deleteAll() via DELETE.
               • Security: PreparedStatement parameterized queries prevent SQL injection completely.

            4. GRAPHICAL USER INTERFACE (GUI):
               • Java Swing (javax.swing.*, java.awt.*) desktop console.
               • Interactive JTable with custom Severity Badges, real-time Attack Launchers,
                 JDBC Database CRUD manager, and live Defense telemetry.
            ========================================================================================
            """);

        panel.add(new JScrollPane(archText), BorderLayout.CENTER);
        return panel;
    }

    // ── Helper UI Methods ───────────────────────────────────────────────────

    private JPanel createCard(String title, String body) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(new Color(30, 41, 59));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(51, 65, 85)),
                new EmptyBorder(10, 12, 10, 12)));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(56, 189, 248));

        JTextArea txtBody = new JTextArea(body);
        txtBody.setEditable(false);
        txtBody.setOpaque(false);
        txtBody.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtBody.setForeground(new Color(203, 213, 225));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(txtBody, BorderLayout.CENTER);
        return card;
    }

    private JLabel createFormLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(203, 213, 225));
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        return l;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker()),
                new EmptyBorder(6, 12, 6, 12)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void applyDarkTheme() {
        UIManager.put("Panel.background", new Color(15, 23, 42));
        UIManager.put("TabbedPane.background", new Color(30, 41, 59));
        UIManager.put("TabbedPane.foreground", Color.WHITE);
        UIManager.put("Table.background", new Color(15, 23, 42));
        UIManager.put("Table.foreground", Color.WHITE);
        UIManager.put("TableHeader.background", new Color(30, 41, 59));
        UIManager.put("TableHeader.foreground", Color.WHITE);
    }

    // ── Business Actions ─────────────────────────────────────────────────────

    private void refreshAlertTable() {
        CompletableFuture.runAsync(() -> {
            try {
                List<AlertRecord> list = alertRepo.findAll();
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (AlertRecord a : list) {
                        tableModel.addRow(new Object[]{
                                a.getId(), a.getFormattedTime(), a.getSeverity(),
                                a.getRuleName(), a.getMitreTechnique(), a.getTargetIp(),
                                a.getUsername(), a.getStatus(), a.getDescription()
                        });
                    }
                    statsLabel.setText(String.format("Total Alerts in JDBC DB: %d | Status: ACTIVE | Target: 127.0.0.1:9090", list.size()));
                });
            } catch (DatabaseException e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Failed to load from JDBC: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE));
            }
        });
    }

    private void applyFilter() {
        String filter = (String) severityFilter.getSelectedItem();
        CompletableFuture.runAsync(() -> {
            try {
                List<AlertRecord> list = alertRepo.findAll();
                if (filter != null && !"ALL".equals(filter)) {
                    list = list.stream()
                            .filter(a -> a.getSeverity().equalsIgnoreCase(filter))
                            .collect(Collectors.toList());
                }
                List<AlertRecord> finalList = list;
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (AlertRecord a : finalList) {
                        tableModel.addRow(new Object[]{
                                a.getId(), a.getFormattedTime(), a.getSeverity(),
                                a.getRuleName(), a.getMitreTechnique(), a.getTargetIp(),
                                a.getUsername(), a.getStatus(), a.getDescription()
                        });
                    }
                });
            } catch (DatabaseException e) {
                e.printStackTrace();
            }
        });
    }

    private void handleExecuteSql(ActionEvent e) {
        String sql = txtSqlQuery.getText().trim();
        if (sql.isEmpty()) return;
        lblSqlStatus.setText("Executing SQL query over JDBC...");
        lblSqlStatus.setForeground(new Color(245, 158, 11));

        CompletableFuture.runAsync(() -> {
            long start = System.currentTimeMillis();
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 Statement stmt = conn.createStatement()) {
                boolean isResultSet = stmt.execute(sql);
                if (isResultSet) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        ResultSetMetaData md = rs.getMetaData();
                        int colCount = md.getColumnCount();
                        Vector<String> columns = new Vector<>();
                        for (int i = 1; i <= colCount; i++) {
                            columns.add(md.getColumnLabel(i));
                        }
                        Vector<Vector<Object>> data = new Vector<>();
                        while (rs.next()) {
                            Vector<Object> row = new Vector<>();
                            for (int i = 1; i <= colCount; i++) {
                                row.add(rs.getObject(i));
                            }
                            data.add(row);
                        }
                        long elapsed = System.currentTimeMillis() - start;
                        SwingUtilities.invokeLater(() -> {
                            sqlResultModel.setDataVector(data, columns);
                            lblSqlStatus.setText(String.format("SQL Query executed successfully in %d ms | %d row(s) returned", elapsed, data.size()));
                            lblSqlStatus.setForeground(new Color(34, 197, 94));
                        });
                    }
                } else {
                    int updated = stmt.getUpdateCount();
                    long elapsed = System.currentTimeMillis() - start;
                    SwingUtilities.invokeLater(() -> {
                        sqlResultModel.setRowCount(0);
                        sqlResultModel.setColumnCount(0);
                        lblSqlStatus.setText(String.format("SQL Statement executed successfully in %d ms | %d row(s) affected", elapsed, updated));
                        lblSqlStatus.setForeground(new Color(56, 189, 248));
                        refreshAlertTable();
                    });
                }
            } catch (SQLException ex) {
                SwingUtilities.invokeLater(() -> {
                    lblSqlStatus.setText("SQL Syntax/Execution Error: " + ex.getMessage());
                    lblSqlStatus.setForeground(new Color(225, 29, 72));
                });
            }
        });
    }

    private void handleCreateCrud(ActionEvent e) {
        try {
            AlertRecord record = new AlertRecord(
                    txtAlertId.getText().trim(),
                    "MANUAL_RULE",
                    txtRuleName.getText().trim(),
                    (String) comboSeverity.getSelectedItem(),
                    txtTargetIp.getText().trim(),
                    txtUsername.getText().trim(),
                    System.currentTimeMillis(),
                    txtDescription.getText().trim(),
                    txtMitre.getText().trim(),
                    (String) comboStatus.getSelectedItem(),
                    "Manually inserted via Swing JDBC GUI"
            );
            alertRepo.save(record);
            JOptionPane.showMessageDialog(this, "Alert successfully inserted into SQLite JDBC database!", "Success", JOptionPane.INFORMATION_MESSAGE);
            txtAlertId.setText("ALT-" + UUID.randomUUID().toString().substring(0, 8));
            refreshAlertTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "CRUD Insert failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleUpdateCrud(ActionEvent e) {
        try {
            String id = txtAlertId.getText().trim();
            boolean ok = alertRepo.updateStatus(id, (String) comboStatus.getSelectedItem(), txtDescription.getText().trim());
            if (ok) {
                JOptionPane.showMessageDialog(this, "Alert " + id + " updated in JDBC database!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshAlertTable();
            } else {
                JOptionPane.showMessageDialog(this, "Record with ID " + id + " not found to update.", "Not Found", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "CRUD Update failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteCrud(ActionEvent e) {
        try {
            String id = txtAlertId.getText().trim();
            boolean ok = alertRepo.deleteById(id);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Alert " + id + " deleted from JDBC database!", "Success", JOptionPane.INFORMATION_MESSAGE);
                refreshAlertTable();
            } else {
                JOptionPane.showMessageDialog(this, "Record with ID " + id + " not found to delete.", "Not Found", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "CRUD Delete failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleUpdateStatus(ActionEvent e) {
        int row = alertTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an alert row to update.", "Select Row", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String id = (String) tableModel.getValueAt(row, 0);
        String[] options = {"INVESTIGATING", "CONFIRMED_INCIDENT", "FALSE_POSITIVE", "RESOLVED"};
        String choice = (String) JOptionPane.showInputDialog(this, "Select new status for " + id + ":",
                "Update Alert Status", JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice != null) {
            try {
                alertRepo.updateStatus(id, choice, "Updated via Swing GUI");
                refreshAlertTable();
            } catch (DatabaseException ex) {
                JOptionPane.showMessageDialog(this, "Update failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteAlert(ActionEvent e) {
        int row = alertTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an alert row to delete.", "Select Row", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String id = (String) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete alert " + id + " permanently from JDBC SQLite?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                alertRepo.deleteById(id);
                refreshAlertTable();
            } catch (DatabaseException ex) {
                JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handlePurgeAlerts(ActionEvent e) {
        int confirm = JOptionPane.showConfirmDialog(this, "Purge ALL alerts from the SQLite database?", "Confirm Purge", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int count = alertRepo.deleteAll();
                JOptionPane.showMessageDialog(this, "Purged " + count + " alerts from JDBC database.", "Purge Complete", JOptionPane.INFORMATION_MESSAGE);
                refreshAlertTable();
            } catch (DatabaseException ex) {
                JOptionPane.showMessageDialog(this, "Purge failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void runAttack(String scenarioKey, String description) {
        logArea.append(String.format("\n[SIMULATION LAUNCHED] %s: %s\nTarget: %s\n",
                scenarioKey, description, targetClient.getBaseUrl()));
        CompletableFuture.runAsync(() -> {
            try {
                switch (scenarioKey) {
                    case "BRUTE_FORCE" -> {
                        for (int i = 0; i < 6; i++) {
                            int code = targetClient.sendLogin("admin", "wrong_pass_" + i, "203.0.113.10");
                            final int reqNum = i + 1;
                            SwingUtilities.invokeLater(() -> logArea.append(String.format("  -> [%d/6] POST /api/login user=admin IP=203.0.113.10 [HTTP %d]\n", reqNum, code)));
                            Thread.sleep(150);
                        }
                    }
                    case "SPRAY" -> {
                        String[] users = {"alice", "bob", "charlie", "admin", "guest"};
                        for (String u : users) {
                            int code = targetClient.sendLogin(u, "Season2026!", "203.0.113.20");
                            SwingUtilities.invokeLater(() -> logArea.append(String.format("  -> POST /api/login user=%s pass=Season2026! IP=203.0.113.20 [HTTP %d]\n", u, code)));
                            Thread.sleep(150);
                        }
                    }
                    case "DISTRIBUTED" -> {
                        for (int i = 1; i <= 5; i++) {
                            final int botIndex = i;
                            final String ip = "198.51.100." + (10 + i);
                            final int code = targetClient.sendLogin("admin", "pass" + i, ip);
                            SwingUtilities.invokeLater(() -> logArea.append(String.format("  -> Bot #%d POST /api/login user=admin IP=%s [HTTP %d]\n", botIndex, ip, code)));
                            Thread.sleep(150);
                        }
                    }
                    case "COMPROMISE" -> {
                        targetClient.sendLogin("alice", "badpass1", "192.0.2.1");
                        targetClient.sendLogin("alice", "badpass2", "192.0.2.1");
                        targetClient.sendLogin("alice", "badpass3", "192.0.2.1");
                        Thread.sleep(200);
                        int code = targetClient.sendLogin("alice", "Wonder@2024", "192.0.2.1");
                        SwingUtilities.invokeLater(() -> logArea.append(String.format("  -> Compromise success: POST /api/login user=alice [HTTP %d]\n", code)));
                    }
                    case "FLOOD" -> {
                        for (int i = 0; i < 15; i++) {
                            int code = targetClient.sendLogin("guest", "flood_" + i, "198.51.100.99");
                            final int burst = i + 1;
                            SwingUtilities.invokeLater(() -> logArea.append(String.format("  -> Flood burst #%d: [HTTP %d]\n", burst, code)));
                            Thread.sleep(30);
                        }
                    }
                    case "NORMAL" -> {
                        int code1 = targetClient.sendLogin("bob", "Builder#77", "192.0.2.2");
                        int code2 = targetClient.sendLogin("adarsh", "Adarsh@123", "192.0.2.10");
                        SwingUtilities.invokeLater(() -> logArea.append(String.format("  -> Normal login bob [HTTP %d], adarsh [HTTP %d]\n", code1, code2)));
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    logArea.append(String.format("[SUCCESS] Scenario %s completed against DemoBank.\nCheck SOC Alert Feed for real-time detections.\n", scenarioKey));
                    refreshAlertTable();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> logArea.append("[ERROR] Attack failed: " + ex.getMessage() + "\n"));
            }
        });
    }

    // ── Custom Severity Table Cell Renderer ─────────────────────────────────

    private static class SeverityRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            String sev = value != null ? value.toString() : "";
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(getFont().deriveFont(Font.BOLD));

            switch (sev.toUpperCase()) {
                case "CRITICAL" -> {
                    c.setBackground(new Color(159, 18, 57));
                    c.setForeground(Color.WHITE);
                }
                case "HIGH" -> {
                    c.setBackground(new Color(194, 65, 12));
                    c.setForeground(Color.WHITE);
                }
                case "MEDIUM" -> {
                    c.setBackground(new Color(161, 98, 7));
                    c.setForeground(Color.WHITE);
                }
                case "LOW" -> {
                    c.setBackground(new Color(21, 128, 61));
                    c.setForeground(Color.WHITE);
                }
                default -> {
                    c.setBackground(new Color(30, 41, 59));
                    c.setForeground(Color.WHITE);
                }
            }
            return c;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SentinelSwingApp app = new SentinelSwingApp();
            app.setVisible(true);
        });
    }
}
