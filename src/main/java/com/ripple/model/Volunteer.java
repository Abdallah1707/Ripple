package com.ripple.model;

/**
 * A volunteer. Can be assigned to events.
 */
public class Volunteer extends Person {

    private final String skills;
    private final String availability;

    /** Creates a volunteer. */
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
