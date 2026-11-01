package com.ripple.receipt;

/**
 * Concrete decorator that appends a personal thank-you message to whatever receipt it wraps.
 */
public class ThankYouMessageDecorator extends ReceiptDecorator {

    private final String donorName;

    /**
     * Wraps a receipt with a thank-you message.
     *
     * @param wrapped   the receipt to decorate
     * @param donorName the donor's name, shown in the message
     */
    public ThankYouMessageDecorator(Receipt wrapped, String donorName) {
        super(wrapped);
        this.donorName = donorName;
    }

    @Override
    public String render() {
        return super.render() + String.format("%nThank you, %s, for your generous support.", donorName);
    }
}
