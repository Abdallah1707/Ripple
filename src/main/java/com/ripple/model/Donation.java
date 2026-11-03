package com.ripple.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Base class for donations (cash, online, in-kind, cheque).
 * Created by the factories. Also the component that the decorators wrap.
 */
public abstract class Donation {

    private int id;
    private final Donor donor;
    private final DonationType donationType;
    private final LocalDateTime createdAt;

    /** Creates a donation. */
    protected Donation(Donor donor, DonationType donationType, LocalDateTime createdAt) {
        this.donor = donor;
        this.donationType = donationType;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    /** Sets the id after saving. */
    public void setId(int id) {
        this.id = id;
    }

    public Donor getDonor() {
        return donor;
    }

    public DonationType getDonationType() {
        return donationType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** How the donation was given. */
    public abstract DonationMethod getMethod();

    /** Value in EGP. */
    public abstract double getValue();

    /** Short description for tables and receipts. */
    public abstract String describe();

    /** Receipt number like RCP-2026-0007 (CR3). */
    public String getReceiptNumber() {
        return String.format("RCP-%d-%04d", createdAt.getYear(), id);
    }

    /** True if the donation type is tax-deductible (CR4). */
    public boolean isTaxDeductible() {
        return donationType.isTaxDeductible();
    }

    // ---- methods the decorators override ----

    /** The plain donation without decorators. Needed for saving. */
    public Donation getBase() {
        return this;
    }

    /** Name shown to others. Anonymous donations hide it. */
    public String getDonorDisplayName() {
        return getDonor().getName();
    }

    /** True if marked urgent. */
    public boolean isUrgent() {
        return false;
    }

    /** Who the gift is for, or empty. */
    public String getHonoree() {
        return "";
    }

    /** True if anonymous. */
    public boolean isAnonymous() {
        return false;
    }

    /** Options added by decorators, like Gift or Urgent. */
    public List<String> getOptions() {
        return List.of();
    }
}
