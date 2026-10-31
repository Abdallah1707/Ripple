package com.ripple.observer;

import com.ripple.model.Donation;

/**
 * Concrete observer in the Observer pattern that will build and save a receipt for each
 * new donation, using the Decorator pattern.
 *
 * <p><b>TEMPORARY STUB (Section 2).</b> It does nothing yet. Section 3 adds the
 * {@code ReceiptRepository}, {@code buildReceipt(...)} and the receipt decorators, and
 * attaches this observer in {@code Main}.
 */
public class ReceiptObserver implements DonationObserver {

    @Override
    public void onDonationRecorded(Donation donation) {
        // TODO Section 3: build the receipt (basic + thank-you + tax if deductible) and save it.
    }
}
