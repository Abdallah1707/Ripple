package com.ripple.extras;

import com.ripple.model.Donation;

import java.util.List;

/**
 * Concrete decorator: marks a donation as meant for an urgent need, so staff handle it first.
 * The donations table lists urgent donations at the top, and the description starts with
 * "URGENT: " everywhere it is shown (table, log, receipt).
 */
public class UrgentDonation extends DonationDecorator {

    /**
     * Makes a donation urgent.
     *
     * @param wrapped the donation to decorate
     */
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
