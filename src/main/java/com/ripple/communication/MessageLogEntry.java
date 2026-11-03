package com.ripple.communication;

import com.ripple.model.ContactChannel;

import java.time.LocalDateTime;

/**
 * One row of the message log: a message Ripple sent or skipped.
 *
 * @param id        database id
 * @param channel   Email, SMS or Social
 * @param recipient the person's name and the address used, e.g. {@code "Mona Ali <mona@example.com>"}
 * @param subject   the subject line
 * @param body      the message text
 * @param status    {@code "SIMULATED"}, or {@code "FAILED: "} followed by the reason
 * @param sentAt    when the attempt was made
 */
public record MessageLogEntry(int id, ContactChannel channel, String recipient, String subject, String body,
                              String status, LocalDateTime sentAt) {
}
