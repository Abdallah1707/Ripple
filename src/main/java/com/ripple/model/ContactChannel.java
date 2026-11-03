package com.ripple.model;

/** Ways to contact someone. */
public enum ContactChannel {
    EMAIL("email address"),
    SMS("phone number"),
    SOCIAL("social media handle");

    private final String addressLabel;

    ContactChannel(String addressLabel) {
        this.addressLabel = addressLabel;
    }

    /** What the address is called, like "phone number". */
    public String getAddressLabel() {
        return addressLabel;
    }
}
