package com.ripple.extras;

import com.ripple.model.Donation;

import java.util.List;

/**
 * Decorator for urgent donations. They show at the top of the table
 * and the description starts with "URGENT: ".
 */
public class UrgentDonation extends DonationDecorator {

    /** Makes a donation urgent. */
    public UrgentDonation(Donation wrapped) {
        super(wrapped);
    }

    @Override
    public boolean isUrgent() {
        return true;
    }

    @Override
    public String describe() {
        return "URGENT: " + wrapped.describe();
    }

    @Override
    public List<String> getOptions() {
        return optionsPlus("Urgent");
    }
}
