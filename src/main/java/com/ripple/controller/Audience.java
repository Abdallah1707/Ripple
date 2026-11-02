package com.ripple.controller;

/** Which group of people the user picked on the Communication screen. */
public enum Audience {
    DONORS("All donors"),
    VOLUNTEERS("All volunteers"),
    BENEFICIARIES("All beneficiaries"),
    EVENT_ATTENDEES("Attendees of one event");

    private final String label;

    Audience(String label) {
        this.label = label;
    }

    /**
     * Returns the text shown in the combo box.
     *
     * @return e.g. {@code "All donors"}
     */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label; // shown in combo boxes
    }
}
