package com.ripple.factory;

import java.time.LocalDateTime;

/**
 * Donation details passed to the factory. Each factory only uses the fields it needs.
 * createdAt null means now.
 */
public record DonationRequest(double amount,
                              String transactionReference,
                              String itemDescription,
                              int quantity,
                              double estimatedValue,
                              LocalDateTime createdAt) {
}
