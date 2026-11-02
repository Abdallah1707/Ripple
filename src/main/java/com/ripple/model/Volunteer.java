package com.ripple.model;

/**
 * A person who gives their time to the NGO. Volunteers can be assigned to events and
 * can receive messages, so they are {@link Contactable}.
 */
public class Volunteer implements Contactable {

    private int id;
    private final String name;
    private final String email;
    private final String phone;
    private final String skills;
    private final String availability;

    /**
     * Creates a volunteer.
     *
     * @param id           database id, or 0 if not saved yet
     * @param name         display name
     * @param email        email address, or an empty string
     * @param phone        phone number, or an empty string
     * @param skills       free text, e.g. "First aid, driving"
     * @param availability free text, e.g. "Weekends"
     */
    public Volunteer(int id, String name, String email, String phone, String skills, String availability) {
        this.id = id;
        this.name = name;
        this.email = email == null ? "" : email;
        this.phone = phone == null ? "" : phone;
        this.skills = skills == null ? "" : skills;
        this.availability = availability == null ? "" : availability;
    }

    public int getId() {
        return id;
    }

    /**
     * Sets the id given by the database after the volunteer is saved.
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

    public String getSkills() {
        return skills;
    }

    public String getAvailability() {
        return availability;
    }

    @Override
    public String toString() {
        return name; // shown in combo boxes
    }
}
