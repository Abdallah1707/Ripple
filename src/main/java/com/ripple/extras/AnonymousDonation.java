package com.ripple.extras;

import com.ripple.model.Donation;

import java.util.List;

/**
 * Decorator that hides the donor's name from others.
 * getDonor() still returns the real donor so they still get their receipt.
 */
public class AnonymousDonation extends DonationDecorator {

    /** Shown instead of the donor's name. */
    public static final String DISPLAY_NAME = "Anonymous donor";

    /** Makes a donation anonymous. */
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
