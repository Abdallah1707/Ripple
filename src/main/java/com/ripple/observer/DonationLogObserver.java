package com.ripple.observer;

import com.ripple.logging.ApplicationLogger;
import com.ripple.model.Donation;

/**
 * Concrete observer in the Observer pattern: writes one line to the application log
 * every time a donation is recorded.
 *
 * <p>It uses the {@link ApplicationLogger} Singleton, so the line ends up in
 * {@code logs/ripple.log} and in the Dashboard's recent activity.
 */
public class DonationLogObserver implements DonationObserver {

    private final ApplicationLogger logger;

    /** Creates the observer. The logger is the shared Singleton instance. */
    public DonationLogObserver() {
        this.logger = ApplicationLogger.getInstance();
    }

    @Override
    public void onDonationRecorded(Donation donation) {
        logger.info("Donation " + donation.getReceiptNumber() + " recorded: "
                + donation.describe() + " from " + donation.getDonor().getName());
    }
}
