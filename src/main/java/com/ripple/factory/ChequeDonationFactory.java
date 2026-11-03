package com.ripple.factory;

import com.ripple.model.ChequeDonation;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import com.ripple.service.InputValidator;

import java.time.LocalDateTime;

/**
 * Creates cheque donations. The cheque number is in transactionReference.
 */
public class ChequeDonationFactory extends DonationFactory {

    @Override
    protected Donation createDonation(Donor donor, DonationType type, DonationRequest request,
                                      LocalDateTime createdAt) {
        requirePositiveAmount(request.amount());
        String chequeNumber = InputValidator.requireText(request.transactionReference(), "Cheque number", 3, 30);
        return new ChequeDonation(donor, type, createdAt, request.amount(), chequeNumber);
    }

    @Override
    public DonationMethod getMethod() {
        return DonationMethod.CHEQUE;
    }
}
