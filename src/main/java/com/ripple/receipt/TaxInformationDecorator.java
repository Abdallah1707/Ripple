package com.ripple.receipt;

import com.ripple.model.Donation;

import java.util.Locale;

/**
 * Adds the tax-deductible section to a receipt (CR4).
 */
public class TaxInformationDecorator extends ReceiptDecorator {

    private final Donation donation;

    /** Wraps a receipt with tax info. */
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
