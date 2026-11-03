package com.ripple.communication;

/**
 * A message ready to be sent.
 */
public record Message(String recipientName, String address, String subject, String body) {
}
