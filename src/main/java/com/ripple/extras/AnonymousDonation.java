package com.ripple.extras;

import com.ripple.model.Donation;

import java.util.List;

/**
 * Concrete decorator: the donor asked not to be named publicly. Everywhere the donor is shown
 * to others (donations table, log, Dashboard activity) "Anonymous donor" is shown instead.
 *
 * <p>Only the <i>display name</i> changes: {@link #getDonor()} still returns the real donor,
 * so they still get their own receipt, tax information and thank-you message.
 */
public class AnonymousDonation extends DonationDecorator {

    /** What is shown instead of the donor's name. */
    public static final String DISPLAY_NAME = "Anonymous donor";

    /**
     * Makes a donation anonymous.
     *
     * @param wrapped the donation to decorate
     */
    public AnonymousDonation(Donation wrapped) {
        super(wrapped);
    }

    @Override
    public String getDonorDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public boolean isAnonymous() {
        return true;
    }

    @Override
    public List<String> getOptions() {
        return optionsPlus("Anonymous");
    }
}
