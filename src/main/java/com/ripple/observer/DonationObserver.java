package com.ripple.observer;

import com.ripple.model.Donation;

/**
 * Observer interface in the Observer pattern: anything that wants to know when a new
 * donation has been recorded implements this interface and is attached to the
 * {@link DonationSubject}.
 */
public interface DonationObserver {

    /**
     * Called by the subject right after a donation has been saved.
     *
     * @param donation the donation that was just recorded (it already has its id)
     */
    void onDonationRecorded(Donation donation);
}
