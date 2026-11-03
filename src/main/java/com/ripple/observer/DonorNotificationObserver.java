package com.ripple.observer;

import com.ripple.communication.CommunicationService;
import com.ripple.communication.CommunicationStrategy;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.Donor;

import java.util.Map;

/**
 * Concrete observer in the Observer pattern that sends the donor a thank-you message on
 * their preferred channel (Email, SMS or Social) every time a donation is recorded (CR5).
 *
 * <p>It is also a client of the Strategy pattern: it looks up the strategy for the donor's
 * channel in its map and gives it to {@link CommunicationService}, so there is no
 * "if email ... else if SMS ..." here either. The message is kept short enough for an SMS.
 *
 * <p>The "Notify donors on new donation" checkbox on the Donations screen attaches and
 * detaches this observer.
 */
public class DonorNotificationObserver implements DonationObserver {

    private final CommunicationService communicationService;
    private final Map<ContactChannel, CommunicationStrategy> strategies;

    /**
     * Creates the observer.
     *
     * @param communicationService the Strategy context that sends (and logs) the message
     * @param strategies           one strategy per channel
     */
    public DonorNotificationObserver(CommunicationService communicationService,
                                     Map<ContactChannel, CommunicationStrategy> strategies) {
        this.communicationService = communicationService;
        this.strategies = strategies;
    }

    @Override
    public void update(Donation donation) {
        Donor donor = donation.getDonor();
        communicationService.setStrategy(strategies.get(donor.getPreferredChannel()));
        communicationService.send(donor, "Thank you for your donation",
                "Thank you, " + donor.getName() + "! We received your donation "
                        + donation.getReceiptNumber() + ". - Ripple");
    }
}
