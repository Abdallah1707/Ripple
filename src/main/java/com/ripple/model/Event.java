package com.ripple.model;

import java.time.LocalDate;

/** An NGO event. */
public class Event {

    private int id;
    private final String name;
    private final EventCategory category;
    private final LocalDate eventDate;
    private final String location;
    private final String description;

    /** Creates an event. */
    public Event(int id, String name, EventCategory category, LocalDate eventDate, String location,
                 String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.eventDate = eventDate;
        this.location = location == null ? "" : location;
        this.description = description == null ? "" : description;
    }

    public int getId() {
        return id;
    }

    /** Sets the id after saving. */
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public EventCategory getCategory() {
        return category;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name + " (" + eventDate + ")"; // shown in combo boxes
    }
}
