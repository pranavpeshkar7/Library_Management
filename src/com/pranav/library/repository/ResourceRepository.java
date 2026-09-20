package com.pranav.library.repository;

import com.pranav.library.model.Book;
import com.pranav.library.model.Dvd;
import com.pranav.library.model.LibraryResource;
import com.pranav.library.util.DbConnection;

import java.sql.*;
import java.util.*;

/** JDBC implementation of Repository<LibraryResource, String> over the `resources` table. */
public class ResourceRepository implements Repository<LibraryResource, String> {

    @Override
    public LibraryResource save(LibraryResource resource) throws SQLException {
        String sql = "INSERT INTO resources (id, type, title, author, total_copies, available_copies) " +
                "VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                "title = VALUES(title), author = VALUES(author), total_copies = VALUES(total_copies), " +
                "available_copies = VALUES(available_copies)";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resource.getId());
            ps.setString(2, resource.getType());
            ps.setString(3, resource.getTitle());
            ps.setString(4, resource.getAuthor());
            ps.setInt(5, resource.getTotalCopies());
            ps.setInt(6, resource.getAvailableCopies());
            ps.executeUpdate();
        }
        return resource;
    }

    @Override
    public Optional<LibraryResource> findById(String id) throws SQLException {
        String sql = "SELECT * FROM resources WHERE id = ?";
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
    public List<LibraryResource> findAll() throws SQLException {
        List<LibraryResource> resources = new ArrayList<>();
        String sql = "SELECT * FROM resources ORDER BY title";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) resources.add(mapRow(rs));
        }
        return resources;
    }

    @Override
    public void deleteById(String id) throws SQLException {
        String sql = "DELETE FROM resources WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    /** Atomically persists a copy-count change (used by ResourceManager inside its lock). */
    public void updateAvailableCopies(String id, int availableCopies) throws SQLException {
        String sql = "UPDATE resources SET available_copies = ? WHERE id = ?";
        try (Connection conn = DbConnection.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, availableCopies);
            ps.setString(2, id);
            ps.executeUpdate();
        }
    }

    private LibraryResource mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String type = rs.getString("type");
        String title = rs.getString("title");
        String author = rs.getString("author");
        int totalCopies = rs.getInt("total_copies");
        int availableCopies = rs.getInt("available_copies");

        LibraryResource resource = "DVD".equals(type)
                ? new Dvd(id, title, author, totalCopies)
                : new Book(id, title, author, totalCopies);

        // Reconcile in-memory availableCopies with the persisted value.
        int diff = totalCopies - availableCopies;
        for (int i = 0; i < diff; i++) resource.decrementAvailable();

        return resource;
    }
}
