package com.ripple.model;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * An online donation with a transaction reference.
 */
public class OnlineDonation extends Donation {

    private final double amount;
    private final String transactionReference;

    /** Creates an online donation. Use OnlineDonationFactory instead. */
    public OnlineDonation(Donor donor, DonationType donationType, LocalDateTime createdAt,
                          double amount, String transactionReference) {
        super(donor, donationType, createdAt);
        this.amount = amount;
        this.transactionReference = transactionReference;
    }

    public double getAmount() {
        return amount;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    @Override
    public DonationMethod getMethod() {
        return DonationMethod.ONLINE;
    }

    @Override
    public double getValue() {
        return amount;
    }

    @Override
    public String describe() {
        return String.format(Locale.US, "Online donation of EGP %,.2f (ref %s)", amount, transactionReference);
    }
}
