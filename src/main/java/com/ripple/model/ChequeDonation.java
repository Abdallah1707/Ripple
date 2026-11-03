package com.ripple.model;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * A cheque donation.
 */
public class ChequeDonation extends Donation {

    private final double amount;
    private final String chequeNumber;

    /** Creates a cheque donation. Use ChequeDonationFactory instead. */
    public ChequeDonation(Donor donor, DonationType donationType, LocalDateTime createdAt,
                          double amount, String chequeNumber) {
        super(donor, donationType, createdAt);
        this.amount = amount;
        this.chequeNumber = chequeNumber;
    }

    public double getAmount() {
        return amount;
    }

    public String getChequeNumber() {
        return chequeNumber;
    }

    @Override
    public DonationMethod getMethod() {
        return DonationMethod.CHEQUE;
    }

    @Override
    public double getValue() {
        return amount;
    }

    @Override
    public String describe() {
        return String.format(Locale.US, "Cheque donation of EGP %,.2f (cheque no. %s)", amount, chequeNumber);
    }
}
