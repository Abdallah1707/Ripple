package com.ripple.model;

/** A person registered to attend one {@link Event}. Can receive messages about that event. */
public class Attendee implements Contactable {

    private int id;
    private final int eventId;
    private final String name;
    private final String email;
    private final String phone;

    /**
     * Creates an attendee.
     *
     * @param id      database id, or 0 if not saved yet
     * @param eventId id of the event the person registered for
     * @param name    display name
     * @param email   email address, or an empty string
     * @param phone   phone number, or an empty string
     */
    public Attendee(int id, int eventId, String name, String email, String phone) {
        this.id = id;
        this.eventId = eventId;
        this.name = name;
        this.email = email == null ? "" : email;
        this.phone = phone == null ? "" : phone;
    }

    public int getId() {
        return id;
    }

    /**
     * Sets the id given by the database after the attendee is saved.
     *
     * @param id the generated id
     */
    public void setId(int id) {
        this.id = id;
    }

    public int getEventId() {
        return eventId;
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
        return name;
    }
}
