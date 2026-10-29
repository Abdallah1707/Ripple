package com.ripple.model;

import java.time.LocalDateTime;

/**
 * A donation: who gave it, its type, and when it was recorded.
 *
 * <p>This is the <b>Product</b> in the Factory pattern. The three kinds of donation
 * ({@link CashDonation}, {@link OnlineDonation}, {@link InKindDonation}) are created only by
 * {@code DonationFactory}. The rest of the app works with {@code Donation} and calls
 * {@link #getValue()} and {@link #describe()} without knowing which kind it has
 * (polymorphism).
 */
public abstract class Donation {

    private int id;
    private final Donor donor;
    private final DonationType donationType;
    private final LocalDateTime createdAt;

    /**
     * Creates a donation.
     *
     * @param donor        who gave it
     * @param donationType its lookup type
     * @param createdAt    when it was recorded (CR3)
     */
    protected Donation(Donor donor, DonationType donationType, LocalDateTime createdAt) {
        this.donor = donor;
        this.donationType = donationType;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    /**
     * Sets the id given by the database after the donation is saved.
     *
     * @param id the generated id
     */
    public void setId(int id) {
        this.id = id;
    }

    public Donor getDonor() {
        return donor;
    }

    public DonationType getDonationType() {
        return donationType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns how this donation was given.
     *
     * @return the donation method
     */
    public abstract DonationMethod getMethod();

    /**
     * Returns the donation's value in EGP: the amount for money, the estimated value for goods.
     *
     * @return the value in EGP
     */
    public abstract double getValue();

    /**
     * Returns a one-line description for tables and receipts.
     *
     * @return e.g. {@code "Cash donation of EGP 500.00"}
     */
    public abstract String describe();

    /**
     * Returns the receipt number, built from the year and the id (CR3).
     *
     * @return e.g. {@code "RCP-2026-0007"}
     */
    public String getReceiptNumber() {
        return String.format("RCP-%d-%04d", createdAt.getYear(), id);
    }

    /**
     * Tells whether this donation is tax-deductible, which depends on its donation type (CR4).
     *
     * @return {@code true} if the donation type is tax-deductible
     */
    public boolean isTaxDeductible() {
        return donationType.isTaxDeductible();
    }
}
