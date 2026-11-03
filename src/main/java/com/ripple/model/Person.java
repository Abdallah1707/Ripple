package com.ripple.model;

/**
 * Base class for people: id, name, email and phone.
 * Donor, Volunteer, Beneficiary and Attendee extend it.
 */
public abstract class Person implements Contactable {

    private int id;
    private final String name;
    private final String email;
    private final String phone;

    /** Sets the shared fields. Email and phone can be null or empty. */
    protected Person(int id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email == null ? "" : email;
        this.phone = phone == null ? "" : phone;
    }

    public int getId() {
        return id;
    }

    /** Sets the id after saving. */
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
