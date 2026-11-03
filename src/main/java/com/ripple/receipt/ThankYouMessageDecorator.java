package com.ripple.receipt;

/**
 * Adds a thank-you message to a receipt.
 */
public class ThankYouMessageDecorator extends ReceiptDecorator {

    private final String donorName;

    /** Wraps a receipt with a thank-you message. */
    public ThankYouMessageDecorator(Receipt wrapped, String donorName) {
        super(wrapped);
        this.donorName = donorName;
    }

    @Override
    public String render() {
        return super.render() + String.format("%nThank you, %s, for your generous support.", donorName);
    }
}
