package com.ripple.observer;

import com.ripple.model.Donation;

/**
 * Observer interface in the Observer pattern: anything that wants to know when a new
 * donation has been recorded implements this interface and is attached to the
 * {@link DonationSubject}.
 *
 * <p>Ripple uses the <b>push model</b>: the subject passes the new donation to
 * {@link #update(Donation)}, so an observer never has to ask the subject for it.
 */
public interface DonationObserver {

    /**
     * Called by the subject right after a donation has been saved.
     *
     * @param donation the donation that was just recorded (it already has its id)
     */
    void update(Donation donation);
}
