package com.ripple.observer;

import com.ripple.model.Donation;
import com.ripple.receipt.BasicReceipt;
import com.ripple.receipt.Receipt;
import com.ripple.receipt.TaxInformationDecorator;
import com.ripple.receipt.ThankYouMessageDecorator;
import com.ripple.repository.ReceiptRepository;

/**
 * Concrete observer in the Observer pattern that builds and saves a receipt for each new
 * donation, using the Decorator pattern.
 *
 * <p>Every receipt gets a thank-you message. A tax information section (CR4) is added only
 * when the donation's type is tax-deductible; {@code BasicReceipt} itself never changes.
 */
public class ReceiptObserver implements DonationObserver {

    private final ReceiptRepository receiptRepository;

    /**
     * Creates the observer.
     *
     * @param receiptRepository where the rendered receipt is saved
     */
    public ReceiptObserver(ReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

    @Override
    public void onDonationRecorded(Donation donation) {
        Receipt receipt = buildReceipt(donation);
        receiptRepository.save(donation.getId(), receipt.render());
    }

    /**
     * Builds the decorated receipt for a donation: a basic receipt, a thank-you message,
     * and a tax information section if (and only if) the donation's type is tax-deductible.
     *
     * @param donation the donation to build a receipt for
     * @return the fully decorated receipt
     */
    public Receipt buildReceipt(Donation donation) {
        Receipt receipt = new ThankYouMessageDecorator(new BasicReceipt(donation), donation.getDonor().getName());
        if (donation.isTaxDeductible()) {
            receipt = new TaxInformationDecorator(receipt, donation);
        }
        return receipt;
    }
}
