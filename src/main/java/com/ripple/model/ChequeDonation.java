package com.ripple.model;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * A donation of money paid by cheque, identified by the cheque number.
 * ConcreteProduct in the Factory Method pattern, created by {@code ChequeDonationFactory}.
 */
public class ChequeDonation extends Donation {

    private final double amount;
    private final String chequeNumber;

    /**
     * Creates a cheque donation. Use {@code ChequeDonationFactory} instead of calling this directly.
     *
     * @param donor        who gave it
     * @param donationType its lookup type
     * @param createdAt    when it was recorded
     * @param amount       amount in EGP
     * @param chequeNumber the number printed on the cheque, e.g. {@code "000123"}
     */
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
