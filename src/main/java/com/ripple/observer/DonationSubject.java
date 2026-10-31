package com.ripple.observer;

import com.ripple.model.Donation;

/**
 * Subject interface in the Observer pattern: an object that keeps a list of
 * {@link DonationObserver}s and tells all of them when a donation is recorded.
 * {@code DonationService} is the concrete subject.
 */
public interface DonationSubject {

    /**
     * Adds an observer. Attaching the same observer twice has no effect.
     *
     * @param observer the observer to add
     */
    void attach(DonationObserver observer);

    /**
     * Removes an observer, so it no longer hears about new donations.
     *
     * @param observer the observer to remove
     */
    void detach(DonationObserver observer);

    /**
     * Tells every attached observer about a donation.
     *
     * @param donation the donation that was recorded
     */
    void notifyObservers(Donation donation);
}
