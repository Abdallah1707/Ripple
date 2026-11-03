package com.ripple.model;

/**
 * A person who gives their time to the NGO. Volunteers can be assigned to events and,
 * like every {@link Person}, can receive messages.
 */
public class Volunteer extends Person {

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
        super(id, name, email, phone);
        this.skills = skills == null ? "" : skills;
        this.availability = availability == null ? "" : availability;
    }

    public String getSkills() {
        return skills;
    }

    public String getAvailability() {
        return availability;
    }
}
