package com.ripple.receipt;

import com.ripple.model.Donation;

import java.util.Locale;

/**
 * Concrete decorator that appends a tax-deductible statement and the deductible amount
 * (CR4). {@code ReceiptObserver} only wraps a receipt with this decorator when the
 * donation's type is tax-deductible; {@code BasicReceipt} never carries a flag for it.
 */
public class TaxInformationDecorator extends ReceiptDecorator {

    private final Donation donation;

    /**
     * Wraps a receipt with a tax information section.
     *
     * @param wrapped  the receipt to decorate
     * @param donation the donation the deductible amount is taken from
     */
    public TaxInformationDecorator(Receipt wrapped, Donation donation) {
        super(wrapped);
        this.donation = donation;
    }

    @Override
    public String render() {
        return super.render() + String.format(Locale.US,
                "%nThis donation is tax-deductible. Deductible amount: EGP %,.2f", donation.getValue());
    }
}
