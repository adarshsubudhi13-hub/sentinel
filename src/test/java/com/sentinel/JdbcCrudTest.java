package com.sentinel;

import com.sentinel.database.AlertRecord;
import com.sentinel.database.DatabaseException;
import com.sentinel.database.DatabaseManager;
import com.sentinel.database.JdbcAlertRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test verifying JDBC Database connectivity and full CRUD operations.
 * Demonstrates testing of:
 * - Create: Inserting alert records
 * - Read: Finding by ID, finding all, finding by severity via Java Streams
 * - Update: Modifying status and resolution notes
 * - Delete: Removing records and purging
 */
class JdbcCrudTest {

    private DatabaseManager dbManager;
    private JdbcAlertRepository repository;
    private final String testDbPath = "data/test_sentinel.db";

    @BeforeEach
    void setUp() throws DatabaseException {
        new File("data").mkdirs();
        new File(testDbPath).delete();
        dbManager = new DatabaseManager(testDbPath);
        repository = new JdbcAlertRepository(dbManager);
    }

    @AfterEach
    void tearDown() {
        new File(testDbPath).delete();
    }

    @Test
    @DisplayName("Create (Insert) and Read (Select) operations via JDBC")
    void testCreateAndRead() throws DatabaseException {
        AlertRecord alert = new AlertRecord(
                "TEST-001",
                "R1",
                "Brute Force Detected",
                "HIGH",
                "192.168.1.50",
                "alice",
                System.currentTimeMillis(),
                "Exceeded failure threshold",
                "T1110",
                "NEW",
                "Auto-generated test"
        );

        // 1. Create
        AlertRecord saved = repository.save(alert);
        assertNotNull(saved);
        assertEquals("TEST-001", saved.getId());

        // 2. Read by ID
        Optional<AlertRecord> fetched = repository.findById("TEST-001");
        assertTrue(fetched.isPresent());
        assertEquals("HIGH", fetched.get().getSeverity());
        assertEquals("alice", fetched.get().getUsername());
        assertEquals("T1110", fetched.get().getMitreTechnique());

        // 3. Read All
        List<AlertRecord> all = repository.findAll();
        assertEquals(1, all.size());
        assertEquals(1L, repository.count());
    }

    @Test
    @DisplayName("Update (Modify) operations via JDBC PreparedStatement")
    void testUpdate() throws DatabaseException {
        AlertRecord alert = new AlertRecord(
                "TEST-002",
                "R4",
                "Account Takeover",
                "CRITICAL",
                "10.0.0.1",
                "bob",
                System.currentTimeMillis(),
                "Compromise detected",
                "T1078",
                "NEW",
                "Initial alert"
        );
        repository.save(alert);

        // Update status
        boolean updated = repository.updateStatus("TEST-002", "RESOLVED", "Investigated and contained by SOC");
        assertTrue(updated);

        Optional<AlertRecord> fetched = repository.findById("TEST-002");
        assertTrue(fetched.isPresent());
        assertEquals("RESOLVED", fetched.get().getStatus());
        assertEquals("Investigated and contained by SOC", fetched.get().getResolutionNotes());
    }

    @Test
    @DisplayName("Delete (Remove) operations via JDBC PreparedStatement")
    void testDelete() throws DatabaseException {
        AlertRecord a1 = new AlertRecord("DEL-1", "R1", "Alert 1", "LOW", "1.1.1.1", "user1", System.currentTimeMillis(), "", "T1110", "NEW", "");
        AlertRecord a2 = new AlertRecord("DEL-2", "R2", "Alert 2", "CRITICAL", "1.1.1.2", "user2", System.currentTimeMillis(), "", "T1110.003", "NEW", "");

        repository.save(a1);
        repository.save(a2);
        assertEquals(2L, repository.count());

        // Delete single
        boolean deleted = repository.deleteById("DEL-1");
        assertTrue(deleted);
        assertEquals(1L, repository.count());
        assertTrue(repository.findById("DEL-1").isEmpty());
        assertTrue(repository.findById("DEL-2").isPresent());

        // Delete all
        int purged = repository.deleteAll();
        assertEquals(1, purged);
        assertEquals(0L, repository.count());
    }

    @Test
    @DisplayName("Java Streams filter by severity on JDBC result sets")
    void testStreamsFilter() throws DatabaseException {
        repository.save(new AlertRecord("S1", "R1", "A1", "CRITICAL", "1.1.1.1", "u1", System.currentTimeMillis(), "", "T1110", "NEW", ""));
        repository.save(new AlertRecord("S2", "R2", "A2", "HIGH", "1.1.1.2", "u2", System.currentTimeMillis(), "", "T1110", "NEW", ""));
        repository.save(new AlertRecord("S3", "R3", "A3", "CRITICAL", "1.1.1.3", "u3", System.currentTimeMillis(), "", "T1110", "NEW", ""));

        List<AlertRecord> criticals = repository.findBySeverity("CRITICAL");
        assertEquals(2, criticals.size());

        List<AlertRecord> highs = repository.findBySeverity("HIGH");
        assertEquals(1, highs.size());
    }
}
