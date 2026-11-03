package com.ripple.factory;

import java.time.LocalDateTime;

/**
 * The details of a donation, already converted from text to numbers, handed to
 * {@link DonationFactory#orderDonation}. Each creator uses only its own fields; the others are ignored:
 * <ul>
 *   <li>Cash: {@code amount}</li>
 *   <li>Online: {@code amount}, {@code transactionReference}</li>
 *   <li>In-kind: {@code itemDescription}, {@code quantity}, {@code estimatedValue}</li>
 *   <li>Cheque: {@code amount}, {@code transactionReference} (the cheque number)</li>
 * </ul>
 *
 * @param amount               money amount in EGP (cash, online, cheque)
 * @param transactionReference payment reference (online) or cheque number (cheque)
 * @param itemDescription      what was given (in-kind)
 * @param quantity             how many items (in-kind)
 * @param estimatedValue       estimated value in EGP (in-kind)
 * @param createdAt            when the donation was recorded; {@code null} means "now"
 *                             (a stored time is passed when loading from the database)
 */
public record DonationRequest(double amount,
                              String transactionReference,
                              String itemDescription,
                              int quantity,
                              double estimatedValue,
                              LocalDateTime createdAt) {
}
