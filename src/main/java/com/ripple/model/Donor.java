package com.ripple.model;

/**
 * A person or company that gives donations. The preferred channel decides how the donor
 * receives the automatic thank-you message (client requirement CR5).
 */
public class Donor extends Person {

    private final ContactChannel preferredChannel;

    /**
     * Creates a donor.
     *
     * @param id               database id, or 0 if not saved yet
     * @param name             display name
     * @param email            email address, or an empty string
     * @param phone            phone number, or an empty string
     * @param preferredChannel how the donor wants to be contacted
     */
    public Donor(int id, String name, String email, String phone, ContactChannel preferredChannel) {
        super(id, name, email, phone);
        this.preferredChannel = preferredChannel;
    }

    public ContactChannel getPreferredChannel() {
        return preferredChannel;
    }
}
