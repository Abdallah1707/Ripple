package com.ripple.receipt;

/**
 * Component in the Decorator pattern: anything that can render a donation receipt as text.
 */
public interface Receipt {

    /**
     * Renders the receipt.
     *
     * @return the receipt text
     */
    String render();
}
