package com.ripple.model;

/** The ways Ripple can contact someone. */
public enum ContactChannel {
    EMAIL("email address"),
    SMS("phone number"),
    SOCIAL("social media handle");

    private final String addressLabel;

    ContactChannel(String addressLabel) {
        this.addressLabel = addressLabel;
    }

    /**
     * Returns what this channel's address is called, for messages shown to the user.
     *
     * @return e.g. {@code "phone number"} for SMS
     */
    public String getAddressLabel() {
        return addressLabel;
    }
}
