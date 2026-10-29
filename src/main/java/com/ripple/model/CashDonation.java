package com.ripple.model;

import java.time.LocalDateTime;
import java.util.Locale;

/** A donation of money handed over in cash. ConcreteProduct in the Factory pattern. */
public class CashDonation extends Donation {

    private final double amount;

    /**
     * Creates a cash donation. Use {@code DonationFactory} instead of calling this directly.
     *
     * @param donor        who gave it
     * @param donationType its lookup type
     * @param createdAt    when it was recorded
     * @param amount       amount in EGP
     */
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
