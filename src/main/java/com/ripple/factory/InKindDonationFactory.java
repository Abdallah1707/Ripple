package com.ripple.factory;

import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;
import com.ripple.model.InKindDonation;
import com.ripple.service.InputValidator;
import com.ripple.service.ValidationException;

import java.time.LocalDateTime;

/**
 * Creates in-kind donations (goods instead of money).
 */
public class InKindDonationFactory extends DonationFactory {

    @Override
    protected Donation createDonation(Donor donor, DonationType type, DonationRequest request,
                                      LocalDateTime createdAt) {
        String item = InputValidator.requireText(request.itemDescription(), "Item description", 2, 100);
        if (request.quantity() <= 0) {
            throw new ValidationException("Quantity must be greater than 0");
        }
        if (!(request.estimatedValue() >= 0)) { // also rejects NaN
            throw new ValidationException("Estimated value cannot be negative");
        }
        return new InKindDonation(donor, type, createdAt, item, request.quantity(), request.estimatedValue());
    }

    @Override
    public DonationMethod getMethod() {
        return DonationMethod.IN_KIND;
    }
}
