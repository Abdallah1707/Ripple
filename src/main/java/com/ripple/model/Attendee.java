package com.ripple.model;

/** A person registered for an event. */
public class Attendee extends Person {

    private final int eventId;

    /** Creates an attendee. */
    public Attendee(int id, int eventId, String name, String email, String phone) {
        super(id, name, email, phone);
        this.eventId = eventId;
    }

    public int getEventId() {
        return eventId;
    }
}
