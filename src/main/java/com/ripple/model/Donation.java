package com.ripple.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A donation: who gave it, its type, and when it was recorded.
 *
 * <p>This is the <b>Product</b> in the Factory Method pattern. The four kinds of donation
 * ({@link CashDonation}, {@link OnlineDonation}, {@link InKindDonation}, {@link ChequeDonation})
 * are created only by the subclasses of {@code DonationFactory}, one creator per kind. The rest of the app works with {@code Donation} and calls
 * {@link #getValue()} and {@link #describe()} without knowing which kind it has
 * (polymorphism).
 *
 * <p>It is also the <b>Component</b> in a Decorator pattern: {@code DonationDecorator} (package
 * {@code com.ripple.extras}) wraps a donation to make it urgent, a gift or anonymous, in any combination. The methods from
 * {@link #getBase()} down are the hooks those decorators change; a plain donation answers them
 * with the defaults below.
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

    // ---------------- Decorator hooks: a plain donation's answers ----------------

    /**
     * Returns the plain donation inside any decorators. Saving needs it, because only the plain
     * donation knows its own fields (amount, cheque number...).
     *
     * @return this donation; a decorator returns the base of the donation it wraps
     */
    public Donation getBase() {
        return this;
    }

    /**
     * Returns the donor's name as it may be shown to everyone (lists, log, Dashboard).
     *
     * @return the donor's name; "Anonymous donor" when the donation is anonymous
     */
    public String getDonorDisplayName() {
        return getDonor().getName();
    }

    /**
     * Tells whether the donation is for an urgent need and should be handled first.
     *
     * @return {@code false} unless decorated as urgent
     */
    public boolean isUrgent() {
        return false;
    }

    /**
     * Returns the person the donation is given in honor of.
     *
     * @return the honoree's name, or an empty string if the donation is not a gift
     */
    public String getHonoree() {
        return "";
    }

    /**
     * Tells whether the donor asked not to be named publicly.
     *
     * @return {@code false} unless decorated as anonymous
     */
    public boolean isAnonymous() {
        return false;
    }

    /**
     * Returns the names of the options added by decorators, for the donations table.
     *
     * @return e.g. {@code ["Gift", "Urgent"]}; empty for a plain donation
     */
    public List<String> getOptions() {
        return List.of();
    }
}
