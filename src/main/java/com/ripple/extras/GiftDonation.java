package com.ripple.extras;

import com.ripple.model.Donation;

import java.util.List;

/**
 * Decorator for a donation given in honor of someone.
 * Adds the honoree's name to the description.
 */
public class GiftDonation extends DonationDecorator {

    private final String honoree;

    /** Makes a donation a gift. */
    public GiftDonation(Donation wrapped, String honoree) {
        super(wrapped);
        this.honoree = honoree;
    }

    @Override
    public String getHonoree() {
        return honoree;
    }

    @Override
    public String describe() {
        return wrapped.describe() + " (gift in honor of " + honoree + ")";
    }

    @Override
    public List<String> getOptions() {
        return optionsPlus("Gift");
    }
}
