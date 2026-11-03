package com.ripple.receipt;

import com.ripple.model.Donation;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The basic receipt with the main donation details. Decorators add the extra sections.
 */
public class BasicReceipt implements Receipt {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Donation donation;

    /** Creates the basic receipt. */
    public BasicReceipt(Donation donation) {
        this.donation = donation;
    }

    @Override
    public String render() {
        return String.format(Locale.US,
                "Receipt %s%nDate: %s%nDonor: %s%nType: %s%nMethod: %s%nValue: EGP %,.2f%nDetails: %s",
                donation.getReceiptNumber(), donation.getCreatedAt().format(DATE_FORMAT),
                donation.getDonor().getName(), // real name, the receipt is for the donor
                donation.getDonationType().getName(), donation.getMethod().getLabel(), donation.getValue(),
                donation.describe()); // includes urgent / gift text
    }
}
