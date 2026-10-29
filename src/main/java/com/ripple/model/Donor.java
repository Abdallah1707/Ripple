package com.ripple.model;

/**
 * A person or company that gives donations. The preferred channel decides how the donor
 * receives the automatic thank-you message (client requirement CR5).
 */
public class Donor implements Contactable {

    private int id;
    private final String name;
    private final String email;
    private final String phone;
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
        this.id = id;
        this.name = name;
        this.email = email == null ? "" : email;
        this.phone = phone == null ? "" : phone;
        this.preferredChannel = preferredChannel;
    }

    public int getId() {
        return id;
    }

    /**
     * Sets the id given by the database after the donor is saved.
     *
     * @param id the generated id
     */
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getPhone() {
        return phone;
    }

    public ContactChannel getPreferredChannel() {
        return preferredChannel;
    }

    @Override
    public String toString() {
        return name; // shown in combo boxes
    }
}
