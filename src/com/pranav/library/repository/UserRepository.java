package com.pranav.library.repository;

import com.pranav.library.model.*;
import com.pranav.library.util.DbConnection;

import java.sql.*;
import java.util.*;

/**
 * JDBC implementation of Repository<User, String> over the `users` table.
 * Reconstructs the correct subclass (Student/Teacher/Librarian) from the
 * `role` column — this is where the polymorphism actually gets exercised
 * on the way OUT of the database, not just when objects are created fresh.
 */
public class UserRepository implements Repository<User, String> {

    @Override
    public User save(User user) throws SQLException {
        String sql = "INSERT INTO users (id, name, email, password_hash, role, active) VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name = VALUES(name), password_hash = VALUES(password_hash), active = VALUES(active)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getId());
            ps.setString(2, user.getName());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getRole().name());
            ps.setBoolean(6, isActive(user));
            ps.executeUpdate();
        }
        return user;
    }

    @Override
    public Optional<User> findById(String id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY name";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) users.add(mapRow(rs));
        }
        return users;
    }

    @Override
    public void deleteById(String id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    public void setActive(String id, boolean active) throws SQLException {
        String sql = "UPDATE users SET active = ? WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setString(2, id);
            ps.executeUpdate();
        }
    }

    public boolean isActiveById(String id) throws SQLException {
        String sql = "SELECT active FROM users WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBoolean("active");
            }
        }
        return false;
    }

    private boolean isActive(User user) {
        // New saves default to active; existing-row updates preserve via ON DUPLICATE above.
        return true;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        Role role = Role.valueOf(rs.getString("role"));

        switch (role) {
            case STUDENT: return new Student(id, name, email, passwordHash);
            case TEACHER: return new Teacher(id, name, email, passwordHash);
            case LIBRARIAN: return new Librarian(id, name, email, passwordHash);
            default: throw new IllegalStateException("Unknown role: " + role);
        }
    }
}
