package com.ripple.model;

/**
 * Base class for lookup table rows: id, name and description.
 */
public abstract class LookupEntry {

    private int id;
    private String name;
    private String description;

    /** Creates a lookup entry. */
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

    /** Shows the name in combo boxes. */
    @Override
    public String toString() {
        return name;
    }
}
