package com.ripple.observer;

import com.ripple.model.Donation;

/**
 * Gets told when a new donation is recorded.
 */
public interface DonationObserver {

    /** Called after a donation is saved. */
    void update(Donation donation);
}
