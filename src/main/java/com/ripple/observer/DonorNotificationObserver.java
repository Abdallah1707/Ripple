package com.ripple.observer;

import com.ripple.communication.CommunicationService;
import com.ripple.communication.CommunicationStrategy;
import com.ripple.model.ContactChannel;
import com.ripple.model.Donation;
import com.ripple.model.Donor;

import java.util.Map;

/**
 * Sends the donor a thank-you message on their preferred channel (CR5).
 * The checkbox on the Donations screen turns it on or off.
 */
public class DonorNotificationObserver implements DonationObserver {

    private final CommunicationService communicationService;
    private final Map<ContactChannel, CommunicationStrategy> strategies;

    /** Creates the observer. */
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
