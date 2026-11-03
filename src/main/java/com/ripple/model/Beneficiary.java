package com.ripple.model;

/** A person or family that receives help from the NGO. Can receive messages. */
public class Beneficiary extends Person {

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
        super(id, name, email, phone);
        this.address = address == null ? "" : address;
        this.needs = needs == null ? "" : needs;
    }

    public String getAddress() {
        return address;
    }

    public String getNeeds() {
        return needs;
    }
}
