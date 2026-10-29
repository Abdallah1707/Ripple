package com.ripple.model;

/**
 * How a donation was given. {@link com.ripple.factory.DonationFactory} uses this value to
 * decide which {@link Donation} subclass to create.
 */
public enum DonationMethod {
    CASH("Cash"),
    ONLINE("Online"),
    IN_KIND("In-kind");

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
