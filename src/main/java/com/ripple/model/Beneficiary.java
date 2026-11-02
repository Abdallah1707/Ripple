package com.ripple.model;

/** A person or family that receives help from the NGO. Can receive messages. */
public class Beneficiary implements Contactable {

    private int id;
    private final String name;
    private final String email;
    private final String phone;
    private final String address;
    private final String needs;

    /**
     * Creates a beneficiary.
     *
     * @param id      database id, or 0 if not saved yet
     * @param name    display name
     * @param email   email address, or an empty string
     * @param phone   phone number, or an empty string
     * @param address where the beneficiary lives (free text)
     * @param needs   what help they need (free text)
     */
    public Beneficiary(int id, String name, String email, String phone, String address, String needs) {
        this.id = id;
        this.name = name;
        this.email = email == null ? "" : email;
        this.phone = phone == null ? "" : phone;
        this.address = address == null ? "" : address;
        this.needs = needs == null ? "" : needs;
    }

    public int getId() {
        return id;
    }

    /**
     * Sets the id given by the database after the beneficiary is saved.
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

    public String getAddress() {
        return address;
    }

    public String getNeeds() {
        return needs;
    }

    @Override
    public String toString() {
        return name;
    }
}
