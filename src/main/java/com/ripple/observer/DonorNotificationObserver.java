package com.ripple.observer;

import com.ripple.model.Donation;

/**
 * Concrete observer in the Observer pattern that will send the donor a thank-you message
 * on their preferred channel (Email or SMS), using the Strategy pattern.
 *
 * <p>The "Notify donors on new donation" checkbox on the Donations screen attaches and
 * detaches this observer.
 *
 * <p><b>TEMPORARY STUB (Section 2).</b> It does nothing yet. Section 5 adds the
 * {@code CommunicationService} and the strategies and fills in this method.
 */
public class DonorNotificationObserver implements DonationObserver {

    @Override
    public void onDonationRecorded(Donation donation) {
        // TODO Section 5: pick the strategy for the donor's preferred channel and send a thank-you.
    }
}
