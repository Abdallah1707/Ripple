package com.ripple.repository;

import com.ripple.communication.DeliveryResult;
import com.ripple.communication.Message;
import com.ripple.communication.MessageLogEntry;
import com.ripple.model.ContactChannel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes the {@code message_log} table: every message Ripple tried to send,
 * whether it was sent or skipped. Contains SQL only, no business rules.
 */
public class MessageLogRepository {

    private final Database database;

    /**
     * Creates the repository.
     *
     * @param database the database to use
     */
    public MessageLogRepository(Database database) {
        this.database = database;
    }

    /**
     * Saves one send attempt. A failed attempt keeps its reason in the status, e.g.
     * {@code "FAILED: no valid phone number"}.
     *
     * @param channel the channel that was used
     * @param message the message (its address may be empty if the person had none)
     * @param result  what happened
     */
    public void save(ContactChannel channel, Message message, DeliveryResult result) {
        String sql = "INSERT INTO message_log (channel, recipient, subject, body, status, sent_at) VALUES (?, ?, ?, ?, ?, ?)";
        String recipient = message.address().isBlank()
                ? message.recipientName()
                : message.recipientName() + " <" + message.address() + ">";
        String status = result.success() ? result.status() : result.status() + ": " + result.detail();
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setString(1, channel.name());
            statement.setString(2, recipient);
            statement.setString(3, message.subject());
            statement.setString(4, message.body());
            statement.setString(5, status);
            statement.setString(6, LocalDateTime.now().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the whole message log, newest first.
     *
     * @return all log entries
     */
    public List<MessageLogEntry> findAll() {
        String sql = "SELECT id, channel, recipient, subject, body, status, sent_at FROM message_log ORDER BY id DESC";
        try (Connection c = database.getConnection();
             PreparedStatement statement = c.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            List<MessageLogEntry> result = new ArrayList<>();
            while (rows.next()) {
                result.add(new MessageLogEntry(rows.getInt("id"), ContactChannel.valueOf(rows.getString("channel")),
                        rows.getString("recipient"), rows.getString("subject"), rows.getString("body"),
                        rows.getString("status"), LocalDateTime.parse(rows.getString("sent_at"))));
            }
            return result;
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }
}
