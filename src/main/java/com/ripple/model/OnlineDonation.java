package com.ripple.model;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * A donation of money paid online, identified by the payment's transaction reference.
 * ConcreteProduct in the Factory pattern.
 */
public class OnlineDonation extends Donation {

    private final double amount;
    private final String transactionReference;

    /**
     * Creates an online donation. Use {@code DonationFactory} instead of calling this directly.
     *
     * @param donor                who gave it
     * @param donationType         its lookup type
     * @param createdAt            when it was recorded
     * @param amount               amount in EGP
     * @param transactionReference the payment provider's reference, e.g. {@code "TX-1001"}
     */
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
