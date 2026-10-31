package com.ripple.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Reads and writes the {@code receipts} table. Contains SQL only, no business rules: the
 * rendered text comes from {@code ReceiptObserver}, which builds it with the Decorator pattern.
 */
public class ReceiptRepository {

    private final Database database;

    /**
     * Creates the repository.
     *
     * @param database the database to use
     */
    public ReceiptRepository(Database database) {
        this.database = database;
    }

    /**
     * Saves the rendered text of a donation's receipt. There is at most one receipt per
     * donation, so saving again for the same donation replaces the previous text.
     *
     * @param donationId id of the donation the receipt belongs to
     * @param content    the rendered receipt text
     */
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

    /**
     * Finds the saved receipt text for a donation.
     *
     * @param donationId id of the donation
     * @return the receipt text, or empty if no receipt was saved for it
     */
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
