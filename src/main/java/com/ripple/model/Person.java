package com.ripple.model;

/**
 * What every person Ripple knows has in common: an id, a name, an email and a phone.
 *
 * <p>{@link Donor}, {@link Volunteer}, {@link Beneficiary} and {@link Attendee} extend this class
 * instead of each repeating these four fields. It is abstract because nobody is "just a person":
 * they are always one of those four kinds. It implements {@link Contactable}, so every person can
 * receive messages.
 */
public abstract class Person implements Contactable {

    private int id;
    private final String name;
    private final String email;
    private final String phone;

    /**
     * Sets the fields shared by every kind of person.
     *
     * @param id    database id, or 0 if not saved yet
     * @param name  display name
     * @param email email address, or {@code null} / an empty string if there is none
     * @param phone phone number, or {@code null} / an empty string if there is none
     */
    protected Person(int id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email == null ? "" : email;
        this.phone = phone == null ? "" : phone;
    }

    public int getId() {
        return id;
    }

    /**
     * Sets the id given by the database after the person is saved.
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

    @Override
    public String toString() {
        return name; // shown in combo boxes and lists
    }
}
