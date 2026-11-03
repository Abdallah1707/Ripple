package com.ripple.extras;

import com.ripple.model.Donation;

/**
 * The extras a donor can pick: urgent, gift, anonymous.
 */
public record DonationOptions(boolean urgent, String honoree, boolean anonymous) {

    /** No extras. */
    public static final DonationOptions NONE = new DonationOptions(false, "", false);

    /** Trims the honoree, null becomes empty. */
    public DonationOptions {
        honoree = honoree == null ? "" : honoree.trim();
    }

    /** True if there is an honoree. */
    public boolean isGift() {
        return !honoree.isEmpty();
    }

    /**
     * Wraps the donation in a decorator for each chosen extra.
     * Order is always gift, urgent, then anonymous.
     */
    public Donation decorate(Donation base) {
        Donation donation = base;
        if (isGift()) {
            donation = new GiftDonation(donation, honoree);
        }
        if (urgent) {
            donation = new UrgentDonation(donation);
        }
        if (anonymous) {
            donation = new AnonymousDonation(donation);
        }
        return donation;
    }
}
