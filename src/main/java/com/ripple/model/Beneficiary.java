package com.ripple.model;

/** A person or family that gets help from the NGO. */
public class Beneficiary extends Person {

    private final String address;
    private final String needs;

    /** Creates a beneficiary. */
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
