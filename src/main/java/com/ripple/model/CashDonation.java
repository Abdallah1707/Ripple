package com.ripple.model;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * A cash donation.
 */
public class CashDonation extends Donation {

    private final double amount;

    /** Creates a cash donation. Use CashDonationFactory instead. */
    public CashDonation(Donor donor, DonationType donationType, LocalDateTime createdAt, double amount) {
        super(donor, donationType, createdAt);
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public DonationMethod getMethod() {
        return DonationMethod.CASH;
    }

    @Override
    public double getValue() {
        return amount;
    }

    @Override
    public String describe() {
        return String.format(Locale.US, "Cash donation of EGP %,.2f", amount);
    }
}
