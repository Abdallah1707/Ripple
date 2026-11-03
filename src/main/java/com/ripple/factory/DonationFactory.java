package com.ripple.factory;

import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import com.ripple.service.ValidationException;

import java.time.LocalDateTime;

/**
 * Creator in the Factory Method pattern (like {@code PizzaStore} in the lab slides).
 *
 * <p>{@link #orderDonation} is the common workflow that every kind of donation goes through
 * (like {@code orderPizza()}): check the shared details, stamp the time, then call the
 * <b>factory method</b> {@link #createDonation}. Each subclass ({@link CashDonationFactory},
 * {@link OnlineDonationFactory}, {@link InKindDonationFactory}, {@link ChequeDonationFactory})
 * is a Concrete Creator: it decides which {@link Donation} subclass to create and checks the
 * fields that only that kind needs.
 *
 * <p>Callers hold a {@code DonationFactory} and never name a concrete donation class. Adding a
 * new method means one new product and one new creator; this class and its callers do not
 * change ("closed for modification, open for extension").
 */
public abstract class DonationFactory {

    /**
     * The common workflow: checks what every donation needs, then lets the subclass create it.
     * It is {@code final} so a subclass can change <i>what</i> is created but not the steps
     * (the Hollywood principle: "don't call us, we'll call you").
     *
     * @param donor   who gave it
     * @param type    its donation type (lookup)
     * @param request the details; each creator reads only its own fields
     * @return the new donation (not saved yet)
     * @throws ValidationException if anything is missing or the details are invalid
     */
    public final Donation orderDonation(Donor donor, DonationType type, DonationRequest request) {
        if (donor == null) {
            throw new ValidationException("Donor is required");
        }
        if (type == null) {
            throw new ValidationException("Donation type is required");
        }
        if (request == null) {
            throw new ValidationException("Donation details are required");
        }
        // New donations are stamped with the current time (CR3); loaded ones keep their stored time.
        LocalDateTime createdAt = request.createdAt() != null ? request.createdAt() : LocalDateTime.now();

        Donation donation = createDonation(donor, type, request, createdAt);
        return donation;
    }

    /**
     * The factory method: creates this creator's kind of donation and checks its own fields.
     *
     * @param donor     who gave it (never {@code null})
     * @param type      its donation type (never {@code null})
     * @param request   the details (never {@code null})
     * @param createdAt when it was recorded
     * @return the new donation
     * @throws ValidationException if the details are invalid for this kind of donation
     */
    protected abstract Donation createDonation(Donor donor, DonationType type, DonationRequest request,
                                               LocalDateTime createdAt);

    /**
     * Tells which method this creator handles. {@code Main} uses it as the key of the
     * creators map, so the right creator is picked without any {@code if/else}.
     *
     * @return the donation method this creator makes
     */
    public abstract DonationMethod getMethod();

    /**
     * Shared rule for the money donations (cash, online, cheque).
     *
     * @param amount the amount in EGP
     * @throws ValidationException if the amount is not greater than 0
     */
    protected static void requirePositiveAmount(double amount) {
        if (!(amount > 0)) { // written this way so NaN is rejected too
            throw new ValidationException("Amount must be greater than 0");
        }
    }
}
