package com.ripple.factory;

import com.ripple.model.CashDonation;
import com.ripple.model.Donation;
import com.ripple.model.DonationMethod;
import com.ripple.model.DonationType;
import com.ripple.model.Donor;

import java.time.LocalDateTime;

/** Creates cash donations. */
public class CashDonationFactory extends DonationFactory {

    @Override
    protected Donation createDonation(Donor donor, DonationType type, DonationRequest request,
                                      LocalDateTime createdAt) {
        requirePositiveAmount(request.amount());
        return new CashDonation(donor, type, createdAt, request.amount());
    }

    @Override
    public DonationMethod getMethod() {
        return DonationMethod.CASH;
    }
}
