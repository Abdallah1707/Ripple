package com.ripple.model;

/** A person registered to attend one {@link Event}. Can receive messages about that event. */
public class Attendee extends Person {

    private final int eventId;

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
        super(id, name, email, phone);
        this.eventId = eventId;
    }

    public int getEventId() {
        return eventId;
    }
}
