package com.ripple.observer;

import com.ripple.model.Donation;

/**
 * Subject interface in the Observer pattern: an object that keeps a list of
 * {@link DonationObserver}s and tells all of them when a donation is recorded.
 * {@code DonationService} is the concrete subject. The method names are the ones from the
 * lab slides: {@code registerObserver}, {@code removeObserver}, {@code notifyObservers}.
 */
public interface DonationSubject {

    /**
     * Adds an observer. Registering the same observer twice has no effect.
     *
     * @param observer the observer to add
     */
    void registerObserver(DonationObserver observer);

    /**
     * Removes an observer, so it no longer hears about new donations.
     *
     * @param observer the observer to remove
     */
    void removeObserver(DonationObserver observer);

    /**
     * Tells every attached observer about a donation.
     *
     * @param donation the donation that was recorded
     */
    void notifyObservers(Donation donation);
}
