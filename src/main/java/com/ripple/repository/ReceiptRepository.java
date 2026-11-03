package com.ripple.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Database access for receipts.
 */
public class ReceiptRepository {

    private final Database database;

    /** Creates the repository. */
    public ReceiptRepository(Database database) {
        this.database = database;
    }

    /** Saves a receipt. Replaces the old one if it exists. */
    public void save(int donationId, String content) {
        String sql = """
                INSERT INTO receipts (donation_id, content, created_at) VALUES (?, ?, ?)
                ON CONFLICT(donation_id) DO UPDATE SET content = excluded.content, created_at = excluded.created_at
                """;
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setInt(1, donationId);
            statement.setString(2, content);
            statement.setString(3, LocalDateTime.now().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /** Finds the receipt for a donation. */
    public Optional<String> findByDonationId(int donationId) {
        String sql = "SELECT content FROM receipts WHERE donation_id = ?";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setInt(1, donationId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(rows.getString("content")) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }
}
