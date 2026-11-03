package com.ripple.extras;

import com.ripple.model.Donation;

import java.util.List;

/**
 * Concrete decorator: a donation given in honor of someone (a gift). The honoree's name is
 * added to the description, so it appears on the receipt and in the donations table.
 */
public class GiftDonation extends DonationDecorator {

    private final String honoree;

    /**
     * Makes a donation a gift.
     *
     * @param wrapped the donation to decorate
     * @param honoree who the donation honors, e.g. {@code "Mona Ali"} (already checked by the service)
     */
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
