package com.pranav.library.repository;

import com.pranav.library.model.BorrowRecord;
import com.pranav.library.util.DbConnection;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

public class BorrowRecordRepository implements Repository<BorrowRecord, String> {

    @Override
    public BorrowRecord save(BorrowRecord record) throws SQLException {
        String sql = "INSERT INTO borrow_records (id, user_id, resource_id, borrowed_on, due_date, returned_on) " +
                "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE returned_on = VALUES(returned_on)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, record.getId());
            ps.setString(2, record.getUserId());
            ps.setString(3, record.getResourceId());
            ps.setDate(4, Date.valueOf(record.getBorrowedOn()));
            ps.setDate(5, Date.valueOf(record.getDueDate()));
            ps.setDate(6, record.getReturnedOn() != null ? Date.valueOf(record.getReturnedOn()) : null);
            ps.executeUpdate();
        }
        return record;
    }

    @Override
    public Optional<BorrowRecord> findById(String id) throws SQLException {
        String sql = "SELECT * FROM borrow_records WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<BorrowRecord> findAll() throws SQLException {
        List<BorrowRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM borrow_records ORDER BY borrowed_on DESC";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) records.add(mapRow(rs));
        }
        return records;
    }

    @Override
    public void deleteById(String id) throws SQLException {
        String sql = "DELETE FROM borrow_records WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    /** The one query that matters for correctness: is there an active (unreturned) borrow for this user+resource? */
    public Optional<BorrowRecord> findActiveByUserAndResource(String userId, String resourceId) throws SQLException {
        String sql = "SELECT * FROM borrow_records WHERE user_id = ? AND resource_id = ? AND returned_on IS NULL";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, resourceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public int countActiveByUser(String userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrow_records WHERE user_id = ? AND returned_on IS NULL";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public List<BorrowRecord> findByUser(String userId) throws SQLException {
        List<BorrowRecord> records = new ArrayList<>();
        String sql = "SELECT * FROM borrow_records WHERE user_id = ? ORDER BY borrowed_on DESC";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) records.add(mapRow(rs));
            }
        }
        return records;
    }

    private BorrowRecord mapRow(ResultSet rs) throws SQLException {
        BorrowRecord record = new BorrowRecord(
                rs.getString("id"),
                rs.getString("user_id"),
                rs.getString("resource_id"),
                rs.getDate("borrowed_on").toLocalDate(),
                rs.getDate("due_date").toLocalDate()
        );
        Date returnedOn = rs.getDate("returned_on");
        if (returnedOn != null) record.markReturned(returnedOn.toLocalDate());
        return record;
    }
}
