package com.ripple.receipt;

/**
 * Base decorator for receipts. Wraps another receipt.
 */
public abstract class ReceiptDecorator implements Receipt {

    protected final Receipt wrapped;

    /** Wraps a receipt. */
    protected ReceiptDecorator(Receipt wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String render() {
        return wrapped.render();
    }
}
