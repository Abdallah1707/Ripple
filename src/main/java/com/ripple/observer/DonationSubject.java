package com.ripple.observer;

import com.ripple.model.Donation;

/**
 * Keeps a list of donation observers and notifies them.
 */
public interface DonationSubject {

    /** Adds an observer. Adding twice does nothing. */
    void registerObserver(DonationObserver observer);

    /** Removes an observer. */
    void removeObserver(DonationObserver observer);

    /** Notifies all observers. */
    void notifyObservers(Donation donation);
}
