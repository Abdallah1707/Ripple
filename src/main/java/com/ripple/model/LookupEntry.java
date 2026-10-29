package com.ripple.model;

/**
 * Common shape of a lookup (reference) table row: an id, a unique name and a description.
 *
 * <p>Both lookup tables, {@link DonationType} and {@link EventCategory}, extend this class,
 * so one service ({@code LookupService}) and one screen ({@code LookupTablePanel}) can
 * manage both.
 */
public abstract class LookupEntry {

    private int id;
    private String name;
    private String description;

    /**
     * Creates a lookup entry.
     *
     * @param id          database id, or 0 if not saved yet
     * @param name        the unique name
     * @param description optional description (may be empty)
     */
    protected LookupEntry(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description == null ? "" : description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    /**
     * Returns the name, so lookup entries display nicely in combo boxes.
     *
     * @return the name
     */
    @Override
    public String toString() {
        return name;
    }
}
