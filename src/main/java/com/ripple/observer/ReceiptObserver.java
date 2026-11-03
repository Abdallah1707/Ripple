package com.ripple.observer;

import com.ripple.model.Donation;
import com.ripple.receipt.BasicReceipt;
import com.ripple.receipt.Receipt;
import com.ripple.receipt.TaxInformationDecorator;
import com.ripple.receipt.ThankYouMessageDecorator;
import com.ripple.repository.ReceiptRepository;

/**
 * Builds and saves a receipt for each new donation.
 * Tax info is only added for tax-deductible types (CR4).
 */
public class ReceiptObserver implements DonationObserver {

    private final ReceiptRepository receiptRepository;

    /** Creates the observer. */
    public ReceiptObserver(ReceiptRepository receiptRepository) {
        this.receiptRepository = receiptRepository;
    }

    @Override
    public void update(Donation donation) {
        Receipt receipt = buildReceipt(donation);
        receiptRepository.save(donation.getId(), receipt.render());
    }

    /** Builds the receipt: basic, thank-you, and tax info if needed. */
    public Receipt buildReceipt(Donation donation) {
        Receipt receipt = new ThankYouMessageDecorator(new BasicReceipt(donation), donation.getDonor().getName());
        if (donation.isTaxDeductible()) {
            receipt = new TaxInformationDecorator(receipt, donation);
        }
        return receipt;
    }
}
