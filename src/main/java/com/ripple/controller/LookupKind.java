package com.ripple.controller;

/** The lookup table being edited. */
public enum LookupKind {
    DONATION_TYPE("Donation Types", true),
    EVENT_CATEGORY("Event Categories", false);

    private final String title;
    private final boolean taxFlag;

    LookupKind(String title, boolean taxFlag) {
        this.title = title;
        this.taxFlag = taxFlag;
    }

    /** Tab title. */
    public String getTitle() {
        return title;
    }

    /** True only for donation types. */
    public boolean hasTaxFlag() {
        return taxFlag;
    }
}
