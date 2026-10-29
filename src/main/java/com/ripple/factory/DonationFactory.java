package com.ripple.factory;

import com.ripple.model.CashDonation;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import com.ripple.model.InKindDonation;
import com.ripple.model.OnlineDonation;
import com.ripple.service.InputValidator;
import com.ripple.service.ValidationException;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Factory in the Factory pattern: the <b>only</b> place that decides which {@link Donation}
 * subclass to create, and checks that the details make sense for that kind of donation.
 *
 * <p>Callers ask for "a donation of method X" and get back a {@code Donation}; they never
 * call {@code new CashDonation(...)} themselves. Adding a new method (e.g. cheques) means
 * one new subclass and one new {@code case} here, and nothing else changes.
 *
 * <p>Used when a new donation is recorded and also when donations are loaded from the
 * database, so every donation object in the app comes from here.
 */
public class DonationFactory {

    /**
     * Creates the right kind of donation for {@code method}.
     *
     * @param method  how the donation was given
     * @param donor   who gave it
     * @param type    its donation type (lookup)
     * @param request the details; only the fields for {@code method} are used
     * @return a {@link CashDonation}, {@link OnlineDonation} or {@link InKindDonation}
     * @throws ValidationException if anything is missing or the details are invalid
     */
    public Donation create(DonationMethod method, Donor donor, DonationType type, DonationRequest request) {
        if (method == null) {
            throw new ValidationException("Donation method is required");
        }
        if (donor == null) {
            throw new ValidationException("Donor is required");
        }
        if (type == null) {
            throw new ValidationException("Donation type is required");
        }
        if (request == null) {
            throw new ValidationException("Donation details are required");
        }
        return switch (method) {
            case CASH -> createCash(donor, type, request);
            case ONLINE -> createOnline(donor, type, request);
            case IN_KIND -> createInKind(donor, type, request);
        };
    }

    /**
     * Converts a method name typed or chosen by the user into a {@link DonationMethod}.
     * Accepts the enum name or the label, ignoring case: {@code "online"}, {@code "In-kind"}.
     *
     * @param name the method name
     * @return the matching method
     * @throws ValidationException if the name is blank or not a supported method
     */
    public DonationMethod parseMethod(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Donation method is required");
        }
        String key = name.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return DonationMethod.valueOf(key);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Unsupported donation method: " + name.trim());
        }
    }

    private CashDonation createCash(Donor donor, DonationType type, DonationRequest request) {
        requirePositiveAmount(request.amount());
        return new CashDonation(donor, type, createdAtOf(request), request.amount());
    }

    private OnlineDonation createOnline(Donor donor, DonationType type, DonationRequest request) {
        requirePositiveAmount(request.amount());
        String reference = InputValidator.requireText(request.transactionReference(), "Transaction reference", 3, 50);
        return new OnlineDonation(donor, type, createdAtOf(request), request.amount(), reference);
    }

    private InKindDonation createInKind(Donor donor, DonationType type, DonationRequest request) {
        String item = InputValidator.requireText(request.itemDescription(), "Item description", 2, 100);
        if (request.quantity() <= 0) {
            throw new ValidationException("Quantity must be greater than 0");
        }
        if (!(request.estimatedValue() >= 0)) { // written this way so NaN is rejected too
            throw new ValidationException("Estimated value cannot be negative");
        }
        return new InKindDonation(donor, type, createdAtOf(request), item, request.quantity(), request.estimatedValue());
    }

    private static void requirePositiveAmount(double amount) {
        if (!(amount > 0)) {
            throw new ValidationException("Amount must be greater than 0");
        }
    }

    /** New donations are stamped with the current time (CR3); loaded ones keep their stored time. */
    private static LocalDateTime createdAtOf(DonationRequest request) {
        return request.createdAt() != null ? request.createdAt() : LocalDateTime.now();
    }
}
