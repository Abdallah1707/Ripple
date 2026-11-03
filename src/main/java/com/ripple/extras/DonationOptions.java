package com.ripple.extras;

import com.ripple.model.Donation;

/**
 * The optional extras a donor can choose: urgent, a gift in honor of someone, anonymous.
 *
 * <p>{@link #decorate(Donation)} is the one place that turns these choices into decorators, so a
 * donation is wrapped the same way when it is recorded and when it is loaded from the database.
 *
 * @param urgent    whether the donation is for an urgent need
 * @param honoree   who the donation honors, or an empty string if it is not a gift
 * @param anonymous whether the donor should not be named publicly
 */
public record DonationOptions(boolean urgent, String honoree, boolean anonymous) {

    /** No extras: a plain donation. */
    public static final DonationOptions NONE = new DonationOptions(false, "", false);

    /** Turns a missing honoree into an empty string and trims the name. */
    public DonationOptions {
        honoree = honoree == null ? "" : honoree.trim();
    }

    /**
     * Tells whether the donation is a gift.
     *
     * @return {@code true} if an honoree was given
     */
    public boolean isGift() {
        return !honoree.isEmpty();
    }

    /**
     * Wraps a plain donation in one decorator per chosen extra. The order is fixed (gift, then
     * urgent, then anonymous), so a description always reads
     * {@code "URGENT: Cash donation of EGP 500.00 (gift in honor of Mona Ali)"}.
     *
     * @param base the plain donation made by its creator
     * @return the decorated donation, or {@code base} itself if nothing was chosen
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
