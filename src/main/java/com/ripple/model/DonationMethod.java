package com.ripple.model;

/**
 * How a donation was given. Each method has its own factory.
 */
public enum DonationMethod {
    CASH("Cash"),
    ONLINE("Online"),
    IN_KIND("In-kind"),
    CHEQUE("Cheque");

    private final String label;

    DonationMethod(String label) {
        this.label = label;
    }

    /** Name shown to the user. */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label; // show "In-kind" not "IN_KIND"
    }
}
