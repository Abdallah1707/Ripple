package com.ripple.receipt;

/**
 * Decorator in the Decorator pattern: wraps another {@link Receipt} and, by default,
 * delegates {@link #render()} to it. A concrete decorator overrides {@code render()} to
 * append its own section on top of what the wrapped receipt already produced.
 */
public abstract class ReceiptDecorator implements Receipt {

    protected final Receipt wrapped;

    /**
     * Wraps another receipt.
     *
     * @param wrapped the receipt to decorate
     */
    protected ReceiptDecorator(Receipt wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String render() {
        return wrapped.render();
    }
}
