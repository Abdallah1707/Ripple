package com.ripple.controller;

/** Which lookup table the user is working with in Lookup Management. */
public enum LookupKind {
    DONATION_TYPE("Donation Types", true),
    EVENT_CATEGORY("Event Categories", false);

    private final String title;
    private final boolean taxFlag;

    LookupKind(String title, boolean taxFlag) {
        this.title = title;
        this.taxFlag = taxFlag;
    }

    /**
     * Returns the tab title.
     *
     * @return e.g. {@code "Donation Types"}
     */
    public String getTitle() {
        return title;
    }

    /**
     * Tells whether entries of this table have a tax-deductible flag.
     *
     * @return {@code true} only for donation types
     */
    public boolean hasTaxFlag() {
        return taxFlag;
    }
}
