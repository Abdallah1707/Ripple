package com.ripple.communication;

import com.ripple.model.ContactChannel;

import java.time.LocalDateTime;

/**
 * One row of the message log.
 */
public record MessageLogEntry(int id, ContactChannel channel, String recipient, String subject, String body,
                              String status, LocalDateTime sentAt) {
}
