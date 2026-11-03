package com.ripple.observer;

import com.ripple.logging.ApplicationLogger;
import com.ripple.model.Donation;

/**
 * Logs a line every time a donation is recorded.
 */
public class DonationLogObserver implements DonationObserver {

    private final ApplicationLogger logger;

    /** Creates the observer. */
    public DonationLogObserver() {
        this.logger = ApplicationLogger.getInstance();
    }

    @Override
    public void update(Donation donation) {
        logger.info("Donation " + donation.getReceiptNumber() + " recorded: "
                + donation.describe() + " from " + donation.getDonorDisplayName()); // hides the name if anonymous
    }
}
