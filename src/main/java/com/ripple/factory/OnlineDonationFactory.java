package com.ripple.factory;

import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import com.ripple.model.OnlineDonation;
import com.ripple.service.InputValidator;

import java.time.LocalDateTime;

/**
 * Concrete Creator in the Factory Method pattern: creates {@link OnlineDonation}s, which also
 * need the payment's transaction reference.
 */
public class OnlineDonationFactory extends DonationFactory {

    @Override
    protected Donation createDonation(Donor donor, DonationType type, DonationRequest request,
                                      LocalDateTime createdAt) {
        requirePositiveAmount(request.amount());
        String reference = InputValidator.requireText(request.transactionReference(), "Transaction reference", 3, 50);
        return new OnlineDonation(donor, type, createdAt, request.amount(), reference);
    }

    @Override
    public DonationMethod getMethod() {
        return DonationMethod.ONLINE;
    }
}
