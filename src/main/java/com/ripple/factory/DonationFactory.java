package com.ripple.factory;

import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import com.ripple.service.ValidationException;

import java.time.LocalDateTime;

/**
 * Factory Method creator for donations.
 * orderDonation does the shared checks and then calls createDonation,
 * which each subclass implements for its own donation type.
 */
public abstract class DonationFactory {

    /**
     * Checks the common fields and then creates the donation.
     * Final so subclasses can't change the steps.
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
        // new donations get the current time (CR3), loaded ones keep their saved time
        LocalDateTime createdAt = request.createdAt() != null ? request.createdAt() : LocalDateTime.now();

        Donation donation = createDonation(donor, type, request, createdAt);
        return donation;
    }

    /** Creates this kind of donation and checks its own fields. */
    protected abstract Donation createDonation(Donor donor, DonationType type, DonationRequest request,
                                               LocalDateTime createdAt);

    /** The donation method this factory handles. */
    public abstract DonationMethod getMethod();

    /** Amount must be more than 0. */
    protected static void requirePositiveAmount(double amount) {
        if (!(amount > 0)) { // also rejects NaN
            throw new ValidationException("Amount must be greater than 0");
        }
    }
}
