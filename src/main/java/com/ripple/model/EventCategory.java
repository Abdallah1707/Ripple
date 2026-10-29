package com.ripple.model;

/** Lookup value that classifies an event, e.g. "Fundraiser" or "Workshop". */
public class EventCategory extends LookupEntry {

    /**
     * Creates an event category.
     *
     * @param id          database id, or 0 if not saved yet
     * @param name        unique name
     * @param description optional description
     */
    public EventCategory(int id, String name, String description) {
        super(id, name, description);
    }
}
