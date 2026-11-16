package com.sentinel.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JDBC implementation of CrudRepository for AlertRecord entities.
 * Demonstrates:
 * - OOP: Inheritance (extends AbstractJdbcRepository), Polymorphism (implements CrudRepository).
 * - Advanced Java: Exception handling with try-with-resources, Collections (List, Optional), Java Streams.
 * - Database: Full CRUD operations via JDBC PreparedStatement and ResultSet.
 */
public class JdbcAlertRepository extends AbstractJdbcRepository<AlertRecord, String> {

    public JdbcAlertRepository(DatabaseManager dbManager) {
        super(dbManager);
    }

    @Override
    public AlertRecord save(AlertRecord alert) throws DatabaseException {
        String sql = """
            INSERT INTO security_alerts
            (id, rule_id, rule_name, severity, target_ip, username, timestamp, description, mitre_technique, status, resolution_notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
            status = excluded.status,
            resolution_notes = excluded.resolution_notes;
            """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alert.getId());
            ps.setString(2, alert.getRuleId());
            ps.setString(3, alert.getRuleName());
            ps.setString(4, alert.getSeverity());
            ps.setString(5, alert.getTargetIp());
            ps.setString(6, alert.getUsername());
            ps.setLong(7, alert.getTimestamp());
            ps.setString(8, alert.getDescription());
            ps.setString(9, alert.getMitreTechnique());
            ps.setString(10, alert.getStatus());
            ps.setString(11, alert.getResolutionNotes());
            ps.executeUpdate();
            return alert;
        } catch (SQLException e) {
            throw wrapSqlException("save(AlertRecord)", e);
        }
    }

    @Override
    public Optional<AlertRecord> findById(String id) throws DatabaseException {
        String sql = "SELECT * FROM security_alerts WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw wrapSqlException("findById(" + id + ")", e);
        }
    }

    @Override
    public List<AlertRecord> findAll() throws DatabaseException {
        String sql = "SELECT * FROM security_alerts ORDER BY timestamp DESC";
        List<AlertRecord> list = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw wrapSqlException("findAll()", e);
        }
    }

    /**
     * Demonstrates Java Streams to filter and sort alerts by severity.
     */
    public List<AlertRecord> findBySeverity(String severity) throws DatabaseException {
        return findAll().stream()
                .filter(a -> a.getSeverity().equalsIgnoreCase(severity))
                .collect(Collectors.toList());
    }

    @Override
    public boolean update(AlertRecord alert) throws DatabaseException {
        String sql = """
            UPDATE security_alerts
            SET rule_name = ?, severity = ?, description = ?, status = ?, resolution_notes = ?
            WHERE id = ?
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alert.getRuleName());
            ps.setString(2, alert.getSeverity());
            ps.setString(3, alert.getDescription());
            ps.setString(4, alert.getStatus());
            ps.setString(5, alert.getResolutionNotes());
            ps.setString(6, alert.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw wrapSqlException("update(AlertRecord)", e);
        }
    }

    public boolean updateStatus(String id, String newStatus, String notes) throws DatabaseException {
        String sql = "UPDATE security_alerts SET status = ?, resolution_notes = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setString(2, notes);
            ps.setString(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw wrapSqlException("updateStatus", e);
        }
    }

    @Override
    public boolean deleteById(String id) throws DatabaseException {
        String sql = "DELETE FROM security_alerts WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw wrapSqlException("deleteById(" + id + ")", e);
        }
    }

    public int deleteAll() throws DatabaseException {
        String sql = "DELETE FROM security_alerts";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        } catch (SQLException e) {
            throw wrapSqlException("deleteAll()", e);
        }
    }

    @Override
    public long count() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM security_alerts";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw wrapSqlException("count()", e);
        }
    }

    private AlertRecord mapRow(ResultSet rs) throws SQLException {
        return new AlertRecord(
                rs.getString("id"),
                rs.getString("rule_id"),
                rs.getString("rule_name"),
                rs.getString("severity"),
                rs.getString("target_ip"),
                rs.getString("username"),
                rs.getLong("timestamp"),
                rs.getString("description"),
                rs.getString("mitre_technique"),
                rs.getString("status"),
                rs.getString("resolution_notes")
        );
    }
}
