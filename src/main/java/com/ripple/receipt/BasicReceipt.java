package com.ripple.receipt;

import com.ripple.model.Donation;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Concrete component in the Decorator pattern: the receipt number, date, donor, type,
 * method and value, and nothing else. No thank-you or tax logic lives here; decorators
 * add those sections without changing this class.
 */
public class BasicReceipt implements Receipt {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Donation donation;

    /**
     * Creates the basic receipt for a donation.
     *
     * @param donation the donation the receipt is for
     */
    public BasicReceipt(Donation donation) {
        this.donation = donation;
    }

    @Override
    public String render() {
        return String.format(Locale.US,
                "Receipt %s%nDate: %s%nDonor: %s%nType: %s%nMethod: %s%nValue: EGP %,.2f%nDetails: %s",
                donation.getReceiptNumber(), donation.getCreatedAt().format(DATE_FORMAT),
                donation.getDonor().getName(), // the receipt is the donor's own, so it keeps their name
                donation.getDonationType().getName(), donation.getMethod().getLabel(), donation.getValue(),
                donation.describe()); // shows the donation decorators: "URGENT: ... (gift in honor of ...)"
    }
}
