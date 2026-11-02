package com.ripple.communication;

/**
 * One message ready to be sent by a {@link CommunicationStrategy}.
 *
 * @param recipientName the person's display name
 * @param address       where to send it: an email address or a phone number, depending on the strategy
 * @param subject       the subject line (SMS ignores it)
 * @param body          the message text
 */
public record Message(String recipientName, String address, String subject, String body) {
}
