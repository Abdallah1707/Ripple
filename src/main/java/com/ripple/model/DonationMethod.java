package com.ripple.model;

/**
 * How a donation was given. Each method has its own creator (a subclass of
 * {@link com.ripple.factory.DonationFactory}), and {@code Main} keeps them in a map keyed by
 * this value, so choosing a method picks the creator without any {@code if/else}.
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

    /**
     * Returns the name shown to the user.
     *
     * @return e.g. {@code "In-kind"}
     */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label; // so combo boxes show "In-kind" instead of "IN_KIND"
    }
}
