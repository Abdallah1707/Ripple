package com.ripple.receipt;

/**
 * A receipt that can be shown as text.
 */
public interface Receipt {

    /** The receipt text. */
    String render();
}
