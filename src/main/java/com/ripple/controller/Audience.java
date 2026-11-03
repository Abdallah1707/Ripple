package com.ripple.controller;

/** The group of people picked on the Communication screen. */
public enum Audience {
    DONORS("All donors"),
    VOLUNTEERS("All volunteers"),
    BENEFICIARIES("All beneficiaries"),
    EVENT_ATTENDEES("Attendees of one event");

    private final String label;

    Audience(String label) {
        this.label = label;
    }

    /** Text shown in the combo box. */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label; // shown in combo boxes
    }
}
